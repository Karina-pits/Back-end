package com.clinic.scheduling.job;

import com.clinic.scheduling.service.AppointmentReminderService;
import com.clinic.scheduling.service.ReminderResult;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * "Легке" завдання: зчитує параметри, робить ОДИН виклик сервісу, власної
 * бізнес-логіки не містить. Транзакція починається в методі сервісу.
 *
 * @DisallowConcurrentExecution: якщо попередній запуск ще триває, коли настав
 * час наступного (наприклад, збіглися тригери daily і urgent), наступний
 * чекає завершення попереднього, тож один прийом не обробляється двічі одночасно.
 *
 * Стратегія збою: помилка ФІКСУЄТЬСЯ в лозі, завдання завершується з
 * JobExecutionException БЕЗ негайного повторного запуску (refireImmediately = false),
 * і Quartz чекає наступного запланованого запуску. Нескінченного циклу немає.
 *
 * Job створюється Quartz через конструктор без аргументів, тому залежність
 * впроваджується через @Autowired у поле (конструкторне впровадження тут не працює).
 */
@Slf4j
@DisallowConcurrentExecution
public class AppointmentReminderJob implements Job {

    public static final String HOURS_AHEAD_KEY = "hoursAhead";
    public static final String SUBJECT_KEY = "subject";

    @Autowired
    private AppointmentReminderService reminderService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        // getMergedJobDataMap: значення з тригера перекривають значення за замовчуванням з JobDetail
        JobDataMap params = context.getMergedJobDataMap();
        int hoursAhead = JobParams.requireInt(params, HOURS_AHEAD_KEY);
        String subject = JobParams.requireString(params, SUBJECT_KEY);

        try {
            ReminderResult result = reminderService.sendReminders(hoursAhead, subject);
            log.info("[JOB] reminders hoursAhead={} sent={} skipped={} failed={}",
                    hoursAhead, result.sent(), result.skipped(), result.failed());
        } catch (RuntimeException e) {
            log.error("[JOB] reminders failed, waiting for the next scheduled run: {}", e.toString(), e);
            throw new JobExecutionException("Помилка завдання нагадувань", e);
        }
    }
}
