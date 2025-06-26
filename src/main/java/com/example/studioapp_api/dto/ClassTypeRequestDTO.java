package com.example.studioapp_api.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
public class ClassTypeRequestDTO {

    @NotBlank(message = "Class type name is required and cannot be blank.")
    @Size(max = 255, message = "Class type name cannot exceed 255 characters.")
    private String name;

    // Description is optional (TEXT type in DB), so no @NotBlank.
    // You could add @Size if you want to limit its length from the API.
    private String description;

    @NotNull(message = "Organization ID is required.")
    private Integer organizationId;
}