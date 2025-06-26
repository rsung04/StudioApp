package com.example.studioapp_api.dto;

import lombok.Data;
import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.FutureOrPresent; // For start_date if applicable
import jakarta.validation.constraints.Size;

@Data
public class TermRequestDTO {
    @NotBlank(message = "Term name is required")
    @Size(max = 100, message = "Term name cannot exceed 100 characters")
    private String name;

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    private boolean isActiveForPlanning = false; // Default, not usually validated as required

    @NotNull(message = "Organization ID is required")
    private Integer organizationId; // To link to an existing organization

    private Integer studioLocationId; // Optional, to link to a specific studio location
}