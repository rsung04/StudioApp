package com.example.solverservice.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolverPriorityRequest {
    private Integer id;
    private SolverInstructor instructor;
    private SolverStudioLocation studioLocation; // Preferred location for the request
    private int blockLengthHours;
    private boolean active; 
    // Add any other fields from the original InstructorPriorityRequest entity 
    // that DanceTimetableSolver's prepareStageAPriorityRequests method might need.
}