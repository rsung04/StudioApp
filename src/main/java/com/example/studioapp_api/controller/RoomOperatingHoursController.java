package com.example.studioapp_api.controller;

import com.example.studioapp_api.dto.RoomOperatingHoursRequestDTO;
import com.example.studioapp_api.dto.RoomOperatingHoursResponseDTO;
import com.example.studioapp_api.service.RoomOperatingHoursService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class RoomOperatingHoursController {

    private final RoomOperatingHoursService operatingHoursService;

    @Autowired
    public RoomOperatingHoursController(RoomOperatingHoursService operatingHoursService) {
        this.operatingHoursService = operatingHoursService;
    }

    @PostMapping("/rooms/{roomId}/operating-hours")
    public ResponseEntity<RoomOperatingHoursResponseDTO> createOperatingHours(
            @PathVariable Integer roomId,
            @Valid @RequestBody RoomOperatingHoursRequestDTO requestDTO) {
        
        // Logic to ensure roomId from path matches/sets roomId in DTO
        if (requestDTO.getRoomId() == null) {
            requestDTO.setRoomId(roomId);
        } else if (!requestDTO.getRoomId().equals(roomId)) {
            throw new IllegalArgumentException("Room ID in path (" + roomId +
                                               ") does not match Room ID in request body (" +
                                               requestDTO.getRoomId() + ").");
        }
        RoomOperatingHoursResponseDTO createdHours = operatingHoursService.addOperatingHours(roomId, requestDTO);
        return new ResponseEntity<>(createdHours, HttpStatus.CREATED);
    }

    @GetMapping("/rooms/{roomId}/operating-hours")
    public ResponseEntity<List<RoomOperatingHoursResponseDTO>> getOperatingHoursByRoom(
            @PathVariable Integer roomId) {
        List<RoomOperatingHoursResponseDTO> hours = operatingHoursService.getOperatingHoursByRoom(roomId);
        return ResponseEntity.ok(hours);
    }

    @GetMapping("/operating-hours/{hoursId}")
    public ResponseEntity<RoomOperatingHoursResponseDTO> getOperatingHoursById(@PathVariable Integer hoursId) {
        RoomOperatingHoursResponseDTO hours = operatingHoursService.getOperatingHoursById(hoursId);
        return ResponseEntity.ok(hours);
    }

    @PutMapping("/operating-hours/{hoursId}")
    public ResponseEntity<RoomOperatingHoursResponseDTO> updateOperatingHours(
            @PathVariable Integer hoursId,
            @Valid @RequestBody RoomOperatingHoursRequestDTO requestDTO) {
        RoomOperatingHoursResponseDTO updatedHours = operatingHoursService.updateOperatingHours(hoursId, requestDTO);
        return ResponseEntity.ok(updatedHours);
    }

    @DeleteMapping("/operating-hours/{hoursId}")
    public ResponseEntity<Void> deleteOperatingHours(@PathVariable Integer hoursId) {
        operatingHoursService.deleteOperatingHours(hoursId);
        return ResponseEntity.noContent().build();
    }
}