package com.example.studioapp_api.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size; // For description

@Data
public class InstructorPriorityRequestRequestDTO {

    @NotNull(message = "Instructor ID is required.")
    private Integer instructorId;

    @NotNull(message = "Term ID is required.")
    private Integer termId;

    private Integer studioLocationId; // Optional

    @NotNull(message = "Relative priority is required.")
    @Min(value = 1, message = "Relative priority must be at least 1.")
    private Integer relativePriority = 1; // DTO default, but still validate if sent

    @NotNull(message = "Block length in hours is required.")
    @Positive(message = "Block length in hours must be positive.")
    private Integer blockLengthHours;

    @Size(max = 500, message = "Description cannot exceed 500 characters.") // Example size limit
    private String description; // Optional

    private boolean isActive = true; // DTO default
}