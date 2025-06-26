package com.example.studioapp_api.repository;

import com.example.studioapp_api.entity.Term;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TermRepository extends JpaRepository<Term, Integer> {

    // Find by organization ID
    List<Term> findByOrganizationId(Integer organizationId);

    // Find by organization ID and studio location ID (location can be null)
    List<Term> findByOrganizationIdAndStudioLocationId(Integer organizationId, Integer studioLocationId);

    // Find active planning terms for an organization (studioLocationId might be null or specified)
    @Query("SELECT t FROM Term t WHERE t.organization.id = :organizationId AND t.isActiveForPlanning = true AND (:studioLocationId IS NULL OR t.studioLocation.id = :studioLocationId)")
    List<Term> findActivePlanningTerms(@Param("organizationId") Integer organizationId, @Param("studioLocationId") Integer studioLocationId);

    // Variation for org-wide active planning term (studioLocation IS NULL)
    @Query("SELECT t FROM Term t WHERE t.organization.id = :organizationId AND t.isActiveForPlanning = true AND t.studioLocation IS NULL")
    List<Term> findOrgWideActivePlanningTerms(@Param("organizationId") Integer organizationId);


    // For unique constraint (orgId, studioLocationId, name)
    Optional<Term> findByOrganizationIdAndStudioLocationIdAndName(Integer organizationId, Integer studioLocationId, String name);

    // For unique constraint (orgId, name) where studioLocationId IS NULL
    Optional<Term> findByOrganizationIdAndStudioLocationIdIsNullAndName(Integer organizationId, String name);

}