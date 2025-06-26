package com.example.studioapp_api.solver; // New package

import com.example.studioapp_api.dto.LockedBlockDTO;

// Import your JPA entities from com.example.studioapp_api.entity.*
// Import com.google.ortools.*
// Import other necessary Java types

import com.example.studioapp_api.entity.*; // JPA Entities
import com.google.ortools.Loader;
import com.google.ortools.sat.*;
import org.slf4j.Logger; // For logging
import org.slf4j.LoggerFactory; // For logging
import org.springframework.stereotype.Component; // Make it a Spring bean

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;


@Component // So Spring can manage it and SolverServiceImpl can inject it
public class DanceTimetableSolver {
    private static final Logger logger = LoggerFactory.getLogger(DanceTimetableSolver.class);

    // Instance fields - these will be initialized per solve run
    private int slotMinutes;
    private int dur60; // slots per 60 minutes
    private Map<DayOfWeek, OperatingHoursSpan> effectiveDayWindows; // From SolverInput
    private Map<DayOfWeek, Integer> dayOfWeekSlotPrefix;
    private int totalWeekSlots;

    // Helper for LocalTime parsing, similar to your TIME_FMT
    private static final DateTimeFormatter INTERNAL_TIME_PARSER = 
        DateTimeFormatter.ofPattern("[H:mm][HH:mm][:ss][.SSS]");


    // Constructor - could be empty or take some global config if needed
    public DanceTimetableSolver() {
         Loader.loadNativeLibraries(); // Load OR-Tools natives when an instance is created
    }

    // --- Initialization method called by executeSolve ---
    private void initializeSolverConfig(SolverInput input) {
        this.slotMinutes = input.getSlotMinutes();
        this.dur60 = 60 / this.slotMinutes;
        this.effectiveDayWindows = input.getEffectiveDayWindows();

        this.dayOfWeekSlotPrefix = new EnumMap<>(DayOfWeek.class);
        int accumulatedSlots = 0;
        for (DayOfWeek d : DayOfWeek.values()) { // Ensure consistent ordering
            OperatingHoursSpan span = this.effectiveDayWindows.get(d);
            this.dayOfWeekSlotPrefix.put(d, accumulatedSlots);
            if (span != null && span.getStart() != null && span.getEnd() != null) {
                long minutesInDay = span.getStart().until(span.getEnd(), ChronoUnit.MINUTES);
                accumulatedSlots += (int) (minutesInDay / this.slotMinutes);
            } // else, if no span, 0 slots for that day in the master grid
        }
        // totalWeekSlots will be the prefix of SUNDAY + slots in SUNDAY
        OperatingHoursSpan sundaySpan = this.effectiveDayWindows.get(DayOfWeek.SUNDAY);
        int sundaySlots = 0;
        if (sundaySpan != null && sundaySpan.getStart() != null && sundaySpan.getEnd() != null) {
             long minutesInSunday = sundaySpan.getStart().until(sundaySpan.getEnd(), ChronoUnit.MINUTES);
             sundaySlots = (int) (minutesInSunday / this.slotMinutes);
        }
        this.totalWeekSlots = this.dayOfWeekSlotPrefix.get(DayOfWeek.SUNDAY) + sundaySlots;
        logger.info("Solver initialized. SlotMin: {}, TotalWeekSlots: {}", this.slotMinutes, this.totalWeekSlots);
    }

    // --- Adapted Slot Conversion ---
    // Converts a day and time to a global slot index based on effectiveDayWindows
    private int toGlobalSlot(DayOfWeek day, LocalTime time) {
        OperatingHoursSpan daySpan = this.effectiveDayWindows.get(day);
        if (daySpan == null || time.isBefore(daySpan.getStart()) || time.isAfter(daySpan.getEnd())) {
            // Time is outside the effective operating window for this day
            // Or, day has no operating window defined.
            // This case needs careful handling - throw error, or return sentinel value?
            // For now, let's assume inputs are validated to be within these master windows.
            logger.warn("Time {} on {} is outside effective operating window {} for the master grid.", time, day, daySpan);
            // Fallback or error needed if we expect times to be strictly within.
            // For simplicity, if time is before start, count from start. If after end, this is problematic.
        }
        Integer prefix = this.dayOfWeekSlotPrefix.get(day);
        if (prefix == null || daySpan == null || daySpan.getStart() == null) return -1; // Should not happen if initialized correctly

        long minutesFromDayStart = daySpan.getStart().until(time, ChronoUnit.MINUTES);
        return prefix + (int) (minutesFromDayStart / this.slotMinutes);
    }

