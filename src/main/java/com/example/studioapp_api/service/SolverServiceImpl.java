package com.example.studioapp_api.service;

import com.example.studioapp_api.dto.LockedBlockDTO;
import com.example.studioapp_api.dto.SolveRequestDTO;
import com.example.studioapp_api.dto.SolverJobResponseDTO;
import com.example.studioapp_api.entity.*;
import com.example.studioapp_api.repository.*;
// DanceTimetableSolver, SolverInput, SolverOutput are no longer directly used here.
// They are part of the solver-service.
import com.example.studioapp_api.mapper.SolverInputMapper; // For mapping to DTOs for solver-service
import com.example.studioapp_api.dto.solver_service_dtos.PubSubSolveRequestStructure; // To define the Pub/Sub message structure

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import jakarta.persistence.EntityNotFoundException;

import com.example.studioapp_api.solver.OperatingHoursSpan;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID; // For generating job IDs
import java.util.stream.Collectors;

@Service
public class SolverServiceImpl implements SolverService {

    private static final Logger logger = LoggerFactory.getLogger(SolverServiceImpl.class);

    // Inject all repositories needed to fetch data for the solver
    private final OrganizationRepository organizationRepository;
    private final TermRepository termRepository;
    private final InstructorRepository instructorRepository;
    private final InstructorAvailabilitySlotRepository availabilitySlotRepository;
    private final StudioLocationRepository studioLocationRepository;
    private final RoomRepository roomRepository;
    private final RoomOperatingHoursRepository roomOperatingHoursRepository;
    private final ClassTypeRepository classTypeRepository;
    private final ClassDefinitionRepository classDefinitionRepository;
    private final InstructorClassQualificationRepository qualificationRepository;
    private final InstructorPriorityRequestRepository priorityRequestRepository;
    private final ClassSessionRequirementRepository sessionRequirementRepository;
    // private final ScheduledEventRepository scheduledEventRepository; // For saving final results from job store

    private final PubSubTemplate pubSubTemplate;
    private final ObjectMapper objectMapper;

    @Value("${solver.gcp.topic-id}") // Configure in application.properties (e.g., solve-requests-topic)
    private String pubsubTopicId;

    @Autowired
    public SolverServiceImpl(OrganizationRepository organizationRepository,
                             TermRepository termRepository,
                             InstructorRepository instructorRepository,
                             InstructorAvailabilitySlotRepository availabilitySlotRepository,
                             StudioLocationRepository studioLocationRepository,
                             RoomRepository roomRepository,
                             RoomOperatingHoursRepository roomOperatingHoursRepository,
                             ClassTypeRepository classTypeRepository,
                             ClassDefinitionRepository classDefinitionRepository,
                             InstructorClassQualificationRepository qualificationRepository,
                             InstructorPriorityRequestRepository priorityRequestRepository,
                             ClassSessionRequirementRepository sessionRequirementRepository,
                             PubSubTemplate pubSubTemplate,
                             ObjectMapper objectMapper) {
        this.organizationRepository = organizationRepository;
        this.termRepository = termRepository;
        this.instructorRepository = instructorRepository;
        this.availabilitySlotRepository = availabilitySlotRepository;
        this.studioLocationRepository = studioLocationRepository;
        this.roomRepository = roomRepository;
        this.roomOperatingHoursRepository = roomOperatingHoursRepository;
        this.classTypeRepository = classTypeRepository;
        this.classDefinitionRepository = classDefinitionRepository;
        this.qualificationRepository = qualificationRepository;
        this.priorityRequestRepository = priorityRequestRepository;
        this.sessionRequirementRepository = sessionRequirementRepository;
        this.pubSubTemplate = pubSubTemplate;
        this.objectMapper = objectMapper;
    }

