package com.example.studioapp_api.controller;

import com.example.studioapp_api.dto.ClassTypeRequestDTO;
import com.example.studioapp_api.dto.ClassTypeResponseDTO;
import com.example.studioapp_api.service.ClassTypeService;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class ClassTypeController {
    private final ClassTypeService classTypeService;

    public ClassTypeController(ClassTypeService classTypeService) {
        this.classTypeService = classTypeService;
    }

    @PostMapping("/organizations/{organizationId}/classtypes")
    public ResponseEntity<ClassTypeResponseDTO> createClassType(
            @PathVariable Integer organizationId, 
            @Valid @RequestBody ClassTypeRequestDTO requestDTO) {
        // Ensure DTO's organizationId matches path variable or set it if not present in DTO
        if (requestDTO.getOrganizationId() == null) {
            requestDTO.setOrganizationId(organizationId);
        } else if (!requestDTO.getOrganizationId().equals(organizationId)) {
            // Or throw an exception for mismatch
            return ResponseEntity.badRequest().build(); // Example: mismatch
        }
        ClassTypeResponseDTO created = classTypeService.createClassType(requestDTO);
        return new ResponseEntity<>(created, HttpStatus.CREATED);
    }

    @GetMapping("/organizations/{organizationId}/classtypes")
    public ResponseEntity<List<ClassTypeResponseDTO>> getClassTypesByOrganization(@PathVariable Integer organizationId) {
        List<ClassTypeResponseDTO> classTypes = classTypeService.getClassTypesByOrganization(organizationId);
        return ResponseEntity.ok(classTypes);
    }

    @GetMapping("/classtypes/{classTypeId}")
    public ResponseEntity<ClassTypeResponseDTO> getClassTypeById(@PathVariable Integer classTypeId) {
        ClassTypeResponseDTO classType = classTypeService.getClassTypeById(classTypeId);
        return ResponseEntity.ok(classType);
    }
        // (Existing methods: constructor, createClassType, getClassTypesByOrganization, getClassTypeById are above this)

    @PutMapping("/classtypes/{classTypeId}")
    public ResponseEntity<ClassTypeResponseDTO> updateClassType(
            @PathVariable Integer classTypeId,
            @Valid @RequestBody ClassTypeRequestDTO requestDTO) { // Add @Valid if using validation
        // Note: The organizationId is usually not part of the update path for a nested resource if
        // the resource's organization cannot change. If it could change, the requestDTO would need it.
        // Here, we assume the ClassType's organization is fixed.
        ClassTypeResponseDTO updatedClassType = classTypeService.updateClassType(classTypeId, requestDTO);
        return ResponseEntity.ok(updatedClassType);
    }

    @DeleteMapping("/classtypes/{classTypeId}")
    public ResponseEntity<Void> deleteClassType(@PathVariable Integer classTypeId) {
        classTypeService.deleteClassType(classTypeId);
        return ResponseEntity.noContent().build(); // Standard response for successful DELETE
    }
}