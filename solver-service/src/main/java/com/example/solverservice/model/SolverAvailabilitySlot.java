package com.example.solverservice.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolverAvailabilitySlot {
    private Integer id; // Can be useful for debugging or mapping back if needed
    private DayOfWeek dayOfWeek; // Using java.time.DayOfWeek
    private LocalTime startTime;
    private LocalTime endTime;
    // No need for instructor reference here, it will be part of SolverInstructor
}