    // Converts a global slot index back to LocalDateTime
    private LocalDateTime globalSlotToLocalDateTime(long slot) {
        DayOfWeek currentDay = DayOfWeek.MONDAY;
        for (DayOfWeek d : DayOfWeek.values()) {
            Integer prefix = this.dayOfWeekSlotPrefix.get(d);
            OperatingHoursSpan daySpan = this.effectiveDayWindows.get(d);
            int slotsInThisDay = 0;
            if (daySpan != null && daySpan.getStart() != null && daySpan.getEnd() != null) {
                slotsInThisDay = (int) (daySpan.getStart().until(daySpan.getEnd(), ChronoUnit.MINUTES) / this.slotMinutes);
            }

            if (slot >= prefix && slot < prefix + slotsInThisDay) {
                currentDay = d;
                break;
            }
        }
        
        OperatingHoursSpan daySpan = this.effectiveDayWindows.get(currentDay);
        if (daySpan == null || daySpan.getStart() == null) {
             logger.error("Could not determine date for slot {} - missing daySpan start for {}", slot, currentDay);
             return LocalDateTime.now(); // Fallback, should not happen
        }

        int offsetInDaySlots = (int) (slot - this.dayOfWeekSlotPrefix.get(currentDay));
        LocalTime time = daySpan.getStart().plusMinutes((long)offsetInDaySlots * this.slotMinutes);
        
        // Find a representative date for this DayOfWeek (e.g., in the current week)
        // This is just for display. The solver only cares about relative slots.
        LocalDate today = LocalDate.now();
        LocalDate representativeDate = today.with(DayOfWeek.MONDAY).plusDays(currentDay.getValue() - 1);
        return LocalDateTime.of(representativeDate, time);
    }


    public SolverOutput executeSolve(SolverInput input) {
        logger.info("Solver Engine: Starting execution...");
        initializeSolverConfig(input); // Sets up slotMinutes, effectiveDayWindows, prefix, totalSlots
        SolverOutput output = new SolverOutput();
        StringBuilder internalConsoleLog = new StringBuilder();
    
        try {
            // --- STAGE A ---
            logger.info("Solver Engine: Preparing for Stage A.");
            // 1. Adapt input.getPriorityRequests() (JPA Entities) to what your OR-Tools logic for Stage A needs.
            //    This might involve creating a list of an internal "SolverPriorityRequest" record/class
            //    that holds the OR-Tools variables (IntVar for start, BoolVar for presence) and
            //    references to the original JPA entities or their IDs.
            //    Your original solver had `PriorityReq(Instructor instr, Studio studio, int hours)`
            //    We need to map from `input.getPriorityRequests()` (JPA) to something similar.
    
            List<InternalSolverPriorityRequest> solverStageARequests = 
                prepareStageAPriorityRequests(input.getPriorityRequests(), input.getInstructors(), input.getRooms(), internalConsoleLog);
    
            if (solverStageARequests.isEmpty() && !input.getPriorityRequests().isEmpty()) {
                logger.warn("No priority requests were prepared for Stage A, though input contained some. Check mapping.");
            } else if (solverStageARequests.isEmpty()) {
                logger.info("No priority requests to process for Stage A.");
            }
    
    
            List<InternalLockedBlock> stageARawResults = new ArrayList<>();
            if (!solverStageARequests.isEmpty()) {
                 stageARawResults = runActualStageA(solverStageARequests, input.getInstructors(), input.getRooms(), internalConsoleLog);
            }
            
            output.setStageAResults(convertToLockedBlockDTOs(stageARawResults, input)); // Convert internal results to DTOs
            logger.info("Solver Engine: Stage A completed. Found {} locked blocks.", stageARawResults.size());
    
            // --- STAGE B (Placeholder for now) ---
            logger.info("Solver Engine: Preparing for Stage B (currently placeholder).");
            // TODO: Adapt Stage B similarly, taking stageARawResults as input
            // output.setStageBResults(new ArrayList<>()); 
    
            output.setSolveSuccess(true);
            output.setStatusMessage("Solver run (Stage A simulated, Stage B placeholder) completed.");
    
        } catch (Exception e) {
            logger.error("Solver Engine: Exception during solve process!", e);
            output.setSolveSuccess(false);
            output.setStatusMessage("Solver failed: " + e.getMessage());
            internalConsoleLog.append("\nERROR: ").append(e.toString());
        }
    
        output.setConsoleLog(internalConsoleLog.toString());
        logger.info("Solver Engine: Execution finished.");
        return output;
    }
    
