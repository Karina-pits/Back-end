package com.clinic.scheduling.service;

import com.clinic.async.gateway.MailGateway;
import com.clinic.entity.Appointment;
import com.clinic.entity.AppointmentStatus;
import com.clinic.scheduling.entity.AppointmentReminder;
import com.clinic.scheduling.repository.AppointmentReminderRepository;
import com.clinic.scheduling.repository.AppointmentSchedulingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Уся бізнес-логіка нагадувань. Межа транзакції - цей метод (не Job).
 *
 * Ідемпотентність: перед надсиланням перевіряємо журнал appointment_reminder.
 * Якщо про цей прийом уже нагадували, пропускаємо. Тому повторний запуск
 * одразу після першого нічого не змінює (жодного другого листа).
 *
 * Збій одного листа не зупиняє решту: помилка логується, запис у журнал
 * НЕ створюється, тож наступний запуск спробує ще раз.
 * Виняток із транзакційного методу навмисно не кидаємо, щоб не відкотити
 * записи в журнал для листів, які вже пішли.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentReminderService {

    private final AppointmentSchedulingRepository appointmentRepository;
    private final AppointmentReminderRepository reminderRepository;
    private final MailGateway mailGateway;
    private final Clock clock;

    @Transactional
    public ReminderResult sendReminders(int hoursAhead, String subject) {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Appointment> upcoming = appointmentRepository.findByStatusAndScheduledAtBetween(
                AppointmentStatus.SCHEDULED, now, now.plusHours(hoursAhead));

        int sent = 0;
        int skipped = 0;
        int failed = 0;
        for (Appointment appointment : upcoming) {
            if (reminderRepository.existsByAppointmentId(appointment.getId())) {
                skipped++;
                continue;
            }
            String email = appointment.getPatient().getEmail();
            if (email == null || email.isBlank()) {
                skipped++;
                continue;
            }
            try {
                mailGateway.send(email, subject,
                        "Нагадуємо: ваш прийом заплановано на " + appointment.getScheduledAt() + ".");
                reminderRepository.save(new AppointmentReminder(appointment.getId(), now));
                sent++;
            } catch (RuntimeException e) {
                failed++;
                log.error("[REMINDER] failed for appointment {}: {}", appointment.getId(), e.toString());
            }
        }
        return new ReminderResult(sent, skipped, failed);
    }
}
