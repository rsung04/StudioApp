package com.example.solverservice.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolverClassSessionRequirement {
    private Integer id;
    private SolverClassDefinition classDefinition;
    private int requiredSessions;
    private SolverStudioLocation studioLocation; // Preferred location, if any
    private boolean active;
    // Add other fields if Stage B logic requires them.
}