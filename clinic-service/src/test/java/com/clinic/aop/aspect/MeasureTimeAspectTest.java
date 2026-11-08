package com.clinic.aop.aspect;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.clinic.aop.annotation.MeasureTime;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Тестуємо advice напряму: ProceedingJoinPoint і Clock - mock-и,
 * жодного Spring-контексту / @SpringBootTest.
 */
@ExtendWith(MockitoExtension.class)
class MeasureTimeAspectTest {

    @Mock
    private Clock clock;
    @Mock
    private ProceedingJoinPoint joinPoint;
    @Mock
    private Signature signature;

    @InjectMocks
    private MeasureTimeAspect aspect;

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    static class Sample {
        @MeasureTime(warnThresholdMs = 100)
        public void annotated() {
        }
    }

    private MeasureTime annotation() throws NoSuchMethodException {
        return Sample.class.getMethod("annotated").getAnnotation(MeasureTime.class);
    }

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(MeasureTimeAspect.class);
        logger.setLevel(Level.INFO);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        lenient().when(joinPoint.getSignature()).thenReturn(signature);
        lenient().when(signature.toShortString()).thenReturn("Sample.annotated()");
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    @Test
    void measure_fastMethod_proceedsReturnsResultAndLogsInfo() throws Throwable {
        // Arrange
        when(joinPoint.proceed()).thenReturn("result");
        when(clock.millis()).thenReturn(1_000L, 1_050L); // 50 мс <= порогу 100

        // Act
        Object result = aspect.measure(joinPoint, annotation());

        // Assert
        assertThat(result).isEqualTo("result");
        verify(joinPoint, times(1)).proceed();
        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.get(0).getLevel()).isEqualTo(Level.INFO);
        assertThat(appender.list.get(0).getFormattedMessage()).contains("took 50 ms");
    }

    @Test
    void measure_methodSlowerThanThreshold_logsWarning() throws Throwable {
        // Arrange
        when(joinPoint.proceed()).thenReturn("result");
        when(clock.millis()).thenReturn(1_000L, 1_500L); // 500 мс > порогу 100

        // Act
        aspect.measure(joinPoint, annotation());

        // Assert
        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.get(0).getLevel()).isEqualTo(Level.WARN);
        assertThat(appender.list.get(0).getFormattedMessage()).contains("SLOW");
    }

    @Test
    void measure_methodThrows_exceptionPropagatesAndTimeIsStillLogged() throws Throwable {
        // Arrange
        when(joinPoint.proceed()).thenThrow(new IllegalStateException("boom"));
        when(clock.millis()).thenReturn(0L, 20L);

        // Act + Assert
        assertThrows(IllegalStateException.class, () -> aspect.measure(joinPoint, annotation()));
        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.get(0).getFormattedMessage()).contains("took 20 ms");
    }
}
