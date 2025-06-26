package com.example.studioapp_api.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size; // Optional for notes

@Data
public class InstructorClassQualificationRequestDTO {
    @NotNull
    private Integer instructorId;

    @NotNull
    private Integer classDefinitionId;

    @Size(max = 1000, message = "Notes cannot exceed 1000 characters.") // Example if you want a limit
    private String notes; // Optional
}