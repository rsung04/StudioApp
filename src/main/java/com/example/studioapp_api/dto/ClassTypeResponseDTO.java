package com.example.studioapp_api.dto;
import lombok.Data;
import java.time.OffsetDateTime;
@Data
public class ClassTypeResponseDTO {
    private Integer id;
    private String name;
    private String description;
    private Integer organizationId;
    private String organizationName; // Denormalized
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}