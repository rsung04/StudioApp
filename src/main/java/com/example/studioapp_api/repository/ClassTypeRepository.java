package com.example.studioapp_api.repository;

import com.example.studioapp_api.entity.ClassType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClassTypeRepository extends JpaRepository<ClassType, Integer> {
    List<ClassType> findByOrganizationId(Integer organizationId);
    Optional<ClassType> findByOrganizationIdAndName(Integer organizationId, String name);
}