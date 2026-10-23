package com.clinic.aop.aspect;

import com.clinic.aop.annotation.Audited;
import com.clinic.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Advice типу @AfterReturning: виконується ПІСЛЯ методу, але тільки якщо
 * він завершився без винятку. Якщо метод кинув помилку (наприклад
 * AccessDeniedException) - запису в аудиті не буде, бо дії фактично не було.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final SecurityUtils securityUtils;

    @AfterReturning("@annotation(audited)")
    public void audit(JoinPoint joinPoint, Audited audited) {
        String userId = securityUtils.getCurrentUserId();
        log.info("[AUDIT] action={} user={} method={} args={}",
                audited.action(),
                userId == null ? "anonymous" : userId,
                joinPoint.getSignature().toShortString(),
                Arrays.toString(joinPoint.getArgs()));
    }
}
