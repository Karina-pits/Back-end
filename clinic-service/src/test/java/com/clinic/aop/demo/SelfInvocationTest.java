package com.clinic.aop.demo;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.clinic.aop.aspect.MeasureTimeAspect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Демонстрація self-invocation. Мінімальний Spring-контекст: тільки аспект
 * і демо-сервіс (без БД, Keycloak і решти застосунку).
 *
 * Той самий метод timedOperation() викликаємо двома шляхами:
 *  - ззовні (через проксі)   -> аспект спрацьовує, є запис у лозі;
 *  - через this. всередині   -> аспект НЕ спрацьовує, запису немає, помилки теж.
 * Відсутність запису в другому тесті - свідомий задум вправи, не баг.
 */
@SpringJUnitConfig(SelfInvocationTest.TestConfig.class)
class SelfInvocationTest {

    @Configuration
    @EnableAspectJAutoProxy
    @Import({MeasureTimeAspect.class, SelfInvocationDemoService.class})
    static class TestConfig {
        @Bean
        Clock clock() {
            return Clock.systemUTC();
        }
    }

    @Autowired
    private SelfInvocationDemoService service;

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(MeasureTimeAspect.class);
        logger.setLevel(Level.INFO);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    @Test
    void serviceBean_isWrappedInSpringAopProxy() {
        assertThat(AopUtils.isAopProxy(service)).isTrue();
    }

    @Test
    void externalCall_goesThroughProxy_aspectRuns() {
        // Act
        String result = service.timedOperation();

        // Assert
        assertThat(result).isEqualTo("timedOperation executed");
        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.get(0).getFormattedMessage()).contains("timedOperation");
    }

    @Test
    void selfInvocation_viaThis_bypassesProxy_aspectDoesNotRun() {
        // Act
        String result = service.callViaThis();

        // Assert: метод виконався, але аспект - ні
        assertThat(result).isEqualTo("timedOperation executed");
        assertThat(appender.list).isEmpty();
    }
}
