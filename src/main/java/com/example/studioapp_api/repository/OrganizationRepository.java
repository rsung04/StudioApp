package com.example.studioapp_api.repository;

import com.example.studioapp_api.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository // Marks this as a Spring Data repository
public interface OrganizationRepository extends JpaRepository<Organization, Integer> {
    // JpaRepository<EntityType, IdType>

    // Spring Data JPA will automatically implement methods like:
    // - save(Organization organization)
    // - findById(Integer id)
    // - findAll()
    // - deleteById(Integer id)
    // ...and many more.

    // You can also define custom query methods just by their name:
    Optional<Organization> findByName(String name);
    Optional<Organization> findBySubdomain(String subdomain);

}