package com.example.solverservice.dto; // Updated package

import lombok.Data;
import lombok.Builder;

@Data
@Builder
public class LockedBlockDTO {
    // These fields will correspond to what your solver's Stage A produces
    // and what your ScheduledEvent entity (or a simplified version) might store for these.
    private String instructorName;
    private Integer instructorId;
    
    private String roomName; // Or Studio Name from your original solver's context
    private Integer roomId;   // Or Studio/Room ID

    private String studioLocationName; // If applicable
    private Integer studioLocationId;  // If applicable
    
    private Integer startSlot; // The solver's internal slot index
    private Integer lengthSlots;

    // Optional: Human-readable start/end times, day of week
    private String dayOfWeek;
    private String startTime; // e.g., "10:00"
    private String endTime;   // e.g., "12:00"
}