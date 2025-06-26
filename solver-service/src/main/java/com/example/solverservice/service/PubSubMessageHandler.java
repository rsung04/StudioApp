package com.example.solverservice.service;

import com.example.solverservice.dto.PubSubSolveRequest;
import com.example.solverservice.solver.DanceTimetableSolver;
import com.example.solverservice.solver.SolverInput;
import com.example.solverservice.solver.SolverOutput;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.google.cloud.spring.pubsub.integration.AckMode;
import com.google.cloud.spring.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.cloud.spring.pubsub.support.GcpPubSubHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class PubSubMessageHandler {

    private static final Logger logger = LoggerFactory.getLogger(PubSubMessageHandler.class);

    @Value("${solver.gcp.subscription-id}") // Configure in application.properties
    private String subscriptionId;

    private final DanceTimetableSolver danceTimetableSolver;
    private final ObjectMapper objectMapper; // For JSON deserialization
    private final JobStoreService jobStoreService;

    public PubSubMessageHandler(DanceTimetableSolver danceTimetableSolver, ObjectMapper objectMapper, JobStoreService jobStoreService) {
        this.danceTimetableSolver = danceTimetableSolver;
        this.objectMapper = objectMapper;
        this.jobStoreService = jobStoreService;
    }

    @Bean
    public MessageChannel pubsubInputChannel() {
        return new DirectChannel();
    }

    @Bean
    public PubSubInboundChannelAdapter messageChannelAdapter(
            @Qualifier("pubsubInputChannel") MessageChannel inputChannel,
            PubSubTemplate pubSubTemplate) {
        PubSubInboundChannelAdapter adapter =
                new PubSubInboundChannelAdapter(pubSubTemplate, this.subscriptionId);
        adapter.setOutputChannel(inputChannel);
        adapter.setAckMode(AckMode.MANUAL); // Manual ack for robust processing
        adapter.setPayloadType(String.class); // Receive message payload as String (JSON)
        return adapter;
    }

    @ServiceActivator(inputChannel = "pubsubInputChannel")
    public void messageReceiver(String payload,
                                @Qualifier(GcpPubSubHeaders.ORIGINAL_MESSAGE) BasicAcknowledgeablePubsubMessage message) {
        logger.info("Message arrived! Payload: {}", payload);
        PubSubSolveRequest solveRequest = null;
        try {
            solveRequest = objectMapper.readValue(payload, PubSubSolveRequest.class);
            String jobId = solveRequest.getJobId();
            SolverInput solverInput = solveRequest.getSolverInput();

            logger.info("Processing Pub/Sub request for Job ID: {}", jobId);
            jobStoreService.initJob(jobId); // Initialize or update status
            jobStoreService.updateJobStatus(jobId, JobStatus.PROCESSING);

            SolverOutput solverOutput = danceTimetableSolver.executeSolve(solverInput);

            if (solverOutput.isSolveSuccess()) {
                jobStoreService.storeJobOutput(jobId, solverOutput);
                logger.info("Job ID: {} completed successfully.", jobId);
            } else {
                jobStoreService.storeJobError(jobId, solverOutput.getStatusMessage());
                logger.error("Job ID: {} failed. Reason: {}", jobId, solverOutput.getStatusMessage());
            }
            message.ack(); // Acknowledge after processing
            logger.info("Pub/Sub message for Job ID: {} acknowledged.", jobId);

        } catch (IOException e) {
            logger.error("Failed to deserialize Pub/Sub message payload: {}", payload, e);
            // Consider not acking or sending to a dead-letter topic if deserialization fails
            // For now, we ack to prevent reprocessing of a malformed message.
            // If solveRequest is null, we can't get jobId to update status.
            message.ack(); // Acknowledge malformed message to avoid loop
        } catch (Exception e) {
            logger.error("Error processing Pub/Sub message for Job ID: {}", (solveRequest != null ? solveRequest.getJobId() : "UNKNOWN"), e);
            if (solveRequest != null) {
                jobStoreService.storeJobError(solveRequest.getJobId(), "Unexpected error: " + e.getMessage());
            }
            // Acknowledge the message to prevent reprocessing loops for unexpected errors.
            // A more robust system might use a dead-letter queue.
            message.ack();
        }
    }
}