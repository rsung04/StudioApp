package com.example.studioapp_api.repository;

import com.example.studioapp_api.entity.InstructorAvailabilitySlot;
import com.example.studioapp_api.entity.DayOfWeekEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalTime;
import java.util.List;

@Repository
public interface InstructorAvailabilitySlotRepository 
    extends JpaRepository<InstructorAvailabilitySlot, Integer> {

    List<InstructorAvailabilitySlot> findByInstructorId(Integer instructorId);

    // Could add more specific finders if needed, e.g.,
    List<InstructorAvailabilitySlot> findByInstructorIdAndDayOfWeek(Integer instructorId, DayOfWeekEnum dayOfWeek);
    
    List<InstructorAvailabilitySlot> findByInstructorIdAndStudioLocationId(Integer instructorId, Integer studioLocationId);

    // Example for checking overlaps (more complex, usually done in service layer or by solver)
    // @Query("SELECT COUNT(s) > 0 FROM InstructorAvailabilitySlot s WHERE s.instructor.id = :instructorId AND s.dayOfWeek = :dayOfWeek " +
    //        "AND s.id <> :excludeSlotId AND ((s.startTime < :endTime AND s.endTime > :startTime))")
    // boolean existsOverlappingSlot(@Param("instructorId") Integer instructorId,
    //                               @Param("dayOfWeek") DayOfWeekEnum dayOfWeek,
    //                               @Param("startTime") LocalTime startTime,
    //                               @Param("endTime") LocalTime endTime,
    //                               @Param("excludeSlotId") Integer excludeSlotId); // exclude current slot if updating
}