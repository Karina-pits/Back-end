package com.clinic.notification.service;

import com.clinic.common.dto.EmailRequest;
import com.clinic.notification.MailSenderService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MailSenderServiceTest {

    private final EmailRequest request = new EmailRequest("a@clinic.local", "Тема", "Текст");

    @Test
    void send_waitsForSimulatedDelay() {
        // Arrange
        MailSenderService service = new MailSenderService(100);

        // Act
        long start = System.nanoTime();
        service.send(request);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        // Assert
        assertThat(elapsedMs).isGreaterThanOrEqualTo(90);
    }

    @Test
    void send_interruptedThread_throwsIllegalStateAndKeepsInterruptFlag() {
        // Arrange
        MailSenderService service = new MailSenderService(1_000);
        Thread.currentThread().interrupt();

        // Act + Assert
        assertThatThrownBy(() -> service.send(request)).isInstanceOf(IllegalStateException.class);
        assertThat(Thread.interrupted()).isTrue(); // прапорець перервання збережено (і тут же скинуто для інших тестів)
    }
}
