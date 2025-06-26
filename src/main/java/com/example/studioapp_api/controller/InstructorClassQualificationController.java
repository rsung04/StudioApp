package com.example.studioapp_api.controller;

import com.example.studioapp_api.dto.InstructorClassQualificationRequestDTO;
import com.example.studioapp_api.dto.InstructorClassQualificationResponseDTO;
import com.example.studioapp_api.dto.UpdateNotesRequestDTO; // Simple DTO for updating notes
import com.example.studioapp_api.service.InstructorClassQualificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class InstructorClassQualificationController {

    private final InstructorClassQualificationService qualificationService;

    public InstructorClassQualificationController(InstructorClassQualificationService qualificationService) {
        this.qualificationService = qualificationService;
    }

    @PostMapping("/qualifications")
    public ResponseEntity<InstructorClassQualificationResponseDTO> addQualification(
            @Valid @RequestBody InstructorClassQualificationRequestDTO requestDTO) {
        InstructorClassQualificationResponseDTO created = qualificationService.addQualification(requestDTO);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    // Get all qualifications for a specific instructor
    @GetMapping("/instructors/{instructorId}/qualifications")
    public ResponseEntity<List<InstructorClassQualificationResponseDTO>> getQualificationsByInstructor(
            @PathVariable Integer instructorId) {
        List<InstructorClassQualificationResponseDTO> qualifications = qualificationService.getQualificationsByInstructor(instructorId);
        return ResponseEntity.ok(qualifications);
    }

    // Get all instructors qualified for a specific class definition
    @GetMapping("/classdefinitions/{classDefinitionId}/qualified-instructors")
    public ResponseEntity<List<InstructorClassQualificationResponseDTO>> getQualificationsByClassDefinition(
            @PathVariable Integer classDefinitionId) {
        List<InstructorClassQualificationResponseDTO> qualifications = qualificationService.getQualificationsByClassDefinition(classDefinitionId);
        return ResponseEntity.ok(qualifications);
    }
    
    // DTO for updating just the notes
    // Could be nested class or separate file com.example.studioapp_api.dto.UpdateNotesRequestDTO
    // @lombok.Data static class UpdateNotesRequestDTO { private String notes; }

    @PutMapping("/instructors/{instructorId}/classdefinitions/{classDefinitionId}/qualifications")
    public ResponseEntity<InstructorClassQualificationResponseDTO> updateQualificationNotes(
            @PathVariable Integer instructorId,
            @PathVariable Integer classDefinitionId,
            @Valid @RequestBody UpdateNotesRequestDTO notesDTO) { // RequestBody needs a DTO
        InstructorClassQualificationResponseDTO updated = qualificationService.updateQualificationNotes(instructorId, classDefinitionId, notesDTO.getNotes());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/instructors/{instructorId}/classdefinitions/{classDefinitionId}/qualifications")
    public ResponseEntity<Void> removeQualification(
            @PathVariable Integer instructorId,
            @PathVariable Integer classDefinitionId) {
        qualificationService.removeQualification(instructorId, classDefinitionId);
        return ResponseEntity.noContent().build();
    }
}