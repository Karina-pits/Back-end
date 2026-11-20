package com.clinic.common.dto;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Подія про запис на прийом. Єдиний спільний контракт між clinic-service (публікує)
 * і notification-service (споживає): лише дані, без логіки.
 *
 * @param eventId унікальний ідентифікатор події; за ним споживач відсікає дублікати (ідемпотентність)
 */
public record AppointmentEvent(
        String eventId,
        AppointmentEventType type,
        Long appointmentId,
        String patientEmail,
        String patientName,
        String doctorName,
        LocalDateTime scheduledAt,
        Instant occurredAt) {
}
