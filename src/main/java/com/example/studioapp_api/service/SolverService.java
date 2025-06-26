package com.example.studioapp_api.service;

import com.example.studioapp_api.dto.LockedBlockDTO; // Example result DTO
import com.example.studioapp_api.dto.SolveRequestDTO;
import com.example.studioapp_api.dto.SolverJobResponseDTO;
// Import other DTOs for full timetable results later

import java.util.List; // For list of locked blocks

public interface SolverService {

    /**
     * Triggers the timetable solving process.
     * This will eventually be asynchronous.
     *
     * @param solveRequestDTO Parameters for the solve run.
     * @return A response indicating the job has been submitted.
     */
    SolverJobResponseDTO triggerSolver(SolveRequestDTO solveRequestDTO);

    /**
     * Retrieves the status of a given solver job.
     * @param jobId The ID of the solver job.
     * @return Current status and information about the job.
     */
    SolverJobResponseDTO getJobStatus(String jobId);

    /**
     * Retrieves the results of Stage A (locked blocks) for a completed job.
     * @param jobId The ID of the solver job.
     * @return A list of locked blocks.
     */
    List<LockedBlockDTO> getStageAResults(String jobId); // Example

    // We'll add methods for getting Stage B results / full timetable later
    // List<ScheduledEventResponseDTO> getFullTimetableResults(String jobId);
}