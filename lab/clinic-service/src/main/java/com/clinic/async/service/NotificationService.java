package com.clinic.async.service;

import com.clinic.async.config.AsyncConfig;
import com.clinic.async.gateway.MailGateway;
import com.clinic.dto.AppointmentDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * "Запустив і забув" (fire-and-forget): викликаючий код не чекає завершення
 * і не отримує результату. Метод void, тому виняток усередині не повертається
 * викликаючому - його фіксує AsyncUncaughtExceptionHandler (LoggingAsyncExceptionHandler).
 *
 * Явний try/catch тут не потрібен свідомо: помилку побачить обробник і запише в лог.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final MailGateway mailGateway;

    @Async(AsyncConfig.CLINIC_EXECUTOR)
    public void sendAppointmentConfirmation(AppointmentDto appointment, String toEmail) {
        log.info("[NOTIFY] sending confirmation for appointment {}", appointment.getId());
        String subject = "Підтвердження запису на прийом #" + appointment.getId();
        String body = "Ваш прийом заплановано на " + appointment.getScheduledAt() + ".";
        mailGateway.send(toEmail, subject, body);
        log.info("[NOTIFY] confirmation for appointment {} sent", appointment.getId());
    }
}
