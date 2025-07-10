package com.example.solverservice.controller;

import com.example.solverservice.solver.DanceTimetableSolver;
import com.example.solverservice.solver.SolverInput;
import com.example.solverservice.solver.SolverOutput;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@WebMvcTest(SolverController.class)
public class SolverControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DanceTimetableSolver danceTimetableSolver;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testSolveTimetable_Success() throws Exception {
        SolverInput input = SolverInput.builder().build(); // Create a basic input
        SolverOutput successOutput = SolverOutput.builder()
                .solveSuccess(true)
                .statusMessage("Successfully solved")
                .stageAResults(new ArrayList<>())
                .consoleLog("")
                .build();

        when(danceTimetableSolver.executeSolve(any(SolverInput.class))).thenReturn(successOutput);

        mockMvc.perform(post("/solver/solve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(content().json(objectMapper.writeValueAsString(successOutput)));
    }

    @Test
    void testSolveTimetable_SolverError() throws Exception {
        SolverInput input = SolverInput.builder().build(); // Create a basic input
        SolverOutput errorOutput = SolverOutput.builder()
                .solveSuccess(false)
                .statusMessage("Solver failed internally")
                .stageAResults(new ArrayList<>())
                .consoleLog("Error details")
                .build();

        when(danceTimetableSolver.executeSolve(any(SolverInput.class))).thenReturn(errorOutput);

        mockMvc.perform(post("/solver/solve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().json(objectMapper.writeValueAsString(errorOutput)));
    }

    @Test
    void testSolveTimetable_Exception() throws Exception {
        SolverInput input = SolverInput.builder().build(); // Create a basic input
        String exceptionMessage = "Unexpected runtime exception";

        when(danceTimetableSolver.executeSolve(any(SolverInput.class))).thenThrow(new RuntimeException(exceptionMessage));

        SolverOutput expectedErrorOutput = SolverOutput.builder()
                .solveSuccess(false)
                .statusMessage("Internal server error: " + exceptionMessage)
                .consoleLog(new RuntimeException(exceptionMessage).toString())
                .build();

        mockMvc.perform(post("/solver/solve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().json(objectMapper.writeValueAsString(expectedErrorOutput)));
    }
}
