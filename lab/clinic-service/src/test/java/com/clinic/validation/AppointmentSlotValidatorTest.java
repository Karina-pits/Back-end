package com.clinic.validation;

import com.clinic.dto.AppointmentDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AC2: перевіряємо, що коректні дані НЕ викликають жодного порушення.
 * AC3: перевіряємо, що некоректні дані викликають порушення з чітким
 * повідомленням (а не загальним "invalid").
 *
 * Валідуємо напряму через jakarta.validation.Validator - без підняття
 * Spring-контексту, бо Bean Validation працює незалежно від Spring.
 */
class AppointmentSlotValidatorTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validator = null;
    }

    private AppointmentDto dtoWithScheduledAt(LocalDateTime scheduledAt) {
        AppointmentDto dto = new AppointmentDto();
        dto.setPatientId(1L);
        dto.setDoctorId(1L);
        dto.setReason("Консультація");
        dto.setScheduledAt(scheduledAt);
        return dto;
    }

    // Знаходимо найближчий у майбутньому будній день о 10:00 для стабільного тесту
    private LocalDateTime nextValidWorkdaySlot() {
        LocalDateTime candidate = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        while (candidate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            candidate = candidate.plusDays(1);
        }
        return candidate;
    }

    @Test
    void validate_scheduledAtInFutureWorkdayWorkingHours_noViolations() {
        // Arrange
        AppointmentDto dto = dtoWithScheduledAt(nextValidWorkdaySlot());

        // Act
        Set<ConstraintViolation<AppointmentDto>> violations = validator.validate(dto);

        // Assert
        assertThat(violations).isEmpty();
    }

    @Test
    void validate_scheduledAtInThePast_hasViolationWithClearMessage() {
        // Arrange
        AppointmentDto dto = dtoWithScheduledAt(LocalDateTime.now().minusDays(1));

        // Act
        Set<ConstraintViolation<AppointmentDto>> violations = validator.validate(dto);

        // Assert
        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .anyMatch(msg -> msg.contains("майбутньому"));
    }

    @Test
    void validate_scheduledAtOnSunday_hasViolationWithClearMessage() {
        // Arrange: знаходимо найближчу неділю в майбутньому о 10:00
        LocalDateTime sunday = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0);
        while (sunday.getDayOfWeek() != DayOfWeek.SUNDAY) {
            sunday = sunday.plusDays(1);
        }
        AppointmentDto dto = dtoWithScheduledAt(sunday);

        // Act
        Set<ConstraintViolation<AppointmentDto>> violations = validator.validate(dto);

        // Assert
        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .anyMatch(msg -> msg.contains("неділю"));
    }

    @Test
    void validate_scheduledAtOutsideWorkingHours_hasViolationWithClearMessage() {
        // Arrange: беремо правильний робочий день, але 22:00 - поза графіком
        LocalDateTime lateHour = nextValidWorkdaySlot().withHour(22);
        AppointmentDto dto = dtoWithScheduledAt(lateHour);

        // Act
        Set<ConstraintViolation<AppointmentDto>> violations = validator.validate(dto);

        // Assert
        assertThat(violations).isNotEmpty();
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .anyMatch(msg -> msg.contains("08:00") && msg.contains("18:00"));
    }
}
