package com.example.studioapp_api.controller;

import com.example.studioapp_api.dto.ClassSessionRequirementRequestDTO;
import com.example.studioapp_api.dto.ClassSessionRequirementResponseDTO;
import com.example.studioapp_api.service.ClassSessionRequirementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ClassSessionRequirementController {

    private final ClassSessionRequirementService requirementService;

    public ClassSessionRequirementController(ClassSessionRequirementService requirementService) {
        this.requirementService = requirementService;
    }

    // Create a class session requirement (termId, classDefinitionId in DTO)
    @PostMapping("/class-session-requirements")
    public ResponseEntity<ClassSessionRequirementResponseDTO> createRequirement(
            @Valid @RequestBody ClassSessionRequirementRequestDTO requestDTO) {
        ClassSessionRequirementResponseDTO createdReq = requirementService.createRequirement(requestDTO);
        return new ResponseEntity<>(createdReq, HttpStatus.CREATED);
    }

    // Get all requirements for a specific term
    @GetMapping("/terms/{termId}/class-session-requirements")
    public ResponseEntity<List<ClassSessionRequirementResponseDTO>> getRequirementsByTerm(
            @PathVariable Integer termId) {
        List<ClassSessionRequirementResponseDTO> requirements = requirementService.getRequirementsByTerm(termId);
        return ResponseEntity.ok(requirements);
    }

    // Get a specific requirement by its ID
    @GetMapping("/class-session-requirements/{requirementId}")
    public ResponseEntity<ClassSessionRequirementResponseDTO> getRequirementById(
            @PathVariable Integer requirementId) {
        ClassSessionRequirementResponseDTO requirement = requirementService.getRequirementById(requirementId);
        return ResponseEntity.ok(requirement);
    }

    // Update a specific requirement
    @PutMapping("/class-session-requirements/{requirementId}")
    public ResponseEntity<ClassSessionRequirementResponseDTO> updateRequirement(
            @PathVariable Integer requirementId,
            @Valid @RequestBody ClassSessionRequirementRequestDTO requestDTO) {
        ClassSessionRequirementResponseDTO updatedReq = requirementService.updateRequirement(requirementId, requestDTO);
        return ResponseEntity.ok(updatedReq);
    }

    // Delete a specific requirement
    @DeleteMapping("/class-session-requirements/{requirementId}")
    public ResponseEntity<Void> deleteRequirement(@PathVariable Integer requirementId) {
        requirementService.deleteRequirement(requirementId);
        return ResponseEntity.noContent().build();
    }
}