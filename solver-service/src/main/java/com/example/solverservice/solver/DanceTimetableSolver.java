package com.example.solverservice.solver; // Updated package

import com.example.solverservice.dto.LockedBlockDTO; // Updated import
import com.example.solverservice.model.*; // Import new POJO models

import com.google.ortools.Loader;
import com.google.ortools.sat.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;


@Component
public class DanceTimetableSolver {
    private static final Logger logger = LoggerFactory.getLogger(DanceTimetableSolver.class);

    private int slotMinutes;
    private int dur60; // slots per 60 minutes
    private Map<DayOfWeek, OperatingHoursSpan> effectiveDayWindows;
    private Map<DayOfWeek, Integer> dayOfWeekSlotPrefix;
    private int totalWeekSlots;

    private static final DateTimeFormatter INTERNAL_TIME_PARSER =
        DateTimeFormatter.ofPattern("[H:mm][HH:mm][:ss][.SSS]");

    public DanceTimetableSolver() {
         Loader.loadNativeLibraries();
    }

    private void initializeSolverConfig(SolverInput input) {
        this.slotMinutes = input.getSlotMinutes();
        this.dur60 = 60 / this.slotMinutes;
        this.effectiveDayWindows = input.getEffectiveDayWindows();

        this.dayOfWeekSlotPrefix = new EnumMap<>(DayOfWeek.class);
        int accumulatedSlots = 0;
        for (DayOfWeek d : DayOfWeek.values()) {
            OperatingHoursSpan span = this.effectiveDayWindows.get(d);
            this.dayOfWeekSlotPrefix.put(d, accumulatedSlots);
            if (span != null && span.getStart() != null && span.getEnd() != null) {
                long minutesInDay = span.getStart().until(span.getEnd(), ChronoUnit.MINUTES);
                accumulatedSlots += (int) (minutesInDay / this.slotMinutes);
            }
        }
        OperatingHoursSpan sundaySpan = this.effectiveDayWindows.get(DayOfWeek.SUNDAY);
        int sundaySlots = 0;
        if (sundaySpan != null && sundaySpan.getStart() != null && sundaySpan.getEnd() != null) {
             long minutesInSunday = sundaySpan.getStart().until(sundaySpan.getEnd(), ChronoUnit.MINUTES);
             sundaySlots = (int) (minutesInSunday / this.slotMinutes);
        }
        this.totalWeekSlots = this.dayOfWeekSlotPrefix.get(DayOfWeek.SUNDAY) + sundaySlots;
        logger.info("Solver initialized. SlotMin: {}, TotalWeekSlots: {}", this.slotMinutes, this.totalWeekSlots);
    }

    private int toGlobalSlot(DayOfWeek day, LocalTime time) {
        OperatingHoursSpan daySpan = this.effectiveDayWindows.get(day);
        if (daySpan == null || daySpan.getStart() == null || time.isBefore(daySpan.getStart()) || time.isAfter(daySpan.getEnd())) {
            logger.warn("Time {} on {} is outside effective operating window {} for the master grid.", time, day, daySpan);
            // This logic might need refinement based on how strictly out-of-bounds times are handled.
            // For now, if it's outside, it might return -1 or an error, or clamp to bounds.
            // The original code implies it might proceed if time is before start, but after end is problematic.
            // Returning -1 for any out-of-bounds or undefined span.
            if (daySpan == null || daySpan.getStart() == null) return -1;
            if (time.isBefore(daySpan.getStart())) return this.dayOfWeekSlotPrefix.get(day); // Clamp to start
            if (time.isAfter(daySpan.getEnd())) { // Clamp to end, effectively making it the last possible slot start
                 long minutesFromDayStart = daySpan.getStart().until(daySpan.getEnd(), ChronoUnit.MINUTES);
                 return this.dayOfWeekSlotPrefix.get(day) + (int) (minutesFromDayStart / this.slotMinutes) -1; // -1 if duration > slotmin
            }
        }
        Integer prefix = this.dayOfWeekSlotPrefix.get(day);
        if (prefix == null) return -1; 

        long minutesFromDayStart = daySpan.getStart().until(time, ChronoUnit.MINUTES);
        return prefix + (int) (minutesFromDayStart / this.slotMinutes);
    }

