package com.example.studioapp_api.controller;

import com.example.studioapp_api.dto.InstructorRequestDTO;
import com.example.studioapp_api.dto.InstructorResponseDTO;
import com.example.studioapp_api.service.InstructorService;
import jakarta.validation.Valid; // <<<--- ADD THIS IMPORT
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class InstructorController {

    private final InstructorService instructorService;

    @Autowired
    public InstructorController(InstructorService instructorService) {
        this.instructorService = instructorService;
    }

    @PostMapping("/organizations/{organizationId}/instructors")
    public ResponseEntity<InstructorResponseDTO> createInstructor(
            @PathVariable Integer organizationId,
            @Valid @RequestBody InstructorRequestDTO requestDTO) { // <<<--- ADDED @Valid
        
        if (requestDTO.getOrganizationId() == null) {
            requestDTO.setOrganizationId(organizationId);
        } else if (!requestDTO.getOrganizationId().equals(organizationId)) {
             throw new IllegalArgumentException("Organization ID in path ("+ organizationId +
                                               ") does not match Organization ID in request body (" + 
                                               requestDTO.getOrganizationId() + ").");
        }
        InstructorResponseDTO createdInstructor = instructorService.createInstructor(requestDTO);
        return new ResponseEntity<>(createdInstructor, HttpStatus.CREATED);
    }

    @GetMapping("/organizations/{organizationId}/instructors")
    public ResponseEntity<List<InstructorResponseDTO>> getInstructorsByOrganization(@PathVariable Integer organizationId) {
        List<InstructorResponseDTO> instructors = instructorService.getInstructorsByOrganization(organizationId);
        return ResponseEntity.ok(instructors);
    }

    @GetMapping("/instructors/{instructorId}")
    public ResponseEntity<InstructorResponseDTO> getInstructorById(@PathVariable Integer instructorId) {
        InstructorResponseDTO instructor = instructorService.getInstructorById(instructorId);
        return ResponseEntity.ok(instructor);
    }

    @PutMapping("/instructors/{instructorId}")
    public ResponseEntity<InstructorResponseDTO> updateInstructor(
            @PathVariable Integer instructorId,
            @Valid @RequestBody InstructorRequestDTO requestDTO) { // <<<--- ADDED @Valid
        InstructorResponseDTO updatedInstructor = instructorService.updateInstructor(instructorId, requestDTO);
        return ResponseEntity.ok(updatedInstructor);
    }

    @DeleteMapping("/instructors/{instructorId}")
    public ResponseEntity<Void> deleteInstructor(@PathVariable Integer instructorId) {
        instructorService.deleteInstructor(instructorId);
        return ResponseEntity.noContent().build();
    }
}