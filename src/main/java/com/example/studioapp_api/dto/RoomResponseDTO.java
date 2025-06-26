package com.example.studioapp_api.dto;

import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class RoomResponseDTO {
    private Integer id;
    private String name;
    private Integer capacity;
    private Integer studioLocationId;
    private String studioLocationName; // Denormalized
    private Integer organizationId;    // Denormalized from StudioLocation's Organization
    private String organizationName;   // Denormalized
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    // Could add List<RoomOperatingHoursResponseDTO> here in the future
}