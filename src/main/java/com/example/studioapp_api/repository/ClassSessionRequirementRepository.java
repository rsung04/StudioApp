package com.example.studioapp_api.repository;

import com.example.studioapp_api.entity.ClassSessionRequirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassSessionRequirementRepository 
    extends JpaRepository<ClassSessionRequirement, Integer> {

    List<ClassSessionRequirement> findByTermId(Integer termId);
    List<ClassSessionRequirement> findByTermIdAndClassDefinitionId(Integer termId, Integer classDefinitionId);
    
    // For uniqueness check where studioLocationId is NOT NULL
    Optional<ClassSessionRequirement> findByTermIdAndClassDefinitionIdAndStudioLocationId(
            Integer termId, Integer classDefinitionId, Integer studioLocationId);

    // For uniqueness check where studioLocationId IS NULL
    Optional<ClassSessionRequirement> findByTermIdAndClassDefinitionIdAndStudioLocationIsNull(
            Integer termId, Integer classDefinitionId);
}