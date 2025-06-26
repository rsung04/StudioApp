package com.example.studioapp_api.dto;

import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class InstructorPriorityRequestResponseDTO {
    private Integer id;
    private Integer instructorId;
    private String instructorName; // Denormalized
    private Integer termId;
    private String termName; // Denormalized
    private Integer studioLocationId; // Optional
    private String studioLocationName; // Optional, Denormalized
    private Integer relativePriority;
    private Integer blockLengthHours;
    private String description;
    private boolean isActive;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}