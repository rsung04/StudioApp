package com.example.studioapp_api.dto;

import com.example.studioapp_api.entity.DayOfWeekEnum;
import lombok.Data;
import java.time.LocalTime;
import java.time.OffsetDateTime;

@Data
public class RoomOperatingHoursResponseDTO {
    private Integer id;
    private Integer roomId;
    private String roomName; // Denormalized
    private DayOfWeekEnum dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}