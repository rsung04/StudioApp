package com.example.studioapp_api.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
// Potentially add start/end dates if the solve isn't just for a full term
// or specific week numbers within a term.

@Data
public class SolveRequestDTO {

    @NotNull(message = "Organization ID is required for the solve request.")
    private Integer organizationId;

    @NotNull(message = "Term ID is required for the solve request.")
    private Integer termId;

    // Optional: If a solve is specific to one location within an organization for a given term
    private Integer studioLocationId; 

    // Optional: Define which stages to run, e.g., "STAGE_A_ONLY", "STAGE_B_ONLY", "FULL_SOLVE"
    // For now, let's assume a full solve. Could be an enum later.
    private String solveMode; // Example: "FULL", "STAGE_A" 

    // Optional: Force re-running Stage A even if cached results exist (for later use)
    private boolean forceRunStageA = false; 
}