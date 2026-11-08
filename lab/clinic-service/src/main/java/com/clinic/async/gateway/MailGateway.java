package com.clinic.async.gateway;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Повільний побічний ефект №1: надсилання листа.
 * Зараз це імітація; у реальному проєкті тут був би виклик SMTP (JavaMailSender).
 * Адреса отримувача в лог не пишеться (персональні дані).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MailGateway {

    private final SimulatedWork work;

    public void send(String to, String subject, String body) {
        work.pause();
        log.info("[MAIL] sent, subject='{}'", subject);
    }
}
