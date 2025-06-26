package com.example.studioapp_api.service;

import com.example.studioapp_api.dto.ClassSessionRequirementRequestDTO;
import com.example.studioapp_api.dto.ClassSessionRequirementResponseDTO;
import com.example.studioapp_api.entity.ClassDefinition;
import com.example.studioapp_api.entity.ClassSessionRequirement;
import com.example.studioapp_api.entity.StudioLocation;
import com.example.studioapp_api.entity.Term;
import com.example.studioapp_api.repository.ClassDefinitionRepository;
import com.example.studioapp_api.repository.ClassSessionRequirementRepository;
import com.example.studioapp_api.repository.StudioLocationRepository;
import com.example.studioapp_api.repository.TermRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ClassSessionRequirementService {

    private final ClassSessionRequirementRepository requirementRepository;
    private final TermRepository termRepository;
    private final ClassDefinitionRepository classDefinitionRepository;
    private final StudioLocationRepository studioLocationRepository;

    public ClassSessionRequirementService(
            ClassSessionRequirementRepository requirementRepository,
            TermRepository termRepository,
            ClassDefinitionRepository classDefinitionRepository,
            StudioLocationRepository studioLocationRepository) {
        this.requirementRepository = requirementRepository;
        this.termRepository = termRepository;
        this.classDefinitionRepository = classDefinitionRepository;
        this.studioLocationRepository = studioLocationRepository;
    }

    private ClassSessionRequirementResponseDTO convertToDTO(ClassSessionRequirement req) {
        ClassSessionRequirementResponseDTO dto = new ClassSessionRequirementResponseDTO();
        dto.setId(req.getId());
        dto.setTermId(req.getTerm().getId());
        dto.setTermName(req.getTerm().getName());
        dto.setClassDefinitionId(req.getClassDefinition().getId());
        dto.setClassDefinitionName(req.getClassDefinition().getName());
        dto.setClassDefinitionCode(req.getClassDefinition().getClassCode());
        if (req.getStudioLocation() != null) {
            dto.setStudioLocationId(req.getStudioLocation().getId());
            dto.setStudioLocationName(req.getStudioLocation().getName());
        }
        dto.setSessionsPerWeek(req.getSessionsPerWeek());
        dto.setActive(req.isActive());
        dto.setCreatedAt(req.getCreatedAt());
        dto.setUpdatedAt(req.getUpdatedAt());
        return dto;
    }

    @Transactional
    public ClassSessionRequirementResponseDTO createRequirement(ClassSessionRequirementRequestDTO requestDTO) {
        Term term = termRepository.findById(requestDTO.getTermId())
            .orElseThrow(() -> new EntityNotFoundException("Term not found: " + requestDTO.getTermId()));
        
        ClassDefinition classDefinition = classDefinitionRepository.findById(requestDTO.getClassDefinitionId())
            .orElseThrow(() -> new EntityNotFoundException("ClassDefinition not found: " + requestDTO.getClassDefinitionId()));

        // Ensure class definition and term belong to the same organization
        if (!term.getOrganization().getId().equals(classDefinition.getOrganization().getId())) {
            throw new IllegalArgumentException("Term and ClassDefinition must belong to the same organization.");
        }

        StudioLocation studioLocation = null;
        if (requestDTO.getStudioLocationId() != null) {
            studioLocation = studioLocationRepository.findById(requestDTO.getStudioLocationId())
                .orElseThrow(() -> new EntityNotFoundException("StudioLocation not found: " + requestDTO.getStudioLocationId()));
            // Ensure studio location belongs to the same organization
            if(!studioLocation.getOrganization().getId().equals(term.getOrganization().getId())) {
                throw new IllegalArgumentException("StudioLocation does not belong to the term's organization.");
            }
            // If term is location-specific, ensure requirement's location matches term's location
            if(term.getStudioLocation() != null && !term.getStudioLocation().getId().equals(studioLocation.getId())) {
                throw new IllegalArgumentException("Requirement's studio location does not match term's specific studio location.");
            }
        } else {
            // If request is for org-wide (studioLocationId is null), but term is location-specific,
            // this requirement applies to that term's specific location.
            if (term.getStudioLocation() != null) {
                studioLocation = term.getStudioLocation();
            }
        }
        
        // Uniqueness check based on (term_id, class_definition_id, studio_location_id)
        Optional<ClassSessionRequirement> existing;
        if (studioLocation != null) {
            existing = requirementRepository.findByTermIdAndClassDefinitionIdAndStudioLocationId(
                term.getId(), classDefinition.getId(), studioLocation.getId());
        } else {
            existing = requirementRepository.findByTermIdAndClassDefinitionIdAndStudioLocationIsNull(
                term.getId(), classDefinition.getId());
        }
        if (existing.isPresent()) {
            throw new IllegalArgumentException("A class session requirement for this class definition, term, and location (or org-wide) already exists.");
        }

        if (requestDTO.getSessionsPerWeek() <= 0) {
            throw new IllegalArgumentException("Sessions per week must be positive.");
        }

        ClassSessionRequirement newRequirement = new ClassSessionRequirement();
        newRequirement.setTerm(term);
        newRequirement.setClassDefinition(classDefinition);
        newRequirement.setStudioLocation(studioLocation); // Will be null if org-wide and term is org-wide
        newRequirement.setSessionsPerWeek(requestDTO.getSessionsPerWeek());
        newRequirement.setActive(requestDTO.isActive());

        requirementRepository.saveAndFlush(newRequirement);
        ClassSessionRequirement savedAndRefetched = requirementRepository.findById(newRequirement.getId())
            .orElseThrow(() -> new IllegalStateException("Failed to re-fetch requirement after save."));
        return convertToDTO(savedAndRefetched);
    }

    @Transactional(readOnly = true)
    public List<ClassSessionRequirementResponseDTO> getRequirementsByTerm(Integer termId) {
        if (!termRepository.existsById(termId)) {
            throw new EntityNotFoundException("Term not found: " + termId);
        }
        return requirementRepository.findByTermId(termId).stream()
            .map(this::convertToDTO).collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public ClassSessionRequirementResponseDTO getRequirementById(Integer requirementId) {
        return requirementRepository.findById(requirementId).map(this::convertToDTO)
            .orElseThrow(() -> new EntityNotFoundException("Class Session Requirement not found: " + requirementId));
    }

    @Transactional
    public ClassSessionRequirementResponseDTO updateRequirement(Integer requirementId, ClassSessionRequirementRequestDTO requestDTO) {
        ClassSessionRequirement existingReq = requirementRepository.findById(requirementId)
            .orElseThrow(() -> new EntityNotFoundException("Class Session Requirement not found: " + requirementId));

        // Term, ClassDefinition, and StudioLocation (if set) are generally not changed in an update.
        // If they were, complex re-validation and uniqueness checks would be needed.
        // We'll focus on updating sessionsPerWeek and isActive.
        if (requestDTO.getTermId() != null && !requestDTO.getTermId().equals(existingReq.getTerm().getId())) {
            throw new IllegalArgumentException("Cannot change the term of an existing requirement.");
        }
        if (requestDTO.getClassDefinitionId() != null && !requestDTO.getClassDefinitionId().equals(existingReq.getClassDefinition().getId())) {
            throw new IllegalArgumentException("Cannot change the class definition of an existing requirement.");
        }
        // Similar logic for studioLocationId if it's considered immutable after creation for a specific entry.

        if (requestDTO.getSessionsPerWeek() != null) {
            if (requestDTO.getSessionsPerWeek() <= 0) {
                 throw new IllegalArgumentException("Sessions per week must be positive.");
            }
            existingReq.setSessionsPerWeek(requestDTO.getSessionsPerWeek());
        }
        existingReq.setActive(requestDTO.isActive()); // DTO defaults to true, so it will always provide a value

        requirementRepository.saveAndFlush(existingReq);
        ClassSessionRequirement updatedAndRefetched = requirementRepository.findById(requirementId)
             .orElseThrow(() -> new IllegalStateException("Failed to re-fetch requirement after update."));
        return convertToDTO(updatedAndRefetched);
    }

    @Transactional
    public void deleteRequirement(Integer requirementId) {
        if (!requirementRepository.existsById(requirementId)) {
            throw new EntityNotFoundException("Class Session Requirement not found: " + requirementId);
        }
        // FK from scheduled_events to class_session_requirements is ON DELETE SET NULL
        requirementRepository.deleteById(requirementId);
    }
}