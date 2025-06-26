package com.example.studioapp_api.dto;

import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class ClassDefinitionResponseDTO {
    private Integer id;
    private String classCode;
    private String name;
    private String description;
    private Integer durationMinutes;
    private String level;
    private Integer defaultStudioCapacityNeeded;

    private Integer organizationId;
    private String organizationName; // Denormalized

    private Integer classTypeId;
    private String classTypeName; // Denormalized

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}