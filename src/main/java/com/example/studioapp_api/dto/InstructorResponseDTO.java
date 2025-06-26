package com.example.studioapp_api.dto;

import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class InstructorResponseDTO {
    private Integer id;
    private String name;
    private String email;
    private String phoneNumber;
    private String bio;
    private String instagramUsername;
    private String tiktokUsername;
    private Integer organizationId;
    private String organizationName; // Denormalized
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    // We can add lists of availability, qualifications etc. here later
    // when those APIs/DTOs are built.
}