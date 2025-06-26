package com.example.studioapp_api.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Data
public class ClassSessionRequirementRequestDTO {
    @NotNull(message = "Term ID is required.")
    private Integer termId;

    @NotNull(message = "Class Definition ID is required.")
    private Integer classDefinitionId;

    private Integer studioLocationId; // Optional

    @NotNull(message = "Sessions per week is required.")
    @Positive(message = "Sessions per week must be positive.")
    private Integer sessionsPerWeek;

    private boolean isActive = true;
}