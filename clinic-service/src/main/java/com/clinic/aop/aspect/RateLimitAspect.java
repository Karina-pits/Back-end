package com.clinic.aop.aspect;

import com.clinic.aop.annotation.RateLimited;
import com.clinic.exception.RateLimitExceededException;
import com.clinic.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Advice типу @Around з РІШЕННЯМ: або викликати joinPoint.proceed()
 * (метод виконається), або кинути виняток і НЕ викликати proceed()
 * (метод не виконається взагалі).
 *
 * Для кожної пари "користувач + метод" зберігаємо час останніх викликів.
 * Старіші за вікно викидаємо; якщо в вікні вже maxCalls викликів - блокуємо.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final SecurityUtils securityUtils;
    private final Clock clock;

    private final Map<String, Deque<Long>> callsByKey = new ConcurrentHashMap<>();

    @Around("@annotation(rateLimited)")
    public Object limit(ProceedingJoinPoint joinPoint, RateLimited rateLimited) throws Throwable {
        String userId = securityUtils.getCurrentUserId();
        String key = (userId == null ? "anonymous" : userId)
                + ":" + joinPoint.getSignature().toShortString();

        long now = clock.millis();
        long windowStart = now - rateLimited.windowSeconds() * 1000L;

        Deque<Long> calls = callsByKey.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (calls) {
            while (!calls.isEmpty() && calls.peekFirst() <= windowStart) {
                calls.pollFirst();
            }
            if (calls.size() >= rateLimited.maxCalls()) {
                log.warn("[RateLimit] blocked {} (limit {} per {} s)",
                        key, rateLimited.maxCalls(), rateLimited.windowSeconds());
                throw new RateLimitExceededException("Забагато запитів: не більше "
                        + rateLimited.maxCalls() + " викликів за "
                        + rateLimited.windowSeconds() + " с. Спробуйте пізніше");
            }
            calls.addLast(now);
        }
        return joinPoint.proceed();
    }
}
