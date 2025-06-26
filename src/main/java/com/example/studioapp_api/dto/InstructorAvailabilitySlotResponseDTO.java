package com.example.studioapp_api.dto;

import com.example.studioapp_api.entity.DayOfWeekEnum;
import lombok.Data;
import java.time.LocalTime;
import java.time.OffsetDateTime;

@Data
public class InstructorAvailabilitySlotResponseDTO {
    private Integer id;
    private Integer instructorId;
    private String instructorName; // Denormalized
    private Integer studioLocationId; // Can be null
    private String studioLocationName; // Denormalized, can be null
    private DayOfWeekEnum dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}