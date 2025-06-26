package com.example.studioapp_api.controller;

import com.example.studioapp_api.dto.OrganizationRequestDTO;
import com.example.studioapp_api.dto.OrganizationResponseDTO;
import com.example.studioapp_api.service.OrganizationService;
import jakarta.validation.Valid; // UNCOMMENT and add this import
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationController {

    private final OrganizationService organizationService;

    @Autowired
    public OrganizationController(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }

    @PostMapping
    public ResponseEntity<OrganizationResponseDTO> createOrganization(
            @Valid @RequestBody OrganizationRequestDTO organizationRequestDTO) { // ADDED @Valid
        OrganizationResponseDTO createdOrganization = organizationService.createOrganization(organizationRequestDTO);
        return new ResponseEntity<>(createdOrganization, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<OrganizationResponseDTO>> getAllOrganizations() {
        List<OrganizationResponseDTO> organizations = organizationService.getAllOrganizations();
        return ResponseEntity.ok(organizations);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrganizationResponseDTO> getOrganizationById(@PathVariable Integer id) {
        OrganizationResponseDTO organization = organizationService.getOrganizationById(id);
        return ResponseEntity.ok(organization);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrganizationResponseDTO> updateOrganization(
            @PathVariable Integer id,
            @Valid @RequestBody OrganizationRequestDTO organizationRequestDTO) { // ADDED @Valid
        OrganizationResponseDTO updatedOrganization = organizationService.updateOrganization(id, organizationRequestDTO);
        return ResponseEntity.ok(updatedOrganization);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrganization(@PathVariable Integer id) {
        organizationService.deleteOrganization(id);
        return ResponseEntity.noContent().build();
    }
}