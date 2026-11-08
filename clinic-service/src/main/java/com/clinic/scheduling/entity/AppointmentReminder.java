package com.clinic.scheduling.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Запис "нагадування про цей прийом уже надіслано".
 * Унікальність appointment_id в БД робить логіку завдання ідемпотентною.
 */
@Entity
@Table(name = "appointment_reminder")
@Getter
@Setter
@NoArgsConstructor
public class AppointmentReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "appointment_id", nullable = false, unique = true)
    private Long appointmentId;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    public AppointmentReminder(Long appointmentId, LocalDateTime sentAt) {
        this.appointmentId = appointmentId;
        this.sentAt = sentAt;
    }
}
