package com.clinic.async;

import com.clinic.async.config.AsyncConfig;
import com.clinic.async.gateway.MailGateway;
import com.clinic.async.service.NotificationService;
import com.clinic.dto.AppointmentDto;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

/**
 * Демонстрація відхилення завдання: виконавець із дуже малим пулом (1 потік)
 * і чергою на 1 завдання. Перше завдання займає потік, друге стає в чергу,
 * третє вже не вміщується -> TaskRejectedException.
 *
 * У робочому застосунку цей виняток перехоплює AsyncAppointmentController
 * і повертає 503 + Retry-After (див. AsyncAppointmentControllerTest).
 */
@SpringJUnitConfig(ExecutorOverflowTest.TinyExecutorConfig.class)
class ExecutorOverflowTest {

    @Configuration
    @EnableAsync
    @Import(NotificationService.class)
    static class TinyExecutorConfig {
        // та сама назва біна, яку вказують @Async-методи, але крихітні розміри
        @Bean(name = AsyncConfig.CLINIC_EXECUTOR)
        ThreadPoolTaskExecutor tinyExecutor() {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(1);
            executor.setMaxPoolSize(1);
            executor.setQueueCapacity(1);
            executor.setThreadNamePrefix("tiny-async-");
            return executor;
        }

        @Bean
        MailGateway mailGateway() {
            return Mockito.mock(MailGateway.class);
        }
    }

    @Autowired
    private NotificationService notificationService;
    @Autowired
    private MailGateway mailGateway;

    private AppointmentDto appointment() {
        AppointmentDto dto = new AppointmentDto();
        dto.setId(1L);
        dto.setPatientId(1L);
        dto.setScheduledAt(LocalDateTime.of(2026, 11, 18, 10, 0));
        return dto;
    }

    @Test
    void sendConfirmation_executorPoolAndQueueFull_throwsTaskRejectedException() throws Exception {
        // Arrange: перше завдання "зависає", поки ми не відпустимо його
        CountDownLatch firstStarted = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        doAnswer(inv -> {
            firstStarted.countDown();
            release.await(5, TimeUnit.SECONDS);
            return null;
        }).when(mailGateway).send(any(), any(), any());

        try {
            // Act 1: займає єдиний потік
            notificationService.sendAppointmentConfirmation(appointment(), "a@clinic.local");
            assertThat(firstStarted.await(2, TimeUnit.SECONDS)).isTrue();

            // Act 2: стає в чергу (місткість 1)
            notificationService.sendAppointmentConfirmation(appointment(), "b@clinic.local");

            // Assert: третє не вміщується ні в потік, ні в чергу
            assertThatThrownBy(() ->
                    notificationService.sendAppointmentConfirmation(appointment(), "c@clinic.local"))
                    .isInstanceOf(TaskRejectedException.class);
        } finally {
            release.countDown();
        }
    }
}
