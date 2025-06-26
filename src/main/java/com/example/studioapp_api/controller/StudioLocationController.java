package com.example.studioapp_api.controller;

import com.example.studioapp_api.dto.StudioLocationRequestDTO;
import com.example.studioapp_api.dto.StudioLocationResponseDTO;
import com.example.studioapp_api.service.StudioLocationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class StudioLocationController {

    private final StudioLocationService studioLocationService;

    @Autowired
    public StudioLocationController(StudioLocationService studioLocationService) {
        this.studioLocationService = studioLocationService;
    }

    @PostMapping("/organizations/{organizationId}/studiolocations")
    public ResponseEntity<StudioLocationResponseDTO> createStudioLocation(
            @PathVariable Integer organizationId,
            @Valid @RequestBody StudioLocationRequestDTO requestDTO) {
        
        // Ensure DTO's organizationId is consistent with path variable
        if (requestDTO.getOrganizationId() == null) {
            requestDTO.setOrganizationId(organizationId);
        } else if (!requestDTO.getOrganizationId().equals(organizationId)) {
            // Consider throwing an IllegalArgumentException or returning ResponseEntity.badRequest()
             throw new IllegalArgumentException("Organization ID in path ("+ organizationId +
                                               ") does not match Organization ID in request body (" + 
                                               requestDTO.getOrganizationId() + ").");
        }
        StudioLocationResponseDTO createdLocation = studioLocationService.createStudioLocation(requestDTO);
        return new ResponseEntity<>(createdLocation, HttpStatus.CREATED);
    }

    @GetMapping("/organizations/{organizationId}/studiolocations")
    public ResponseEntity<List<StudioLocationResponseDTO>> getStudioLocationsByOrganization(
            @PathVariable Integer organizationId) {
        List<StudioLocationResponseDTO> locations = studioLocationService.getStudioLocationsByOrganization(organizationId);
        return ResponseEntity.ok(locations);
    }

    @GetMapping("/studiolocations/{locationId}")
    public ResponseEntity<StudioLocationResponseDTO> getStudioLocationById(@PathVariable Integer locationId) {
        StudioLocationResponseDTO location = studioLocationService.getStudioLocationById(locationId);
        return ResponseEntity.ok(location);
    }

    @PutMapping("/studiolocations/{locationId}")
    public ResponseEntity<StudioLocationResponseDTO> updateStudioLocation(
            @PathVariable Integer locationId,
            @Valid @RequestBody StudioLocationRequestDTO requestDTO) {
        StudioLocationResponseDTO updatedLocation = studioLocationService.updateStudioLocation(locationId, requestDTO);
        return ResponseEntity.ok(updatedLocation);
    }

    @DeleteMapping("/studiolocations/{locationId}")
    public ResponseEntity<Void> deleteStudioLocation(@PathVariable Integer locationId) {
        studioLocationService.deleteStudioLocation(locationId);
        return ResponseEntity.noContent().build();
    }
}