    private LocalDateTime globalSlotToLocalDateTime(long slot) {
        DayOfWeek currentDay = DayOfWeek.MONDAY; // Default
        for (DayOfWeek d : DayOfWeek.values()) {
            Integer prefix = this.dayOfWeekSlotPrefix.get(d);
            OperatingHoursSpan daySpan = this.effectiveDayWindows.get(d);
            int slotsInThisDay = 0;
            if (prefix != null && daySpan != null && daySpan.getStart() != null && daySpan.getEnd() != null) {
                slotsInThisDay = (int) (daySpan.getStart().until(daySpan.getEnd(), ChronoUnit.MINUTES) / this.slotMinutes);
                if (slot >= prefix && slot < prefix + slotsInThisDay) {
                    currentDay = d;
                    break;
                }
            } else if (prefix != null && slot >= prefix && (daySpan == null || daySpan.getStart() == null || daySpan.getEnd() == null)){
                // This case means the slot falls into a day that has a prefix but no defined operating hours.
                // This shouldn't happen if effectiveDayWindows is comprehensive.
                logger.error("Slot {} falls into day {} which has no defined operating hours. Defaulting to MONDAY.", slot, d);
                currentDay = DayOfWeek.MONDAY; // Fallback, but indicates an issue.
                break;
            }
        }
        
        OperatingHoursSpan daySpan = this.effectiveDayWindows.get(currentDay);
        if (daySpan == null || daySpan.getStart() == null) {
             logger.error("Could not determine date for slot {} - missing daySpan start for {}", slot, currentDay);
             // Fallback to a very distinct, clearly wrong time to indicate error
             return LocalDateTime.of(1970, 1, 1, 0, 0); 
        }

        int offsetInDaySlots = (int) (slot - this.dayOfWeekSlotPrefix.get(currentDay));
        LocalTime time = daySpan.getStart().plusMinutes((long)offsetInDaySlots * this.slotMinutes);
        
        LocalDate today = LocalDate.now(); // Representative date, not critical for solver logic
        LocalDate representativeDate = today.with(DayOfWeek.MONDAY).plusDays(currentDay.getValue() - 1);
        return LocalDateTime.of(representativeDate, time);
    }


