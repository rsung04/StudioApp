package com.example.studioapp_api.controller;

import com.example.studioapp_api.dto.SolveRequestDTO;
import com.example.studioapp_api.dto.SolverJobResponseDTO;
import com.example.studioapp_api.service.SolverService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/solver")
public class SolverController {

    private final SolverService solverService;

    public SolverController(SolverService solverService) {
        this.solverService = solverService;
    }

    @PostMapping("/run")
    public ResponseEntity<SolverJobResponseDTO> triggerSolver(
            @Valid @RequestBody SolveRequestDTO solveRequestDTO) {
        SolverJobResponseDTO jobResponse = solverService.triggerSolver(solveRequestDTO);
        
        // Use 202 Accepted for asynchronous job submission
        return ResponseEntity.accepted().body(jobResponse); 
    }

    // NEW ENDPOINT
    @GetMapping("/status/{jobId}")
    public ResponseEntity<SolverJobResponseDTO> getJobStatus(@PathVariable String jobId) {
        SolverJobResponseDTO jobStatus = solverService.getJobStatus(jobId);
        return ResponseEntity.ok(jobStatus);
    }
}