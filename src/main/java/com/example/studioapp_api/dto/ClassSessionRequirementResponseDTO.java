package com.example.studioapp_api.dto;

import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class ClassSessionRequirementResponseDTO {
    private Integer id;
    private Integer termId;
    private String termName; // Denormalized
    private Integer classDefinitionId;
    private String classDefinitionName; // Denormalized
    private String classDefinitionCode; // Denormalized
    private Integer studioLocationId; // Optional
    private String studioLocationName; // Optional, Denormalized
    private Integer sessionsPerWeek;
    private boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}