package com.example.studioapp_api.controller;

import com.example.studioapp_api.dto.TermRequestDTO;
import com.example.studioapp_api.dto.TermResponseDTO;
import com.example.studioapp_api.service.TermService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid; // Uncomment if you add validation

import java.util.List;

@RestController
@RequestMapping("/api/v1") // You can choose a more specific base like /api/v1/terms if all methods relate to terms
public class TermController {

    private final TermService termService;

    @Autowired
    public TermController(TermService termService) {
        this.termService = termService;
    }

    // Create a new term
    // The TermRequestDTO will contain organizationId and optional studioLocationId
    @PostMapping("/terms")
    public ResponseEntity<TermResponseDTO> createTerm(@Valid @RequestBody TermRequestDTO termRequestDTO) {
        TermResponseDTO createdTerm = termService.createTerm(termRequestDTO);
        return new ResponseEntity<>(createdTerm, HttpStatus.CREATED);
    }

    // Get all terms for a specific organization
    @GetMapping("/organizations/{organizationId}/terms")
    public ResponseEntity<List<TermResponseDTO>> getTermsByOrganization(@PathVariable Integer organizationId) {
        List<TermResponseDTO> terms = termService.getTermsByOrganization(organizationId);
        return ResponseEntity.ok(terms);
    }

    // Get a specific term by its ID
    @GetMapping("/terms/{termId}")
    public ResponseEntity<TermResponseDTO> getTermById(@PathVariable Integer termId) {
        TermResponseDTO term = termService.getTermById(termId);
        return ResponseEntity.ok(term);
    }

    // ... (existing constructor, createTerm, getTermsByOrganization, getTermById methods are above this)
    // Make sure to remove the "TODO" comments if they were there.

    @PutMapping("/terms/{termId}")
    public ResponseEntity<TermResponseDTO> updateTerm(
            @PathVariable Integer termId,
            @Valid @RequestBody TermRequestDTO termRequestDTO) { // Add @Valid if using validation
        TermResponseDTO updatedTerm = termService.updateTerm(termId, termRequestDTO);
        return ResponseEntity.ok(updatedTerm);
    }

    @DeleteMapping("/terms/{termId}")
    public ResponseEntity<Void> deleteTerm(@PathVariable Integer termId) {
        termService.deleteTerm(termId);
        return ResponseEntity.noContent().build();
    }
}