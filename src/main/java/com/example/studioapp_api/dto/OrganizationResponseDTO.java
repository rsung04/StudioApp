package com.example.studioapp_api.dto;

import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class OrganizationResponseDTO {
    private Integer id;
    private String name;
    private String subdomain;
    private String contactEmail;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}