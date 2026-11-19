package com.clinic.common.dto;

/**
 * Запит на відправку листа. Спільний контракт між clinic-service (відправник запиту)
 * і notification-service (виконавець). Лише дані, без логіки.
 */
public record EmailRequest(String to, String subject, String body) {
}
