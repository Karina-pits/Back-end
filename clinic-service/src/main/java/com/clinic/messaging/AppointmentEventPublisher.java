package com.clinic.messaging;

import com.clinic.common.dto.AppointmentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Публікує подію в брокер ТІЛЬКИ ПІСЛЯ успішної фіксації (commit) бізнес-транзакції.
 *
 * Сервіс просто викликає ApplicationEventPublisher.publishEvent(...) всередині транзакції,
 * а цей метод спрацьовує AFTER_COMMIT. Якщо транзакція відкотилась, метод не викликається
 * взагалі, тобто подія не публікується.
 *
 * Збій брокера вже ПІСЛЯ commit не повинен ламати відповідь клієнту (дані збережено),
 * тому він лише логується. Для повної гарантії доставки в такому випадку потрібен
 * патерн Transactional Outbox (поза межами цієї лаби).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(AppointmentEvent event) {
        String routingKey = "appointment." + event.type().name().toLowerCase();
        try {
            rabbitTemplate.convertAndSend(ClinicMessagingConfig.EXCHANGE, routingKey, event);
            log.info("[EVENT] published {} for appointment {} (eventId={})",
                    routingKey, event.appointmentId(), event.eventId());
        } catch (AmqpException e) {
            log.error("[EVENT] failed to publish {} for appointment {}: {}",
                    routingKey, event.appointmentId(), e.toString());
        }
    }
}
