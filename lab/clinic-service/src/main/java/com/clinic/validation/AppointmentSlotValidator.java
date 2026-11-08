package com.clinic.validation;

import com.clinic.dto.AppointmentDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

/**
 * Реалізація правила @ValidAppointmentSlot.
 *
 * AC1: Bean Validation сам знаходить і викликає цей клас через
 * @Constraint(validatedBy = AppointmentSlotValidator.class) в анотації —
 * ніде в коді застосунку ми не створюємо "new AppointmentSlotValidator()" вручну.
 */
public class AppointmentSlotValidator implements ConstraintValidator<ValidAppointmentSlot, AppointmentDto> {

    private static final int WORKDAY_START_HOUR = 8;
    private static final int WORKDAY_END_HOUR = 18;

    @Override
    public boolean isValid(AppointmentDto dto, ConstraintValidatorContext context) {
        if (dto == null || dto.getScheduledAt() == null) {
            // Відсутність значення - відповідальність окремого @NotNull на полі,
            // це правило перевіряє лише КОРЕКТНІСТЬ уже заповненого значення.
            return true;
        }

        LocalDateTime scheduledAt = dto.getScheduledAt();
        context.disableDefaultConstraintViolation();

        if (!scheduledAt.isAfter(LocalDateTime.now())) {
            context.buildConstraintViolationWithTemplate(
                            "Дата й час прийому мають бути в майбутньому")
                    .addPropertyNode("scheduledAt")
                    .addConstraintViolation();
            return false;
        }

        if (scheduledAt.getDayOfWeek() == DayOfWeek.SUNDAY) {
            context.buildConstraintViolationWithTemplate(
                            "Клініка не працює в неділю - оберіть інший день")
                    .addPropertyNode("scheduledAt")
                    .addConstraintViolation();
            return false;
        }

        int hour = scheduledAt.getHour();
        if (hour < WORKDAY_START_HOUR || hour >= WORKDAY_END_HOUR) {
            context.buildConstraintViolationWithTemplate(
                            "Запис можливий лише з 08:00 до 18:00")
                    .addPropertyNode("scheduledAt")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}
