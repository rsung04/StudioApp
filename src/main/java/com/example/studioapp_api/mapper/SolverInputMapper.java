package com.example.studioapp_api.mapper;

import com.example.studioapp_api.entity.*;
// The solver-service models are defined independently in that module.
// This mapper will map JPA entities to local DTOs that mirror the structure
// expected by the solver-service. These local DTOs will then be serialized to JSON.

import com.example.studioapp_api.solver.OperatingHoursSpan; // Re-using from main app for simplicity, or define locally.

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.time.DayOfWeek; // For mapping DayOfWeekEnum

public class SolverInputMapper {

    // --- Redefinitions for clarity - In a real project, share these model classes ---
    // These would ideally be in a shared module or solver-service becomes a dependency
    // For now, this illustrates the mapping.
    // If solver-service is a separate microservice, these are the DTOs it expects.

    @lombok.Data @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
    public static class LocalSolverStudioLocation {
        private Integer id;
        private String name;
    }

    @lombok.Data @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
    public static class LocalSolverAvailabilitySlot {
        private Integer id;
        private DayOfWeek dayOfWeek;
        private java.time.LocalTime startTime;
        private java.time.LocalTime endTime;
    }

    @lombok.Data @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
    public static class LocalSolverInstructor {
        private Integer id;
        private String name;
        private List<LocalSolverAvailabilitySlot> availabilitySlots;
    }

    @lombok.Data @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
    public static class LocalSolverRoom {
        private Integer id;
        private String name;
        private LocalSolverStudioLocation studioLocation;
    }

    @lombok.Data @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
    public static class LocalSolverPriorityRequest {
        private Integer id;
        private LocalSolverInstructor instructor;
        private LocalSolverStudioLocation studioLocation;
        private int blockLengthHours;
        private boolean active;
    }

    @lombok.Data @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
    public static class LocalSolverClassDefinition {
        private Integer id;
        private String name;
        private int durationMinutes; // Corrected field name
    }

    @lombok.Data @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
    public static class LocalSolverClassSessionRequirement {
        private Integer id;
        private LocalSolverClassDefinition classDefinition;
        private int sessionsPerWeek; // Corrected field name
        private LocalSolverStudioLocation studioLocation;
        private boolean active;
    }

    // DTO for the overall SolverInput structure expected by the solver-service
    @lombok.Data @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
    public static class LocalSolverServiceInput {
        private int slotMinutes;
        private java.util.Map<java.time.DayOfWeek, OperatingHoursSpan> effectiveDayWindows;
        private List<LocalSolverInstructor> instructors;
        private List<LocalSolverRoom> rooms;
        private List<LocalSolverPriorityRequest> priorityRequests;
        private List<LocalSolverClassDefinition> classDefinitions;
        private List<LocalSolverClassSessionRequirement> classRequirements;
    }


    // --- End Redefinitions / Local DTOs ---


    public static LocalSolverStudioLocation toSolverStudioLocation(StudioLocation entity) {
        if (entity == null) return null;
        return LocalSolverStudioLocation.builder()
                .id(entity.getId())
                .name(entity.getName())
                .build();
    }

    public static LocalSolverAvailabilitySlot toSolverAvailabilitySlot(InstructorAvailabilitySlot entity) {
        if (entity == null) return null;
        return LocalSolverAvailabilitySlot.builder()
                .id(entity.getId())
                .dayOfWeek(entity.getDayOfWeek().toJavaTimeDayOfWeek()) // Assumes DayOfWeekEnum has toJavaTimeDayOfWeek()
                .startTime(entity.getStartTime())
                .endTime(entity.getEndTime())
                .build();
    }

    public static List<LocalSolverAvailabilitySlot> toSolverAvailabilitySlots(List<InstructorAvailabilitySlot> entities) {
        if (entities == null) return Collections.emptyList();
        return entities.stream().map(SolverInputMapper::toSolverAvailabilitySlot).collect(Collectors.toList());
    }

    public static LocalSolverInstructor toSolverInstructor(Instructor entity) {
        if (entity == null) return null;
        return LocalSolverInstructor.builder()
                .id(entity.getId())
                .name(entity.getName())
                .availabilitySlots(toSolverAvailabilitySlots(entity.getAvailabilitySlots()))
                .build();
    }

