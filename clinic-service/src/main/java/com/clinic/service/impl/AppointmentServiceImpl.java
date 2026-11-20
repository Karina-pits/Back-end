package com.clinic.service.impl;

import com.clinic.aop.annotation.Audited;
import com.clinic.aop.annotation.MeasureTime;
import com.clinic.aop.annotation.RateLimited;
import com.clinic.common.dto.AppointmentEvent;
import com.clinic.common.dto.AppointmentEventType;
import com.clinic.dto.AppointmentDto;
import com.clinic.entity.Appointment;
import com.clinic.entity.AppointmentStatus;
import com.clinic.entity.Doctor;
import com.clinic.entity.Patient;
import com.clinic.exception.ResourceNotFoundException;
import com.clinic.mapper.AppointmentMapper;
import com.clinic.repository.AppointmentRepository;
import com.clinic.repository.DoctorRepository;
import com.clinic.repository.PatientRepository;
import com.clinic.security.SecurityUtils;
import com.clinic.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AppointmentServiceImpl implements AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final AppointmentMapper appointmentMapper;
    private final SecurityUtils securityUtils;
    // Лаба 8: подія публікується всередині транзакції, а в брокер іде лише після commit
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @MeasureTime
    @Transactional(readOnly = true)
    public List<AppointmentDto> findAllVisibleToCurrentUser() {
        String userId = securityUtils.getCurrentUserId();
        List<Appointment> appointments;
        if (securityUtils.isAdmin()) {
            appointments = appointmentRepository.findAll();
        } else if (securityUtils.isDoctor()) {
            appointments = appointmentRepository.findByDoctorKeycloakUserId(userId);
        } else {
            appointments = appointmentRepository.findByPatientKeycloakUserId(userId);
        }
        return appointments.stream().map(appointmentMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AppointmentDto findById(Long id) {
        Appointment appointment = getOrThrow(id);
        assertVisible(appointment);
        return appointmentMapper.toDto(appointment);
    }

    @Override
    @RateLimited(maxCalls = 5, windowSeconds = 60)
    @PreAuthorize("isAuthenticated()")
    public AppointmentDto create(AppointmentDto dto) {
        Patient patient = patientRepository.findById(dto.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Пацієнта не знайдено: id=" + dto.getPatientId()));
        Doctor doctor = doctorRepository.findById(dto.getDoctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Лікаря не знайдено: id=" + dto.getDoctorId()));

        if (securityUtils.isPatient() && !securityUtils.isAdmin()
                && !patient.getKeycloakUserId().equals(securityUtils.getCurrentUserId())) {
            throw new AccessDeniedException("Пацієнт може записуватись на прийом лише сам для себе");
        }

        Appointment appointment = appointmentMapper.toEntity(dto);
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        Appointment saved = appointmentRepository.save(appointment);
        publishEvent(AppointmentEventType.CREATED, saved);
        return appointmentMapper.toDto(saved);
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR')")
    public AppointmentDto update(Long id, AppointmentDto dto) {
        Appointment appointment = getOrThrow(id);
        assertVisible(appointment);

        if (dto.getPatientId() != null) {
            Patient patient = patientRepository.findById(dto.getPatientId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Пацієнта не знайдено: id=" + dto.getPatientId()));
            appointment.setPatient(patient);
        }
        if (dto.getDoctorId() != null) {
            Doctor doctor = doctorRepository.findById(dto.getDoctorId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Лікаря не знайдено: id=" + dto.getDoctorId()));
            appointment.setDoctor(doctor);
        }
        if (dto.getScheduledAt() != null) {
            appointment.setScheduledAt(dto.getScheduledAt());
        }
        if (dto.getReason() != null) {
            appointment.setReason(dto.getReason());
        }
        return appointmentMapper.toDto(appointmentRepository.save(appointment));
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN','DOCTOR')")
    public AppointmentDto updateStatus(Long id, AppointmentStatus status) {
        Appointment appointment = getOrThrow(id);
        assertVisible(appointment);
        appointment.setStatus(status);
        Appointment saved = appointmentRepository.save(appointment);
        AppointmentEventType eventType = eventTypeFor(status);
        if (eventType != null) {
            publishEvent(eventType, appointment);
        }
        return appointmentMapper.toDto(saved);
    }

    @Override
    @Audited(action = "CANCEL_APPOINTMENT")
    @PreAuthorize("isAuthenticated()")
    public void cancel(Long id) {
        Appointment appointment = getOrThrow(id);
        assertVisible(appointment);
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appointment);
        publishEvent(AppointmentEventType.CANCELLED, appointment);
    }

    @Override
    @Audited(action = "DELETE_APPOINTMENT")
    @PreAuthorize("hasRole('ADMIN')")
    public void delete(Long id) {
        Appointment appointment = getOrThrow(id);
        appointmentRepository.delete(appointment);
    }

    private Appointment getOrThrow(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Запис на прийом не знайдено: id=" + id));
    }

    private void assertVisible(Appointment appointment) {
        if (securityUtils.isAdmin()) {
            return;
        }
        String userId = securityUtils.getCurrentUserId();
        boolean isOwnerDoctor = securityUtils.isDoctor()
                && appointment.getDoctor().getKeycloakUserId() != null
                && appointment.getDoctor().getKeycloakUserId().equals(userId);
        boolean isOwnerPatient = appointment.getPatient().getKeycloakUserId() != null
                && appointment.getPatient().getKeycloakUserId().equals(userId);
        if (!isOwnerDoctor && !isOwnerPatient) {
            throw new AccessDeniedException("Доступ до цього запису заборонено");
        }
    }

    /** Лише ці дві зміни статусу є бізнес-подіями; решта (наприклад SCHEDULED) подій не породжують. */
    private static AppointmentEventType eventTypeFor(AppointmentStatus status) {
        return switch (status) {
            case COMPLETED -> AppointmentEventType.COMPLETED;
            case CANCELLED -> AppointmentEventType.CANCELLED;
            default -> null;
        };
    }

    private void publishEvent(AppointmentEventType type, Appointment appointment) {
        Patient patient = appointment.getPatient();
        Doctor doctor = appointment.getDoctor();
        eventPublisher.publishEvent(new AppointmentEvent(
                UUID.randomUUID().toString(),
                type,
                appointment.getId(),
                patient.getEmail(),
                fullName(patient.getFirstName(), patient.getLastName()),
                fullName(doctor.getFirstName(), doctor.getLastName()),
                appointment.getScheduledAt(),
                Instant.now()));
    }

    private static String fullName(String firstName, String lastName) {
        return ((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName)).trim();
    }
}
