package com.example.studioapp_api.service;

import com.example.studioapp_api.dto.InstructorRequestDTO;
import com.example.studioapp_api.dto.InstructorResponseDTO;
import com.example.studioapp_api.entity.Instructor;
import com.example.studioapp_api.entity.Organization;
import com.example.studioapp_api.repository.InstructorRepository;
import com.example.studioapp_api.repository.OrganizationRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class InstructorService {

    private final InstructorRepository instructorRepository;
    private final OrganizationRepository organizationRepository;

    @Autowired
    public InstructorService(InstructorRepository instructorRepository, OrganizationRepository organizationRepository) {
        this.instructorRepository = instructorRepository;
        this.organizationRepository = organizationRepository;
    }

    private InstructorResponseDTO convertToDTO(Instructor instructor) {
        InstructorResponseDTO dto = new InstructorResponseDTO();
        dto.setId(instructor.getId());
        dto.setName(instructor.getName());
        dto.setEmail(instructor.getEmail());
        dto.setPhoneNumber(instructor.getPhoneNumber());
        dto.setBio(instructor.getBio());
        dto.setInstagramUsername(instructor.getInstagramUsername());
        dto.setTiktokUsername(instructor.getTiktokUsername());
        if (instructor.getOrganization() != null) {
            dto.setOrganizationId(instructor.getOrganization().getId());
            dto.setOrganizationName(instructor.getOrganization().getName());
        }
        dto.setCreatedAt(instructor.getCreatedAt());
        dto.setUpdatedAt(instructor.getUpdatedAt());
        return dto;
    }

    private void mapDtoToEntity(InstructorRequestDTO dto, Instructor instructor, Organization organization) {
        instructor.setName(dto.getName());
        instructor.setEmail(dto.getEmail()); // Consider null/empty string handling for unique constraint
        instructor.setPhoneNumber(dto.getPhoneNumber());
        instructor.setBio(dto.getBio());
        instructor.setInstagramUsername(dto.getInstagramUsername());
        instructor.setTiktokUsername(dto.getTiktokUsername());
        instructor.setOrganization(organization);
    }

    @Transactional
    public InstructorResponseDTO createInstructor(InstructorRequestDTO requestDTO) {
        Organization organization = organizationRepository.findById(requestDTO.getOrganizationId())
            .orElseThrow(() -> new EntityNotFoundException("Organization not found with id: " + requestDTO.getOrganizationId()));

        instructorRepository.findByOrganizationIdAndName(organization.getId(), requestDTO.getName())
            .ifPresent(i -> { throw new IllegalArgumentException("Instructor with name '" + requestDTO.getName() + "' already exists in this organization."); });
        
        if (requestDTO.getEmail() != null && !requestDTO.getEmail().isBlank()) {
            instructorRepository.findByOrganizationIdAndEmail(organization.getId(), requestDTO.getEmail())
                .ifPresent(i -> { throw new IllegalArgumentException("Instructor with email '" + requestDTO.getEmail() + "' already exists in this organization."); });
        }

        Instructor instructor = new Instructor();
        mapDtoToEntity(requestDTO, instructor, organization);
        
        // Using saveAndFlush and re-fetch for consistent timestamp DTO response
        instructorRepository.saveAndFlush(instructor);
        Instructor savedAndRefetched = instructorRepository.findById(instructor.getId())
            .orElseThrow(() -> new IllegalStateException("Failed to re-fetch instructor after save")); // Should not happen
        return convertToDTO(savedAndRefetched);
    }

    @Transactional(readOnly = true)
    public List<InstructorResponseDTO> getInstructorsByOrganization(Integer organizationId) {
        if (!organizationRepository.existsById(organizationId)) {
            throw new EntityNotFoundException("Organization not found with id: " + organizationId);
        }
        return instructorRepository.findByOrganizationId(organizationId).stream()
            .map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InstructorResponseDTO getInstructorById(Integer instructorId) {
        Instructor instructor = instructorRepository.findById(instructorId)
            .orElseThrow(() -> new EntityNotFoundException("Instructor not found with id: " + instructorId));
        return convertToDTO(instructor);
    }

    @Transactional
    public InstructorResponseDTO updateInstructor(Integer instructorId, InstructorRequestDTO requestDTO) {
        Instructor instructor = instructorRepository.findById(instructorId)
            .orElseThrow(() -> new EntityNotFoundException("Instructor not found with id: " + instructorId));

        // Ensure the organization from the DTO matches the instructor's current organization if provided,
        // or prevent changing organization through this update method.
        if (requestDTO.getOrganizationId() != null && !requestDTO.getOrganizationId().equals(instructor.getOrganization().getId())) {
            throw new IllegalArgumentException("Changing an instructor's organization is not supported via this update method. Organization ID mismatch.");
        }
        
        // Check uniqueness if name or email is changing
        if (requestDTO.getName() != null && !requestDTO.getName().equals(instructor.getName())) {
            instructorRepository.findByOrganizationIdAndName(instructor.getOrganization().getId(), requestDTO.getName())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(instructorId)) {
                        throw new IllegalArgumentException("Another instructor with name '" + requestDTO.getName() + "' already exists in this organization.");
                    }
                });
        }
        if (requestDTO.getEmail() != null && !requestDTO.getEmail().isBlank() && !requestDTO.getEmail().equals(instructor.getEmail())) {
             instructorRepository.findByOrganizationIdAndEmail(instructor.getOrganization().getId(), requestDTO.getEmail())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(instructorId)) {
                        throw new IllegalArgumentException("Another instructor with email '" + requestDTO.getEmail() + "' already exists in this organization.");
                    }
                });
        }


        mapDtoToEntity(requestDTO, instructor, instructor.getOrganization()); // Pass existing organization
        
        instructorRepository.saveAndFlush(instructor);
        Instructor savedAndRefetched = instructorRepository.findById(instructor.getId())
            .orElseThrow(() -> new IllegalStateException("Failed to re-fetch instructor after update"));
        return convertToDTO(savedAndRefetched);
    }

    @Transactional
    public void deleteInstructor(Integer instructorId) {
        Instructor instructor = instructorRepository.findById(instructorId)
            .orElseThrow(() -> new EntityNotFoundException("Instructor not found with id: " + instructorId));
        
        // DB constraints: instructor_availability_slots, instructor_class_qualifications, 
        // instructor_priority_requests, scheduled_events all have FKs to instructors
        // with ON DELETE CASCADE or ON DELETE RESTRICT.
        // If any are RESTRICT and records exist, this will fail.
        try {
            instructorRepository.delete(instructor); // or deleteById(instructorId)
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Cannot delete instructor id " + instructorId + ": they may be linked to other records (availability, qualifications, schedules, etc.).", e);
        }
    }
}