    public static List<LocalSolverInstructor> toSolverInstructors(List<Instructor> entities) {
        if (entities == null) return Collections.emptyList();
        return entities.stream().map(SolverInputMapper::toSolverInstructor).collect(Collectors.toList());
    }

    public static LocalSolverRoom toSolverRoom(Room entity) {
        if (entity == null) return null;
        return LocalSolverRoom.builder()
                .id(entity.getId())
                .name(entity.getName())
                .studioLocation(toSolverStudioLocation(entity.getStudioLocation()))
                .build();
    }

    public static List<LocalSolverRoom> toSolverRooms(List<Room> entities) {
        if (entities == null) return Collections.emptyList();
        return entities.stream().map(SolverInputMapper::toSolverRoom).collect(Collectors.toList());
    }

    public static LocalSolverPriorityRequest toSolverPriorityRequest(InstructorPriorityRequest entity) {
        if (entity == null) return null;
        return LocalSolverPriorityRequest.builder()
                .id(entity.getId())
                .instructor(toSolverInstructor(entity.getInstructor()))
                .studioLocation(toSolverStudioLocation(entity.getStudioLocation()))
                .blockLengthHours(entity.getBlockLengthHours())
                .active(entity.isActive())
                .build();
    }

    public static List<LocalSolverPriorityRequest> toSolverPriorityRequests(List<InstructorPriorityRequest> entities) {
        if (entities == null) return Collections.emptyList();
        return entities.stream().map(SolverInputMapper::toSolverPriorityRequest).collect(Collectors.toList());
    }

    public static LocalSolverClassDefinition toSolverClassDefinition(ClassDefinition entity) {
        if (entity == null) return null;
        return LocalSolverClassDefinition.builder()
                .id(entity.getId())
                .name(entity.getName())
                .durationMinutes(entity.getDurationMinutes()) // Corrected getter
                .build();
    }

    public static List<LocalSolverClassDefinition> toSolverClassDefinitions(List<ClassDefinition> entities) {
        if (entities == null) return Collections.emptyList();
        return entities.stream().map(SolverInputMapper::toSolverClassDefinition).collect(Collectors.toList());
    }

    public static LocalSolverClassSessionRequirement toSolverClassSessionRequirement(ClassSessionRequirement entity) {
        if (entity == null) return null;
        return LocalSolverClassSessionRequirement.builder()
                .id(entity.getId())
                .classDefinition(toSolverClassDefinition(entity.getClassDefinition()))
                .sessionsPerWeek(entity.getSessionsPerWeek()) // Corrected getter
                .studioLocation(toSolverStudioLocation(entity.getStudioLocation()))
                .active(entity.isActive())
                .build();
    }

    public static List<LocalSolverClassSessionRequirement> toSolverClassSessionRequirements(List<ClassSessionRequirement> entities) {
        if (entities == null) return Collections.emptyList();
        return entities.stream().map(SolverInputMapper::toSolverClassSessionRequirement).collect(Collectors.toList());
    }

    // This method will be used by SolverServiceImpl to construct the LocalSolverServiceInput DTO
    public static LocalSolverServiceInput createSolverServiceInputStructure(
            int slotMinutes,
            java.util.Map<java.time.DayOfWeek, OperatingHoursSpan> effectiveDayWindows,
            List<Instructor> instructors,
            List<Room> rooms,
            List<InstructorPriorityRequest> priorityRequests,
            List<ClassDefinition> classDefinitions,
            List<ClassSessionRequirement> classRequirements) {

        return LocalSolverServiceInput.builder()
                .slotMinutes(slotMinutes)
                .effectiveDayWindows(effectiveDayWindows)
                .instructors(toSolverInstructors(instructors))
                .rooms(toSolverRooms(rooms))
                .priorityRequests(toSolverPriorityRequests(priorityRequests))
                .classDefinitions(toSolverClassDefinitions(classDefinitions))
                .classRequirements(toSolverClassSessionRequirements(classRequirements))
                .build();
    }
}