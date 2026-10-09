package com.clinic.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DoctorDto {
    private Long id;

    @NotBlank(message = "Ім'я обов'язкове")
    private String firstName;

    @NotBlank(message = "Прізвище обов'язкове")
    private String lastName;

    @NotBlank(message = "Спеціалізація обов'язкова")
    private String specialization;

    @Email(message = "Некоректний email")
    private String email;

    private String keycloakUserId;

    @NotNull(message = "Відділення обов'язкове")
    private Long departmentId;

    private String departmentName;
}
