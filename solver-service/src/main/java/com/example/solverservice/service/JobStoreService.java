package com.example.solverservice.service;

import com.example.solverservice.solver.SolverOutput;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

enum JobStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}

class JobResult {
    private JobStatus status;
    private SolverOutput output; // Null if not completed or failed before output generation
    private String errorMessage;

    public JobResult(JobStatus status) {
        this.status = status;
    }

    // Getters and Setters
    public JobStatus getStatus() { return status; }
    public void setStatus(JobStatus status) { this.status = status; }
    public SolverOutput getOutput() { return output; }
    public void setOutput(SolverOutput output) { this.output = output; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}

@Service
public class JobStoreService {
    // In-memory store. Replace with Firestore/Cloud SQL in production.
    private final Map<String, JobResult> jobResults = new ConcurrentHashMap<>();

    public void initJob(String jobId) {
        jobResults.put(jobId, new JobResult(JobStatus.PENDING));
    }

    public void updateJobStatus(String jobId, JobStatus status) {
        JobResult result = jobResults.getOrDefault(jobId, new JobResult(status));
        result.setStatus(status);
        jobResults.put(jobId, result);
    }

    public void storeJobOutput(String jobId, SolverOutput output) {
        JobResult result = jobResults.getOrDefault(jobId, new JobResult(JobStatus.COMPLETED));
        result.setStatus(JobStatus.COMPLETED);
        result.setOutput(output);
        jobResults.put(jobId, result);
    }

    public void storeJobError(String jobId, String errorMessage) {
        JobResult result = jobResults.getOrDefault(jobId, new JobResult(JobStatus.FAILED));
        result.setStatus(JobStatus.FAILED);
        result.setErrorMessage(errorMessage);
        jobResults.put(jobId, result);
    }

    public JobResult getJobResult(String jobId) {
        return jobResults.get(jobId);
    }
}