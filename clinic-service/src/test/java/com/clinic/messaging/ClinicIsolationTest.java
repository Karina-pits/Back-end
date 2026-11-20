package com.clinic.messaging;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Сценарій 3: видавець не залежить від класів споживача.
 * У classpath clinic-service немає жодного класу notification-service
 * (спільними є лише DTO з модуля common).
 */
class ClinicIsolationTest {

    @Test
    void clinicService_doesNotSeeNotificationServiceClasses() {
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("com.clinic.notification.NotificationServiceApplication"));
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("com.clinic.notification.service.NotificationProcessingService"));
        assertThrows(ClassNotFoundException.class,
                () -> Class.forName("com.clinic.notification.entity.AppointmentNotification"));
    }
}
