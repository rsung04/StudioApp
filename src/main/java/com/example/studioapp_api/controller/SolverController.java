package com.example.studioapp_api.controller;

import com.example.studioapp_api.dto.SolveRequestDTO;
import com.example.studioapp_api.dto.SolverJobResponseDTO;
import com.example.studioapp_api.service.SolverService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        // For now, this is synchronous and returns immediately after queuing (simulated)
        // If truly async, 202 Accepted might be more appropriate
        return ResponseEntity.ok(jobResponse); 
    }

    // TODO: Add GET endpoints for /jobs/{jobId}/status and /jobs/{jobId}/results later
}