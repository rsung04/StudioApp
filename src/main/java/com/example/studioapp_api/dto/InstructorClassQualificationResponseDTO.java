package com.example.studioapp_api.dto;

import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class InstructorClassQualificationResponseDTO {
    private Integer instructorId;
    private String instructorName; // Denormalized

    private Integer classDefinitionId;
    private String classDefinitionName; // Denormalized
    private String classDefinitionCode; // Denormalized

    private String notes;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}