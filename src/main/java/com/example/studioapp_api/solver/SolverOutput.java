package com.example.studioapp_api.solver;

import com.example.studioapp_api.dto.LockedBlockDTO; // Using existing DTO for Stage A results
// We'll need a new DTO for Stage B scheduled classes, e.g., ScheduledClassDetailDTO
import lombok.Data;
import java.util.List;

@Data
public class SolverOutput {
    private List<LockedBlockDTO> stageAResults;
    // private List<ScheduledClassDetailDTO> stageBResults; // To be defined
    private String consoleLog; // To capture any print statements from the solver for debugging
    private boolean solveSuccess;
    private String statusMessage;
}