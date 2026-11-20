package com.clinic.common.dto;

/**
 * Три бізнес-дії, про які сповіщається решта системи.
 * Кожній відповідає ключ маршрутизації: appointment.created, appointment.cancelled, appointment.completed.
 */
public enum AppointmentEventType {
    CREATED,
    CANCELLED,
    COMPLETED
}
