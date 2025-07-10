package com.example.studioapp_api.controller;

import com.example.studioapp_api.dto.SolveRequestDTO;
import com.example.studioapp_api.dto.SolverJobResponseDTO;
import com.example.studioapp_api.service.SolverService;
import jakarta.validation.Valid; // Ensuring this import is present, as per provided code
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*; // Ensuring this import is present

// Added missing imports from the original instructions for clarity, though your provided code was fine.
// No, the provided code already had @GetMapping and @PathVariable, this comment is incorrect.
// The user's provided code was already complete.

@RestController
@RequestMapping("/api/v1/solver")
public class SolverController {

    private final SolverService solverService;

    public SolverController(SolverService solverService) {
        this.solverService = solverService;
    }

    @PostMapping("/run")
    public ResponseEntity<SolverJobResponseDTO> triggerSolver(@Valid @RequestBody SolveRequestDTO solveRequestDTO) {
        SolverJobResponseDTO jobResponse = solverService.triggerSolver(solveRequestDTO);

        // Return 202 Accepted, the correct HTTP status for starting an async task.
        return ResponseEntity.accepted().body(jobResponse);
    }

    // This is the new endpoint for clients to check the job status.
    @GetMapping("/status/{jobId}")
    public ResponseEntity<SolverJobResponseDTO> getJobStatus(@PathVariable String jobId) {
        SolverJobResponseDTO jobStatus = solverService.getJobStatus(jobId);
        return ResponseEntity.ok(jobStatus);
    }
}
