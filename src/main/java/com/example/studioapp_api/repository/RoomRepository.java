package com.example.studioapp_api.repository;

import com.example.studioapp_api.entity.Room;
import com.example.studioapp_api.entity.StudioLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Integer> {

    // Find all rooms belonging to a specific studio location object
    List<Room> findByStudioLocation(StudioLocation studioLocation);

    // Find all rooms belonging to a specific studio location by its ID
    List<Room> findByStudioLocationId(Integer studioLocationId);

    // Find a specific room by its name within a specific studio location object
    Optional<Room> findByStudioLocationAndName(StudioLocation studioLocation, String name);

    // Find a specific room by its name within a specific studio location by ID
    Optional<Room> findByStudioLocationIdAndName(Integer studioLocationId, String name);
}