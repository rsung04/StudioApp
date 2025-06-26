package com.example.solverservice.dto;

import com.example.solverservice.solver.SolverInput;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PubSubSolveRequest {
    private String jobId;
    private SolverInput solverInput;
}