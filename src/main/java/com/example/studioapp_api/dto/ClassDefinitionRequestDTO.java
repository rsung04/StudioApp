package com.example.studioapp_api.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;      // For positive integers starting from 1
import jakarta.validation.constraints.Positive; // For strictly positive integers (>0)

@Data
public class ClassDefinitionRequestDTO {

    @NotBlank(message = "Class code is required and cannot be blank.")
    @Size(max = 100, message = "Class code cannot exceed 100 characters.")
    private String classCode;

    @NotBlank(message = "Class name is required and cannot be blank.")
    @Size(max = 255, message = "Class name cannot exceed 255 characters.")
    private String name;

    private String description; // Optional

    @NotNull(message = "Duration in minutes is required.")
    @Positive(message = "Duration in minutes must be positive.") // Ensures > 0
    private Integer durationMinutes;

    @Size(max = 50, message = "Level cannot exceed 50 characters.")
    private String level; // Optional

    @Min(value = 1, message = "Default studio capacity needed must be at least 1, if specified.")
    // @Positive would also work if 0 is not allowed. Min(1) is clearer for "at least 1".
    // This field is nullable in DB, so @NotNull is not used here. Validation applies if value is present.
    private Integer defaultStudioCapacityNeeded; // Optional

    @NotNull(message = "Organization ID is required.")
    private Integer organizationId;

    @NotNull(message = "Class Type ID is required.")
    private Integer classTypeId;
}