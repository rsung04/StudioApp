package com.example.studioapp_api.dto;

import lombok.Data;
import lombok.Builder; // For easy construction
import java.time.OffsetDateTime;

@Data
@Builder // Allows for .builder().field(value).build() pattern
public class SolverJobResponseDTO {
    private String jobId; // A unique ID for this solver run
    private String status; // e.g., "QUEUED", "RUNNING", "COMPLETED", "FAILED"
    private String message;
    private OffsetDateTime submittedAt;
    // Could add links to check status or results later
}