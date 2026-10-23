package com.clinic.aop.aspect;

import com.clinic.aop.annotation.RateLimited;
import com.clinic.exception.RateLimitExceededException;
import com.clinic.security.SecurityUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Перевіряємо саме ЛОГІКУ РІШЕННЯ всередині advice:
 * коли викликається joinPoint.proceed(), а коли - ні.
 * Без Spring-контексту; час підставляється через mock Clock.
 */
@ExtendWith(MockitoExtension.class)
class RateLimitAspectTest {

    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private Clock clock;
    @Mock
    private ProceedingJoinPoint joinPoint;
    @Mock
    private Signature signature;

    @InjectMocks
    private RateLimitAspect aspect;

    static class Sample {
        @RateLimited(maxCalls = 2, windowSeconds = 60)
        public void annotated() {
        }
    }

    private RateLimited annotation() throws NoSuchMethodException {
        return Sample.class.getMethod("annotated").getAnnotation(RateLimited.class);
    }

    @BeforeEach
    void setUp() throws Throwable {
        lenient().when(joinPoint.getSignature()).thenReturn(signature);
        lenient().when(signature.toShortString()).thenReturn("Service.create(..)");
        lenient().when(joinPoint.proceed()).thenReturn("ok");
    }

    @Test
    void limit_callsWithinLimit_proceedsEveryTime() throws Throwable {
        // Arrange
        when(securityUtils.getCurrentUserId()).thenReturn("user-1");
        when(clock.millis()).thenReturn(1_000L, 2_000L);

        // Act
        Object first = aspect.limit(joinPoint, annotation());
        Object second = aspect.limit(joinPoint, annotation());

        // Assert
        assertThat(first).isEqualTo("ok");
        assertThat(second).isEqualTo("ok");
        verify(joinPoint, times(2)).proceed();
    }

    @Test
    void limit_exceedsMaxCalls_throwsAndDoesNotProceed() throws Throwable {
        // Arrange
        when(securityUtils.getCurrentUserId()).thenReturn("user-1");
        when(clock.millis()).thenReturn(1_000L, 2_000L, 3_000L);

        // Act
        aspect.limit(joinPoint, annotation());
        aspect.limit(joinPoint, annotation());

        // Assert: третій виклик у межах вікна блокується
        assertThrows(RateLimitExceededException.class, () -> aspect.limit(joinPoint, annotation()));
        verify(joinPoint, times(2)).proceed(); // третій раз proceed() НЕ викликався
    }

    @Test
    void limit_differentUsers_haveIndependentLimits() throws Throwable {
        // Arrange
        when(securityUtils.getCurrentUserId()).thenReturn("user-1", "user-1", "user-2");
        when(clock.millis()).thenReturn(1_000L);

        // Act
        aspect.limit(joinPoint, annotation());
        aspect.limit(joinPoint, annotation());
        Object thirdByOtherUser = aspect.limit(joinPoint, annotation());

        // Assert: user-2 не страждає від ліміту user-1
        assertThat(thirdByOtherUser).isEqualTo("ok");
        verify(joinPoint, times(3)).proceed();
    }

    @Test
    void limit_afterWindowExpires_allowsCallsAgain() throws Throwable {
        // Arrange: два виклики на початку, третій - через 61 секунду
        when(securityUtils.getCurrentUserId()).thenReturn("user-1");
        when(clock.millis()).thenReturn(1_000L, 2_000L, 62_000L);

        // Act
        aspect.limit(joinPoint, annotation());
        aspect.limit(joinPoint, annotation());
        Object afterWindow = aspect.limit(joinPoint, annotation());

        // Assert
        assertThat(afterWindow).isEqualTo("ok");
        verify(joinPoint, times(3)).proceed();
    }
}
