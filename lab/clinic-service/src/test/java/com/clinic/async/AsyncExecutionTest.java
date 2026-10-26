package com.clinic.async;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.clinic.async.config.AsyncConfig;
import com.clinic.async.config.LoggingAsyncExceptionHandler;
import com.clinic.async.gateway.InsuranceGateway;
import com.clinic.async.gateway.MailGateway;
import com.clinic.async.gateway.SimulatedWork;
import com.clinic.async.service.AppointmentSummary;
import com.clinic.async.service.AppointmentSummaryService;
import com.clinic.async.service.InsuranceCheckService;
import com.clinic.async.service.NotificationService;
import com.clinic.async.service.PdfReportService;
import com.clinic.dto.AppointmentDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.LocalDateTime;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * Мінімальний Spring-контекст (без БД, Keycloak і решти застосунку):
 * справжня AsyncConfig + справжні асинхронні сервіси, а повільні шлюзи замоканi.
 */
@SpringJUnitConfig(AsyncExecutionTest.TestConfig.class)
@TestPropertySource(properties = {
        "clinic.async.simulated-delay-ms=500",
        "clinic.async.summary-timeout-ms=3000"
})
class AsyncExecutionTest {

    @Configuration
    @Import({AsyncConfig.class, NotificationService.class, PdfReportService.class,
            InsuranceCheckService.class, AppointmentSummaryService.class, SimulatedWork.class})
    static class TestConfig {
        @Bean
        MailGateway mailGateway() {
            return Mockito.mock(MailGateway.class);
        }

        @Bean
        InsuranceGateway insuranceGateway() {
            return Mockito.mock(InsuranceGateway.class);
        }
    }

    /** Потокобезпечний збирач логів (події пишуться з потоків пулу). */
    static class CapturingAppender extends AppenderBase<ILoggingEvent> {
        final Queue<ILoggingEvent> events = new ConcurrentLinkedQueue<>();

        @Override
        protected void append(ILoggingEvent event) {
            events.add(event);
        }
    }

    @Autowired
    @Qualifier(AsyncConfig.CLINIC_EXECUTOR)
    private ThreadPoolTaskExecutor executor;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private InsuranceCheckService insuranceCheckService;
    @Autowired
    private AppointmentSummaryService summaryService;
    @Autowired
    private MailGateway mailGateway;
    @Autowired
    private InsuranceGateway insuranceGateway;

    private Logger handlerLogger;
    private CapturingAppender appender;

    @BeforeEach
    void setUp() {
        reset(mailGateway, insuranceGateway);
        handlerLogger = (Logger) LoggerFactory.getLogger(LoggingAsyncExceptionHandler.class);
        handlerLogger.setLevel(Level.ERROR);
        appender = new CapturingAppender();
        appender.start();
        handlerLogger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        handlerLogger.detachAppender(appender);
    }

    private AppointmentDto appointment() {
        AppointmentDto dto = new AppointmentDto();
        dto.setId(1L);
        dto.setPatientId(1L);
        dto.setDoctorId(1L);
        dto.setScheduledAt(LocalDateTime.of(2026, 11, 18, 10, 0));
        dto.setReason("Консультація");
        return dto;
    }

    // ---------- Виконавець явно заданий і обмежений ----------

    @Test
    void executor_isExplicitlyConfiguredWithBoundedPoolQueueAndPrefix() {
        assertThat(executor.getCorePoolSize()).isEqualTo(2);
        assertThat(executor.getMaxPoolSize()).isEqualTo(4);
        assertThat(executor.getThreadPoolExecutor().getQueue().remainingCapacity()).isEqualTo(10);
        assertThat(executor.getThreadNamePrefix()).isEqualTo("clinic-async-");
    }

    // ---------- Повільний метод не блокує і працює в нашому пулі ----------

    @Test
    void sendConfirmation_slowMailGateway_returnsImmediatelyAndRunsInClinicPool() throws Exception {
        // Arrange
        CountDownLatch finished = new CountDownLatch(1);
        AtomicReference<String> workerThread = new AtomicReference<>();
        doAnswer(inv -> {
            workerThread.set(Thread.currentThread().getName());
            Thread.sleep(800);
            finished.countDown();
            return null;
        }).when(mailGateway).send(any(), any(), any());

        // Act
        long start = System.nanoTime();
        notificationService.sendAppointmentConfirmation(appointment(), "patient@clinic.local");
        long callerMs = (System.nanoTime() - start) / 1_000_000;

        // Assert: виклик повернувся до завершення повільного методу
        assertThat(callerMs).isLessThan(400);
        assertThat(finished.getCount()).isEqualTo(1);

        // ...а сам метод завершується пізніше, у потоці нашого виконавця
        assertThat(finished.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(workerThread.get())
                .startsWith("clinic-async-")
                .isNotEqualTo(Thread.currentThread().getName());
    }

    // ---------- Два незалежні виклики виконуються одночасно ----------

    @Test
    void buildSummary_twoIndependentSlowCalls_runConcurrentlyAndAreCombined() throws Exception {
        // Arrange: PDF триває 500 мс (SimulatedWork), страховка теж 500 мс
        when(insuranceGateway.check(any())).thenAnswer(inv -> {
            Thread.sleep(500);
            return "ACTIVE";
        });

        // Act
        long start = System.nanoTime();
        AppointmentSummary summary = summaryService.buildSummary(appointment());
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        // Assert: результати об'єднані
        assertThat(summary.complete()).isTrue();
        assertThat(summary.insuranceStatus()).isEqualTo("ACTIVE");
        assertThat(summary.pdfSizeBytes()).isPositive();
        // послідовно було б >= 1000 мс, одночасно ~500 мс
        assertThat(elapsedMs).isGreaterThanOrEqualTo(500).isLessThan(900);
    }

    // ---------- Збої не ламають викликаючий код ----------

    @Test
    void checkInsurance_gatewayThrows_futureCompletesExceptionallyAndCallerContinues() throws Exception {
        // Arrange
        when(insuranceGateway.check(any())).thenThrow(new IllegalStateException("insurance api down"));

        // Act
        CompletableFuture<String> future = insuranceCheckService.checkInsurance(1L);
        String recovered = future.exceptionally(ex -> "UNKNOWN").get(3, TimeUnit.SECONDS);

        // Assert
        assertThat(recovered).isEqualTo("UNKNOWN");
        assertThat(future).isCompletedExceptionally();
    }

    @Test
    void buildSummary_insuranceGatewayDown_returnsPartialSummaryWithoutError() {
        // Arrange
        when(insuranceGateway.check(any())).thenThrow(new IllegalStateException("insurance api down"));

        // Act
        AppointmentSummary summary = summaryService.buildSummary(appointment());

        // Assert
        assertThat(summary.complete()).isFalse();
        assertThat(summary.insuranceStatus()).isEqualTo("UNKNOWN");
        assertThat(summary.pdfSizeBytes()).isPositive();
    }

    @Test
    void sendConfirmation_voidMethodThrows_callerNotAffectedAndHandlerLogsError() {
        // Arrange
        doThrow(new IllegalStateException("smtp down")).when(mailGateway).send(any(), any(), any());

        // Act: виклик не кидає виняток у викликаючому потоці
        notificationService.sendAppointmentConfirmation(appointment(), "patient@clinic.local");

        // Assert: AsyncUncaughtExceptionHandler зафіксував помилку (а не сховав)
        await().atMost(3, TimeUnit.SECONDS).until(() ->
                appender.events.stream().anyMatch(e ->
                        e.getLevel() == Level.ERROR && e.getFormattedMessage().contains("smtp down")));
    }
}
