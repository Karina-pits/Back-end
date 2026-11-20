package com.clinic.notification.service;

import com.clinic.common.dto.AppointmentEvent;
import com.clinic.common.dto.AppointmentEventType;
import com.clinic.common.dto.EmailRequest;
import com.clinic.notification.MailSenderService;
import com.clinic.notification.NotificationProcessingService;
import com.clinic.notification.entity.AppointmentNotification;
import com.clinic.notification.repository.AppointmentNotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Без Spring, брокера і БД: репозиторій та відправник листів замоканi.
 */
@ExtendWith(MockitoExtension.class)
class NotificationProcessingServiceTest {

    @Mock
    private AppointmentNotificationRepository repository;
    @Mock
    private MailSenderService mailSender;

    @InjectMocks
    private NotificationProcessingService service;

    private AppointmentEvent event(String eventId, AppointmentEventType type, String email) {
        return new AppointmentEvent(eventId, type, 5L, email, "Марія Іваненко", "Олена Коваленко",
                LocalDateTime.of(2026, 11, 20, 10, 0), Instant.parse("2026-11-19T10:00:00Z"));
    }

    @Test
    void process_newEvent_sendsMailAndSavesVisibleRecord() {
        // Arrange
        AppointmentEvent event = event("e-1", AppointmentEventType.CREATED, "patient@clinic.local");

        // Act
        boolean processed = service.process(event);

        // Assert
        assertThat(processed).isTrue();
        ArgumentCaptor<EmailRequest> mail = ArgumentCaptor.forClass(EmailRequest.class);
        verify(mailSender).send(mail.capture());
        assertThat(mail.getValue().to()).isEqualTo("patient@clinic.local");

        ArgumentCaptor<AppointmentNotification> saved = ArgumentCaptor.forClass(AppointmentNotification.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getEventId()).isEqualTo("e-1");
        assertThat(saved.getValue().getEventType()).isEqualTo("CREATED");
        assertThat(saved.getValue().getAppointmentId()).isEqualTo(5L);
        assertThat(saved.getValue().getStatus()).isEqualTo(NotificationProcessingService.STATUS_SENT);
        assertThat(saved.getValue().getSubject()).contains("#5");
    }

    @Test
    void process_sameEventTwice_secondRunChangesNothing() {
        // Arrange: "журнал" у пам'яті повторює унікальний event_id у БД
        Set<String> processedIds = new HashSet<>();
        when(repository.existsByEventId(any())).thenAnswer(inv -> processedIds.contains(inv.<String>getArgument(0)));
        when(repository.save(any(AppointmentNotification.class))).thenAnswer(inv -> {
            AppointmentNotification notification = inv.getArgument(0);
            processedIds.add(notification.getEventId());
            return notification;
        });
        AppointmentEvent event = event("e-1", AppointmentEventType.CREATED, "patient@clinic.local");

        // Act
        boolean first = service.process(event);
        boolean second = service.process(event);

        // Assert
        assertThat(first).isTrue();
        assertThat(second).isFalse();
        verify(mailSender, times(1)).send(any());
        verify(repository, times(1)).save(any());
    }

    @Test
    void process_eventWithoutEmail_savesRecordWithoutSendingMail() {
        AppointmentEvent event = event("e-2", AppointmentEventType.CANCELLED, null);

        service.process(event);

        verifyNoInteractions(mailSender);
        ArgumentCaptor<AppointmentNotification> saved = ArgumentCaptor.forClass(AppointmentNotification.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(NotificationProcessingService.STATUS_SKIPPED);
    }

    @Test
    void process_mailFails_exceptionPropagatesAndNothingIsSaved() {
        // Arrange: виняток піде в механізм повторів / DLQ, а не буде проковтнутий
        doThrow(new IllegalStateException("smtp down")).when(mailSender).send(any());

        // Act + Assert
        assertThrows(IllegalStateException.class,
                () -> service.process(event("e-3", AppointmentEventType.CREATED, "patient@clinic.local")));
        verify(repository, never()).save(any());
    }

    @Test
    void process_eachEventType_producesItsOwnSubject() {
        for (AppointmentEventType type : AppointmentEventType.values()) {
            service.process(event("e-" + type, type, "patient@clinic.local"));
        }

        ArgumentCaptor<AppointmentNotification> saved = ArgumentCaptor.forClass(AppointmentNotification.class);
        verify(repository, times(3)).save(saved.capture());
        List<AppointmentNotification> all = saved.getAllValues();
        Set<String> subjects = all.stream().map(AppointmentNotification::getSubject).collect(Collectors.toSet());
        assertThat(subjects).hasSize(3);
    }
}