    public SolverOutput executeSolve(SolverInput input) {
        logger.info("Solver Engine: Starting execution...");
        initializeSolverConfig(input);
        SolverOutput output = SolverOutput.builder().build(); // Use builder
        StringBuilder internalConsoleLog = new StringBuilder();
    
        try {
            logger.info("Solver Engine: Preparing for Stage A.");
            List<InternalSolverPriorityRequest> solverStageARequests =
                prepareStageAPriorityRequests(input.getPriorityRequests(), input.getInstructors(), input.getRooms(), internalConsoleLog);
    
            if (solverStageARequests.isEmpty() && input.getPriorityRequests() != null && !input.getPriorityRequests().isEmpty()) {
                logger.warn("No priority requests were prepared for Stage A, though input contained some. Check mapping.");
            } else if (solverStageARequests.isEmpty()) {
                logger.info("No priority requests to process for Stage A.");
            }
    
            List<InternalLockedBlock> stageARawResults = new ArrayList<>();
            if (!solverStageARequests.isEmpty()) {
                 stageARawResults = runActualStageA(solverStageARequests, input.getInstructors(), input.getRooms(), internalConsoleLog);
            }
            
            output.setStageAResults(convertToLockedBlockDTOs(stageARawResults)); 
            logger.info("Solver Engine: Stage A completed. Found {} locked blocks.", stageARawResults.size());
    
            // --- STAGE B (Placeholder for now) ---
            logger.info("Solver Engine: Preparing for Stage B (currently placeholder).");
            // output.setStageBResults(new ArrayList<>()); 
    
            output.setSolveSuccess(true);
            output.setStatusMessage("Solver run (Stage A, Stage B placeholder) completed.");
    
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
    
    // --- Helper data structures for internal solver use (using new POJO models) ---
    private record InternalSolverPriorityRequest(
        SolverPriorityRequest originalRequestPojo, // Changed from JPA entity
        SolverInstructor instructorPojo,        // Changed from JPA entity
        SolverRoom roomPojo,                    // Changed from JPA entity
        int lengthSlots,
        IntVar startVar, 
        BoolVar presentVar,
        IntervalVar intervalVar
    ) {}
    
    private record InternalLockedBlock(
        SolverInstructor instructorPojo,        // Changed from JPA entity
        SolverRoom roomPojo,                    // Changed from JPA entity
        int startSlot,
        int lengthSlots
    ) {}
    
    
    private List<InternalSolverPriorityRequest> prepareStageAPriorityRequests(
        List<SolverPriorityRequest> pojoPriorityRequests, // Changed from JPA list
        List<SolverInstructor> allInstructors,          // Changed from JPA list
        List<SolverRoom> allRooms,                      // Changed from JPA list
        StringBuilder internalConsoleLog) {
        
        List<InternalSolverPriorityRequest> solverRequests = new ArrayList<>();
        if (pojoPriorityRequests == null) return solverRequests;
    
        Map<Integer, SolverInstructor> instructorMap = 
            allInstructors.stream().collect(Collectors.toMap(SolverInstructor::getId, i -> i));
        
        // Map<Integer, SolverRoom> roomMap = 
        //     allRooms.stream().collect(Collectors.toMap(SolverRoom::getId, r -> r)); // Not directly used like this anymore
    
        for (SolverPriorityRequest pojoReq : pojoPriorityRequests) {
            if (!pojoReq.isActive()){
                internalConsoleLog.append("Skipping inactive priority request ID: ").append(pojoReq.getId()).append("\n");
                continue;
            }
    
            SolverInstructor instructor = instructorMap.get(pojoReq.getInstructor().getId());
            if (instructor == null) {
                internalConsoleLog.append("Warning: Instructor not found for priority request ID: ").append(pojoReq.getId()).append("\n");
                continue;
            }
    
            SolverRoom targetRoom = null;
            if (pojoReq.getStudioLocation() != null) {
                Integer targetLocationId = pojoReq.getStudioLocation().getId();
                targetRoom = allRooms.stream()
                                    .filter(r -> r.getStudioLocation() != null && r.getStudioLocation().getId().equals(targetLocationId))
                                    .findFirst().orElse(null);
                if (targetRoom == null) {
                     internalConsoleLog.append("Warning: No room found in specified StudioLocation ID ").append(targetLocationId)
                                       .append(" for priority request ID: ").append(pojoReq.getId()).append(". Trying any room.\n");
                }
            }
            
            if (targetRoom == null && !allRooms.isEmpty()) {
                targetRoom = allRooms.get(0); 
                internalConsoleLog.append("Warning: Assigning priority request ID: ").append(pojoReq.getId())
                                  .append(" to fallback room ID: ").append(targetRoom.getId()).append("\n");
            } else if (targetRoom == null && allRooms.isEmpty()){
                 internalConsoleLog.append("Error: No rooms available for priority request ID: ").append(pojoReq.getId()).append(". Skipping.\n");
                continue;
            }
    
            int lenSlots = pojoReq.getBlockLengthHours() * this.dur60;
            solverRequests.add(new InternalSolverPriorityRequest(pojoReq, instructor, targetRoom, lenSlots, null, null, null));
        }
        return solverRequests;
    }
    
    
    private List<InternalLockedBlock> runActualStageA(
        List<InternalSolverPriorityRequest> solverRequests,
        List<SolverInstructor> allInstructors, // Parameter kept for consistency, though availability is now on individual SolverInstructor objects
        List<SolverRoom> allRooms,             // Parameter kept for consistency, though room details are on individual SolverRoom objects
        StringBuilder internalConsoleLog) {
    
        internalConsoleLog.append("Running OR-Tools Stage A with ").append(solverRequests.size()).append(" requests.\n");
        List<InternalLockedBlock> confirmedBlocks = new ArrayList<>();
        if (solverRequests.isEmpty()) return confirmedBlocks;
    
        CpModel model = new CpModel();
    
        List<InternalSolverPriorityRequest> requestsWithORToolsVars = new ArrayList<>();
        for (InternalSolverPriorityRequest req : solverRequests) {
            IntVar startVar = model.newIntVar(0, this.totalWeekSlots - req.lengthSlots(), "pstart_" + req.originalRequestPojo().getId());
            BoolVar presentVar = model.newBoolVar("present_" + req.originalRequestPojo().getId());
            IntVar endVar = model.newIntVar(0, this.totalWeekSlots, "pend_" + req.originalRequestPojo().getId()); // Corrected end var domain
            model.addEquality(LinearExpr.sum(new IntVar[]{startVar, model.newConstant(req.lengthSlots())}), endVar).onlyEnforceIf(presentVar); // Define end based on start and length
            IntervalVar intervalVar = model.newOptionalIntervalVar(startVar, model.newConstant(req.lengthSlots()), endVar, presentVar, "iv_p_" + req.originalRequestPojo().getId());
            requestsWithORToolsVars.add(new InternalSolverPriorityRequest(
                req.originalRequestPojo(), req.instructorPojo(), req.roomPojo(), req.lengthSlots(),
                startVar, presentVar, intervalVar
            ));
        }
        
        Map<Integer, List<IntervalVar>> byInstrORTools = new HashMap<>();
        for (InternalSolverPriorityRequest req : requestsWithORToolsVars) {
            byInstrORTools.computeIfAbsent(req.instructorPojo().getId(), k -> new ArrayList<>()).add(req.intervalVar());
        }
        byInstrORTools.values().forEach(model::addNoOverlap);
    
        Map<Integer, List<IntervalVar>> byRoomORTools = new HashMap<>();
        for (InternalSolverPriorityRequest req : requestsWithORToolsVars) {
            byRoomORTools.computeIfAbsent(req.roomPojo().getId(), k -> new ArrayList<>()).add(req.intervalVar());
        }
        byRoomORTools.values().forEach(model::addNoOverlap);
    
        for (InternalSolverPriorityRequest req : requestsWithORToolsVars) {
            addWindowConstraintsInternal(model, req.startVar(), req.lengthSlots(), 
                                         req.instructorPojo().getAvailabilitySlots(), // Pass POJO availability
                                         req.presentVar(),
                                         req.instructorPojo().getName(), // For logging
                                         req.originalRequestPojo().getId().toString() // For logging
                                         );
        }
    
        IntVar[] presencesArray = requestsWithORToolsVars.stream().map(InternalSolverPriorityRequest::presentVar).toArray(IntVar[]::new);
        model.maximize(LinearExpr.sum(presencesArray));
    
        CpSolver solver = new CpSolver();
        solver.getParameters().setMaxTimeInSeconds(10); 
        CpSolverStatus status = solver.solve(model);
    
        internalConsoleLog.append("Stage A Solver status: ").append(status).append("\n");
    
        if (status == CpSolverStatus.OPTIMAL || status == CpSolverStatus.FEASIBLE) {
            for (InternalSolverPriorityRequest req : requestsWithORToolsVars) {
                if (solver.value(req.presentVar()) == 1) {
                    int startSlotVal = (int) solver.value(req.startVar());
                    confirmedBlocks.add(new InternalLockedBlock(
                        req.instructorPojo(), 
                        req.roomPojo(), 
                        startSlotVal, 
                        req.lengthSlots()
                    ));
                    internalConsoleLog.append("Scheduled Priority: Instr ")
                        .append(req.instructorPojo().getName()).append(" in Room ")
                        .append(req.roomPojo().getName()).append(" at slot ").append(startSlotVal)
                        .append(" for ").append(req.lengthSlots()).append(" slots.\n");
                }
            }
        }
        return confirmedBlocks;
    }
    
    private void addWindowConstraintsInternal(CpModel model, IntVar startVar, int lengthSlots,
                                            List<SolverAvailabilitySlot> availabilitySlots, // Changed from JPA list
                                            BoolVar presentLiteral,
                                            String instructorNameForLog, // Added for better logging
                                            String requestIdentifierForLog // Added for better logging
                                            ) {
        if (availabilitySlots == null || availabilitySlots.isEmpty()) {
            model.addImplication(presentLiteral, model.falseLiteral());
            logger.warn("Instructor {} (Req ID: {}) has no availability slots defined; request linked to literal {} cannot be scheduled.",
                instructorNameForLog, requestIdentifierForLog, presentLiteral.getName());
            return;
        }
    
        List<Literal> windowOptions = new ArrayList<>();
        for (SolverAvailabilitySlot pojoAvail : availabilitySlots) { // Changed from JPA
            // DayOfWeekEnum toJavaTimeDayOfWeek() is no longer needed as SolverAvailabilitySlot uses java.time.DayOfWeek
            int windowStartSlot = toGlobalSlot(pojoAvail.getDayOfWeek(), pojoAvail.getStartTime());
            int windowEndSlot = toGlobalSlot(pojoAvail.getDayOfWeek(), pojoAvail.getEndTime());
            
            if (windowStartSlot == -1 || windowEndSlot == -1 || windowEndSlot <= windowStartSlot) {
                logger.warn("Invalid availability slot for constraint (Instructor: {}, Req ID: {}): {} {}-{} mapped to {}-{}", 
                    instructorNameForLog, requestIdentifierForLog, pojoAvail.getDayOfWeek(), pojoAvail.getStartTime(), pojoAvail.getEndTime(),
                    windowStartSlot, windowEndSlot);
                continue; 
            }
    
            // Unique name for windowSelectedLiteral, incorporating availability slot ID if available, or hash
            String windowLiteralName = "win_" + (pojoAvail.getId() != null ? pojoAvail.getId() : Objects.hash(pojoAvail.getDayOfWeek(), pojoAvail.getStartTime())) + "_" + presentLiteral.getName();
            BoolVar windowSelectedLiteral = model.newBoolVar(windowLiteralName);
            
            model.addGreaterOrEqual(startVar, windowStartSlot).onlyEnforceIf(windowSelectedLiteral);
            model.addLessOrEqual(startVar, windowEndSlot - lengthSlots).onlyEnforceIf(windowSelectedLiteral); 
            
            windowOptions.add(windowSelectedLiteral);
            model.addImplication(windowSelectedLiteral, presentLiteral); 
        }
    
        if (windowOptions.isEmpty()) {
            model.addImplication(presentLiteral, model.falseLiteral());
             logger.warn("No valid, schedulable windows found for Instructor {} (Req ID: {}) based on availability; request linked to literal {} cannot be scheduled.",
                instructorNameForLog, requestIdentifierForLog, presentLiteral.getName());
            return;
        }
    
        windowOptions.add(presentLiteral.not()); 
        model.addExactlyOne(windowOptions); 
    }
    
    
    private List<LockedBlockDTO> convertToLockedBlockDTOs(List<InternalLockedBlock> internalBlocks) { // Removed SolverInput from params
        if (internalBlocks == null) return new ArrayList<>();
        return internalBlocks.stream().map(ib -> {
            LocalDateTime startDateTime = globalSlotToLocalDateTime(ib.startSlot());
            LocalDateTime endDateTime = globalSlotToLocalDateTime(ib.startSlot() + ib.lengthSlots());
    
            SolverStudioLocation loc = ib.roomPojo().getStudioLocation();

            return LockedBlockDTO.builder()
                .instructorId(ib.instructorPojo().getId())
                .instructorName(ib.instructorPojo().getName())
                .roomId(ib.roomPojo().getId())
                .roomName(ib.roomPojo().getName())
                .studioLocationId(loc != null ? loc.getId() : null)
                .studioLocationName(loc != null ? loc.getName() : null)
                .startSlot(ib.startSlot())
                .lengthSlots(ib.lengthSlots())
                .dayOfWeek(startDateTime.getDayOfWeek().toString())
                .startTime(startDateTime.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")))
                .endTime(endDateTime.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")))
                .build();
        }).collect(Collectors.toList());
    }
    
    // TODO: Implement `runActualStageB`, `prepareStageBClassRequests`, `convertToScheduledClassDTOs` using POJOs
    // TODO: Review mapping of "Studio A"/"Studio B" if that concept is still relevant for Stage B with POJOs.
}