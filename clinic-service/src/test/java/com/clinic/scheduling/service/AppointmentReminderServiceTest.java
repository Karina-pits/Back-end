package com.clinic.scheduling.service;

import com.clinic.async.gateway.MailGateway;
import com.clinic.entity.Appointment;
import com.clinic.entity.AppointmentStatus;
import com.clinic.entity.Patient;
import com.clinic.scheduling.entity.AppointmentReminder;
import com.clinic.scheduling.repository.AppointmentReminderRepository;
import com.clinic.scheduling.repository.AppointmentSchedulingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Без Spring і без БД: репозиторії замоканi, час фіксований.
 */
@ExtendWith(MockitoExtension.class)
class AppointmentReminderServiceTest {

    @Mock
    private AppointmentSchedulingRepository appointmentRepository;
    @Mock
    private AppointmentReminderRepository reminderRepository;
    @Mock
    private MailGateway mailGateway;

    private AppointmentReminderService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-18T09:00:00Z"), ZoneOffset.UTC);
        service = new AppointmentReminderService(appointmentRepository, reminderRepository, mailGateway, clock);
    }

    private Appointment appointment(Long id, String email) {
        Patient patient = new Patient();
        patient.setEmail(email);
        Appointment appointment = new Appointment();
        appointment.setId(id);
        appointment.setPatient(patient);
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointment.setScheduledAt(LocalDateTime.of(2026, 11, 18, 15, 0));
        return appointment;
    }

    @Test
    void sendReminders_runTwiceForSameData_secondRunChangesNothing() {
        // Arrange: "журнал" у пам'яті повторює поведінку унікального appointment_id в БД
        Set<Long> reminded = new HashSet<>();
        when(appointmentRepository.findByStatusAndScheduledAtBetween(
                eq(AppointmentStatus.SCHEDULED), any(), any()))
                .thenReturn(List.of(appointment(1L, "a@clinic.local")));
        when(reminderRepository.existsByAppointmentId(any()))
                .thenAnswer(inv -> reminded.contains(inv.<Long>getArgument(0)));
        when(reminderRepository.save(any(AppointmentReminder.class))).thenAnswer(inv -> {
            AppointmentReminder reminder = inv.getArgument(0);
            reminded.add(reminder.getAppointmentId());
            return reminder;
        });

        // Act
        ReminderResult first = service.sendReminders(24, "Тема");
        ReminderResult second = service.sendReminders(24, "Тема");

        // Assert
        assertThat(first).isEqualTo(new ReminderResult(1, 0, 0));
        assertThat(second).isEqualTo(new ReminderResult(0, 1, 0));
        verify(mailGateway, times(1)).send(eq("a@clinic.local"), eq("Тема"), any());
        verify(reminderRepository, times(1)).save(any());
    }

    @Test
    void sendReminders_mailFails_isNotRecordedSoNextRunRetries() {
        // Arrange
        when(appointmentRepository.findByStatusAndScheduledAtBetween(any(), any(), any()))
                .thenReturn(List.of(appointment(1L, "a@clinic.local")));
        doThrow(new IllegalStateException("smtp down")).when(mailGateway).send(any(), any(), any());

        // Act
        ReminderResult result = service.sendReminders(24, "Тема");

        // Assert: збій зафіксовано, запису в журнал немає, виняток не кинуто
        assertThat(result).isEqualTo(new ReminderResult(0, 0, 1));
        verify(reminderRepository, never()).save(any());
    }

    @Test
    void sendReminders_patientWithoutEmail_isSkipped() {
        when(appointmentRepository.findByStatusAndScheduledAtBetween(any(), any(), any()))
                .thenReturn(List.of(appointment(1L, null)));

        ReminderResult result = service.sendReminders(24, "Тема");

        assertThat(result).isEqualTo(new ReminderResult(0, 1, 0));
        verifyNoInteractions(mailGateway);
    }

    @Test
    void sendReminders_looksOnlyAtWindowFromNowToHoursAhead() {
        when(appointmentRepository.findByStatusAndScheduledAtBetween(any(), any(), any()))
                .thenReturn(List.of());

        service.sendReminders(3, "Тема");

        verify(appointmentRepository).findByStatusAndScheduledAtBetween(
                AppointmentStatus.SCHEDULED,
                LocalDateTime.of(2026, 11, 18, 9, 0),
                LocalDateTime.of(2026, 11, 18, 12, 0));
    }
}
