package com.example.studioapp_api.service;

import com.example.studioapp_api.dto.TermRequestDTO;
import com.example.studioapp_api.dto.TermResponseDTO;
import com.example.studioapp_api.entity.Organization;
import com.example.studioapp_api.entity.StudioLocation;
import com.example.studioapp_api.entity.Term;
import com.example.studioapp_api.repository.OrganizationRepository;
import com.example.studioapp_api.repository.StudioLocationRepository;
import com.example.studioapp_api.repository.TermRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class TermService {

    private final TermRepository termRepository;
    private final OrganizationRepository organizationRepository;
    private final StudioLocationRepository studioLocationRepository;

    @Autowired
    public TermService(TermRepository termRepository,
                       OrganizationRepository organizationRepository,
                       StudioLocationRepository studioLocationRepository) {
        this.termRepository = termRepository;
        this.organizationRepository = organizationRepository;
        this.studioLocationRepository = studioLocationRepository;
    }

    private TermResponseDTO convertToDTO(Term term) {
        TermResponseDTO dto = new TermResponseDTO();
        dto.setId(term.getId());
        dto.setName(term.getName());
        dto.setStartDate(term.getStartDate());
        dto.setEndDate(term.getEndDate());
        dto.setActiveForPlanning(term.isActiveForPlanning()); // Still mapping it from entity
        dto.setOrganizationId(term.getOrganization().getId());
        dto.setOrganizationName(term.getOrganization().getName());
        if (term.getStudioLocation() != null) {
            dto.setStudioLocationId(term.getStudioLocation().getId());
            dto.setStudioLocationName(term.getStudioLocation().getName());
        }
        dto.setCreatedAt(term.getCreatedAt());
        dto.setUpdatedAt(term.getUpdatedAt());
        return dto;
    }

    @Transactional
    public TermResponseDTO createTerm(TermRequestDTO requestDTO) {
        Organization organization = organizationRepository.findById(requestDTO.getOrganizationId())
            .orElseThrow(() -> new EntityNotFoundException("Organization not found with id: " + requestDTO.getOrganizationId()));

        StudioLocation studioLocation = null;
        if (requestDTO.getStudioLocationId() != null) {
            studioLocation = studioLocationRepository.findById(requestDTO.getStudioLocationId())
                .orElseThrow(() -> new EntityNotFoundException("StudioLocation not found with id: " + requestDTO.getStudioLocationId()));
            
            if (!studioLocation.getOrganization().getId().equals(organization.getId())) {
                throw new IllegalArgumentException("StudioLocation id " + requestDTO.getStudioLocationId() +
                                                   " does not belong to Organization id " + organization.getId());
            }
        }

        // Uniqueness check (delegated to DB, but good to have an early check here)
        if (studioLocation != null) {
            termRepository.findByOrganizationIdAndStudioLocationIdAndName(
                organization.getId(), studioLocation.getId(), requestDTO.getName())
                .ifPresent(t -> { throw new IllegalArgumentException("Term with name '" + requestDTO.getName() + "' already exists for this organization and location."); });
        } else {
            termRepository.findByOrganizationIdAndStudioLocationIdIsNullAndName(
                organization.getId(), requestDTO.getName())
                .ifPresent(t -> { throw new IllegalArgumentException("Term with name '" + requestDTO.getName() + "' already exists for this organization (org-wide)."); });
        }

        if (requestDTO.getEndDate().isBefore(requestDTO.getStartDate())) {
            throw new IllegalArgumentException("Term end date cannot be before start date.");
        }

        Term term = new Term();
        term.setName(requestDTO.getName());
        term.setStartDate(requestDTO.getStartDate());
        term.setEndDate(requestDTO.getEndDate());
        term.setActiveForPlanning(requestDTO.isActiveForPlanning()); // Set as per request
        term.setOrganization(organization);
        term.setStudioLocation(studioLocation);

        Term savedTerm = termRepository.save(term);
        return convertToDTO(savedTerm);
    }

    @Transactional(readOnly = true)
    public List<TermResponseDTO> getTermsByOrganization(Integer organizationId) {
        if (!organizationRepository.existsById(organizationId)) {
            throw new EntityNotFoundException("Organization not found with id: " + organizationId);
        }
        return termRepository.findByOrganizationId(organizationId)
            .stream().map(this::convertToDTO).collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public TermResponseDTO getTermById(Integer termId) {
        Term term = termRepository.findById(termId)
            .orElseThrow(() -> new EntityNotFoundException("Term not found with id: " + termId));
        return convertToDTO(term);
    }

    // ... (existing constructor, convertToDTO, createTerm, getTermsByOrganization, getTermById methods are above this)

    @Transactional
    public TermResponseDTO updateTerm(Integer termId, TermRequestDTO requestDTO) {
        Term term = termRepository.findById(termId)
            .orElseThrow(() -> new EntityNotFoundException("Term not found with id: " + termId));

        // Prevent changing the organization or studio location of an existing term directly
        // If this needs to be allowed, it requires more complex logic (e.g., re-checking uniqueness in new scope)
        if (requestDTO.getOrganizationId() != null && !requestDTO.getOrganizationId().equals(term.getOrganization().getId())) {
            throw new IllegalArgumentException("Changing a term's organization is not supported.");
        }
        if (requestDTO.getStudioLocationId() != null && term.getStudioLocation() != null &&
            !requestDTO.getStudioLocationId().equals(term.getStudioLocation().getId())) {
            throw new IllegalArgumentException("Changing a term's specific studio location is not supported directly. Create a new term or handle as a separate operation.");
        }
        // If original term was org-wide (studioLocation null) and trying to make it location-specific, or vice-versa
        if ((requestDTO.getStudioLocationId() != null && term.getStudioLocation() == null) ||
            (requestDTO.getStudioLocationId() == null && term.getStudioLocation() != null)) {
            throw new IllegalArgumentException("Changing between an org-wide term and a location-specific term is not supported directly.");
        }


        // Check for name uniqueness if the name is being changed, within the same scope
        if (requestDTO.getName() != null && !requestDTO.getName().equals(term.getName())) {
            if (term.getStudioLocation() != null) {
                termRepository.findByOrganizationIdAndStudioLocationIdAndName(
                    term.getOrganization().getId(), term.getStudioLocation().getId(), requestDTO.getName())
                    .ifPresent(t -> { 
                        if (!t.getId().equals(termId)) { // Ensure it's not the same entity
                            throw new IllegalArgumentException("Another Term with name '" + requestDTO.getName() + "' already exists for this organization and location.");
                        }
                    });
            } else {
                termRepository.findByOrganizationIdAndStudioLocationIdIsNullAndName(
                    term.getOrganization().getId(), requestDTO.getName())
                    .ifPresent(t -> {
                        if (!t.getId().equals(termId)) { // Ensure it's not the same entity
                            throw new IllegalArgumentException("Another Term with name '" + requestDTO.getName() + "' already exists for this organization (org-wide).");
                        }
                    });
            }
            term.setName(requestDTO.getName());
        }
        
        if (requestDTO.getStartDate() != null) {
            term.setStartDate(requestDTO.getStartDate());
        }
        if (requestDTO.getEndDate() != null) {
            term.setEndDate(requestDTO.getEndDate());
        }
        
        // Validate dates after potential updates
        if (term.getEndDate().isBefore(term.getStartDate())) {
            throw new IllegalArgumentException("Term end date cannot be before start date.");
        }

        // Handle isActiveForPlanning (if changing)
        // The logic to deactivate other terms if this one becomes active should be considered here as well,
        // similar to the createTerm method, if that's the desired business rule.
        // For simplicity in this step, we'll just set the flag.
        // A more robust solution would re-use or call a common method for managing active flags.
        if (term.isActiveForPlanning() != requestDTO.isActiveForPlanning()) {
            term.setActiveForPlanning(requestDTO.isActiveForPlanning());
            if (term.isActiveForPlanning()) {
                // Logic from createTerm to deactivate others:
                List<Term> activeTerms;
                if (term.getStudioLocation() != null) {
                    activeTerms = termRepository.findActivePlanningTerms(term.getOrganization().getId(), term.getStudioLocation().getId());
                } else {
                    activeTerms = termRepository.findOrgWideActivePlanningTerms(term.getOrganization().getId());
                }
                for (Term activeTerm : activeTerms) {
                    if (!activeTerm.getId().equals(term.getId())) { 
                        activeTerm.setActiveForPlanning(false);
                        termRepository.save(activeTerm); // Save the change to other terms
                    }
                }
            }
        }


        termRepository.saveAndFlush(term);
        Term updatedAndRefetched = termRepository.findById(termId)
            .orElseThrow(() -> new IllegalStateException("Failed to re-fetch term after update."));
        return convertToDTO(updatedAndRefetched);
    }

    @Transactional
    public void deleteTerm(Integer termId) {
        if (!termRepository.existsById(termId)) {
            throw new EntityNotFoundException("Term not found with id: " + termId);
        }
        // DB schema for related tables (class_session_requirements, instructor_priority_requests, scheduled_events)
        // has ON DELETE CASCADE. This means deleting a term will automatically delete related records in those tables.
        // This is powerful but be aware of the cascading effect.
        termRepository.deleteById(termId);
    }
}