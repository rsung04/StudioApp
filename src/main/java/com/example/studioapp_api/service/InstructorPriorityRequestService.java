package com.example.studioapp_api.service;

import com.example.studioapp_api.dto.InstructorPriorityRequestRequestDTO;
import com.example.studioapp_api.dto.InstructorPriorityRequestResponseDTO;
import com.example.studioapp_api.entity.Instructor;
import com.example.studioapp_api.entity.InstructorPriorityRequest;
import com.example.studioapp_api.entity.StudioLocation;
import com.example.studioapp_api.entity.Term;
import com.example.studioapp_api.repository.InstructorPriorityRequestRepository;
import com.example.studioapp_api.repository.InstructorRepository;
import com.example.studioapp_api.repository.StudioLocationRepository;
import com.example.studioapp_api.repository.TermRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;


import java.util.List;
import java.util.stream.Collectors;

@Service
public class InstructorPriorityRequestService {

    private final InstructorPriorityRequestRepository requestRepository;
    private final InstructorRepository instructorRepository;
    private final TermRepository termRepository;
    private final StudioLocationRepository studioLocationRepository;

    public InstructorPriorityRequestService(
            InstructorPriorityRequestRepository requestRepository,
            InstructorRepository instructorRepository,
            TermRepository termRepository,
            StudioLocationRepository studioLocationRepository) {
        this.requestRepository = requestRepository;
        this.instructorRepository = instructorRepository;
        this.termRepository = termRepository;
        this.studioLocationRepository = studioLocationRepository;
    }

    private InstructorPriorityRequestResponseDTO convertToDTO(InstructorPriorityRequest req) {
        InstructorPriorityRequestResponseDTO dto = new InstructorPriorityRequestResponseDTO();
        dto.setId(req.getId());
        dto.setInstructorId(req.getInstructor().getId());
        dto.setInstructorName(req.getInstructor().getName());
        dto.setTermId(req.getTerm().getId());
        dto.setTermName(req.getTerm().getName());
        if (req.getStudioLocation() != null) {
            dto.setStudioLocationId(req.getStudioLocation().getId());
            dto.setStudioLocationName(req.getStudioLocation().getName());
        }
        dto.setRelativePriority(req.getRelativePriority());
        dto.setBlockLengthHours(req.getBlockLengthHours());
        dto.setDescription(req.getDescription());
        dto.setActive(req.isActive());
        dto.setCreatedAt(req.getCreatedAt());
        dto.setUpdatedAt(req.getUpdatedAt());
        return dto;
    }

    @Transactional
    public InstructorPriorityRequestResponseDTO createRequest(InstructorPriorityRequestRequestDTO requestDTO) {
        Instructor instructor = instructorRepository.findById(requestDTO.getInstructorId())
            .orElseThrow(() -> new EntityNotFoundException("Instructor not found: " + requestDTO.getInstructorId()));
        
        Term term = termRepository.findById(requestDTO.getTermId())
            .orElseThrow(() -> new EntityNotFoundException("Term not found: " + requestDTO.getTermId()));

        // Ensure term belongs to the same organization as the instructor
        if (!instructor.getOrganization().getId().equals(term.getOrganization().getId())) {
            throw new IllegalArgumentException("Instructor and Term must belong to the same organization.");
        }

        StudioLocation studioLocation = null;
        if (requestDTO.getStudioLocationId() != null) {
            studioLocation = studioLocationRepository.findById(requestDTO.getStudioLocationId())
                .orElseThrow(() -> new EntityNotFoundException("StudioLocation not found: " + requestDTO.getStudioLocationId()));
            // Ensure studio location also belongs to the same organization
            if (!studioLocation.getOrganization().getId().equals(instructor.getOrganization().getId())) {
                throw new IllegalArgumentException("StudioLocation does not belong to the instructor's organization.");
            }
            // Also ensure if term is location-specific, this request's location matches the term's location
             if (term.getStudioLocation() != null && !term.getStudioLocation().getId().equals(studioLocation.getId())) {
                throw new IllegalArgumentException("Priority request's studio location does not match the term's specific studio location.");
            }
        } else {
            // If request is org-wide, term must also be org-wide OR this request implies any location within a location-specific term
            // For simplicity now, if requestDTO.studioLocationId is null, we assume it applies to the term's scope.
            // If term is location-specific, an org-wide request for that term might imply that specific location.
            // The current logic correctly assigns null to slot.studioLocation if requestDTO.studioLocationId is null.
            if (term.getStudioLocation() != null) {
                 // If term is location-specific, a priority request for that term *without* a specified location
                 // should probably default to the term's location. Or be an error.
                 // For now, let's assume it defaults to the term's location if term is location-specific.
                 studioLocation = term.getStudioLocation();
            }
        }


        InstructorPriorityRequest newRequest = new InstructorPriorityRequest();
        newRequest.setInstructor(instructor);
        newRequest.setTerm(term);
        newRequest.setStudioLocation(studioLocation); // This will be correct based on above logic
        newRequest.setRelativePriority(requestDTO.getRelativePriority() != null ? requestDTO.getRelativePriority() : 1);
        newRequest.setBlockLengthHours(requestDTO.getBlockLengthHours());
        newRequest.setDescription(requestDTO.getDescription());
        newRequest.setActive(requestDTO.isActive());

        requestRepository.saveAndFlush(newRequest);
        InstructorPriorityRequest savedAndRefetched = requestRepository.findById(newRequest.getId())
            .orElseThrow(() -> new IllegalStateException("Failed to re-fetch priority request after save."));
        return convertToDTO(savedAndRefetched);
    }

