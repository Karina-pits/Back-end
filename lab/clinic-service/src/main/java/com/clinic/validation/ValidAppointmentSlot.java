package com.clinic.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Бізнес-правило для запису на прийом: час прийому має бути в майбутньому,
 * у робочі години клініки (08:00-18:00) і не в неділю (клініка не працює).
 *
 * Анотація ставиться на клас DTO (ElementType.TYPE), бо правило залежить
 * лише від одного поля (scheduledAt), але перевірка охоплює кілька умов
 * одночасно, тому зручніше валідувати весь об'єкт цілісно.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AppointmentSlotValidator.class)
@Documented
public @interface ValidAppointmentSlot {

    String message() default "Некоректний час запису на прийом";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
