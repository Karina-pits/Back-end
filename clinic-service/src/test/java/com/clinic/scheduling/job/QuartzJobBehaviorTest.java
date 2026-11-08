package com.clinic.scheduling.job;

import com.clinic.scheduling.service.AppointmentCleanupService;
import com.clinic.scheduling.service.AppointmentReminderService;
import com.clinic.scheduling.service.ReminderResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Job-класи тестуються без Quartz і без Spring: контекст і сервіси замоканi.
 */
@ExtendWith(MockitoExtension.class)
class QuartzJobBehaviorTest {

    @Mock
    private AppointmentReminderService reminderService;
    @Mock
    private AppointmentCleanupService cleanupService;
    @Mock
    private JobExecutionContext context;

    @InjectMocks
    private AppointmentReminderJob reminderJob;
    @InjectMocks
    private ExpiredAppointmentCleanupJob cleanupJob;

    private JobDataMap params(Object... keyValues) {
        JobDataMap map = new JobDataMap();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    // ---------------- AppointmentReminderJob ----------------

    @Test
    void reminderJob_validParams_makesExactlyOneServiceCallWithThem() throws Exception {
        // Arrange
        when(context.getMergedJobDataMap()).thenReturn(params("hoursAhead", 3, "subject", "Тема"));
        when(reminderService.sendReminders(3, "Тема")).thenReturn(new ReminderResult(1, 0, 0));

        // Act
        reminderJob.execute(context);

        // Assert: Job лише читає параметри і викликає сервіс, більше нічого
        verify(reminderService).sendReminders(3, "Тема");
        verifyNoMoreInteractions(reminderService);
    }

    @Test
    void reminderJob_misspelledKey_throwsExplicitErrorAndDoesNotCallService() {
        // Arrange: у ключі помилка - hoursAhed замість hoursAhead
        when(context.getMergedJobDataMap()).thenReturn(params("hoursAhed", 3, "subject", "Тема"));

        // Act + Assert
        assertThatThrownBy(() -> reminderJob.execute(context))
                .isInstanceOf(JobExecutionException.class)
                .hasMessageContaining("hoursAhead");
        verifyNoInteractions(reminderService);
    }

    @Test
    void reminderJob_wrongValueType_throwsExplicitError() {
        when(context.getMergedJobDataMap()).thenReturn(params("hoursAhead", "abc", "subject", "Тема"));

        assertThatThrownBy(() -> reminderJob.execute(context))
                .isInstanceOf(JobExecutionException.class)
                .hasMessageContaining("hoursAhead");
        verifyNoInteractions(reminderService);
    }

    @Test
    void reminderJob_serviceFails_recordsFailureOnceWithoutImmediateRefire() throws Exception {
        // Arrange
        when(context.getMergedJobDataMap()).thenReturn(params("hoursAhead", 24, "subject", "Тема"));
        when(reminderService.sendReminders(24, "Тема")).thenThrow(new IllegalStateException("db down"));

        // Act
        JobExecutionException error = assertThrows(JobExecutionException.class,
                () -> reminderJob.execute(context));

        // Assert: помилка зафіксована, негайного повтору немає, сервіс викликано рівно один раз
        assertThat(error.refireImmediately()).isFalse();
        assertThat(error.getCause()).isInstanceOf(IllegalStateException.class);
        verify(reminderService, times(1)).sendReminders(24, "Тема");
    }

    // ---------------- ExpiredAppointmentCleanupJob ----------------

    @Test
    void cleanupJob_validParams_makesExactlyOneServiceCall() throws Exception {
        when(context.getMergedJobDataMap()).thenReturn(params("graceHours", 24));
        when(cleanupService.cancelOverdueAppointments(24)).thenReturn(2);

        cleanupJob.execute(context);

        verify(cleanupService).cancelOverdueAppointments(24);
        verifyNoMoreInteractions(cleanupService);
    }

    @Test
    void cleanupJob_missingKey_throwsExplicitErrorAndDoesNotCallService() {
        when(context.getMergedJobDataMap()).thenReturn(params());

        assertThatThrownBy(() -> cleanupJob.execute(context))
                .isInstanceOf(JobExecutionException.class)
                .hasMessageContaining("graceHours");
        verifyNoInteractions(cleanupService);
    }

    @Test
    void cleanupJob_serviceFails_recordsFailureOnceWithoutImmediateRefire() {
        when(context.getMergedJobDataMap()).thenReturn(params("graceHours", 24));
        when(cleanupService.cancelOverdueAppointments(24)).thenThrow(new IllegalStateException("db down"));

        JobExecutionException error = assertThrows(JobExecutionException.class,
                () -> cleanupJob.execute(context));

        assertThat(error.refireImmediately()).isFalse();
        verify(cleanupService, times(1)).cancelOverdueAppointments(24);
    }
}