    @Transactional(readOnly = true)
    public List<InstructorPriorityRequestResponseDTO> getRequestsByInstructorAndTerm(Integer instructorId, Integer termId) {
        // Validate instructor and term exist and belong to same org if necessary
        if (!instructorRepository.existsById(instructorId)) throw new EntityNotFoundException("Instructor not found: " + instructorId);
        if (!termRepository.existsById(termId)) throw new EntityNotFoundException("Term not found: " + termId);
        // Further check if instructor and term are compatible (same org) could be added here.

        return requestRepository.findByInstructorIdAndTermId(instructorId, termId).stream()
            .map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InstructorPriorityRequestResponseDTO getRequestById(Integer requestId) {
        return requestRepository.findById(requestId).map(this::convertToDTO)
            .orElseThrow(() -> new EntityNotFoundException("Priority Request not found: " + requestId));
    }

    @Transactional
    public InstructorPriorityRequestResponseDTO updateRequest(Integer requestId, InstructorPriorityRequestRequestDTO requestDTO) {
        InstructorPriorityRequest existingRequest = requestRepository.findById(requestId)
            .orElseThrow(() -> new EntityNotFoundException("Priority Request not found: " + requestId));

        // Generally, instructor and term should not be changed for an existing request.
        // If requestDTO has instructorId or termId, validate they match existingRequest or throw error.
        if (requestDTO.getInstructorId() != null && !requestDTO.getInstructorId().equals(existingRequest.getInstructor().getId())) {
            throw new IllegalArgumentException("Cannot change instructor for an existing priority request.");
        }
        if (requestDTO.getTermId() != null && !requestDTO.getTermId().equals(existingRequest.getTerm().getId())) {
            throw new IllegalArgumentException("Cannot change term for an existing priority request.");
        }
        
        // Handle StudioLocation update
        if (requestDTO.getStudioLocationId() != null) {
            StudioLocation studioLocation = studioLocationRepository.findById(requestDTO.getStudioLocationId())
                .orElseThrow(() -> new EntityNotFoundException("StudioLocation not found: " + requestDTO.getStudioLocationId()));
            if (!studioLocation.getOrganization().getId().equals(existingRequest.getInstructor().getOrganization().getId())) {
                throw new IllegalArgumentException("StudioLocation does not belong to the instructor's organization.");
            }
            if (existingRequest.getTerm().getStudioLocation() != null && !existingRequest.getTerm().getStudioLocation().getId().equals(studioLocation.getId())) {
                throw new IllegalArgumentException("Priority request's studio location does not match the term's specific studio location.");
            }
            existingRequest.setStudioLocation(studioLocation);
        } else { // If null is passed, it means make it org-wide for the term (if term is org-wide) or apply to term's location
            if (existingRequest.getTerm().getStudioLocation() != null) {
                existingRequest.setStudioLocation(existingRequest.getTerm().getStudioLocation());
            } else {
                existingRequest.setStudioLocation(null);
            }
        }


        if (requestDTO.getRelativePriority() != null) existingRequest.setRelativePriority(requestDTO.getRelativePriority());
        if (requestDTO.getBlockLengthHours() != null) existingRequest.setBlockLengthHours(requestDTO.getBlockLengthHours());
        existingRequest.setDescription(requestDTO.getDescription());
        existingRequest.setActive(requestDTO.isActive()); // Default from DTO is true

        requestRepository.saveAndFlush(existingRequest);
        InstructorPriorityRequest updatedAndRefetched = requestRepository.findById(requestId)
             .orElseThrow(() -> new IllegalStateException("Failed to re-fetch priority request after update."));
        return convertToDTO(updatedAndRefetched);
    }

    @Transactional
    public void deleteRequest(Integer requestId) {
        if (!requestRepository.existsById(requestId)) {
            throw new EntityNotFoundException("Priority Request not found: " + requestId);
        }
        // Foreign key from scheduled_events to instructor_priority_requests is ON DELETE SET NULL
        requestRepository.deleteById(requestId);
    }
}