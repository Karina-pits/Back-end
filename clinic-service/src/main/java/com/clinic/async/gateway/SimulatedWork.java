package com.clinic.async.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Імітує повільну зовнішню операцію (SMTP, генерація PDF, HTTP-виклик).
 * Тривалість налаштовується властивістю clinic.async.simulated-delay-ms
 * (за замовчуванням 1000 мс, тобто "близько секунди", як у завданні).
 */
@Component
public class SimulatedWork {

    private final long delayMs;

    public SimulatedWork(@Value("${clinic.async.simulated-delay-ms:1000}") long delayMs) {
        this.delayMs = delayMs;
    }

    public void pause() {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Повільну операцію перервано", e);
        }
    }
}
