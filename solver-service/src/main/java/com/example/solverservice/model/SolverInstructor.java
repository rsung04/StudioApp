package com.example.solverservice.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolverInstructor {
    private Integer id;
    private String name;
    private List<SolverAvailabilitySlot> availabilitySlots;
    // Add other fields if DanceTimetableSolver uses them, e.g., qualifications, preferences.
    // For now, keeping it simple based on current DanceTimetableSolver usage for availability.
}