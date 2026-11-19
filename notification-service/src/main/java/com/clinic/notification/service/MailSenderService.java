package com.clinic.notification.service;

import com.clinic.common.dto.EmailRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Імітація відправки листа (у реальному проєкті тут був би SMTP / JavaMailSender).
 * Тривалість "відправки" налаштовується властивістю notification.simulated-delay-ms.
 * Адресу отримувача в лог не пишемо (персональні дані).
 */
@Service
public class MailSenderService {

    private static final Logger log = LoggerFactory.getLogger(MailSenderService.class);

    private final long delayMs;

    public MailSenderService(@Value("${notification.simulated-delay-ms:1000}") long delayMs) {
        this.delayMs = delayMs;
    }

    public void send(EmailRequest request) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Відправку листа перервано", e);
        }
        log.info("[MAIL] sent, subject='{}'", request.subject());
    }
}
