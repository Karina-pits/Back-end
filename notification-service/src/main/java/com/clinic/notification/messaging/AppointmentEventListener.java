package com.clinic.notification.messaging;

import com.clinic.common.dto.AppointmentEvent;
import com.clinic.notification.NotificationProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Слухач: отримує подію з черги й передає сервісу. Підтвердження (ack) брокеру
 * надсилається лише після успішного завершення методу, тож якщо споживач
 * впаде посеред обробки, повідомлення залишиться в черзі.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentEventListener {

    private final NotificationProcessingService processingService;

    @RabbitListener(queues = NotificationMessagingConfig.QUEUE)
    public void onAppointmentEvent(AppointmentEvent event) {
        log.info("[CONSUMER] received {} for appointment {}", event.type(), event.appointmentId());
        processingService.process(event);
    }
}
