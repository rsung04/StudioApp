package com.example.studioapp_api.controller;

import com.example.studioapp_api.dto.RoomRequestDTO;
import com.example.studioapp_api.dto.RoomResponseDTO;
import com.example.studioapp_api.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class RoomController {

    private final RoomService roomService;

    @Autowired
    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping("/studiolocations/{studioLocationId}/rooms")
    public ResponseEntity<RoomResponseDTO> createRoom(
            @PathVariable Integer studioLocationId,
            @Valid @RequestBody RoomRequestDTO requestDTO) {
        
        if (requestDTO.getStudioLocationId() == null) {
            requestDTO.setStudioLocationId(studioLocationId);
        } else if (!requestDTO.getStudioLocationId().equals(studioLocationId)) {
             throw new IllegalArgumentException("StudioLocation ID in path ("+ studioLocationId +
                                               ") does not match StudioLocation ID in request body (" + 
                                               requestDTO.getStudioLocationId() + ").");
        }
        RoomResponseDTO createdRoom = roomService.createRoom(requestDTO);
        return new ResponseEntity<>(createdRoom, HttpStatus.CREATED);
    }

    @GetMapping("/studiolocations/{studioLocationId}/rooms")
    public ResponseEntity<List<RoomResponseDTO>> getRoomsByStudioLocation(
            @PathVariable Integer studioLocationId) {
        List<RoomResponseDTO> rooms = roomService.getRoomsByStudioLocation(studioLocationId);
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/rooms/{roomId}")
    public ResponseEntity<RoomResponseDTO> getRoomById(@PathVariable Integer roomId) {
        RoomResponseDTO room = roomService.getRoomById(roomId);
        return ResponseEntity.ok(room);
    }

    @PutMapping("/rooms/{roomId}")
    public ResponseEntity<RoomResponseDTO> updateRoom(
            @PathVariable Integer roomId,
            @Valid @RequestBody RoomRequestDTO requestDTO) {
        RoomResponseDTO updatedRoom = roomService.updateRoom(roomId, requestDTO);
        return ResponseEntity.ok(updatedRoom);
    }

    @DeleteMapping("/rooms/{roomId}")
    public ResponseEntity<Void> deleteRoom(@PathVariable Integer roomId) {
        roomService.deleteRoom(roomId);
        return ResponseEntity.noContent().build();
    }
}