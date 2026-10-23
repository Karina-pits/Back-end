package com.clinic.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Годинник як окремий бін - аспекти беруть час звідси, тому в unit-тестах
 * його легко замінити на mock і не залежати від реального часу.
 */
@Configuration
public class AopConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
