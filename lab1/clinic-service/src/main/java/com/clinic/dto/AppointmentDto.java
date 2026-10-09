package com.clinic.dto;

import com.clinic.entity.AppointmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import com.clinic.validation.ValidAppointmentSlot;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ValidAppointmentSlot
public class AppointmentDto {
    private Long id;

    @NotNull(message = "Пацієнт обов'язковий")
    private Long patientId;

    private String patientFullName;

    @NotNull(message = "Лікар обов'язковий")
    private Long doctorId;

    private String doctorFullName;

    @NotNull(message = "Дата та час обов'язкові")
    private LocalDateTime scheduledAt;

    private String reason;

    private AppointmentStatus status;
}
