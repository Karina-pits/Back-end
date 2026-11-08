package com.clinic.aop.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Позначає метод, успішне виконання якого треба занести в журнал аудиту:
 * хто (ID користувача), що (action) і з якими аргументами зробив.
 * Обробляється AuditAspect (@AfterReturning) - тобто запис з'являється
 * ТІЛЬКИ якщо метод завершився без винятку.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Audited {

    /** Назва дії для журналу, наприклад CANCEL_APPOINTMENT. */
    String action();
}
