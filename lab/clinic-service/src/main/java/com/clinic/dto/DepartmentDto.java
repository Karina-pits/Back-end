package com.clinic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentDto {
    private Long id;

    @NotBlank(message = "Назва відділення обов'язкова")
    @Size(max = 150)
    private String name;

    @Size(max = 500)
    private String description;
}
