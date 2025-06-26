package com.example.studioapp_api.controller;

import com.example.studioapp_api.dto.ClassDefinitionRequestDTO;
import com.example.studioapp_api.dto.ClassDefinitionResponseDTO;
import com.example.studioapp_api.service.ClassDefinitionService;
import jakarta.validation.Valid; // <<<--- ADD THIS IMPORT
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ClassDefinitionController {

    private final ClassDefinitionService classDefinitionService;

    public ClassDefinitionController(ClassDefinitionService classDefinitionService) {
        this.classDefinitionService = classDefinitionService;
    }

    @PostMapping("/organizations/{organizationId}/classdefinitions")
    public ResponseEntity<ClassDefinitionResponseDTO> createClassDefinition(
            @PathVariable Integer organizationId,
            @Valid @RequestBody ClassDefinitionRequestDTO requestDTO) { // <<<--- ADDED @Valid
        
        if (requestDTO.getOrganizationId() == null) {
            requestDTO.setOrganizationId(organizationId);
        } else if (!requestDTO.getOrganizationId().equals(organizationId)) {
             throw new IllegalArgumentException("Organization ID in path ("+ organizationId +
                                               ") does not match Organization ID in request body (" + 
                                               requestDTO.getOrganizationId() + ").");
        }
        ClassDefinitionResponseDTO created = classDefinitionService.createClassDefinition(requestDTO);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/organizations/{organizationId}/classdefinitions")
    public ResponseEntity<List<ClassDefinitionResponseDTO>> getClassDefinitionsByOrganization(
            @PathVariable Integer organizationId) {
        List<ClassDefinitionResponseDTO> definitions = classDefinitionService.getClassDefinitionsByOrganization(organizationId);
        return ResponseEntity.ok(definitions);
    }

    @GetMapping("/classdefinitions/{id}")
    public ResponseEntity<ClassDefinitionResponseDTO> getClassDefinitionById(@PathVariable Integer id) {
        ClassDefinitionResponseDTO definition = classDefinitionService.getClassDefinitionById(id);
        return ResponseEntity.ok(definition);
    }

    @PutMapping("/classdefinitions/{id}")
    public ResponseEntity<ClassDefinitionResponseDTO> updateClassDefinition(
            @PathVariable Integer id,
            @Valid @RequestBody ClassDefinitionRequestDTO requestDTO) { // <<<--- ADDED @Valid
        ClassDefinitionResponseDTO updated = classDefinitionService.updateClassDefinition(id, requestDTO);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/classdefinitions/{id}")
    public ResponseEntity<Void> deleteClassDefinition(@PathVariable Integer id) {
        classDefinitionService.deleteClassDefinition(id);
        return ResponseEntity.noContent().build();
    }
}