package com.example.studioapp_api.repository;

import com.example.studioapp_api.entity.ClassDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassDefinitionRepository extends JpaRepository<ClassDefinition, Integer> {
    List<ClassDefinition> findByOrganizationId(Integer organizationId);
    Optional<ClassDefinition> findByOrganizationIdAndClassCode(Integer organizationId, String classCode);
    List<ClassDefinition> findByOrganizationIdAndClassTypeId(Integer organizationId, Integer classTypeId);
}