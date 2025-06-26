package com.example.studioapp_api.service;

import com.example.studioapp_api.dto.InstructorClassQualificationRequestDTO;
import com.example.studioapp_api.dto.InstructorClassQualificationResponseDTO;
import com.example.studioapp_api.entity.*; // For Instructor, ClassDefinition, etc.
import com.example.studioapp_api.repository.*; // For the repositories
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class InstructorClassQualificationService {

    private final InstructorClassQualificationRepository qualificationRepository;
    private final InstructorRepository instructorRepository;
    private final ClassDefinitionRepository classDefinitionRepository;

    public InstructorClassQualificationService(
            InstructorClassQualificationRepository qualificationRepository,
            InstructorRepository instructorRepository,
            ClassDefinitionRepository classDefinitionRepository) {
        this.qualificationRepository = qualificationRepository;
        this.instructorRepository = instructorRepository;
        this.classDefinitionRepository = classDefinitionRepository;
    }

    private InstructorClassQualificationResponseDTO convertToDTO(InstructorClassQualification qual) {
        InstructorClassQualificationResponseDTO dto = new InstructorClassQualificationResponseDTO();
        dto.setInstructorId(qual.getInstructor().getId());
        dto.setInstructorName(qual.getInstructor().getName());
        dto.setClassDefinitionId(qual.getClassDefinition().getId());
        dto.setClassDefinitionName(qual.getClassDefinition().getName());
        dto.setClassDefinitionCode(qual.getClassDefinition().getClassCode());
        dto.setNotes(qual.getNotes());
        dto.setCreatedAt(qual.getCreatedAt());
        dto.setUpdatedAt(qual.getUpdatedAt());
        return dto;
    }

    @Transactional
    public InstructorClassQualificationResponseDTO addQualification(InstructorClassQualificationRequestDTO requestDTO) {
        Instructor instructor = instructorRepository.findById(requestDTO.getInstructorId())
            .orElseThrow(() -> new EntityNotFoundException("Instructor not found: " + requestDTO.getInstructorId()));
        
        ClassDefinition classDefinition = classDefinitionRepository.findById(requestDTO.getClassDefinitionId())
            .orElseThrow(() -> new EntityNotFoundException("ClassDefinition not found: " + requestDTO.getClassDefinitionId()));

        if (!instructor.getOrganization().getId().equals(classDefinition.getOrganization().getId())) {
            throw new IllegalArgumentException("Instructor and ClassDefinition must belong to the same organization.");
        }

        InstructorClassQualificationId id = new InstructorClassQualificationId(instructor.getId(), classDefinition.getId());
        if (qualificationRepository.existsById(id)) {
            throw new IllegalArgumentException("This instructor is already qualified for this class definition.");
        }

        InstructorClassQualification qualification = new InstructorClassQualification();
        qualification.setId(id);
        qualification.setInstructor(instructor);
        qualification.setClassDefinition(classDefinition);
        qualification.setNotes(requestDTO.getNotes());
        // At this point, createdAt and updatedAt on the 'qualification' object are null.
        // Hibernate's @CreationTimestamp and @UpdateTimestamp are supposed to fill them
        // during the persist operation, or the DB defaults will.

        // Step 1: Save the entity. Hibernate will generate the ID and potentially timestamps.
        // Using saveAndFlush to ensure the SQL INSERT happens now.
        qualificationRepository.saveAndFlush(qualification); 
        // After this, the 'qualification' object *might* have timestamps if Hibernate updated it,
        // OR the database has set them via DEFAULT CURRENT_TIMESTAMP.
        // The 'qualification' instance here may or may not be the fully updated one from the persistence context.

        // Step 2: Re-fetch the entity by its ID to get the definitive state from the database.
        // This will include any values set by DB defaults or triggers (like your CURRENT_TIMESTAMP).
        InstructorClassQualification savedAndRefetchedQualification = qualificationRepository.findById(id)
            .orElseThrow(() -> new IllegalStateException("Failed to re-fetch qualification after save, ID: " + id + 
                                                        ". This should not happen if saveAndFlush succeeded."));
                                                        
        // --- Logging for Debugging ---
        System.out.println("Saved & Refetched Qualification CreatedAt: " + savedAndRefetchedQualification.getCreatedAt());
        System.out.println("Saved & Refetched Qualification UpdatedAt: " + savedAndRefetchedQualification.getUpdatedAt());
        // --- End Logging ---

        return convertToDTO(savedAndRefetchedQualification); // Convert the re-fetched entity
    }

    @Transactional(readOnly = true)
    public List<InstructorClassQualificationResponseDTO> getQualificationsByInstructor(Integer instructorId) {
        if (!instructorRepository.existsById(instructorId)) {
            throw new EntityNotFoundException("Instructor not found: " + instructorId);
        }
        return qualificationRepository.findByIdInstructorId(instructorId).stream()
            .map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InstructorClassQualificationResponseDTO> getQualificationsByClassDefinition(Integer classDefinitionId) {
         if (!classDefinitionRepository.existsById(classDefinitionId)) {
            throw new EntityNotFoundException("ClassDefinition not found: " + classDefinitionId);
        }
        return qualificationRepository.findByIdClassDefinitionId(classDefinitionId).stream()
            .map(this::convertToDTO).collect(Collectors.toList());
    }
    
    @Transactional
    public InstructorClassQualificationResponseDTO updateQualificationNotes(
        Integer instructorId, Integer classDefinitionId, String notes) {
        InstructorClassQualificationId id = new InstructorClassQualificationId(instructorId, classDefinitionId);
        InstructorClassQualification qualification = qualificationRepository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("Qualification not found for instructor " + instructorId + " and class " + classDefinitionId));
        
        qualification.setNotes(notes);
        return convertToDTO(qualificationRepository.save(qualification));
    }


    @Transactional
    public void removeQualification(Integer instructorId, Integer classDefinitionId) {
        InstructorClassQualificationId id = new InstructorClassQualificationId(instructorId, classDefinitionId);
        if (!qualificationRepository.existsById(id)) {
            throw new EntityNotFoundException("Qualification not found for instructor " + instructorId + " and class " + classDefinitionId);
        }
        qualificationRepository.deleteById(id);
    }
}