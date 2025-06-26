package com.example.solverservice.solver; // Updated package

import com.example.solverservice.dto.LockedBlockDTO; // Updated import
// We'll need a new DTO for Stage B scheduled classes, e.g., ScheduledClassDetailDTO
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolverOutput {
    private List<LockedBlockDTO> stageAResults;
    // private List<ScheduledClassDetailDTO> stageBResults; // To be defined
    private String consoleLog; // To capture any print statements from the solver for debugging
    private boolean solveSuccess;
    private String statusMessage;
}