package com.example.studioapp_api.dto;

import com.example.studioapp_api.entity.DayOfWeekEnum;
import lombok.Data;
import jakarta.validation.constraints.NotNull; // For validation

import java.time.LocalTime;

@Data
public class RoomOperatingHoursRequestDTO {
    
    // roomId will typically come from the path variable in the controller
    // but can be included for other use cases if needed.
    // For nested creation, the path variable is preferred to set it in the service.
    // @NotNull(message = "Room ID is required") 
    private Integer roomId; 

    @NotNull(message = "Day of week is required")
    private DayOfWeekEnum dayOfWeek;

    @NotNull(message = "Start time is required")
    private LocalTime startTime; // Spring Boot will parse "HH:mm" or "HH:mm:ss"

    @NotNull(message = "End time is required")
    private LocalTime endTime;
}