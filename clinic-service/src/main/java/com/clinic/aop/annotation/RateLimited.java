package com.clinic.aop.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Обмежує, скільки разів ОДИН користувач може викликати метод за період
 * (захист від спаму, наприклад від масового створення записів на прийом).
 * Обробляється RateLimitAspect (@Around) - саме він вирішує, чи викликати
 * proceed(), чи кинути виняток.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimited {

    /** Максимум викликів одним користувачем за вікно. */
    int maxCalls() default 5;

    /** Розмір вікна в секундах. */
    int windowSeconds() default 60;
}