    private Map<java.time.DayOfWeek, OperatingHoursSpan> calculateEffectiveDayWindows(List<Room> rooms, int slotMinutes) {
        Map<DayOfWeek, OperatingHoursSpan> effectiveDayWindows = new EnumMap<>(DayOfWeek.class);
        if (rooms.isEmpty()) {
            logger.warn("No rooms provided to calculate effective day windows, using default empty spans.");
            for (DayOfWeek d : DayOfWeek.values()) {
                effectiveDayWindows.put(d, new OperatingHoursSpan(LocalTime.MIDNIGHT, LocalTime.MIDNIGHT));
            }
            return effectiveDayWindows;
        }

        for (DayOfWeek day : DayOfWeek.values()) {
            LocalTime overallEarliestStart = null;
            LocalTime overallLatestEnd = null;

            for (Room room : rooms) {
                // Ensure operatingHours are loaded if lazy
                // If triggerSolver is @Transactional, this access should work.
                // Otherwise, this data needs to be eagerly fetched or fetched in a dedicated query.
                if (room.getOperatingHours() != null) { 
                    for (RoomOperatingHours hours : room.getOperatingHours()) {
                        if (hours.getDayOfWeek().name().equals(day.name())) {
                            if (overallEarliestStart == null || hours.getStartTime().isBefore(overallEarliestStart)) {
                                overallEarliestStart = hours.getStartTime();
                            }
                            if (overallLatestEnd == null || hours.getEndTime().isAfter(overallLatestEnd)) {
                                overallLatestEnd = hours.getEndTime();
                            }
                        }
                    }
                }
            }
            if (overallEarliestStart == null || overallLatestEnd == null) {
                 logger.debug("No operating hours found for {} in provided rooms. Defaulting to closed.", day);
                 overallEarliestStart = LocalTime.MIDNIGHT; 
                 overallLatestEnd = LocalTime.MIDNIGHT;   
            }
            effectiveDayWindows.put(day, new OperatingHoursSpan(overallEarliestStart, overallLatestEnd));
        }
        logger.info("Calculated effective day windows: {}", effectiveDayWindows);
        return effectiveDayWindows;
    }

