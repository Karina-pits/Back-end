package com.clinic.scheduling.service;

import com.clinic.entity.Appointment;
import com.clinic.entity.AppointmentStatus;
import com.clinic.scheduling.repository.AppointmentSchedulingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Скасовує записи, які так і залишились у статусі SCHEDULED,
 * хоча дата прийому минула більш ніж на graceHours годин.
 *
 * Ідемпотентність: після першого запуску такі записи мають статус CANCELLED
 * і більше не потрапляють у вибірку, тож другий запуск нічого не змінює.
 * Межа транзакції - цей метод.
 */
@Service
@RequiredArgsConstructor
public class AppointmentCleanupService {

    private final AppointmentSchedulingRepository appointmentRepository;
    private final Clock clock;

    @Transactional
    public int cancelOverdueAppointments(int graceHours) {
        LocalDateTime threshold = LocalDateTime.now(clock).minusHours(graceHours);
        List<Appointment> overdue = appointmentRepository
                .findByStatusAndScheduledAtBefore(AppointmentStatus.SCHEDULED, threshold);
        if (overdue.isEmpty()) {
            return 0;
        }
        overdue.forEach(appointment -> appointment.setStatus(AppointmentStatus.CANCELLED));
        appointmentRepository.saveAll(overdue);
        return overdue.size();
    }
}
