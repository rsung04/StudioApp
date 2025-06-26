package com.example.studioapp_api.solver; // Or a sub-package of service

import com.example.studioapp_api.entity.*; // Import all your JPA entities
import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;
import java.time.DayOfWeek;
import java.time.LocalTime;

@Data
@Builder
public class SolverInput {
    private int slotMinutes;
    // Master operating window for the entire scheduling period for this run.
    // This will be derived by SolverServiceImpl from all relevant rooms' operating hours.
    private Map<DayOfWeek, OperatingHoursSpan> effectiveDayWindows; 
    
    private List<Instructor> instructors; // JPA entities, with their availability slots pre-loaded
    private List<Room> rooms;             // JPA entities, with their operating hours pre-loaded
    // Note: StudioLocation info is part of Room entities if needed (room.getStudioLocation())
    
    private List<InstructorPriorityRequest> priorityRequests; // JPA entities
    private List<ClassDefinition> classDefinitions;         // JPA entities
    private List<ClassSessionRequirement> classRequirements;  // JPA entities
    // InstructorClassQualifications can be accessed via instructor.getClassQualifications() 
    // or classDefinition.getInstructorQualifications() if those JPA relationships are set up,
    // or passed as a separate list if that's easier for the solver.

    // Consider if default class duration needs to be passed, or if it's always from ClassDefinition
}