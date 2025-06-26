package com.example.studioapp_api.dto;

import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class StudioLocationResponseDTO {
    private Integer id;
    private String name;
    private String address;
    private Integer organizationId;
    private String organizationName; // Denormalized
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    // In the future, could include a list of RoomResponseDTOs if needed for specific use cases
}