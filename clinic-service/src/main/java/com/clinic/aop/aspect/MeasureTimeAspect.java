package com.clinic.aop.aspect;

import com.clinic.aop.annotation.MeasureTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * Advice типу @Around: "обгортає" виклик методу - фіксує час до і після proceed().
 *
 * Pointcut @annotation(measureTime) спрацьовує лише для методів, позначених
 * @MeasureTime, і одразу дає доступ до значень анотації (поріг).
 * Час береться з Clock (а не з System.currentTimeMillis), щоб unit-тест
 * міг підставити фейковий годинник і бути детермінованим.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class MeasureTimeAspect {

    private final Clock clock;

    @Around("@annotation(measureTime)")
    public Object measure(ProceedingJoinPoint joinPoint, MeasureTime measureTime) throws Throwable {
        long start = clock.millis();
        try {
            return joinPoint.proceed();
        } finally {
            // finally - щоб час записався і тоді, коли метод кинув виняток
            long elapsed = clock.millis() - start;
            String method = joinPoint.getSignature().toShortString();
            if (elapsed > measureTime.warnThresholdMs()) {
                log.warn("[MeasureTime] SLOW {} took {} ms (threshold {} ms)",
                        method, elapsed, measureTime.warnThresholdMs());
            } else {
                log.info("[MeasureTime] {} took {} ms", method, elapsed);
            }
        }
    }
}
