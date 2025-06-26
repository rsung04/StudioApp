package com.example.studioapp_api.service;

import com.example.studioapp_api.dto.OrganizationRequestDTO;
import com.example.studioapp_api.dto.OrganizationResponseDTO;
import com.example.studioapp_api.entity.Organization;
import com.example.studioapp_api.repository.OrganizationRepository;
import jakarta.persistence.EntityNotFoundException; // Or create a custom exception
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service // Marks this as a Spring service component
public class OrganizationService {

    private final OrganizationRepository organizationRepository;

    @Autowired // Constructor injection is preferred
    public OrganizationService(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    // --- Mapper methods (could be in a separate Mapper class later) ---
    private OrganizationResponseDTO convertToDTO(Organization organization) {
        OrganizationResponseDTO dto = new OrganizationResponseDTO();
        dto.setId(organization.getId());
        dto.setName(organization.getName());
        dto.setSubdomain(organization.getSubdomain());
        dto.setContactEmail(organization.getContactEmail());
        dto.setCreatedAt(organization.getCreatedAt());
        dto.setUpdatedAt(organization.getUpdatedAt());
        return dto;
    }

    private Organization convertToEntity(OrganizationRequestDTO dto) {
        Organization organization = new Organization();
        organization.setName(dto.getName());
        organization.setSubdomain(dto.getSubdomain());
        organization.setContactEmail(dto.getContactEmail());
        // createdAt and updatedAt are handled by @CreationTimestamp/@UpdateTimestamp
        return organization;
    }
    // --- End Mapper methods ---

    @Transactional // Ensures the operation is atomic (all or nothing)
    public OrganizationResponseDTO createOrganization(OrganizationRequestDTO organizationRequestDTO) {
        // Check for existing organization by name or subdomain if they must be unique globally
        // (Your DB schema enforces uniqueness on name and subdomain already)
        // For example:
        // if (organizationRepository.findByName(organizationRequestDTO.getName()).isPresent()) {
        //     throw new IllegalArgumentException("Organization with name '" + organizationRequestDTO.getName() + "' already exists.");
        // }
        // if (organizationRequestDTO.getSubdomain() != null && organizationRepository.findBySubdomain(organizationRequestDTO.getSubdomain()).isPresent()) {
        //     throw new IllegalArgumentException("Organization with subdomain '" + organizationRequestDTO.getSubdomain() + "' already exists.");
        // }

        Organization organization = convertToEntity(organizationRequestDTO);
        Organization savedOrganization = organizationRepository.save(organization);
        return convertToDTO(savedOrganization);
    }

    @Transactional(readOnly = true) // readOnly=true can optimize read operations
    public List<OrganizationResponseDTO> getAllOrganizations() {
        return organizationRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrganizationResponseDTO getOrganizationById(Integer id) {
        Organization organization = organizationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Organization not found with id: " + id));
        return convertToDTO(organization);
    }

    @Transactional
    public OrganizationResponseDTO updateOrganization(Integer id, OrganizationRequestDTO organizationRequestDTO) {
        Organization existingOrganization = organizationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Organization not found with id: " + id));

        // Update fields
        existingOrganization.setName(organizationRequestDTO.getName());
        existingOrganization.setSubdomain(organizationRequestDTO.getSubdomain());
        existingOrganization.setContactEmail(organizationRequestDTO.getContactEmail());
        // 'updatedAt' will be automatically updated by @UpdateTimestamp

        Organization updatedOrganization = organizationRepository.save(existingOrganization);
        return convertToDTO(updatedOrganization);
    }

    @Transactional
    public void deleteOrganization(Integer id) {
        if (!organizationRepository.existsById(id)) {
            throw new EntityNotFoundException("Organization not found with id: " + id);
        }
        organizationRepository.deleteById(id);
    }
}