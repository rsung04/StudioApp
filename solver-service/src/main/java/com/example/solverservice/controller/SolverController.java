package com.example.solverservice.controller;

import com.example.solverservice.solver.DanceTimetableSolver;
import com.example.solverservice.solver.SolverInput;
import com.example.solverservice.solver.SolverOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/solver") // Base path for solver-related endpoints
public class SolverController {

    private static final Logger logger = LoggerFactory.getLogger(SolverController.class);
    private final DanceTimetableSolver danceTimetableSolver;

    @Autowired
    public SolverController(DanceTimetableSolver danceTimetableSolver) {
        this.danceTimetableSolver = danceTimetableSolver;
    }

    @PostMapping("/solve")
    public ResponseEntity<SolverOutput> solveTimetable(@RequestBody SolverInput solverInput) {
        logger.info("Received direct HTTP request to /solve");
        try {
            SolverOutput solverOutput = danceTimetableSolver.executeSolve(solverInput);
            if (solverOutput.isSolveSuccess()) {
                return ResponseEntity.ok(solverOutput);
            } else {
                // Consider returning a more specific error status code based on the failure
                return ResponseEntity.status(500).body(solverOutput);
            }
        } catch (Exception e) {
            logger.error("Error during direct HTTP solve request: ", e);
            SolverOutput errorOutput = SolverOutput.builder()
                                        .solveSuccess(false)
                                        .statusMessage("Internal server error: " + e.getMessage())
                                        .consoleLog(e.toString())
                                        .build();
            return ResponseEntity.status(500).body(errorOutput);
        }
    }
}