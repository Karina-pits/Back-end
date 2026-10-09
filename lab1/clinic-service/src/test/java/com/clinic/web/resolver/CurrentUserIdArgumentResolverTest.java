package com.clinic.web.resolver;

import com.clinic.security.SecurityUtils;
import com.clinic.web.annotation.CurrentUserId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserIdArgumentResolverTest {

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private CurrentUserIdArgumentResolver resolver;

    // Допоміжний клас з двома методами - один параметр з анотацією, інший без
    static class SampleController {
        public void withAnnotation(@CurrentUserId String userId) {
        }

        public void withoutAnnotation(String userId) {
        }
    }

    // ---------------- AC6: supportsParameter() ----------------

    @Test
    void supportsParameter_parameterWithCurrentUserIdAnnotation_returnsTrue() throws NoSuchMethodException {
        // Arrange
        Method method = SampleController.class.getMethod("withAnnotation", String.class);
        MethodParameter parameter = new MethodParameter(method, 0);

        // Act
        boolean result = resolver.supportsParameter(parameter);

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void supportsParameter_parameterWithoutCurrentUserIdAnnotation_returnsFalse() throws NoSuchMethodException {
        // Arrange
        Method method = SampleController.class.getMethod("withoutAnnotation", String.class);
        MethodParameter parameter = new MethodParameter(method, 0);

        // Act
        boolean result = resolver.supportsParameter(parameter);

        // Assert
        assertThat(result).isFalse();
    }

    // ---------------- AC7: resolveArgument() ----------------

    @Test
    void resolveArgument_authenticatedUser_returnsCorrectUserId() {
        // Arrange
        String expectedUserId = "22222222-2222-2222-2222-222222222222";
        when(securityUtils.getCurrentUserId()).thenReturn(expectedUserId);

        // Act
        Object result = resolver.resolveArgument(null, null, null, null);

        // Assert
        assertThat(result).isInstanceOf(String.class);
        assertThat(result).isEqualTo(expectedUserId);
    }
}
