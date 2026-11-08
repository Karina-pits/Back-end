package com.clinic.web.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Позначає параметр методу контролера, в який треба підставити ID (sub-claim)
 * поточного автентифікованого користувача - без того, щоб кожен метод сам
 * викликав SecurityUtils.getCurrentUserId() вручну.
 *
 * Резолвиться класом CurrentUserIdArgumentResolver.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUserId {
}
