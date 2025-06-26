package com.example.studioapp_api.service;

import com.example.studioapp_api.dto.RoomRequestDTO;
import com.example.studioapp_api.dto.RoomResponseDTO;
import com.example.studioapp_api.entity.Room;
import com.example.studioapp_api.entity.StudioLocation;
import com.example.studioapp_api.repository.RoomRepository;
import com.example.studioapp_api.repository.StudioLocationRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final StudioLocationRepository studioLocationRepository;

    @Autowired
    public RoomService(RoomRepository roomRepository, StudioLocationRepository studioLocationRepository) {
        this.roomRepository = roomRepository;
        this.studioLocationRepository = studioLocationRepository;
    }

    private RoomResponseDTO convertToDTO(Room room) {
        RoomResponseDTO dto = new RoomResponseDTO();
        dto.setId(room.getId());
        dto.setName(room.getName());
        dto.setCapacity(room.getCapacity());
        if (room.getStudioLocation() != null) {
            dto.setStudioLocationId(room.getStudioLocation().getId());
            dto.setStudioLocationName(room.getStudioLocation().getName());
            if (room.getStudioLocation().getOrganization() != null) {
                dto.setOrganizationId(room.getStudioLocation().getOrganization().getId());
                dto.setOrganizationName(room.getStudioLocation().getOrganization().getName());
            }
        }
        dto.setCreatedAt(room.getCreatedAt());
        dto.setUpdatedAt(room.getUpdatedAt());
        return dto;
    }

    private void mapDtoToEntity(RoomRequestDTO dto, Room room, StudioLocation studioLocation) {
        room.setName(dto.getName());
        room.setCapacity(dto.getCapacity());
        room.setStudioLocation(studioLocation);
    }

    @Transactional
    public RoomResponseDTO createRoom(RoomRequestDTO requestDTO) {
        StudioLocation studioLocation = studioLocationRepository.findById(requestDTO.getStudioLocationId())
            .orElseThrow(() -> new EntityNotFoundException("StudioLocation not found with id: " + requestDTO.getStudioLocationId()));

        // Check for uniqueness: uq_rooms_location_name
        roomRepository.findByStudioLocationAndName(studioLocation, requestDTO.getName())
            .ifPresent(r -> {
                throw new IllegalArgumentException("Room with name '" + requestDTO.getName() +
                                                   "' already exists for this studio location.");
            });
        
        if (requestDTO.getCapacity() != null && requestDTO.getCapacity() <=0) {
            throw new IllegalArgumentException("Capacity must be positive if specified.");
        }

        Room room = new Room();
        mapDtoToEntity(requestDTO, room, studioLocation);
        
        roomRepository.saveAndFlush(room);
        Room savedAndRefetched = roomRepository.findById(room.getId())
            .orElseThrow(() -> new IllegalStateException("Failed to re-fetch Room after save."));
        return convertToDTO(savedAndRefetched);
    }

    @Transactional(readOnly = true)
    public List<RoomResponseDTO> getRoomsByStudioLocation(Integer studioLocationId) {
        if (!studioLocationRepository.existsById(studioLocationId)) {
            throw new EntityNotFoundException("StudioLocation not found with id: " + studioLocationId);
        }
        return roomRepository.findByStudioLocationId(studioLocationId)
            .stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RoomResponseDTO getRoomById(Integer roomId) {
        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new EntityNotFoundException("Room not found with id: " + roomId));
        return convertToDTO(room);
    }

    @Transactional
    public RoomResponseDTO updateRoom(Integer roomId, RoomRequestDTO requestDTO) {
        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new EntityNotFoundException("Room not found with id: " + roomId));

        // Prevent changing studio location for simplicity
        if (requestDTO.getStudioLocationId() != null && !requestDTO.getStudioLocationId().equals(room.getStudioLocation().getId())) {
            throw new IllegalArgumentException("Changing the StudioLocation of a Room is not supported via this update method.");
        }

        if (requestDTO.getName() != null && !requestDTO.getName().equals(room.getName())) {
            roomRepository.findByStudioLocationAndName(room.getStudioLocation(), requestDTO.getName())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(roomId)) {
                        throw new IllegalArgumentException("Another Room with name '" + requestDTO.getName() +
                                                           "' already exists for this studio location.");
                    }
                });
            room.setName(requestDTO.getName());
        }

        if (requestDTO.getCapacity() != null) {
            if (requestDTO.getCapacity() <=0) {
                 throw new IllegalArgumentException("Capacity must be positive if specified.");
            }
            room.setCapacity(requestDTO.getCapacity());
        } else {
            room.setCapacity(null); // Allow clearing capacity
        }
        
        roomRepository.saveAndFlush(room);
        Room updatedAndRefetched = roomRepository.findById(roomId)
            .orElseThrow(() -> new IllegalStateException("Failed to re-fetch Room after update."));
        return convertToDTO(updatedAndRefetched);
    }

    @Transactional
    public void deleteRoom(Integer roomId) {
        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new EntityNotFoundException("Room not found with id: " + roomId));
        // DB Schema FKs referencing rooms:
        // - room_operating_hours.room_id (ON DELETE CASCADE)
        // - scheduled_events.room_id (ON DELETE RESTRICT) <-- This is the important one!
        try {
            roomRepository.delete(room);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("Cannot delete Room id " + roomId +
                                            ": it may be referenced by scheduled events or other entities with restrictive delete policies.", e);
        }
    }
}