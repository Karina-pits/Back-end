package com.clinic.notification;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Сценарій 3: споживач не залежить від класів видавця.
 * У classpath notification-service немає жодного класу clinic-service
 * (спільними є лише DTO з модуля common).
 */
class NotificationIsolationTest {

    @Test
    void notificationService_doesNotSeeClinicServiceClasses() {
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("com.clinic.service.AppointmentService"));
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("com.clinic.entity.Appointment"));
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("com.clinic.ClinicApplication"));
    }
}
