package com.example.solverservice.solver; // Updated package

// Import new POJO models
import com.example.solverservice.model.SolverInstructor;
import com.example.solverservice.model.SolverRoom;
import com.example.solverservice.model.SolverPriorityRequest;
import com.example.solverservice.model.SolverClassDefinition;
import com.example.solverservice.model.SolverClassSessionRequirement;
// OperatingHoursSpan is already in this package

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Map;
import java.time.DayOfWeek;
// LocalTime is used by OperatingHoursSpan

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SolverInput {
    private int slotMinutes;
    private Map<DayOfWeek, OperatingHoursSpan> effectiveDayWindows; 
    
    private List<SolverInstructor> instructors;
    private List<SolverRoom> rooms;
    
    private List<SolverPriorityRequest> priorityRequests;
    private List<SolverClassDefinition> classDefinitions;
    private List<SolverClassSessionRequirement> classRequirements;
    
    // Consider if default class duration needs to be passed, or if it's always from SolverClassDefinition
    // InstructorClassQualifications would need to be part of SolverInstructor or SolverClassDefinition if used by the solver.
    // For now, assuming they are not directly used by the current Stage A logic in DanceTimetableSolver or will be simplified.
}