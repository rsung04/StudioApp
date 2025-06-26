package com.example.studioapp_api.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;

@Data
public class RoomRequestDTO {

    @NotBlank(message = "Room name is required and cannot be blank.")
    @Size(max = 255, message = "Room name cannot exceed 255 characters.")
    private String name;

    @Min(value = 1, message = "Capacity must be at least 1, if specified.")
    // Capacity is optional, so @NotNull is not used.
    // The @Min annotation will only be checked if the value is not null.
    private Integer capacity; 

    @NotNull(message = "Studio Location ID is required to associate the room.")
    private Integer studioLocationId;
}