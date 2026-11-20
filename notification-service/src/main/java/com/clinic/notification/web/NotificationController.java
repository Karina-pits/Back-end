package com.clinic.notification.web;

import com.clinic.notification.entity.AppointmentNotification;
import com.clinic.notification.repository.AppointmentNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Лише для перегляду результату: GET http://localhost:8082/api/notifications
 * (це HTTP-запит від людини/Postman, а не виклик між сервісами).
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final AppointmentNotificationRepository repository;

    @GetMapping
    public List<NotificationView> list() {
        return repository.findAllByOrderByIdAsc().stream().map(NotificationView::from).toList();
    }

    public record NotificationView(Long id, String eventId, String eventType, Long appointmentId,
                                   String recipientEmail, String subject, String status,
                                   LocalDateTime processedAt) {

        static NotificationView from(AppointmentNotification n) {
            return new NotificationView(n.getId(), n.getEventId(), n.getEventType(), n.getAppointmentId(),
                    n.getRecipientEmail(), n.getSubject(), n.getStatus(), n.getProcessedAt());
        }
    }
}
