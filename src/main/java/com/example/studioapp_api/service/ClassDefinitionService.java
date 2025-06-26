package com.example.studioapp_api.service;

import com.example.studioapp_api.dto.ClassDefinitionRequestDTO;
import com.example.studioapp_api.dto.ClassDefinitionResponseDTO;
import com.example.studioapp_api.entity.ClassDefinition;
import com.example.studioapp_api.entity.ClassType;
import com.example.studioapp_api.entity.Organization;
import com.example.studioapp_api.repository.ClassDefinitionRepository;
import com.example.studioapp_api.repository.ClassTypeRepository;
import com.example.studioapp_api.repository.OrganizationRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClassDefinitionService {

    private final ClassDefinitionRepository classDefinitionRepository;
    private final OrganizationRepository organizationRepository;
    private final ClassTypeRepository classTypeRepository;

    public ClassDefinitionService(ClassDefinitionRepository classDefinitionRepository,
                                  OrganizationRepository organizationRepository,
                                  ClassTypeRepository classTypeRepository) {
        this.classDefinitionRepository = classDefinitionRepository;
        this.organizationRepository = organizationRepository;
        this.classTypeRepository = classTypeRepository;
    }

    private ClassDefinitionResponseDTO convertToDTO(ClassDefinition cd) {
        ClassDefinitionResponseDTO dto = new ClassDefinitionResponseDTO();
        dto.setId(cd.getId());
        dto.setClassCode(cd.getClassCode());
        dto.setName(cd.getName());
        dto.setDescription(cd.getDescription());
        dto.setDurationMinutes(cd.getDurationMinutes());
        dto.setLevel(cd.getLevel());
        dto.setDefaultStudioCapacityNeeded(cd.getDefaultStudioCapacityNeeded());
        dto.setOrganizationId(cd.getOrganization().getId());
        dto.setOrganizationName(cd.getOrganization().getName());
        dto.setClassTypeId(cd.getClassType().getId());
        dto.setClassTypeName(cd.getClassType().getName());
        dto.setCreatedAt(cd.getCreatedAt());
        dto.setUpdatedAt(cd.getUpdatedAt());
        return dto;
    }

    @Transactional
    public ClassDefinitionResponseDTO createClassDefinition(ClassDefinitionRequestDTO requestDTO) {
        Organization organization = organizationRepository.findById(requestDTO.getOrganizationId())
            .orElseThrow(() -> new EntityNotFoundException("Organization not found: " + requestDTO.getOrganizationId()));
        
        ClassType classType = classTypeRepository.findById(requestDTO.getClassTypeId())
            .orElseThrow(() -> new EntityNotFoundException("ClassType not found: " + requestDTO.getClassTypeId()));

        // Ensure classType belongs to the same organization
        if (!classType.getOrganization().getId().equals(organization.getId())) {
            throw new IllegalArgumentException("ClassType id " + requestDTO.getClassTypeId() + 
                                               " does not belong to Organization id " + requestDTO.getOrganizationId());
        }

        classDefinitionRepository.findByOrganizationIdAndClassCode(organization.getId(), requestDTO.getClassCode())
            .ifPresent(cd -> { throw new IllegalArgumentException("ClassDefinition with code '" + requestDTO.getClassCode() + "' already exists for this organization."); });

        if (requestDTO.getDurationMinutes() <= 0) {
             throw new IllegalArgumentException("Duration minutes must be positive.");
        }
        if (requestDTO.getDefaultStudioCapacityNeeded() != null && requestDTO.getDefaultStudioCapacityNeeded() <= 0) {
            throw new IllegalArgumentException("Default studio capacity needed must be positive if specified.");
        }


        ClassDefinition classDefinition = new ClassDefinition();
        classDefinition.setClassCode(requestDTO.getClassCode());
        classDefinition.setName(requestDTO.getName());
        classDefinition.setDescription(requestDTO.getDescription());
        classDefinition.setDurationMinutes(requestDTO.getDurationMinutes());
        classDefinition.setLevel(requestDTO.getLevel());
        classDefinition.setDefaultStudioCapacityNeeded(requestDTO.getDefaultStudioCapacityNeeded());
        classDefinition.setOrganization(organization);
        classDefinition.setClassType(classType);

        ClassDefinition saved = classDefinitionRepository.save(classDefinition);
        return convertToDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<ClassDefinitionResponseDTO> getClassDefinitionsByOrganization(Integer organizationId) {
        if (!organizationRepository.existsById(organizationId)) {
            throw new EntityNotFoundException("Organization not found: " + organizationId);
        }
        return classDefinitionRepository.findByOrganizationId(organizationId).stream()
            .map(this::convertToDTO).collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public ClassDefinitionResponseDTO getClassDefinitionById(Integer id) {
        return classDefinitionRepository.findById(id).map(this::convertToDTO)
            .orElseThrow(() -> new EntityNotFoundException("ClassDefinition not found: " + id));
    }

    // ... (existing constructor, convertToDTO, createClassDefinition, etc. methods are above this)

    @Transactional
    public ClassDefinitionResponseDTO updateClassDefinition(Integer id, ClassDefinitionRequestDTO requestDTO) {
        ClassDefinition classDefinition = classDefinitionRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("ClassDefinition not found: " + id));

        // Generally, organization and classType are not changed during an update.
        // If they were, new uniqueness checks and relationship management would be needed.
        if (requestDTO.getOrganizationId() != null && !requestDTO.getOrganizationId().equals(classDefinition.getOrganization().getId())) {
            throw new IllegalArgumentException("Changing organization of a class definition is not supported.");
        }
        if (requestDTO.getClassTypeId() != null && !requestDTO.getClassTypeId().equals(classDefinition.getClassType().getId())) {
            throw new IllegalArgumentException("Changing class type of a class definition is not supported.");
        }

        // Check classCode uniqueness if it's being changed
        if (requestDTO.getClassCode() != null && !requestDTO.getClassCode().equals(classDefinition.getClassCode())) {
            classDefinitionRepository.findByOrganizationIdAndClassCode(
                classDefinition.getOrganization().getId(), requestDTO.getClassCode())
                .ifPresent(cd -> {
                    if(!cd.getId().equals(id)) { // Ensure it's not the same entity
                        throw new IllegalArgumentException("Another ClassDefinition with code '" + requestDTO.getClassCode() + "' already exists for this organization.");
                    }
                });
            classDefinition.setClassCode(requestDTO.getClassCode());
        }

        // Update other fields
        if (requestDTO.getName() != null) {
            classDefinition.setName(requestDTO.getName());
        }
        classDefinition.setDescription(requestDTO.getDescription()); // Can be set to null if DTO allows

        if (requestDTO.getDurationMinutes() != null) {
            if (requestDTO.getDurationMinutes() <= 0) {
                 throw new IllegalArgumentException("Duration minutes must be positive.");
            }
            classDefinition.setDurationMinutes(requestDTO.getDurationMinutes());
        }
        classDefinition.setLevel(requestDTO.getLevel()); // Can be set to null

        if (requestDTO.getDefaultStudioCapacityNeeded() != null) {
            if (requestDTO.getDefaultStudioCapacityNeeded() <= 0) {
                throw new IllegalArgumentException("Default studio capacity needed must be positive if specified.");
            }
             classDefinition.setDefaultStudioCapacityNeeded(requestDTO.getDefaultStudioCapacityNeeded());
        } else {
            classDefinition.setDefaultStudioCapacityNeeded(null); // Explicitly set to null if not in request
        }


        classDefinitionRepository.saveAndFlush(classDefinition);
        ClassDefinition updatedAndRefetched = classDefinitionRepository.findById(id)
            .orElseThrow(() -> new IllegalStateException("Failed to re-fetch ClassDefinition after update."));
        return convertToDTO(updatedAndRefetched);
    }

    @Transactional
    public void deleteClassDefinition(Integer id) {
        if (!classDefinitionRepository.existsById(id)) {
            throw new EntityNotFoundException("ClassDefinition not found: " + id);
        }
        // DB Schema:
        // instructor_class_qualifications FK to class_definitions is ON DELETE CASCADE
        // class_session_requirements FK to class_definitions is ON DELETE CASCADE
        // scheduled_events FK to class_definitions is ON DELETE SET NULL
        // This means deleting a ClassDefinition will also delete its qualifications and requirements,
        // and set scheduled_events.class_definition_id to NULL.
        classDefinitionRepository.deleteById(id);
    }
}