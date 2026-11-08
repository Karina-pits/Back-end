package com.clinic.scheduling.service;

/**
 * Підсумок одного запуску нагадувань.
 *
 * @param sent    скільки нагадувань надіслано
 * @param skipped скільки пропущено (вже надсилалось раніше або в пацієнта немає email)
 * @param failed  скільки не вдалося надіслати (будуть повторені наступного запуску)
 */
public record ReminderResult(int sent, int skipped, int failed) {
}
