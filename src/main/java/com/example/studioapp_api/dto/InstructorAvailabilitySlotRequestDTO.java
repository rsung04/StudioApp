package com.example.studioapp_api.dto;

import com.example.studioapp_api.entity.DayOfWeekEnum;
import lombok.Data;
import jakarta.validation.constraints.NotNull; // For validation

import java.time.LocalTime;

@Data
public class InstructorAvailabilitySlotRequestDTO {
    
    // While instructorId is often from the path for create,
    // if the DTO is also used for updates where the slot is identified by its own ID,
    // this field might be part of the update payload.
    // Let's assume it's required if present in the DTO for an update scenario or a non-nested create.
    // For nested create POST /instructors/{instructorId}/availability-slots,
    // the controller/service typically uses the path instructorId.
    // @NotNull(message = "Instructor ID is required in the request body if not inferred from path.")
    private Integer instructorId; // Making this optional for now, controller logic handles it

    private Integer studioLocationId; // Optional

    @NotNull(message = "Day of week is required.")
    private DayOfWeekEnum dayOfWeek;

    @NotNull(message = "Start time is required.")
    private LocalTime startTime;

    @NotNull(message = "End time is required.")
    private LocalTime endTime;
}