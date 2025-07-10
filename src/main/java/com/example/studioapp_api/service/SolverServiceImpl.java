package com.example.studioapp_api.service;

import com.example.studioapp_api.dto.LockedBlockDTO;
import com.example.studioapp_api.dto.SolveRequestDTO;
import com.example.studioapp_api.dto.SolverJobResponseDTO;
import com.example.studioapp_api.entity.*;
import com.example.studioapp_api.repository.*;
import com.example.studioapp_api.mapper.SolverInputMapper;
import com.example.studioapp_api.dto.solver_service_dtos.PubSubSolveRequestStructure;

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
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SolverServiceImpl implements SolverService {

    private static final Logger logger = LoggerFactory.getLogger(SolverServiceImpl.class);

    // All your existing repositories
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
    
    // === OUR NEW REPOSITORY ===
    private final SolverJobRepository solverJobRepository;

    private final PubSubTemplate pubSubTemplate;
    private final ObjectMapper objectMapper;

    @Value("${solver.gcp.topic-id}")
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
                             SolverJobRepository solverJobRepository, // <-- Injected here
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
        this.solverJobRepository = solverJobRepository; // <-- Assigned here
        this.pubSubTemplate = pubSubTemplate;
        this.objectMapper = objectMapper;
    }

    private Map<java.time.DayOfWeek, OperatingHoursSpan> calculateEffectiveDayWindows(List<Room> rooms, int slotMinutes) {
        // This method remains unchanged from your original file.
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
    @Transactional // <-- MODIFIED: Removed readOnly=true to allow database writes
    public SolverJobResponseDTO triggerSolver(SolveRequestDTO solveRequestDTO) {
        String jobId = UUID.randomUUID().toString();
        OffsetDateTime now = OffsetDateTime.now();
        logger.info("Solver job {} triggered for OrgID: {}, TermID: {}. Publishing to Pub/Sub topic: {}",
                jobId, solveRequestDTO.getOrganizationId(), solveRequestDTO.getTermId(), pubsubTopicId);

        // --- Data Fetching (your original logic is preserved) ---
        Organization organization = organizationRepository.findById(solveRequestDTO.getOrganizationId())
            .orElseThrow(() -> new EntityNotFoundException("Organization not found: " + solveRequestDTO.getOrganizationId()));
        Term term = termRepository.findById(solveRequestDTO.getTermId())
            .orElseThrow(() -> new EntityNotFoundException("Term not found: " + solveRequestDTO.getTermId()));
        
        // === START: NEW LOGIC TO PERSIST THE JOB ===
        SolverJob newJob = new SolverJob();
        newJob.setJobId(jobId);
        newJob.setTermId(term.getId());
        newJob.setOrganizationId(organization.getId());
        newJob.setStatus("QUEUED"); // Set initial status
        newJob.setSubmittedAt(now);
        newJob.setLastUpdatedAt(now);
        solverJobRepository.save(newJob);
        logger.info("Job {} has been saved to the database with status QUEUED.", jobId);
        // === END: NEW LOGIC TO PERSIST THE JOB ===

        if (!term.getOrganization().getId().equals(organization.getId())) {
            throw new IllegalArgumentException("Term ID " + term.getId() + " does not belong to Organization ID " + organization.getId());
        }

        // --- All the rest of your data fetching logic remains untouched ---
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
        instructors.forEach(i -> {
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
        final int slotMinutesConfig = 5;
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
        
        // --- Your Pub/Sub publishing logic remains untouched ---
        SolverInputMapper.LocalSolverServiceInput solverServiceInput = SolverInputMapper.createSolverServiceInputStructure(
                slotMinutesConfig, effectiveWindows, instructors, relevantRooms, priorityRequests, classDefinitions, classRequirements
        );
        PubSubSolveRequestStructure pubSubRequest = new PubSubSolveRequestStructure(jobId, solverServiceInput);
        try {
            String jsonPayload = objectMapper.writeValueAsString(pubSubRequest);
            pubSubTemplate.publish(this.pubsubTopicId, jsonPayload);
            logger.info("Job {} published to Pub/Sub topic {}.", jobId, this.pubsubTopicId);
        } catch (Exception e) {
            // Your error handling is preserved. If publishing fails, the transaction will roll back,
            // and the job record we tried to save will be removed, which is the correct behavior.
            logger.error("Job {}: Failed to publish message. Error: {}", jobId, e.getMessage(), e);
            throw new RuntimeException("Failed to publish solver job to Pub/Sub.", e);
        }

        return SolverJobResponseDTO.builder()
                .jobId(jobId)
                .status("QUEUED") // <-- MODIFIED: Changed from PENDING to QUEUED for consistency
                .message("Solver job has been successfully queued. You can track its status using the provided jobId.")
                .submittedAt(now)
                .build();
    }

    // --- We will implement getJobStatus in a later step. Your placeholder is perfect for now. ---
    @Override
    @Transactional(readOnly = true) // This operation only reads from the database
    public SolverJobResponseDTO getJobStatus(String jobId) {
        logger.info("Fetching status for job ID: {}", jobId);

        // Find the job in the database by its ID
        SolverJob job = solverJobRepository.findById(jobId)
                .orElseThrow(() -> {
                    logger.warn("Job status request for non-existent job ID: {}", jobId);
                    return new EntityNotFoundException("Job with ID " + jobId + " not found.");
                });

        // Map the SolverJob entity to our response DTO
        return SolverJobResponseDTO.builder()
                .jobId(job.getJobId())
                .status(job.getStatus())
                .message(job.getErrorMessage() != null ? "Job failed: " + job.getErrorMessage() : "Status at " + job.getLastUpdatedAt())
                .submittedAt(job.getSubmittedAt())
                .build();
    }

    // --- This method remains untouched ---
    @Override
    public List<LockedBlockDTO> getStageAResults(String jobId) {
        logger.warn("getStageAResults for job ID: {} - Placeholder. Implement query to shared job store.", jobId);
        return new ArrayList<>();
    }
}
