package com.clinic.scheduling.job;

import com.clinic.scheduling.service.AppointmentCleanupService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * "Легке" завдання: параметр graceHours + один виклик сервісу.
 * Стратегія збою така сама, як у AppointmentReminderJob: помилку фіксуємо
 * в лозі й чекаємо наступного нічного запуску (без негайного повтору).
 */
@Slf4j
@DisallowConcurrentExecution
public class ExpiredAppointmentCleanupJob implements Job {

    public static final String GRACE_HOURS_KEY = "graceHours";

    @Autowired
    private AppointmentCleanupService cleanupService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        int graceHours = JobParams.requireInt(context.getMergedJobDataMap(), GRACE_HOURS_KEY);

        try {
            int cancelled = cleanupService.cancelOverdueAppointments(graceHours);
            log.info("[JOB] cleanup graceHours={} cancelled={}", graceHours, cancelled);
        } catch (RuntimeException e) {
            log.error("[JOB] cleanup failed, waiting for the next scheduled run: {}", e.toString(), e);
            throw new JobExecutionException("Помилка завдання очищення", e);
        }
    }
}
