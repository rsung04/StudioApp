package com.example.studioapp_api.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
public class StudioLocationRequestDTO {

    @NotBlank(message = "Studio location name is required and cannot be blank.")
    @Size(max = 255, message = "Studio location name cannot exceed 255 characters.")
    private String name;

    // Address is optional (TEXT type in DB). You could add @Size if a practical limit is desired.
    // @Size(max = 1000, message = "Address cannot exceed 1000 characters.")
    private String address;

    @NotNull(message = "Organization ID is required to associate the studio location.")
    private Integer organizationId;
}