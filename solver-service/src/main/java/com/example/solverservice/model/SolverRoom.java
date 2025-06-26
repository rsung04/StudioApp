package com.example.solverservice.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

// Assuming OperatingHoursSpan will be in com.example.solverservice.solver package
import com.example.solverservice.solver.OperatingHoursSpan; 

import java.time.DayOfWeek;
import java.util.Map;
// We might not need operating hours directly on the room if effectiveDayWindows in SolverInput covers it.
// However, DanceTimetableSolver's current logic for room assignment in priority requests might use it.
// For now, let's include a simplified representation or assume it's handled by effectiveDayWindows.

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolverRoom {
    private Integer id;
    private String name;
    private SolverStudioLocation studioLocation; 
    // private Map<DayOfWeek, OperatingHoursSpan> operatingHours; // Or handled by effectiveDayWindows
}