package com.clinic.scheduling.job;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Справжній планувальник Quartz у пам'яті (RAMJobStore): без Spring і без БД.
 * Завдання триває 300 мс, а тригер спрацьовує кожні 100 мс, тобто наступний запуск
 * настає, поки попередній ще виконується.
 *  - із @DisallowConcurrentExecution одночасно працює не більше одного запуску;
 *  - без неї (контрольне завдання) запуски накладаються.
 */
public class DisallowConcurrentExecutionTest {

    static class Probe {
        final AtomicInteger running = new AtomicInteger();
        final AtomicInteger maxRunning = new AtomicInteger();
        final AtomicInteger executions = new AtomicInteger();

        void run() {
            int now = running.incrementAndGet();
            maxRunning.accumulateAndGet(now, Math::max);
            executions.incrementAndGet();
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                running.decrementAndGet();
            }
        }
    }

    static final Probe GUARDED = new Probe();
    static final Probe UNGUARDED = new Probe();

    @DisallowConcurrentExecution
    public static class GuardedJob implements Job {
        @Override
        public void execute(JobExecutionContext context) {
            GUARDED.run();
        }
    }

    public static class UnguardedJob implements Job {
        @Override
        public void execute(JobExecutionContext context) {
            UNGUARDED.run();
        }
    }

    private Scheduler scheduler;

    @BeforeEach
    void resetCounters() {
        for (Probe probe : new Probe[]{GUARDED, UNGUARDED}) {
            probe.running.set(0);
            probe.maxRunning.set(0);
            probe.executions.set(0);
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        if (scheduler != null) {
            scheduler.shutdown(true);
        }
    }

    private void runUntilFourExecutions(Class<? extends Job> jobClass, String name, Probe probe) throws Exception {
        Properties props = new Properties();
        props.setProperty("org.quartz.scheduler.instanceName", "test-" + name);
        props.setProperty("org.quartz.scheduler.skipUpdateCheck", "true");
        props.setProperty("org.quartz.threadPool.threadCount", "4");
        props.setProperty("org.quartz.jobStore.class", "org.quartz.simpl.RAMJobStore");
        scheduler = new StdSchedulerFactory(props).getScheduler();

        JobDetail job = JobBuilder.newJob(jobClass).withIdentity(name, "test").build();
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity(name + "Trigger", "test")
                .startNow()
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInMilliseconds(100)
                        .repeatForever())
                .build();
        scheduler.scheduleJob(job, trigger);
        scheduler.start();

        await().atMost(10, TimeUnit.SECONDS).until(() -> probe.executions.get() >= 4);
    }

    @Test
    void guardedJob_nextRunWaitsForPreviousOne_neverTwoAtOnce() throws Exception {
        runUntilFourExecutions(GuardedJob.class, "guarded", GUARDED);

        assertThat(GUARDED.maxRunning.get()).isEqualTo(1);
    }

    @Test
    void unguardedJob_runsOverlap_whichIsWhatTheAnnotationPrevents() throws Exception {
        runUntilFourExecutions(UnguardedJob.class, "unguarded", UNGUARDED);

        assertThat(UNGUARDED.maxRunning.get()).isGreaterThanOrEqualTo(2);
    }
}
