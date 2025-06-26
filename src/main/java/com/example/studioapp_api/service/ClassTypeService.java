package com.example.studioapp_api.service;

import com.example.studioapp_api.dto.ClassTypeRequestDTO;
import com.example.studioapp_api.dto.ClassTypeResponseDTO;
import com.example.studioapp_api.entity.ClassType;
import com.example.studioapp_api.entity.Organization;
import com.example.studioapp_api.repository.ClassTypeRepository;
import com.example.studioapp_api.repository.OrganizationRepository;
import jakarta.persistence.EntityNotFoundException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClassTypeService {
    private final ClassTypeRepository classTypeRepository;
    private final OrganizationRepository organizationRepository;

    public ClassTypeService(ClassTypeRepository classTypeRepository, OrganizationRepository organizationRepository) {
        this.classTypeRepository = classTypeRepository;
        this.organizationRepository = organizationRepository;
    }

    private ClassTypeResponseDTO convertToDTO(ClassType classType) {
        ClassTypeResponseDTO dto = new ClassTypeResponseDTO();
        dto.setId(classType.getId());
        dto.setName(classType.getName());
        dto.setDescription(classType.getDescription());
        dto.setOrganizationId(classType.getOrganization().getId());
        dto.setOrganizationName(classType.getOrganization().getName());
        dto.setCreatedAt(classType.getCreatedAt());
        dto.setUpdatedAt(classType.getUpdatedAt());
        return dto;
    }

    @Transactional
    public ClassTypeResponseDTO createClassType(ClassTypeRequestDTO requestDTO) {
        Organization organization = organizationRepository.findById(requestDTO.getOrganizationId())
            .orElseThrow(() -> new EntityNotFoundException("Organization not found: " + requestDTO.getOrganizationId()));
        
        classTypeRepository.findByOrganizationIdAndName(organization.getId(), requestDTO.getName())
            .ifPresent(ct -> { throw new IllegalArgumentException("ClassType with name '" + requestDTO.getName() + "' already exists for this organization."); });

        ClassType classType = new ClassType();
        classType.setName(requestDTO.getName());
        classType.setDescription(requestDTO.getDescription());
        classType.setOrganization(organization);
        return convertToDTO(classTypeRepository.save(classType));
    }

    @Transactional(readOnly = true)
    public List<ClassTypeResponseDTO> getClassTypesByOrganization(Integer organizationId) {
        if (!organizationRepository.existsById(organizationId)) {
             throw new EntityNotFoundException("Organization not found: " + organizationId);
        }
        return classTypeRepository.findByOrganizationId(organizationId).stream()
            .map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ClassTypeResponseDTO getClassTypeById(Integer classTypeId) {
        ClassType classType = classTypeRepository.findById(classTypeId)
            .orElseThrow(() -> new EntityNotFoundException("ClassType not found: " + classTypeId));
        return convertToDTO(classType);
    }
        // (Existing methods: constructor, convertToDTO, createClassType, getClassTypesByOrganization, getClassTypeById are above this)

    @Transactional
    public ClassTypeResponseDTO updateClassType(Integer classTypeId, ClassTypeRequestDTO requestDTO) {
        ClassType classType = classTypeRepository.findById(classTypeId)
            .orElseThrow(() -> new EntityNotFoundException("ClassType not found with id: " + classTypeId));

        // Ensure the organization isn't being changed if that's not allowed,
        // or handle organization change carefully if it is.
        // For now, let's assume organizationId in requestDTO should match the existing one or be ignored.
        // If requestDTO.getOrganizationId() is present and different from classType.getOrganization().getId(),
        // you might throw an error or handle re-association.
        // For simplicity here, we're not changing the organization of an existing class type.

        // Check for name uniqueness if the name is being changed
        if (requestDTO.getName() != null && !requestDTO.getName().equals(classType.getName())) {
            classTypeRepository.findByOrganizationIdAndName(classType.getOrganization().getId(), requestDTO.getName())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(classTypeId)) { // Ensure it's not the same entity
                        throw new IllegalArgumentException("Another ClassType with name '" + requestDTO.getName() + "' already exists for this organization.");
                    }
                });
            classType.setName(requestDTO.getName());
        }

        if (requestDTO.getDescription() != null) {
            classType.setDescription(requestDTO.getDescription());
        }
        // Note: createdAt and organization are generally not updated.
        // updatedAt will be handled by @UpdateTimestamp.

        ClassType updatedClassType = classTypeRepository.save(classType);
        return convertToDTO(updatedClassType);
    }

    @Transactional
    public void deleteClassType(Integer classTypeId) {
        if (!classTypeRepository.existsById(classTypeId)) {
            throw new EntityNotFoundException("ClassType not found with id: " + classTypeId);
        }
        // Consider consequences: What happens if ClassDefinitions are linked to this ClassType?
        // Your DB schema for class_definitions has:
        // FOREIGN KEY (class_type_id) REFERENCES public.class_types(id) ON DELETE RESTRICT
        // This 'ON DELETE RESTRICT' means the database will PREVENT deletion of a ClassType
        // if any ClassDefinition still refers to it. This is good for data integrity.
        // So, a PSQLException would be thrown by the .deleteById if it's in use.
        // You might want to catch this specifically or add a check here.
        // For now, we'll let the DB constraint handle it.
        try {
            classTypeRepository.deleteById(classTypeId);
        } catch (DataIntegrityViolationException e) { // Example of catching specific JPA/DB exceptions
            // This is a Spring Data exception that often wraps DB constraint violations
            throw new IllegalStateException("Cannot delete ClassType id " + classTypeId + ": it may be in use by ClassDefinitions.", e);
        }
    }
}