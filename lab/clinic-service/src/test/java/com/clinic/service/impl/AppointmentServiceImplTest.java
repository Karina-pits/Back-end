package com.clinic.service.impl;

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
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AC3: усі залежності підмінені через @Mock/@InjectMocks, Spring-контекст
 * НЕ піднімається (немає @SpringBootTest чи будь-якої Spring-анотації).
 * AC5: тести не звертаються до реальної БД/мережі, без Thread.sleep,
 * не залежать один від одного (@BeforeEach створює чистий стан щоразу).
 */
@ExtendWith(MockitoExtension.class)
class AppointmentServiceImplTest {

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

    @InjectMocks
    private AppointmentServiceImpl appointmentService;

    private Patient patient;
    private Doctor doctor;
    private Appointment appointment;
    private AppointmentDto appointmentDto;

    private static final String PATIENT_KEYCLOAK_ID = "33333333-3333-3333-3333-333333333333";
    private static final String OTHER_PATIENT_KEYCLOAK_ID = "44444444-4444-4444-4444-444444444444";
    private static final String DOCTOR_KEYCLOAK_ID = "22222222-2222-2222-2222-222222222222";

    @BeforeEach
    void setUp() {
        patient = new Patient();
        patient.setId(1L);
        patient.setKeycloakUserId(PATIENT_KEYCLOAK_ID);

        doctor = new Doctor();
        doctor.setId(1L);
        doctor.setKeycloakUserId(DOCTOR_KEYCLOAK_ID);

        appointment = new Appointment();
        appointment.setId(1L);
        appointment.setPatient(patient);
        appointment.setDoctor(doctor);
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointment.setScheduledAt(LocalDateTime.of(2026, 10, 1, 9, 0));

        appointmentDto = new AppointmentDto();
        appointmentDto.setPatientId(1L);
        appointmentDto.setDoctorId(1L);
        appointmentDto.setScheduledAt(LocalDateTime.of(2026, 10, 1, 9, 0));
        appointmentDto.setReason("Консультація");
    }

    // create() — AC1, AC2, AC6, AC7

    @Test
    void create_patientBookingForSelf_savesAppointmentAndReturnsDto() {
        //"щасливий сценарій" — пацієнт записується на прийом сам для себе, все має пройти успішно.
        // Arrange
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(securityUtils.isPatient()).thenReturn(true);
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUserId()).thenReturn(PATIENT_KEYCLOAK_ID);
        when(appointmentMapper.toEntity(appointmentDto)).thenReturn(appointment);
        when(appointmentRepository.save(appointment)).thenReturn(appointment);
        when(appointmentMapper.toDto(appointment)).thenReturn(appointmentDto);

        // Act
        AppointmentDto result = appointmentService.create(appointmentDto);

        // Assert
        assertNotNull(result);
        assertEquals(appointmentDto, result);

        ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentRepository).save(captor.capture());
        assertEquals(AppointmentStatus.SCHEDULED, captor.getValue().getStatus());
        assertEquals(patient, captor.getValue().getPatient());
        assertEquals(doctor, captor.getValue().getDoctor());
    }

    @Test
    void create_patientBookingForAnotherPatient_throwsAccessDenied() {
       //поганий" сценарій — пацієнт намагається записати іншого пацієнта (підмінити patientId у формі), а не себе.
        // Arrange
        when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));
        when(securityUtils.isPatient()).thenReturn(true);
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.getCurrentUserId()).thenReturn(OTHER_PATIENT_KEYCLOAK_ID);

        // Act + Assert
        assertThrows(AccessDeniedException.class, () -> appointmentService.create(appointmentDto));
        verify(appointmentRepository, never()).save(any());
    }

    @Test
    void create_patientNotFound_throwsResourceNotFoundException() {
        //переданий patientId, якого взагалі не існує в базі.
        // Arrange
        when(patientRepository.findById(1L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(ResourceNotFoundException.class, () -> appointmentService.create(appointmentDto));
        verify(appointmentRepository, never()).save(any());
    }

    // ---------------- findById() — AC1, AC2 ----------------

    @Test
    void findById_existingAppointmentVisibleToUser_returnsDto() {
        //щасливий сценарій пошуку одного запису за ID
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(securityUtils.isAdmin()).thenReturn(true);
        when(appointmentMapper.toDto(appointment)).thenReturn(appointmentDto);

        AppointmentDto result = appointmentService.findById(1L);

        assertEquals(appointmentDto, result);
    }

    @Test
    void findById_nonExistingId_throwsResourceNotFoundException() {
        //пошук за ID, якого не існує.
        when(appointmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> appointmentService.findById(99L));
    }

    // updateStatus() — AC1, AC2, AC7

    @Test
    void updateStatus_asDoctor_updatesStatusAndSaves() {
        //лікар змінює статус свого власного прийому
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.isDoctor()).thenReturn(true);
        when(securityUtils.getCurrentUserId()).thenReturn(DOCTOR_KEYCLOAK_ID);
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(appointment);
        when(appointmentMapper.toDto(appointment)).thenReturn(appointmentDto);

        appointmentService.updateStatus(1L, AppointmentStatus.COMPLETED);

        ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentRepository).save(captor.capture());
        assertEquals(AppointmentStatus.COMPLETED, captor.getValue().getStatus());
    }

    @Test
    void updateStatus_appointmentNotOwnedByCurrentDoctor_throwsAccessDenied() {
        //лікар намагається змінити статус прийому, який належить іншому лікарю
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.isDoctor()).thenReturn(true);
        when(securityUtils.getCurrentUserId()).thenReturn("99999999-9999-9999-9999-999999999999");

        assertThrows(AccessDeniedException.class,
                () -> appointmentService.updateStatus(1L, AppointmentStatus.COMPLETED));
        verify(appointmentRepository, never()).save(any());
    }

    // delete() — AC1, AC2, AC7

    @Test
    void delete_existingAppointment_deletesItFromRepository() {
        //щасливий сценарій видалення існуючого запису
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));

        appointmentService.delete(1L);

        verify(appointmentRepository, times(1)).delete(appointment);
    }

    @Test
    void delete_nonExistingId_throwsResourceNotFoundExceptionAndDoesNotCallDelete() {
        //спроба видалити запис, якого не існує.
        when(appointmentRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> appointmentService.delete(404L));
        verify(appointmentRepository, never()).delete(any());
    }

    // cancel() — AC1, AC2

    @Test
    void cancel_ownAppointment_setsStatusToCancelled() {
        //щасливий сценарій — пацієнт скасовує власний запис
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.isDoctor()).thenReturn(false);
        when(securityUtils.getCurrentUserId()).thenReturn(PATIENT_KEYCLOAK_ID);
        when(appointmentRepository.save(any(Appointment.class))).thenReturn(appointment);

        appointmentService.cancel(1L);

        ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentRepository).save(captor.capture());
        assertEquals(AppointmentStatus.CANCELLED, captor.getValue().getStatus());
    }

    @Test
    void cancel_appointmentNotOwnedByCurrentUser_throwsAccessDenied() {
        //пацієнт намагається скасувати чужий запис.
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));
        when(securityUtils.isAdmin()).thenReturn(false);
        when(securityUtils.isDoctor()).thenReturn(false);
        when(securityUtils.getCurrentUserId()).thenReturn(OTHER_PATIENT_KEYCLOAK_ID);

        assertThrows(AccessDeniedException.class, () -> appointmentService.cancel(1L));
        verify(appointmentRepository, never()).save(any());
    }
}