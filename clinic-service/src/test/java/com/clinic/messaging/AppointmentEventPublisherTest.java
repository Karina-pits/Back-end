package com.clinic.messaging;

import com.clinic.common.dto.AppointmentEvent;
import com.clinic.common.dto.AppointmentEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Мінімальний Spring-контекст з "порожнім" менеджером транзакцій (без БД і брокера):
 * цього досить, щоб перевірити семантику @TransactionalEventListener(AFTER_COMMIT).
 */
@SpringJUnitConfig(AppointmentEventPublisherTest.TestConfig.class)
class AppointmentEventPublisherTest {

    @Configuration
    @EnableTransactionManagement
    @Import(AppointmentEventPublisher.class)
    static class TestConfig {
        @Bean
        PlatformTransactionManager transactionManager() {
            return new NoopTransactionManager();
        }

        @Bean
        RabbitTemplate rabbitTemplate() {
            return Mockito.mock(RabbitTemplate.class);
        }
    }

    static class NoopTransactionManager extends AbstractPlatformTransactionManager {
        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }

    @Autowired
    private ApplicationEventPublisher events;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    private TransactionTemplate transactionTemplate;
    private final AppointmentEvent event = new AppointmentEvent("e-1", AppointmentEventType.CREATED, 10L,
            "patient@clinic.local", "Марія Іваненко", "Олена Коваленко",
            LocalDateTime.of(2026, 11, 20, 10, 0), Instant.parse("2026-11-19T10:00:00Z"));

    @BeforeEach
    void setUp() {
        reset(rabbitTemplate);
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Test
    void publish_successfulTransaction_sendsEventOnlyAfterCommit() {
        transactionTemplate.executeWithoutResult(status -> {
            events.publishEvent(event);
            // усередині транзакції (до commit) у брокер ще нічого не пішло
            verifyNoInteractions(rabbitTemplate);
        });

        // після commit подія опублікована з правильним ключем маршрутизації
        verify(rabbitTemplate).convertAndSend("clinic.events", "appointment.created", event);
    }

    @Test
    void publish_transactionRollsBack_publishesNothing() {
        assertThrows(IllegalStateException.class, () ->
                transactionTemplate.executeWithoutResult(status -> {
                    events.publishEvent(event);
                    throw new IllegalStateException("бізнес-дія завершилась помилкою");
                }));

        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void publish_brokerUnavailableAfterCommit_doesNotFailTheCommittedAction() {
        doThrow(new AmqpConnectException(new RuntimeException("broker down")))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(AppointmentEvent.class));

        assertDoesNotThrow(() -> transactionTemplate.executeWithoutResult(status -> events.publishEvent(event)));
    }
}
