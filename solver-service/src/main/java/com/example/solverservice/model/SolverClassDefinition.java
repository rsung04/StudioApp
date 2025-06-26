package com.example.solverservice.model;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolverClassDefinition {
    private Integer id;
    private String name;
    private int defaultDurationMinutes; // Assuming this is needed by Stage B
    // Add other relevant fields like difficulty, type, etc., if Stage B uses them.
}