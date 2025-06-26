package com.example.studioapp_api.controller;

import com.example.studioapp_api.dto.InstructorAvailabilitySlotRequestDTO;
import com.example.studioapp_api.dto.InstructorAvailabilitySlotResponseDTO;
import com.example.studioapp_api.service.InstructorAvailabilitySlotService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class InstructorAvailabilitySlotController {

    private final InstructorAvailabilitySlotService availabilityService;

    public InstructorAvailabilitySlotController(InstructorAvailabilitySlotService availabilityService) {
        this.availabilityService = availabilityService;
    }

    // Add an availability slot for an instructor
    @PostMapping("/instructors/{instructorId}/availability-slots")
    public ResponseEntity<InstructorAvailabilitySlotResponseDTO> addAvailabilitySlot(
            @PathVariable Integer instructorId,
            @Valid @RequestBody InstructorAvailabilitySlotRequestDTO requestDTO) {
        // The service will use instructorId from path and validate/use from DTO
        InstructorAvailabilitySlotResponseDTO createdSlot = availabilityService.addAvailabilitySlot(instructorId, requestDTO);
        return new ResponseEntity<>(createdSlot, HttpStatus.CREATED);
    }

    // Get all availability slots for a specific instructor
    @GetMapping("/instructors/{instructorId}/availability-slots")
    public ResponseEntity<List<InstructorAvailabilitySlotResponseDTO>> getAvailabilitySlotsForInstructor(
            @PathVariable Integer instructorId) {
        List<InstructorAvailabilitySlotResponseDTO> slots = availabilityService.getAvailabilitySlotsForInstructor(instructorId);
        return ResponseEntity.ok(slots);
    }

    // Get a specific availability slot by its own ID
    @GetMapping("/availability-slots/{slotId}")
    public ResponseEntity<InstructorAvailabilitySlotResponseDTO> getAvailabilitySlotById(@PathVariable Integer slotId) {
        InstructorAvailabilitySlotResponseDTO slot = availabilityService.getAvailabilitySlotById(slotId);
        return ResponseEntity.ok(slot);
    }

    // Update a specific availability slot
    @PutMapping("/availability-slots/{slotId}")
    public ResponseEntity<InstructorAvailabilitySlotResponseDTO> updateAvailabilitySlot(
            @PathVariable Integer slotId,
            @Valid @RequestBody InstructorAvailabilitySlotRequestDTO requestDTO) {
        InstructorAvailabilitySlotResponseDTO updatedSlot = availabilityService.updateAvailabilitySlot(slotId, requestDTO);
        return ResponseEntity.ok(updatedSlot);
    }

    // Delete a specific availability slot
    @DeleteMapping("/availability-slots/{slotId}")
    public ResponseEntity<Void> deleteAvailabilitySlot(@PathVariable Integer slotId) {
        availabilityService.deleteAvailabilitySlot(slotId);
        return ResponseEntity.noContent().build();
    }
}