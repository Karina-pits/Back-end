package com.clinic.scheduling.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Підтверджує, що всі 11 таблиць Quartz створює саме changeset Liquibase
 * (а не Spring Boot, для якого в application.yml стоїть initialize-schema: never).
 */
class QuartzLiquibaseSchemaTest {

    private static final List<String> QUARTZ_TABLES = List.of(
            "qrtz_job_details", "qrtz_triggers", "qrtz_simple_triggers", "qrtz_cron_triggers",
            "qrtz_simprop_triggers", "qrtz_blob_triggers", "qrtz_calendars",
            "qrtz_paused_trigger_grps", "qrtz_fired_triggers", "qrtz_scheduler_state", "qrtz_locks");

    @Test
    void liquibaseChangelog_createsAllQuartzTables() throws IOException {
        ClassPathResource resource = new ClassPathResource("db/changelog/changes/003-quartz-schema.xml");

        String changelog = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8)
                .toLowerCase();

        assertThat(changelog).contains("<changeset");
        assertThat(QUARTZ_TABLES).allSatisfy(table ->
                assertThat(changelog).contains("create table " + table + " "));
    }
}
