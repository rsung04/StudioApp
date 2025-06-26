package com.example.studioapp_api.dto;

import lombok.Data;
// UNCOMMENT and add these imports for Jakarta Bean Validation
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Email; // For email validation

@Data
public class OrganizationRequestDTO {

    @NotBlank(message = "Organization name cannot be blank")
    @Size(max = 255, message = "Organization name cannot exceed 255 characters")
    private String name;

    @Size(max = 100, message = "Subdomain cannot exceed 100 characters")
    // Subdomain might be optional, so @NotBlank might not be appropriate
    // If it has specific patterns (e.g., alphanumeric, no spaces), @Pattern could be used.
    private String subdomain; 

    @Email(message = "Invalid email format for contact email") // Validates email format
    @Size(max = 255, message = "Contact email cannot exceed 255 characters")
    // ContactEmail is also likely optional in some contexts, so @NotBlank might be too strict
    // If it must be present when not null, or some other condition, service logic might handle it.
    private String contactEmail; 
}