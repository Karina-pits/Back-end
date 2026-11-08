package com.clinic.async.config;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Окремий пул потоків для повільних побічних ефектів (лист, PDF, зовнішній сервіс).
 *
 * Пул ОБМЕЖЕНИЙ, щоб "асинхронність" не вичерпала ресурси сервера:
 *  - core = 2 потоки працюють завжди;
 *  - queue = 10 завдань чекають у черзі, коли обидва потоки зайняті;
 *  - max = 4 потоки - додаткові створюються, лише коли черга повна;
 *  - усе понад 4 + 10 відхиляється (AbortPolicy -> TaskRejectedException),
 *    це явно обробляється в AsyncAppointmentController (відповідь 503).
 *
 * Усі @Async-методи вказують цей виконавець за назвою, тому вони НЕ потрапляють
 * у пул за замовчуванням Spring Boot і не в SimpleAsyncTaskExecutor.
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    public static final String CLINIC_EXECUTOR = "clinicTaskExecutor";

    @Bean(name = CLINIC_EXECUTOR)
    public ThreadPoolTaskExecutor clinicTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("clinic-async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return clinicTaskExecutor();
    }

    /** Обробник помилок для @Async-методів з типом повернення void. */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new LoggingAsyncExceptionHandler();
    }
}
