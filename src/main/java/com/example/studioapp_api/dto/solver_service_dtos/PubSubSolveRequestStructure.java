package com.example.studioapp_api.dto.solver_service_dtos;

import com.example.studioapp_api.mapper.SolverInputMapper.LocalSolverServiceInput;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PubSubSolveRequestStructure {
    private String jobId;
    private LocalSolverServiceInput solverInput;
}