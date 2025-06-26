package com.example.studioapp_api.dto;

import lombok.Data;
import jakarta.validation.constraints.Size; // Optional

@Data
public class UpdateNotesRequestDTO {
    @Size(max = 1000, message = "Notes cannot exceed 1000 characters.")
    private String notes;
}