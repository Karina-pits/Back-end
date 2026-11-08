package com.clinic.aop.aspect;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.clinic.aop.annotation.Audited;
import com.clinic.security.SecurityUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditAspectTest {

    @Mock
    private SecurityUtils securityUtils;
    @Mock
    private JoinPoint joinPoint;
    @Mock
    private Signature signature;

    @InjectMocks
    private AuditAspect aspect;

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    static class Sample {
        @Audited(action = "CANCEL_APPOINTMENT")
        public void annotated() {
        }
    }

    private Audited annotation() throws NoSuchMethodException {
        return Sample.class.getMethod("annotated").getAnnotation(Audited.class);
    }

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(AuditAspect.class);
        logger.setLevel(Level.INFO);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

    @Test
    void audit_authenticatedUser_logsActionUserMethodAndArgs() throws Exception {
        // Arrange
        when(securityUtils.getCurrentUserId()).thenReturn("user-1");
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("AppointmentService.cancel(..)");
        when(joinPoint.getArgs()).thenReturn(new Object[]{5L});

        // Act
        aspect.audit(joinPoint, annotation());

        // Assert
        assertThat(appender.list).hasSize(1);
        String message = appender.list.get(0).getFormattedMessage();
        assertThat(message)
                .contains("CANCEL_APPOINTMENT")
                .contains("user-1")
                .contains("AppointmentService.cancel(..)")
                .contains("[5]");
    }

    @Test
    void audit_noAuthenticatedUser_logsAnonymous() throws Exception {
        // Arrange
        when(securityUtils.getCurrentUserId()).thenReturn(null);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("AppointmentService.cancel(..)");
        when(joinPoint.getArgs()).thenReturn(new Object[]{});

        // Act
        aspect.audit(joinPoint, annotation());

        // Assert
        assertThat(appender.list).hasSize(1);
        assertThat(appender.list.get(0).getFormattedMessage()).contains("anonymous");
    }
}
