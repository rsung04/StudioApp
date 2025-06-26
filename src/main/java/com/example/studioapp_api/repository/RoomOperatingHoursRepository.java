package com.example.studioapp_api.repository;

import com.example.studioapp_api.entity.Room;
import com.example.studioapp_api.entity.RoomOperatingHours;
import com.example.studioapp_api.entity.DayOfWeekEnum; // Ensure this import points to where DayOfWeekEnum is defined
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomOperatingHoursRepository extends JpaRepository<RoomOperatingHours, Integer> {

    // Find all operating hours for a specific room object
    List<RoomOperatingHours> findByRoom(Room room);

    // Find all operating hours for a specific room by its ID
    List<RoomOperatingHours> findByRoomId(Integer roomId);

    // Find the operating hours for a specific room and day of the week
    Optional<RoomOperatingHours> findByRoomAndDayOfWeek(Room room, DayOfWeekEnum dayOfWeek);

    // Find the operating hours for a specific room by ID and day of the week
    Optional<RoomOperatingHours> findByRoomIdAndDayOfWeek(Integer roomId, DayOfWeekEnum dayOfWeek);
}