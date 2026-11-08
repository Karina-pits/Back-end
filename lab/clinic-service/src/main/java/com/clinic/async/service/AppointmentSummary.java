package com.clinic.async.service;

/**
 * Підсумок по запису на прийом, зібраний з двох незалежних асинхронних викликів.
 *
 * @param pdfSizeBytes    розмір згенерованого PDF, або null, якщо генерація не вдалась
 * @param insuranceStatus статус страховки, або "UNKNOWN", якщо зовнішній сервіс не відповів
 * @param complete        true, якщо отримано обидві частини
 * @param note            пояснення, якщо результат неповний
 */
public record AppointmentSummary(
        Long appointmentId,
        Integer pdfSizeBytes,
        String insuranceStatus,
        boolean complete,
        String note) {
}
