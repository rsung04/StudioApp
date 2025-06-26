package com.example.studioapp_api.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
public class TermResponseDTO {
    private Integer id;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean isActiveForPlanning;
    private Integer organizationId;
    private String organizationName; // Example: denormalized data for convenience
    private Integer studioLocationId;  // Can be null
    private String studioLocationName; // Can be null, example
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}