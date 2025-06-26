package com.example.studioapp_api.repository;

import com.example.studioapp_api.entity.Instructor;
// import com.example.studioapp_api.entity.Organization; // Not strictly needed if using ByOrganizationId
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstructorRepository extends JpaRepository<Instructor, Integer> {

    List<Instructor> findByOrganizationId(Integer organizationId);

    // For unique constraint (organization_id, name)
    Optional<Instructor> findByOrganizationIdAndName(Integer organizationId, String name);

    // For unique constraint (organization_id, email) - email can be null, so handle that
    Optional<Instructor> findByOrganizationIdAndEmail(Integer organizationId, String email);
}