package com.clinic.scheduling.config;

import com.clinic.scheduling.job.AppointmentReminderJob;
import com.clinic.scheduling.job.ExpiredAppointmentCleanupJob;
import org.junit.jupiter.api.Test;
import org.quartz.CronTrigger;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.impl.JobExecutionContextImpl;
import org.quartz.spi.OperableTrigger;
import org.quartz.spi.TriggerFiredBundle;

import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Перевіряємо конфігурацію напряму, без Spring-контексту і без запуску планувальника.
 */
class QuartzJobsConfigTest {

    private final QuartzJobsConfig config = new QuartzJobsConfig();

    private List<Trigger> allTriggers() {
        return List.of(config.reminderDailyTrigger(), config.reminderUrgentTrigger(),
                config.cleanupNightlyTrigger());
    }

    @Test
    void triggers_pointToTheirJobsByJobKey() {
        assertThat(config.reminderDailyTrigger().getJobKey()).isEqualTo(QuartzJobsConfig.REMINDER_JOB_KEY);
        assertThat(config.reminderUrgentTrigger().getJobKey()).isEqualTo(QuartzJobsConfig.REMINDER_JOB_KEY);
        assertThat(config.cleanupNightlyTrigger().getJobKey()).isEqualTo(QuartzJobsConfig.CLEANUP_JOB_KEY);

        assertThat(config.appointmentReminderJobDetail().getKey()).isEqualTo(QuartzJobsConfig.REMINDER_JOB_KEY);
        assertThat(config.expiredAppointmentCleanupJobDetail().getKey()).isEqualTo(QuartzJobsConfig.CLEANUP_JOB_KEY);
    }

    @Test
    void jobsAndTriggers_haveExplicitNameAndGroup() {
        List<String[]> nameAndGroup = List.of(
                new String[]{config.appointmentReminderJobDetail().getKey().getName(),
                        config.appointmentReminderJobDetail().getKey().getGroup()},
                new String[]{config.expiredAppointmentCleanupJobDetail().getKey().getName(),
                        config.expiredAppointmentCleanupJobDetail().getKey().getGroup()},
                new String[]{config.reminderDailyTrigger().getKey().getName(),
                        config.reminderDailyTrigger().getKey().getGroup()},
                new String[]{config.reminderUrgentTrigger().getKey().getName(),
                        config.reminderUrgentTrigger().getKey().getGroup()},
                new String[]{config.cleanupNightlyTrigger().getKey().getName(),
                        config.cleanupNightlyTrigger().getKey().getGroup()});

        assertThat(nameAndGroup).allSatisfy(pair -> {
            assertThat(pair[0]).isNotBlank();
            assertThat(pair[1]).isNotBlank().isNotEqualTo("DEFAULT");
        });
    }

    @Test
    void reminderJob_hasTwoTriggers() {
        long count = allTriggers().stream()
                .filter(t -> t.getJobKey().equals(QuartzJobsConfig.REMINDER_JOB_KEY))
                .count();

        assertThat(count).isEqualTo(2);
    }

    @Test
    void mergedJobDataMap_triggerValueWinsAndOtherKeysKeepJobDefaults() throws Exception {
        // Arrange: JobDetail має hoursAhead=24 і subject; urgent-тригер перевизначає лише hoursAhead=3
        JobDetail jobDetail = config.appointmentReminderJobDetail();
        OperableTrigger trigger = (OperableTrigger) config.reminderUrgentTrigger();
        TriggerFiredBundle bundle = new TriggerFiredBundle(
                jobDetail, trigger, null, false, new Date(), new Date(), null, null);

        // Act: контекст об'єднує дві JobDataMap так само, як це робить Quartz під час запуску
        JobExecutionContextImpl context =
                new JobExecutionContextImpl(mock(Scheduler.class), bundle, new AppointmentReminderJob());
        JobDataMap merged = context.getMergedJobDataMap();

        // Assert
        assertThat(jobDetail.getJobDataMap().getInt(AppointmentReminderJob.HOURS_AHEAD_KEY)).isEqualTo(24);
        assertThat(merged.getInt(AppointmentReminderJob.HOURS_AHEAD_KEY)).isEqualTo(3);
        assertThat(merged.getString(AppointmentReminderJob.SUBJECT_KEY))
                .isEqualTo(jobDetail.getJobDataMap().getString(AppointmentReminderJob.SUBJECT_KEY));
    }

    @Test
    void dailyTrigger_withoutOverrides_usesJobDefaults() throws Exception {
        JobDetail jobDetail = config.appointmentReminderJobDetail();
        OperableTrigger trigger = (OperableTrigger) config.reminderDailyTrigger();
        TriggerFiredBundle bundle = new TriggerFiredBundle(
                jobDetail, trigger, null, false, new Date(), new Date(), null, null);

        JobDataMap merged = new JobExecutionContextImpl(
                mock(Scheduler.class), bundle, new AppointmentReminderJob()).getMergedJobDataMap();

        assertThat(merged.getInt(AppointmentReminderJob.HOURS_AHEAD_KEY)).isEqualTo(24);
    }

    @Test
    void everyTrigger_hasExplicitMisfireInstruction() {
        assertThat(config.reminderDailyTrigger().getMisfireInstruction())
                .isEqualTo(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW);
        assertThat(config.reminderUrgentTrigger().getMisfireInstruction())
                .isEqualTo(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
        assertThat(config.cleanupNightlyTrigger().getMisfireInstruction())
                .isEqualTo(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW);

        // жодна не залишена на "розумну політику за замовчуванням"
        assertThat(allTriggers()).allSatisfy(trigger ->
                assertThat(trigger.getMisfireInstruction()).isNotEqualTo(Trigger.MISFIRE_INSTRUCTION_SMART_POLICY));
    }

    @Test
    void everyCronTrigger_hasExplicitTimeZoneInsteadOfServerDefault() {
        assertThat(allTriggers()).allSatisfy(trigger -> {
            TimeZone zone = ((CronTrigger) trigger).getTimeZone();
            assertThat(zone.getID()).isEqualTo(QuartzJobsConfig.TIME_ZONE_ID).isNotEqualTo("GMT");
        });
    }

    @Test
    void unknownTimeZoneId_isSilentlyReplacedByGmt() {
        // Саме тому ідентифікатор поясу в конфігурації перевіряється тестом
        assertThat(TimeZone.getTimeZone("Europe/Nowhere").getID()).isEqualTo("GMT");
    }

    @Test
    void jobs_forbidConcurrentExecution() {
        assertThat(AppointmentReminderJob.class.isAnnotationPresent(DisallowConcurrentExecution.class)).isTrue();
        assertThat(ExpiredAppointmentCleanupJob.class.isAnnotationPresent(DisallowConcurrentExecution.class)).isTrue();
    }
}
