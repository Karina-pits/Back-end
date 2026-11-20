package com.clinic.service.impl;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Які бізнес-дії породжують події, а які ні. Без Spring, брокера і БД.
 * (Що публікація в брокер відбувається саме ПІСЛЯ commit, перевіряє AppointmentEventPublisherTest.)
 */
@ExtendWith(MockitoExtension.class)
class AppointmentServiceEventsTest {

    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private PatientRepository patientRepository;
    @Mock
    private DoctorRepository doctorRepository;
    @Mock
    private AppointmentMapper appointmentMapper;
    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AppointmentServiceImpl service;

    private Patient patient;
    private Doctor doctor;
    private Appointment appointment;
    private AppointmentDto dto;

    @BeforeEach
    void setUp() {
        patient = new Patient();
        patient.setId(1L);
        patient.setFirstName("Марія");
        patient.setLastName("Іваненко");
        patient.setEmail("patient@clinic.local");
        patient.setKeycloakUserId("patient-kc-id");

        doctor = new Doctor();
        doctor.setId(1L);
        doctor.setFirstName("Олена");
        doctor.setLastName("Коваленко");
        doctor.setKeycloakUserId("doctor-kc-id");

        appointment = new Appointment();
        appointment.setId(10L);
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointment.setScheduledAt(LocalDateTime.of(2026, 11, 20, 10, 0));

        dto = new AppointmentDto();
        dto.setPatientId(1L);
        dto.setDoctorId(1L);
        dto.setScheduledAt(LocalDateTime.of(2026, 11, 20, 10, 0));
    }

    private AppointmentEvent capturePublishedEvent() {
        ArgumentCaptor<AppointmentEvent> captor = ArgumentCaptor.forClass(AppointmentEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        return captor.getValue();
    }

    // ---------------- create ----------------

    @Test
    void create_success_publishesCreatedEventWithPatientAndDoctorData() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(appointmentMapper.toEntity(dto)).thenReturn(appointment);
        when(appointmentRepository.save(appointment)).thenReturn(appointment);
        when(appointmentMapper.toDto(appointment)).thenReturn(dto);

        service.create(dto);

        AppointmentEvent event = capturePublishedEvent();
        assertThat(event.type()).isEqualTo(AppointmentEventType.CREATED);
        assertThat(event.appointmentId()).isEqualTo(10L);
        assertThat(event.patientEmail()).isEqualTo("patient@clinic.local");
        assertThat(event.patientName()).isEqualTo("Марія Іваненко");
        assertThat(event.doctorName()).isEqualTo("Олена Коваленко");
        assertThat(event.eventId()).isNotBlank();
    }

    @Test
    void create_patientNotFound_publishesNoEvent() {
        when(patientRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.create(dto));

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void create_patientBookingForAnotherPatient_publishesNoEvent() {
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(securityUtils.isPatient()).thenReturn(true);
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUserId()).thenReturn("another-patient-kc-id");

        assertThrows(AccessDeniedException.class, () -> service.create(dto));

        verifyNoInteractions(eventPublisher);
    }

    // ---------------- cancel ----------------

    @Test
    void cancel_success_publishesCancelledEvent() {
        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appointment));
        when(securityUtils.isAdmin()).thenReturn(true);

        service.cancel(10L);

        assertThat(capturePublishedEvent().type()).isEqualTo(AppointmentEventType.CANCELLED);
    }

    @Test
    void cancel_accessDenied_publishesNoEvent() {
        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appointment));
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.isDoctor()).thenReturn(false);
        when(securityUtils.getCurrentUserId()).thenReturn("someone-else");

        assertThrows(AccessDeniedException.class, () -> service.cancel(10L));

        verifyNoInteractions(eventPublisher);
    }

    // ---------------- updateStatus ----------------

    @Test
    void updateStatus_completed_publishesCompletedEvent() {
        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appointment));
        when(securityUtils.isAdmin()).thenReturn(true);
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(appointment);
        when(appointmentMapper.toDto(appointment)).thenReturn(dto);

        service.updateStatus(10L, AppointmentStatus.COMPLETED);

        assertThat(capturePublishedEvent().type()).isEqualTo(AppointmentEventType.COMPLETED);
    }

    @Test
    void updateStatus_scheduled_publishesNoEvent() {
        when(appointmentRepository.findById(10L)).thenReturn(Optional.of(appointment));
        when(securityUtils.isAdmin()).thenReturn(true);
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(appointment);
        when(appointmentMapper.toDto(appointment)).thenReturn(dto);

        service.updateStatus(10L, AppointmentStatus.SCHEDULED);

        verifyNoInteractions(eventPublisher);
    }
}