    // --- Helper data structures for internal solver use ---
    private record InternalSolverPriorityRequest(
        com.example.studioapp_api.entity.InstructorPriorityRequest originalRequest,
        com.example.studioapp_api.entity.Instructor instructorEntity, // JPA Instructor
        com.example.studioapp_api.entity.Room roomEntity, // JPA Room (or a simplified representation)
        int lengthSlots,
        // OR-Tools specific variables will be created inside runActualStageA
        IntVar startVar, 
        BoolVar presentVar,
        IntervalVar intervalVar
    ) {}
    
    private record InternalLockedBlock(
        com.example.studioapp_api.entity.Instructor instructorEntity,
        com.example.studioapp_api.entity.Room roomEntity,
        int startSlot,
        int lengthSlots
    ) {}
    
    
    // --- Method to prepare data for Stage A ---
    private List<InternalSolverPriorityRequest> prepareStageAPriorityRequests(
        List<com.example.studioapp_api.entity.InstructorPriorityRequest> dbPriorityRequests,
        List<com.example.studioapp_api.entity.Instructor> allInstructors, // To resolve instructor reference
        List<com.example.studioapp_api.entity.Room> allRooms,           // To resolve room reference (simplification)
        StringBuilder internalConsoleLog) {
        
        List<InternalSolverPriorityRequest> solverRequests = new ArrayList<>();
        if (dbPriorityRequests == null) return solverRequests;
    
        // For simplicity, Stage A in original solver used "Studio A". 
        // We need a strategy for assigning priority requests to actual JPA Room entities.
        // Simplest: use the first available room, or a designated "priority" room.
        // Or, if InstructorPriorityRequest has a preferred StudioLocation/Room, use that.
        // Your DB schema for InstructorPriorityRequest has 'studio_location_id'.
        // Let's assume we try to find a room within that location, or any room if null.
    
        Map<Integer, com.example.studioapp_api.entity.Instructor> instructorMap = 
            allInstructors.stream().collect(Collectors.toMap(Instructor::getId, i -> i));
        
        Map<Integer, com.example.studioapp_api.entity.Room> roomMap = 
            allRooms.stream().collect(Collectors.toMap(Room::getId, r -> r));
    
    
        for (com.example.studioapp_api.entity.InstructorPriorityRequest dbReq : dbPriorityRequests) {
            if (dbReq.getBlockLengthHours() <= 1 && dbReq.isActive()) { // Original solver filtered >1hr for stage A
                 // This filter is now in SolverServiceImpl when fetching priorityRequests for Stage A
                 // but double check if the input list `dbPriorityRequests` is already pre-filtered.
                 // For now, assume the input list IS the >1hr active requests.
            }
            if (!dbReq.isActive()){
                internalConsoleLog.append("Skipping inactive priority request ID: ").append(dbReq.getId()).append("\n");
                continue;
            }
    
    
            com.example.studioapp_api.entity.Instructor instructor = instructorMap.get(dbReq.getInstructor().getId());
            if (instructor == null) {
                internalConsoleLog.append("Warning: Instructor not found for priority request ID: ").append(dbReq.getId()).append("\n");
                continue;
            }
    
            // Determine the room for this priority request
            com.example.studioapp_api.entity.Room targetRoom = null;
            if (dbReq.getStudioLocation() != null) {
                // Try to find a room in the specified location
                Integer targetLocationId = dbReq.getStudioLocation().getId();
                targetRoom = allRooms.stream()
                                    .filter(r -> r.getStudioLocation() != null && r.getStudioLocation().getId().equals(targetLocationId))
                                    .findFirst().orElse(null);
                if (targetRoom == null) {
                     internalConsoleLog.append("Warning: No room found in specified StudioLocation ID ").append(targetLocationId)
                                       .append(" for priority request ID: ").append(dbReq.getId()).append(". Trying any room.\n");
                }
            }
            
            if (targetRoom == null && !allRooms.isEmpty()) {
                targetRoom = allRooms.get(0); // Fallback: use the first available room (simplification!)
                internalConsoleLog.append("Warning: Assigning priority request ID: ").append(dbReq.getId())
                                  .append(" to fallback room ID: ").append(targetRoom.getId()).append("\n");
            } else if (targetRoom == null && allRooms.isEmpty()){
                 internalConsoleLog.append("Error: No rooms available for priority request ID: ").append(dbReq.getId()).append(". Skipping.\n");
                continue;
            }
    
    
            int lenSlots = dbReq.getBlockLengthHours() * this.dur60;
            // OR-Tools variables will be created in runActualStageA
            solverRequests.add(new InternalSolverPriorityRequest(dbReq, instructor, targetRoom, lenSlots, null, null, null));
        }
        return solverRequests;
    }
    
    
    // --- Method to run the actual OR-Tools Stage A ---
    private List<InternalLockedBlock> runActualStageA(
        List<InternalSolverPriorityRequest> solverRequests,
        List<com.example.studioapp_api.entity.Instructor> allInstructors, // Full list for availability
        List<com.example.studioapp_api.entity.Room> allRooms,             // Full list for NoOverlap by room
        StringBuilder internalConsoleLog) {
    
        internalConsoleLog.append("Running OR-Tools Stage A with ").append(solverRequests.size()).append(" requests.\n");
        List<InternalLockedBlock> confirmedBlocks = new ArrayList<>();
        if (solverRequests.isEmpty()) return confirmedBlocks;
    
        CpModel model = new CpModel();
    
        // Create OR-Tools variables for each request
        List<InternalSolverPriorityRequest> requestsWithORToolsVars = new ArrayList<>();
        for (InternalSolverPriorityRequest req : solverRequests) {
            IntVar startVar = model.newIntVar(0, this.totalWeekSlots - req.lengthSlots(), "pstart_" + req.originalRequest().getId());
            BoolVar presentVar = model.newBoolVar("present_" + req.originalRequest().getId());
            IntVar endVar = model.newIntVar(0, this.totalWeekSlots, "pend_" + req.originalRequest().getId());
            model.addEquality(LinearExpr.sum(new IntVar[]{startVar, model.newConstant(req.lengthSlots())}), endVar).onlyEnforceIf(presentVar);
            IntervalVar intervalVar = model.newOptionalIntervalVar(startVar, model.newConstant(req.lengthSlots()), endVar, presentVar, "iv_p_" + req.originalRequest().getId());
            requestsWithORToolsVars.add(new InternalSolverPriorityRequest(
                req.originalRequest(), req.instructorEntity(), req.roomEntity(), req.lengthSlots(),
                startVar, presentVar, intervalVar
            ));
        }
        
        // NoOverlap constraints by Instructor
        Map<Integer, List<IntervalVar>> byInstrORTools = new HashMap<>();
        for (InternalSolverPriorityRequest req : requestsWithORToolsVars) {
            byInstrORTools.computeIfAbsent(req.instructorEntity().getId(), k -> new ArrayList<>()).add(req.intervalVar());
        }
        byInstrORTools.values().forEach(model::addNoOverlap);
    
        // NoOverlap constraints by Room
        Map<Integer, List<IntervalVar>> byRoomORTools = new HashMap<>();
        for (InternalSolverPriorityRequest req : requestsWithORToolsVars) {
            byRoomORTools.computeIfAbsent(req.roomEntity().getId(), k -> new ArrayList<>()).add(req.intervalVar());
        }
        byRoomORTools.values().forEach(model::addNoOverlap);
    
        // Add window constraints for each request
        for (InternalSolverPriorityRequest req : requestsWithORToolsVars) {
            addWindowConstraintsInternal(model, req.startVar(), req.lengthSlots(), 
                                         req.instructorEntity().getAvailabilitySlots(), // Pass JPA availability
                                         req.presentVar());
        }
    
        // Objective: Maximize placed blocks
        IntVar[] presencesArray = requestsWithORToolsVars.stream().map(InternalSolverPriorityRequest::presentVar).toArray(IntVar[]::new);
        model.maximize(LinearExpr.sum(presencesArray));
    
        CpSolver solver = new CpSolver();
        solver.getParameters().setMaxTimeInSeconds(10); // Configurable
        CpSolverStatus status = solver.solve(model);
    
        internalConsoleLog.append("Stage A Solver status: ").append(status).append("\n");
    
        if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE) {
            for (InternalSolverPriorityRequest req : requestsWithORToolsVars) {
                if (solver.value(req.presentVar()) == 1) {
                    int startSlotVal = (int) solver.value(req.startVar());
                    confirmedBlocks.add(new InternalLockedBlock(
                        req.instructorEntity(), 
                        req.roomEntity(), 
                        startSlotVal, 
                        req.lengthSlots()
                    ));
                    internalConsoleLog.append("Scheduled Priority: Instr ")
                        .append(req.instructorEntity().getName()).append(" in Room ")
                        .append(req.roomEntity().getName()).append(" at slot ").append(startSlotVal)
                        .append(" for ").append(req.lengthSlots()).append(" slots.\n");
                }
            }
        }
        return confirmedBlocks;
    }
    
    // --- Method to adapt JPA Availability to OR-Tools Window Constraints ---
    private void addWindowConstraintsInternal(CpModel model, IntVar startVar, int lengthSlots,
                                            List<com.example.studioapp_api.entity.InstructorAvailabilitySlot> availabilitySlots,
                                            BoolVar presentLiteral) {
        if (availabilitySlots == null || availabilitySlots.isEmpty()) {
            // If instructor has no defined availability, this priority request cannot be scheduled if present
            model.addImplication(presentLiteral, model.falseLiteral()); // present => false
            logger.warn("Instructor has no availability slots defined; request linked to literal {} cannot be scheduled.", presentLiteral.getName());
            return;
        }
    
        List<Literal> windowOptions = new ArrayList<>();
        for (com.example.studioapp_api.entity.InstructorAvailabilitySlot jpaAvail : availabilitySlots) {
            // Convert JPA DayOfWeekEnum and LocalTime to global slot indices for THIS solve run's config
            int windowStartSlot = toGlobalSlot(jpaAvail.getDayOfWeek().toJavaTimeDayOfWeek(), jpaAvail.getStartTime());
            int windowEndSlot = toGlobalSlot(jpaAvail.getDayOfWeek().toJavaTimeDayOfWeek(), jpaAvail.getEndTime());
            
            if (windowStartSlot == -1 || windowEndSlot == -1 || windowEndSlot <= windowStartSlot) {
                logger.warn("Invalid availability slot for constraint: {} {} {}-{} mapped to {}-{}", 
                    jpaAvail.getInstructor().getName(), jpaAvail.getDayOfWeek(), jpaAvail.getStartTime(), jpaAvail.getEndTime(),
                    windowStartSlot, windowEndSlot);
                continue; // Skip this malformed or out-of-bounds availability
            }
    
    
            BoolVar windowSelectedLiteral = model.newBoolVar("win_" + jpaAvail.getId() + "_" + presentLiteral.getName());
            
            // If this window is selected, activity must be within it
            model.addGreaterOrEqual(startVar, windowStartSlot).onlyEnforceIf(windowSelectedLiteral);
            model.addLessOrEqual(startVar, windowEndSlot - lengthSlots).onlyEnforceIf(windowSelectedLiteral); // start + len <= end_window
            
            windowOptions.add(windowSelectedLiteral);
            model.addImplication(windowSelectedLiteral, presentLiteral); // If window chosen, activity must be present
        }
    
        if (windowOptions.isEmpty()) {
            // No valid windows found for this instructor from their availability
            model.addImplication(presentLiteral, model.falseLiteral());
             logger.warn("No valid, schedulable windows found for instructor based on availability; request linked to literal {} cannot be scheduled.", presentLiteral.getName());
            return;
        }
    
        windowOptions.add(presentLiteral.not()); // If activity is not present, this satisfies the ExactlyOne
        model.addExactlyOne(windowOptions); // If present, exactly one window must be chosen
    }
    
    
    // --- Method to convert internal results to DTOs ---
    private List<LockedBlockDTO> convertToLockedBlockDTOs(List<InternalLockedBlock> internalBlocks, SolverInput input) {
        if (internalBlocks == null) return new ArrayList<>();
        return internalBlocks.stream().map(ib -> {
            LocalDateTime startDateTime = globalSlotToLocalDateTime(ib.startSlot());
            LocalDateTime endDateTime = globalSlotToLocalDateTime(ib.startSlot() + ib.lengthSlots());
    
            return LockedBlockDTO.builder()
                .instructorId(ib.instructorEntity().getId())
                .instructorName(ib.instructorEntity().getName())
                .roomId(ib.roomEntity().getId())
                .roomName(ib.roomEntity().getName())
                .studioLocationId(ib.roomEntity().getStudioLocation() != null ? ib.roomEntity().getStudioLocation().getId() : null)
                .studioLocationName(ib.roomEntity().getStudioLocation() != null ? ib.roomEntity().getStudioLocation().getName() : null)
                .startSlot(ib.startSlot())
                .lengthSlots(ib.lengthSlots())
                .dayOfWeek(startDateTime.getDayOfWeek().toString()) // java.time.DayOfWeek
                .startTime(startDateTime.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")))
                .endTime(endDateTime.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")))
                .build();
        }).collect(Collectors.toList());
    }
    
    // TODO: Implement `runActualStageB`, `prepareStageBClassRequests`, `convertToScheduledClassDTOs`
    // TODO: The original solver's `Studio A` and `Studio B` need to be mapped to actual Room entities.
    
    }
    