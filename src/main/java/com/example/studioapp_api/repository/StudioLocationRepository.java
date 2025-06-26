package com.example.studioapp_api.repository;

import com.example.studioapp_api.entity.StudioLocation;
import com.example.studioapp_api.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudioLocationRepository extends JpaRepository<StudioLocation, Integer> {

    Optional<StudioLocation> findByOrganizationAndName(Organization organization, String name);

    List<StudioLocation> findByOrganization(Organization organization);

    List<StudioLocation> findByOrganizationId(Integer organizationId);
}