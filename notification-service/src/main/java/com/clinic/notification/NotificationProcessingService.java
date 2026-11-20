package com.clinic.notification;

import com.clinic.common.dto.AppointmentEvent;
import com.clinic.common.dto.EmailRequest;
import com.clinic.notification.entity.AppointmentNotification;
import com.clinic.notification.repository.AppointmentNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Дія споживача: сформувати сповіщення, "відправити" лист і зберегти видимий запис.
 *
 * Ідемпотентність: якщо подія з таким eventId уже оброблена, нічого не змінюємо.
 * (Брокер гарантує доставку "щонайменше один раз", тож дублікати можливі.)
 * Якщо відправка листа кине виняток, транзакція відкотиться, запис не збережеться,
 * і повідомлення буде повторене або піде в чергу DLQ.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationProcessingService {

    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_SKIPPED = "SKIPPED_NO_EMAIL";

    private final AppointmentNotificationRepository repository;
    private final MailSenderService mailSender;

    @Transactional
    public boolean process(AppointmentEvent event) {
        if (repository.existsByEventId(event.eventId())) {
            log.info("[CONSUMER] duplicate event {} ignored", event.eventId());
            return false;
        }

        String subject = subject(event);
        String message = message(event);
        String status;
        if (event.patientEmail() == null || event.patientEmail().isBlank()) {
            status = STATUS_SKIPPED;
        } else {
            mailSender.send(new EmailRequest(event.patientEmail(), subject, message));
            status = STATUS_SENT;
        }

        repository.save(new AppointmentNotification(event.eventId(), event.type().name(),
                event.appointmentId(), event.patientEmail(), subject, message, status, LocalDateTime.now()));
        log.info("[CONSUMER] event {} ({}) processed, status={}", event.eventId(), event.type(), status);
        return true;
    }

    private static String subject(AppointmentEvent event) {
        return switch (event.type()) {
            case CREATED -> "Підтвердження запису на прийом #" + event.appointmentId();
            case CANCELLED -> "Скасування запису на прийом #" + event.appointmentId();
            case COMPLETED -> "Дякуємо за візит (прийом #" + event.appointmentId() + ")";
        };
    }

    private static String message(AppointmentEvent event) {
        return switch (event.type()) {
            case CREATED -> "Шановний(а) " + event.patientName() + ", ваш прийом до лікаря "
                    + event.doctorName() + " заплановано на " + event.scheduledAt() + ".";
            case CANCELLED -> "Шановний(а) " + event.patientName() + ", запис на " + event.scheduledAt()
                    + " до лікаря " + event.doctorName() + " скасовано.";
            case COMPLETED -> "Шановний(а) " + event.patientName() + ", дякуємо, що відвідали клініку. "
                    + "Прийом у лікаря " + event.doctorName() + " завершено.";
        };
    }
}
