package com.example.studioapp_api.service;

import com.example.studioapp_api.dto.InstructorAvailabilitySlotRequestDTO;
import com.example.studioapp_api.dto.InstructorAvailabilitySlotResponseDTO;
import com.example.studioapp_api.entity.Instructor;
import com.example.studioapp_api.entity.InstructorAvailabilitySlot;
import com.example.studioapp_api.entity.StudioLocation;
import com.example.studioapp_api.repository.InstructorAvailabilitySlotRepository;
import com.example.studioapp_api.repository.InstructorRepository;
import com.example.studioapp_api.repository.StudioLocationRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class InstructorAvailabilitySlotService {

    private final InstructorAvailabilitySlotRepository availabilityRepository;
    private final InstructorRepository instructorRepository;
    private final StudioLocationRepository studioLocationRepository;

    public InstructorAvailabilitySlotService(
            InstructorAvailabilitySlotRepository availabilityRepository,
            InstructorRepository instructorRepository,
            StudioLocationRepository studioLocationRepository) {
        this.availabilityRepository = availabilityRepository;
        this.instructorRepository = instructorRepository;
        this.studioLocationRepository = studioLocationRepository;
    }

    private InstructorAvailabilitySlotResponseDTO convertToDTO(InstructorAvailabilitySlot slot) {
        InstructorAvailabilitySlotResponseDTO dto = new InstructorAvailabilitySlotResponseDTO();
        dto.setId(slot.getId());
        dto.setInstructorId(slot.getInstructor().getId());
        dto.setInstructorName(slot.getInstructor().getName());
        if (slot.getStudioLocation() != null) {
            dto.setStudioLocationId(slot.getStudioLocation().getId());
            dto.setStudioLocationName(slot.getStudioLocation().getName());
        }
        dto.setDayOfWeek(slot.getDayOfWeek());
        dto.setStartTime(slot.getStartTime());
        dto.setEndTime(slot.getEndTime());
        dto.setCreatedAt(slot.getCreatedAt());
        dto.setUpdatedAt(slot.getUpdatedAt());
        return dto;
    }

    @Transactional
    public InstructorAvailabilitySlotResponseDTO addAvailabilitySlot(Integer instructorIdFromPath, InstructorAvailabilitySlotRequestDTO requestDTO) {
        // Use instructorIdFromPath if requestDTO.instructorId is null, or validate they match
        Integer effectiveInstructorId = requestDTO.getInstructorId() != null ? requestDTO.getInstructorId() : instructorIdFromPath;
        if (!effectiveInstructorId.equals(instructorIdFromPath) && requestDTO.getInstructorId() != null) {
             throw new IllegalArgumentException("Instructor ID in path does not match ID in request body.");
        }


        Instructor instructor = instructorRepository.findById(effectiveInstructorId)
            .orElseThrow(() -> new EntityNotFoundException("Instructor not found: " + effectiveInstructorId));

        StudioLocation studioLocation = null;
        if (requestDTO.getStudioLocationId() != null) {
            studioLocation = studioLocationRepository.findById(requestDTO.getStudioLocationId())
                .orElseThrow(() -> new EntityNotFoundException("StudioLocation not found: " + requestDTO.getStudioLocationId()));
            // Ensure studio location belongs to the same organization as the instructor
            if (!studioLocation.getOrganization().getId().equals(instructor.getOrganization().getId())) {
                throw new IllegalArgumentException("StudioLocation does not belong to the instructor's organization.");
            }
        }
        
        if (requestDTO.getEndTime().isBefore(requestDTO.getStartTime()) || requestDTO.getEndTime().equals(requestDTO.getStartTime())) {
            throw new IllegalArgumentException("End time must be after start time.");
        }

        // Overlap check logic (optional here, can be complex and might be better handled by solver or UI validation)
        // Example: boolean overlap = availabilityRepository.existsOverlappingSlot(instructor.getId(), requestDTO.getDayOfWeek(), requestDTO.getStartTime(), requestDTO.getEndTime(), null);
        // if (overlap) {
        //     throw new IllegalArgumentException("New availability slot overlaps with an existing one for this instructor on this day.");
        // }


        InstructorAvailabilitySlot slot = new InstructorAvailabilitySlot();
        slot.setInstructor(instructor);
        slot.setStudioLocation(studioLocation);
        slot.setDayOfWeek(requestDTO.getDayOfWeek());
        slot.setStartTime(requestDTO.getStartTime());
        slot.setEndTime(requestDTO.getEndTime());

        availabilityRepository.saveAndFlush(slot); // Use saveAndFlush for timestamps
        InstructorAvailabilitySlot savedAndRefetched = availabilityRepository.findById(slot.getId())
             .orElseThrow(() -> new IllegalStateException("Failed to re-fetch slot after save."));

        return convertToDTO(savedAndRefetched);
    }

    @Transactional(readOnly = true)
    public List<InstructorAvailabilitySlotResponseDTO> getAvailabilitySlotsForInstructor(Integer instructorId) {
        if (!instructorRepository.existsById(instructorId)) {
            throw new EntityNotFoundException("Instructor not found: " + instructorId);
        }
        return availabilityRepository.findByInstructorId(instructorId).stream()
            .map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InstructorAvailabilitySlotResponseDTO getAvailabilitySlotById(Integer slotId) {
        InstructorAvailabilitySlot slot = availabilityRepository.findById(slotId)
            .orElseThrow(() -> new EntityNotFoundException("Availability slot not found: " + slotId));
        return convertToDTO(slot);
    }
    
    @Transactional
    public InstructorAvailabilitySlotResponseDTO updateAvailabilitySlot(Integer slotId, InstructorAvailabilitySlotRequestDTO requestDTO) {
        InstructorAvailabilitySlot slot = availabilityRepository.findById(slotId)
            .orElseThrow(() -> new EntityNotFoundException("Availability slot not found: " + slotId));

        // Prevent changing instructor or studio location via this update method for simplicity
        // If needed, these would require careful checks (e.g., new location belongs to same org as instructor)
        if (requestDTO.getInstructorId() != null && !requestDTO.getInstructorId().equals(slot.getInstructor().getId())) {
            throw new IllegalArgumentException("Cannot change the instructor of an availability slot.");
        }
        if (requestDTO.getStudioLocationId() != null && (slot.getStudioLocation() == null || !requestDTO.getStudioLocationId().equals(slot.getStudioLocation().getId()))) {
            // Allow setting a location if it was null, or changing it if it matches org
            StudioLocation newLocation = studioLocationRepository.findById(requestDTO.getStudioLocationId())
                .orElseThrow(() -> new EntityNotFoundException("New StudioLocation not found: " + requestDTO.getStudioLocationId()));
            if(!newLocation.getOrganization().getId().equals(slot.getInstructor().getOrganization().getId())){
                 throw new IllegalArgumentException("New StudioLocation does not belong to the instructor's organization.");
            }
            slot.setStudioLocation(newLocation);
        } else if (requestDTO.getStudioLocationId() == null && slot.getStudioLocation() != null) {
            slot.setStudioLocation(null); // Allow making it org-wide
        }


        if (requestDTO.getDayOfWeek() != null) slot.setDayOfWeek(requestDTO.getDayOfWeek());
        if (requestDTO.getStartTime() != null) slot.setStartTime(requestDTO.getStartTime());
        if (requestDTO.getEndTime() != null) slot.setEndTime(requestDTO.getEndTime());
        
        if (slot.getEndTime().isBefore(slot.getStartTime()) || slot.getEndTime().equals(slot.getStartTime())) {
            throw new IllegalArgumentException("End time must be after start time.");
        }
        
        // Optional: Add overlap check here as well for updates, excluding the current slotId
        
        availabilityRepository.saveAndFlush(slot);
        InstructorAvailabilitySlot updatedAndRefetched = availabilityRepository.findById(slotId)
            .orElseThrow(() -> new IllegalStateException("Failed to re-fetch slot after update."));
        return convertToDTO(updatedAndRefetched);
    }

    @Transactional
    public void deleteAvailabilitySlot(Integer slotId) {
        if (!availabilityRepository.existsById(slotId)) {
            throw new EntityNotFoundException("Availability slot not found: " + slotId);
        }
        availabilityRepository.deleteById(slotId);
    }
}