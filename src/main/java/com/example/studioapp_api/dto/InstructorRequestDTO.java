package com.example.studioapp_api.dto;

import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank; // For non-null and non-empty strings
import jakarta.validation.constraints.NotNull;  // For non-null objects/numbers
import jakarta.validation.constraints.Size;

@Data
public class InstructorRequestDTO {

    @NotBlank(message = "Instructor name is required and cannot be blank.")
    @Size(max = 255, message = "Instructor name cannot exceed 255 characters.")
    private String name;

    @Email(message = "Please provide a valid email address if email is specified.")
    @Size(max = 255, message = "Email cannot exceed 255 characters.")
    private String email; // Optional, but if provided, must be valid email

    @Size(max = 50, message = "Phone number cannot exceed 50 characters.")
    private String phoneNumber; // Optional

    @Size(max = 2000, message = "Bio cannot exceed 2000 characters.") // Example if you want a limit
    private String bio; // Optional

    @Size(max = 100, message = "Instagram username cannot exceed 100 characters.")
    private String instagramUsername; // Optional

    @Size(max = 100, message = "TikTok username cannot exceed 100 characters.")
    private String tiktokUsername; // Optional

    @NotNull(message = "Organization ID is required to associate instructor.")
    private Integer organizationId;
}