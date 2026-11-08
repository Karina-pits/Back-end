package com.clinic.scheduling.service;

import com.clinic.entity.Appointment;
import com.clinic.entity.AppointmentStatus;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentCleanupServiceTest {

    @Mock
    private AppointmentSchedulingRepository appointmentRepository;

    private AppointmentCleanupService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-11-18T09:00:00Z"), ZoneOffset.UTC);
        service = new AppointmentCleanupService(appointmentRepository, clock);
    }

    private Appointment scheduledAppointment(Long id) {
        Appointment appointment = new Appointment();
        appointment.setId(id);
        appointment.setStatus(AppointmentStatus.SCHEDULED);
        appointment.setScheduledAt(LocalDateTime.of(2026, 11, 10, 10, 0));
        return appointment;
    }

    @Test
    void cancelOverdue_runTwiceForSameData_secondRunChangesNothing() {
        // Arrange: репозиторій повертає лише записи, які ще у статусі SCHEDULED
        List<Appointment> all = List.of(scheduledAppointment(1L), scheduledAppointment(2L));
        when(appointmentRepository.findByStatusAndScheduledAtBefore(eq(AppointmentStatus.SCHEDULED), any()))
                .thenAnswer(inv -> all.stream()
                        .filter(a -> a.getStatus() == AppointmentStatus.SCHEDULED)
                        .toList());

        // Act
        int first = service.cancelOverdueAppointments(24);
        int second = service.cancelOverdueAppointments(24);

        // Assert
        assertThat(first).isEqualTo(2);
        assertThat(second).isZero();
        assertThat(all).allSatisfy(a -> assertThat(a.getStatus()).isEqualTo(AppointmentStatus.CANCELLED));
        verify(appointmentRepository, times(1)).saveAll(any());
    }

    @Test
    void cancelOverdue_usesThresholdNowMinusGraceHours() {
        when(appointmentRepository.findByStatusAndScheduledAtBefore(any(), any())).thenReturn(List.of());

        int cancelled = service.cancelOverdueAppointments(24);

        assertThat(cancelled).isZero();
        verify(appointmentRepository).findByStatusAndScheduledAtBefore(
                AppointmentStatus.SCHEDULED, LocalDateTime.of(2026, 11, 17, 9, 0));
    }
}
