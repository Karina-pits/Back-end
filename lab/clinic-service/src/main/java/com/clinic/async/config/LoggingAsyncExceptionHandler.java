package com.clinic.async.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

import java.lang.reflect.Method;

/**
 * Для @Async void-методів нікому "повернути" виняток (немає Future),
 * тому без цього обробника помилка загубилась би непомітно.
 * Тут вона фіксується в лозі з рівнем ERROR.
 *
 * Параметри методу навмисно НЕ логуються: там можуть бути персональні дані (email).
 */
@Slf4j
public class LoggingAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

    @Override
    public void handleUncaughtException(Throwable ex, Method method, Object... params) {
        log.error("[ASYNC] Unhandled exception in {}.{}: {}",
                method.getDeclaringClass().getSimpleName(), method.getName(), ex.getMessage(), ex);
    }
}
