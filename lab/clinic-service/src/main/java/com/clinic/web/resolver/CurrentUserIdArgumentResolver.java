package com.clinic.web.resolver;

import com.clinic.security.SecurityUtils;
import com.clinic.web.annotation.CurrentUserId;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Резолвер для параметрів, позначених @CurrentUserId.
 *
 * AC6: supportsParameter() повертає true ТІЛЬКИ для параметрів з анотацією
 * @CurrentUserId (і коректного типу String), і false для всіх інших.
 * AC7: resolveArgument() дістає реальний sub-claim поточного користувача
 * через вже наявний SecurityUtils (той самий механізм, що працює і для
 * JWT, і для OIDC-сесії - детально розбирали це раніше в AC5/AC7 лаби №1).
 */
@Component
@RequiredArgsConstructor
public class CurrentUserIdArgumentResolver implements HandlerMethodArgumentResolver {

    private final SecurityUtils securityUtils;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUserId.class)
                && parameter.getParameterType().equals(String.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                   ModelAndViewContainer mavContainer,
                                   NativeWebRequest webRequest,
                                   WebDataBinderFactory binderFactory) {
        return securityUtils.getCurrentUserId();
    }
}
