package com.clinic.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Результат обробки події: видимий запис, що сповіщення сформовано
 * (і відправлено, або пропущено через відсутній email).
 */
@Entity
@Table(name = "appointment_notification")
@Getter
@NoArgsConstructor
public class AppointmentNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private String eventId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "appointment_id", nullable = false)
    private Long appointmentId;

    @Column(name = "recipient_email")
    private String recipientEmail;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private String status;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    public AppointmentNotification(String eventId, String eventType, Long appointmentId,
                                   String recipientEmail, String subject, String message,
                                   String status, LocalDateTime processedAt) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.appointmentId = appointmentId;
        this.recipientEmail = recipientEmail;
        this.subject = subject;
        this.message = message;
        this.status = status;
        this.processedAt = processedAt;
    }
}