    @Override
    @Transactional(readOnly = true) // Keep readOnly for data fetching
    public SolverJobResponseDTO triggerSolver(SolveRequestDTO solveRequestDTO) {
        String jobId = UUID.randomUUID().toString();
        logger.info("Solver job {} triggered for OrgID: {}, TermID: {}. Publishing to Pub/Sub topic: {}",
                jobId, solveRequestDTO.getOrganizationId(), solveRequestDTO.getTermId(), pubsubTopicId);

        // --- Data Fetching (remains largely the same) ---
        Organization organization = organizationRepository.findById(solveRequestDTO.getOrganizationId())
            .orElseThrow(() -> new EntityNotFoundException("Organization not found: " + solveRequestDTO.getOrganizationId()));
        Term term = termRepository.findById(solveRequestDTO.getTermId())
            .orElseThrow(() -> new EntityNotFoundException("Term not found: " + solveRequestDTO.getTermId()));
        
        if (!term.getOrganization().getId().equals(organization.getId())) {
            throw new IllegalArgumentException("Term ID " + term.getId() + " does not belong to Organization ID " + organization.getId());
        }

        final StudioLocation finalSpecificLocationForSolve;
        if (solveRequestDTO.getStudioLocationId() != null) {
            StudioLocation tempLocation = studioLocationRepository.findById(solveRequestDTO.getStudioLocationId())
                .orElseThrow(() -> new EntityNotFoundException("StudioLocation for solve not found: " + solveRequestDTO.getStudioLocationId()));
            if (!tempLocation.getOrganization().getId().equals(organization.getId())) {
                 throw new IllegalArgumentException("StudioLocation ID " + tempLocation.getId() + " does not belong to Organization ID " + organization.getId());
            }
            finalSpecificLocationForSolve = tempLocation;
        } else {
            finalSpecificLocationForSolve = null;
        }

        List<Instructor> instructors = instructorRepository.findByOrganizationId(organization.getId());
        instructors.forEach(i -> { // Eagerly load collections if needed, or ensure fetch strategy is appropriate
            if (i.getAvailabilitySlots() != null) i.getAvailabilitySlots().size();
            if (i.getClassQualifications() != null) i.getClassQualifications().size();
        });

        List<Room> relevantRooms = new ArrayList<>();
        if (finalSpecificLocationForSolve != null) {
            relevantRooms.addAll(roomRepository.findByStudioLocationId(finalSpecificLocationForSolve.getId()));
        } else {
            List<StudioLocation> locationsForOrg = studioLocationRepository.findByOrganizationId(organization.getId());
            for (StudioLocation loc : locationsForOrg) {
                relevantRooms.addAll(roomRepository.findByStudioLocationId(loc.getId()));
            }
        }
         relevantRooms.forEach(r -> {
            if (r.getOperatingHours() != null) r.getOperatingHours().size();
         });

        final int slotMinutesConfig = 5; // This could also come from solveRequestDTO or organization settings
        Map<java.time.DayOfWeek, OperatingHoursSpan> effectiveWindows = calculateEffectiveDayWindows(relevantRooms, slotMinutesConfig);

        List<InstructorPriorityRequest> priorityRequests = priorityRequestRepository.findByTermId(term.getId())
            .stream()
            .filter(pr -> pr.getInstructor().getOrganization().getId().equals(organization.getId()))
            .filter(pr -> pr.isActive())
            .filter(pr -> finalSpecificLocationForSolve == null ||
                           pr.getStudioLocation() == null ||
                           (pr.getStudioLocation() != null && pr.getStudioLocation().getId().equals(finalSpecificLocationForSolve.getId())))
            .collect(Collectors.toList());
            
        List<ClassSessionRequirement> classRequirements = sessionRequirementRepository.findByTermId(term.getId())
            .stream()
            .filter(csr -> csr.getClassDefinition().getOrganization().getId().equals(organization.getId()))
            .filter(csr -> csr.isActive())
            .filter(csr -> finalSpecificLocationForSolve == null ||
                            csr.getStudioLocation() == null ||
                            (csr.getStudioLocation() != null && csr.getStudioLocation().getId().equals(finalSpecificLocationForSolve.getId())))
            .collect(Collectors.toList());
        
        List<ClassDefinition> classDefinitions = classDefinitionRepository.findByOrganizationId(organization.getId());
        // --- End Data Fetching ---

        // Map to DTO for solver-service
        SolverInputMapper.LocalSolverServiceInput solverServiceInput = SolverInputMapper.createSolverServiceInputStructure(
                slotMinutesConfig,
                effectiveWindows,
                instructors,
                relevantRooms,
                priorityRequests,
                classDefinitions,
                classRequirements
        );

        // Create Pub/Sub message payload
        PubSubSolveRequestStructure pubSubRequest = new PubSubSolveRequestStructure(jobId, solverServiceInput);
        try {
            String jsonPayload = objectMapper.writeValueAsString(pubSubRequest);
            pubSubTemplate.publish(this.pubsubTopicId, jsonPayload);
            logger.info("Job {} published to Pub/Sub topic {}.", jobId, this.pubsubTopicId);
        } catch (JsonProcessingException e) {
            logger.error("Job {}: Failed to serialize PubSubSolveRequest to JSON. Error: {}", jobId, e.getMessage(), e);
            // Handle serialization error, maybe return an error response
            return SolverJobResponseDTO.builder()
                    .jobId(jobId) // Return job ID even on publish failure for tracking
                    .status("FAILED_TO_PUBLISH")
                    .message("Error preparing solver request: " + e.getMessage())
                    .submittedAt(OffsetDateTime.now())
                    .build();
        } catch (Exception e) {
            logger.error("Job {}: Failed to publish message to Pub/Sub topic {}. Error: {}", jobId, this.pubsubTopicId, e.getMessage(), e);
            return SolverJobResponseDTO.builder()
                    .jobId(jobId)
                    .status("FAILED_TO_PUBLISH")
                    .message("Error sending solver request: " + e.getMessage())
                    .submittedAt(OffsetDateTime.now())
                    .build();
        }

        return SolverJobResponseDTO.builder()
                .jobId(jobId)
                .status("PENDING") // Indicates the job has been submitted for asynchronous processing
                .message("Solver job submitted successfully. Awaiting processing.")
                .submittedAt(OffsetDateTime.now())
                .build();
    }

    @Override
    public SolverJobResponseDTO getJobStatus(String jobId) {
        // TODO: Implement logic to retrieve actual job status from the shared job store (e.g., Firestore, Cloud SQL)
        // This service will query the datastore that solver-service writes to.
        logger.warn("getJobStatus for job ID: {} - Placeholder. Implement query to shared job store.", jobId);
        return SolverJobResponseDTO.builder()
                .jobId(jobId)
                .status("UNKNOWN_PLACEHOLDER")
                .message("Status retrieval from shared job store not implemented yet.")
                .submittedAt(OffsetDateTime.now().minusMinutes(5)) // Placeholder
                .build();
    }

    @Override
    public List<LockedBlockDTO> getStageAResults(String jobId) {
        // TODO: Implement logic to retrieve Stage A results for a completed job from the shared job store.
        logger.warn("getStageAResults for job ID: {} - Placeholder. Implement query to shared job store.", jobId);
        // This would involve fetching the SolverOutput from the job store and extracting stageAResults.
        // The LockedBlockDTO structure is assumed to be the same or mappable.
        return new ArrayList<>(); // Placeholder
    }
}