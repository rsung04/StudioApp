package com.example.studioapp_api.service;

import com.example.studioapp_api.dto.RoomOperatingHoursRequestDTO;
import com.example.studioapp_api.dto.RoomOperatingHoursResponseDTO;
import com.example.studioapp_api.entity.Room;
import com.example.studioapp_api.entity.RoomOperatingHours;
import com.example.studioapp_api.repository.RoomOperatingHoursRepository;
import com.example.studioapp_api.repository.RoomRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RoomOperatingHoursService {

    private final RoomOperatingHoursRepository operatingHoursRepository;
    private final RoomRepository roomRepository;

    @Autowired
    public RoomOperatingHoursService(RoomOperatingHoursRepository operatingHoursRepository,
                                     RoomRepository roomRepository) {
        this.operatingHoursRepository = operatingHoursRepository;
        this.roomRepository = roomRepository;
    }

    private RoomOperatingHoursResponseDTO convertToDTO(RoomOperatingHours hours) {
        RoomOperatingHoursResponseDTO dto = new RoomOperatingHoursResponseDTO();
        dto.setId(hours.getId());
        if (hours.getRoom() != null) {
            dto.setRoomId(hours.getRoom().getId());
            dto.setRoomName(hours.getRoom().getName());
        }
        dto.setDayOfWeek(hours.getDayOfWeek());
        dto.setStartTime(hours.getStartTime());
        dto.setEndTime(hours.getEndTime());
        dto.setCreatedAt(hours.getCreatedAt());
        dto.setUpdatedAt(hours.getUpdatedAt());
        return dto;
    }

    @Transactional
    public RoomOperatingHoursResponseDTO addOperatingHours(Integer roomIdFromPath, RoomOperatingHoursRequestDTO requestDTO) {
        Integer effectiveRoomId = requestDTO.getRoomId() != null ? requestDTO.getRoomId() : roomIdFromPath;
        if (!effectiveRoomId.equals(roomIdFromPath) && requestDTO.getRoomId() != null) {
             throw new IllegalArgumentException("Room ID in path does not match ID in request body.");
        }

        Room room = roomRepository.findById(effectiveRoomId)
            .orElseThrow(() -> new EntityNotFoundException("Room not found with id: " + effectiveRoomId));

        // Check for uniqueness: uq_room_operating_hours_day (room_id, day_of_week)
        operatingHoursRepository.findByRoomAndDayOfWeek(room, requestDTO.getDayOfWeek())
            .ifPresent(roh -> {
                throw new IllegalArgumentException("Operating hours for room '" + room.getName() +
                                                   "' on " + requestDTO.getDayOfWeek() + " already exist.");
            });
        
        if (requestDTO.getEndTime().isBefore(requestDTO.getStartTime()) || requestDTO.getEndTime().equals(requestDTO.getStartTime())) {
            throw new IllegalArgumentException("End time must be after start time.");
        }

        RoomOperatingHours newHours = new RoomOperatingHours();
        newHours.setRoom(room); // Essential for the relationship
        newHours.setDayOfWeek(requestDTO.getDayOfWeek());
        newHours.setStartTime(requestDTO.getStartTime());
        newHours.setEndTime(requestDTO.getEndTime());
        
        // Room entity helper method for bidirectional consistency (optional but good)
        // room.addOperatingHours(newHours); // If Room.addOperatingHours also saves room, manage transaction carefully
        // For simplicity, just save newHours. The relationship is owned by RoomOperatingHours via room_id.
        
        operatingHoursRepository.saveAndFlush(newHours);
        RoomOperatingHours savedAndRefetched = operatingHoursRepository.findById(newHours.getId())
            .orElseThrow(() -> new IllegalStateException("Failed to re-fetch RoomOperatingHours after save."));
        return convertToDTO(savedAndRefetched);
    }

    @Transactional(readOnly = true)
    public List<RoomOperatingHoursResponseDTO> getOperatingHoursByRoom(Integer roomId) {
        if (!roomRepository.existsById(roomId)) {
            throw new EntityNotFoundException("Room not found with id: " + roomId);
        }
        return operatingHoursRepository.findByRoomId(roomId)
            .stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RoomOperatingHoursResponseDTO getOperatingHoursById(Integer hoursId) {
        RoomOperatingHours hours = operatingHoursRepository.findById(hoursId)
            .orElseThrow(() -> new EntityNotFoundException("RoomOperatingHours not found with id: " + hoursId));
        return convertToDTO(hours);
    }

    @Transactional
    public RoomOperatingHoursResponseDTO updateOperatingHours(Integer hoursId, RoomOperatingHoursRequestDTO requestDTO) {
        RoomOperatingHours existingHours = operatingHoursRepository.findById(hoursId)
            .orElseThrow(() -> new EntityNotFoundException("RoomOperatingHours not found with id: " + hoursId));

        // Prevent changing the room for an existing operating hours entry
        if (requestDTO.getRoomId() != null && !requestDTO.getRoomId().equals(existingHours.getRoom().getId())) {
            throw new IllegalArgumentException("Changing the Room of an operating hours entry is not supported.");
        }

        // If dayOfWeek is changing, check uniqueness for the new day
        if (requestDTO.getDayOfWeek() != null && !requestDTO.getDayOfWeek().equals(existingHours.getDayOfWeek())) {
            operatingHoursRepository.findByRoomAndDayOfWeek(existingHours.getRoom(), requestDTO.getDayOfWeek())
                .ifPresent(roh -> {
                    if (!roh.getId().equals(hoursId)) { // Make sure it's not the same entry
                        throw new IllegalArgumentException("Operating hours for room '" + existingHours.getRoom().getName() +
                                                           "' on " + requestDTO.getDayOfWeek() + " already exist.");
                    }
                });
            existingHours.setDayOfWeek(requestDTO.getDayOfWeek());
        }
        
        if (requestDTO.getStartTime() != null) existingHours.setStartTime(requestDTO.getStartTime());
        if (requestDTO.getEndTime() != null) existingHours.setEndTime(requestDTO.getEndTime());

        if (existingHours.getEndTime().isBefore(existingHours.getStartTime()) || existingHours.getEndTime().equals(existingHours.getStartTime())) {
            throw new IllegalArgumentException("End time must be after start time.");
        }

        operatingHoursRepository.saveAndFlush(existingHours);
        RoomOperatingHours updatedAndRefetched = operatingHoursRepository.findById(hoursId)
            .orElseThrow(() -> new IllegalStateException("Failed to re-fetch RoomOperatingHours after update."));
        return convertToDTO(updatedAndRefetched);
    }

    @Transactional
    public void deleteOperatingHours(Integer hoursId) {
        if (!operatingHoursRepository.existsById(hoursId)) {
            throw new EntityNotFoundException("RoomOperatingHours not found with id: " + hoursId);
        }
        // No direct FKs from other tables to room_operating_hours in your schema (other than its own room_id)
        // that would prevent deletion with RESTRICT.
        operatingHoursRepository.deleteById(hoursId);
    }
}