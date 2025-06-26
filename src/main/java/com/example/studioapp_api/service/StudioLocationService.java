package com.example.studioapp_api.service;

import com.example.studioapp_api.dto.StudioLocationRequestDTO;
import com.example.studioapp_api.dto.StudioLocationResponseDTO;
import com.example.studioapp_api.entity.Organization;
import com.example.studioapp_api.entity.StudioLocation;
import com.example.studioapp_api.repository.OrganizationRepository;
import com.example.studioapp_api.repository.StudioLocationRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StudioLocationService {

    private final StudioLocationRepository studioLocationRepository;
    private final OrganizationRepository organizationRepository;

    @Autowired
    public StudioLocationService(StudioLocationRepository studioLocationRepository,
                                 OrganizationRepository organizationRepository) {
        this.studioLocationRepository = studioLocationRepository;
        this.organizationRepository = organizationRepository;
    }

    private StudioLocationResponseDTO convertToDTO(StudioLocation location) {
        StudioLocationResponseDTO dto = new StudioLocationResponseDTO();
        dto.setId(location.getId());
        dto.setName(location.getName());
        dto.setAddress(location.getAddress());
        if (location.getOrganization() != null) {
            dto.setOrganizationId(location.getOrganization().getId());
            dto.setOrganizationName(location.getOrganization().getName());
        }
        dto.setCreatedAt(location.getCreatedAt());
        dto.setUpdatedAt(location.getUpdatedAt());
        return dto;
    }

    private void mapDtoToEntity(StudioLocationRequestDTO dto, StudioLocation location, Organization organization) {
        location.setName(dto.getName());
        location.setAddress(dto.getAddress());
        location.setOrganization(organization);
    }

    @Transactional
    public StudioLocationResponseDTO createStudioLocation(StudioLocationRequestDTO requestDTO) {
        Organization organization = organizationRepository.findById(requestDTO.getOrganizationId())
            .orElseThrow(() -> new EntityNotFoundException("Organization not found with id: " + requestDTO.getOrganizationId()));

        // Check for uniqueness: uq_studio_locations_organization_name
        studioLocationRepository.findByOrganizationAndName(organization, requestDTO.getName())
            .ifPresent(sl -> {
                throw new IllegalArgumentException("Studio Location with name '" + requestDTO.getName() +
                                                   "' already exists for this organization.");
            });

        StudioLocation studioLocation = new StudioLocation();
        mapDtoToEntity(requestDTO, studioLocation, organization);

        studioLocationRepository.saveAndFlush(studioLocation);
        StudioLocation savedAndRefetched = studioLocationRepository.findById(studioLocation.getId())
            .orElseThrow(() -> new IllegalStateException("Failed to re-fetch StudioLocation after save."));
        return convertToDTO(savedAndRefetched);
    }

    @Transactional(readOnly = true)
    public List<StudioLocationResponseDTO> getStudioLocationsByOrganization(Integer organizationId) {
        if (!organizationRepository.existsById(organizationId)) {
            throw new EntityNotFoundException("Organization not found with id: " + organizationId);
        }
        return studioLocationRepository.findByOrganizationId(organizationId)
            .stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StudioLocationResponseDTO getStudioLocationById(Integer locationId) {
        StudioLocation location = studioLocationRepository.findById(locationId)
            .orElseThrow(() -> new EntityNotFoundException("StudioLocation not found with id: " + locationId));
        return convertToDTO(location);
    }

    @Transactional
    public StudioLocationResponseDTO updateStudioLocation(Integer locationId, StudioLocationRequestDTO requestDTO) {
        StudioLocation location = studioLocationRepository.findById(locationId)
            .orElseThrow(() -> new EntityNotFoundException("StudioLocation not found with id: " + locationId));

        // Prevent changing organization
        if (requestDTO.getOrganizationId() != null && !requestDTO.getOrganizationId().equals(location.getOrganization().getId())) {
            throw new IllegalArgumentException("Changing the organization of a StudioLocation is not supported.");
        }

        // Check name uniqueness if name is being changed
        if (requestDTO.getName() != null && !requestDTO.getName().equals(location.getName())) {
            studioLocationRepository.findByOrganizationAndName(location.getOrganization(), requestDTO.getName())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(locationId)) {
                        throw new IllegalArgumentException("Another StudioLocation with name '" + requestDTO.getName() +
                                                           "' already exists for this organization.");
                    }
                });
            location.setName(requestDTO.getName());
        }
        
        if (requestDTO.getAddress() != null) { // Allow setting address to null or new value
            location.setAddress(requestDTO.getAddress());
        }


        studioLocationRepository.saveAndFlush(location);
        StudioLocation updatedAndRefetched = studioLocationRepository.findById(locationId)
            .orElseThrow(() -> new IllegalStateException("Failed to re-fetch StudioLocation after update."));
        return convertToDTO(updatedAndRefetched);
    }

    @Transactional
    public void deleteStudioLocation(Integer locationId) {
        StudioLocation location = studioLocationRepository.findById(locationId)
            .orElseThrow(() -> new EntityNotFoundException("StudioLocation not found with id: " + locationId));

        // DB Schema FKs referencing studio_locations:
        // - terms.studio_location_id (ON DELETE CASCADE)
        // - rooms.studio_location_id (ON DELETE CASCADE)
        // - instructor_availability_slots.studio_location_id (ON DELETE SET NULL)
        // - instructor_priority_requests.studio_location_id (ON DELETE SET NULL)
        // - class_session_requirements.studio_location_id (ON DELETE CASCADE)
        // - scheduled_events.studio_location_id (ON DELETE RESTRICT) <-- This is the one to watch out for!
        
        // If scheduled_events exist for this location, DB will prevent deletion.
        try {
            studioLocationRepository.delete(location);
        } catch (DataIntegrityViolationException e) {
            // This catch is a good safety net if any other unexpected RESTRICT constraint exists or if a CASCADE fails.
            throw new IllegalStateException("Cannot delete StudioLocation id " + locationId +
                                            ": it may be referenced by scheduled events or other entities with restrictive delete policies.", e);
        }
    }
}