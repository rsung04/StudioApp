package com.example.studioapp_api.controller;

import com.example.studioapp_api.dto.InstructorPriorityRequestRequestDTO;
import com.example.studioapp_api.dto.InstructorPriorityRequestResponseDTO;
import com.example.studioapp_api.service.InstructorPriorityRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class InstructorPriorityRequestController {

    private final InstructorPriorityRequestService requestService;

    public InstructorPriorityRequestController(InstructorPriorityRequestService requestService) {
        this.requestService = requestService;
    }

    // Create a priority request (instructorId and termId will be in DTO)
    @PostMapping("/priority-requests")
    public ResponseEntity<InstructorPriorityRequestResponseDTO> createPriorityRequest(
            @Valid @RequestBody InstructorPriorityRequestRequestDTO requestDTO) {
        InstructorPriorityRequestResponseDTO createdRequest = requestService.createRequest(requestDTO);
        return new ResponseEntity<>(createdRequest, HttpStatus.CREATED);
    }

    // Get all priority requests for a specific instructor and term
    @GetMapping("/instructors/{instructorId}/terms/{termId}/priority-requests")
    public ResponseEntity<List<InstructorPriorityRequestResponseDTO>> getRequestsByInstructorAndTerm(
            @PathVariable Integer instructorId, @PathVariable Integer termId) {
        List<InstructorPriorityRequestResponseDTO> requests = requestService.getRequestsByInstructorAndTerm(instructorId, termId);
        return ResponseEntity.ok(requests);
    }

    // Get a specific priority request by its ID
    @GetMapping("/priority-requests/{requestId}")
    public ResponseEntity<InstructorPriorityRequestResponseDTO> getPriorityRequestById(@PathVariable Integer requestId) {
        InstructorPriorityRequestResponseDTO request = requestService.getRequestById(requestId);
        return ResponseEntity.ok(request);
    }

    // Update a specific priority request
    @PutMapping("/priority-requests/{requestId}")
    public ResponseEntity<InstructorPriorityRequestResponseDTO> updatePriorityRequest(
            @PathVariable Integer requestId,
            @Valid @RequestBody InstructorPriorityRequestRequestDTO requestDTO) {
        InstructorPriorityRequestResponseDTO updatedRequest = requestService.updateRequest(requestId, requestDTO);
        return ResponseEntity.ok(updatedRequest);
    }

    // Delete a specific priority request
    @DeleteMapping("/priority-requests/{requestId}")
    public ResponseEntity<Void> deletePriorityRequest(@PathVariable Integer requestId) {
        requestService.deleteRequest(requestId);
        return ResponseEntity.noContent().build();
    }
}