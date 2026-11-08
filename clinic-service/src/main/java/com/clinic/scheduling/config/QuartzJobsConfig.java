package com.clinic.scheduling.config;

import com.clinic.scheduling.job.AppointmentReminderJob;
import com.clinic.scheduling.job.ExpiredAppointmentCleanupJob;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

/**
 * Усі завдання (Job) і тригери оголошені в одному місці.
 *
 * Job 1 "appointmentReminder" (нагадування про прийом) має ДВА тригери:
 *   - reminderDaily  : щодня о 09:00, використовує значення за замовчуванням з JobDetail (24 год);
 *   - reminderUrgent : кожні 30 хв з 08:00 до 19:30, ПЕРЕВИЗНАЧАЄ hoursAhead = 3.
 * Job 2 "expiredAppointmentCleanup" (очищення прострочених записів) має один тригер: щоночі о 02:00.
 *
 * Кожен тригер вказує на своє завдання через JobKey (forJob), а назва й група
 * в усіх завдань і тригерів задані явно.
 */
@Configuration
public class QuartzJobsConfig {

    public static final String GROUP_REMINDERS = "clinic-reminders";
    public static final String GROUP_MAINTENANCE = "clinic-maintenance";

    /**
     * Часовий пояс cron-тригерів - явний, щоб розклад не залежав від поясу сервера.
     * Увага: невідомий ідентифікатор TimeZone.getTimeZone() мовчки замінює на GMT.
     */
    public static final String TIME_ZONE_ID = "Europe/Kiev";

    public static final JobKey REMINDER_JOB_KEY = JobKey.jobKey("appointmentReminder", GROUP_REMINDERS);
    public static final JobKey CLEANUP_JOB_KEY = JobKey.jobKey("expiredAppointmentCleanup", GROUP_MAINTENANCE);

    public static final TriggerKey REMINDER_DAILY_TRIGGER_KEY =
            TriggerKey.triggerKey("reminderDaily", GROUP_REMINDERS);
    public static final TriggerKey REMINDER_URGENT_TRIGGER_KEY =
            TriggerKey.triggerKey("reminderUrgent", GROUP_REMINDERS);
    public static final TriggerKey CLEANUP_NIGHTLY_TRIGGER_KEY =
            TriggerKey.triggerKey("cleanupNightly", GROUP_MAINTENANCE);

    private static TimeZone timeZone() {
        return TimeZone.getTimeZone(TIME_ZONE_ID);
    }

    // ---------------- Job 1: нагадування ----------------

    @Bean
    public JobDetail appointmentReminderJobDetail() {
        return JobBuilder.newJob(AppointmentReminderJob.class)
                .withIdentity(REMINDER_JOB_KEY)
                .withDescription("Надсилає пацієнтам нагадування про найближчі прийоми")
                // значення за замовчуванням; тригер може перевизначити будь-яке
                .usingJobData(AppointmentReminderJob.HOURS_AHEAD_KEY, 24)
                .usingJobData(AppointmentReminderJob.SUBJECT_KEY, "Нагадування про прийом")
                .storeDurably()
                // якщо застосунок впав посеред виконання, після старту завдання запуститься знову;
                // це безпечно, бо логіка ідемпотентна
                .requestRecovery(true)
                .build();
    }

    @Bean
    public Trigger reminderDailyTrigger() {
        return TriggerBuilder.newTrigger()
                .withIdentity(REMINDER_DAILY_TRIGGER_KEY)
                .forJob(REMINDER_JOB_KEY)
                .withDescription("Щоденне нагадування на найближчі 24 години")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 9 * * ?")
                        .inTimeZone(timeZone())
                        // пропущений запуск краще виконати пізніше, ніж втратити нагадування
                        .withMisfireHandlingInstructionFireAndProceed())
                .build();
    }

    @Bean
    public Trigger reminderUrgentTrigger() {
        return TriggerBuilder.newTrigger()
                .withIdentity(REMINDER_URGENT_TRIGGER_KEY)
                .forJob(REMINDER_JOB_KEY)
                .withDescription("Термінові нагадування: прийоми протягом найближчих 3 годин")
                // перевизначає значення за замовчуванням з JobDetail
                .usingJobData(AppointmentReminderJob.HOURS_AHEAD_KEY, 3)
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0/30 8-19 * * ?")
                        .inTimeZone(timeZone())
                        // пропущені запуски кожні 30 хв не накопичуємо, чекаємо наступного
                        .withMisfireHandlingInstructionDoNothing())
                .build();
    }

    // ---------------- Job 2: очищення ----------------

    @Bean
    public JobDetail expiredAppointmentCleanupJobDetail() {
        return JobBuilder.newJob(ExpiredAppointmentCleanupJob.class)
                .withIdentity(CLEANUP_JOB_KEY)
                .withDescription("Скасовує записи, які залишились у статусі SCHEDULED після дати прийому")
                .usingJobData(ExpiredAppointmentCleanupJob.GRACE_HOURS_KEY, 24)
                .storeDurably()
                .requestRecovery(true)
                .build();
    }

    @Bean
    public Trigger cleanupNightlyTrigger() {
        return TriggerBuilder.newTrigger()
                .withIdentity(CLEANUP_NIGHTLY_TRIGGER_KEY)
                .forJob(CLEANUP_JOB_KEY)
                .withDescription("Нічне очищення о 02:00")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 2 * * ?")
                        .inTimeZone(timeZone())
                        .withMisfireHandlingInstructionFireAndProceed())
                .build();
    }
}
