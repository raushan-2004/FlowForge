package com.flowforge.worker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowforge.event.dto.ExecutionCreatedPayload;
import com.flowforge.worker.config.WorkerProperties;
import com.flowforge.worker.metrics.WorkerMetrics;
import com.flowforge.worker.model.ExecutionLease;
import com.flowforge.worker.repository.ExecutionLeaseRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class WorkerClaimService {

    private static final Logger logger = LoggerFactory.getLogger(WorkerClaimService.class);

    private final ExecutionLeaseRepository leaseRepository;
    private final WorkerRegistrationService registrationService;
    private final WorkerProperties properties;
    private final WorkerMetrics metrics;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final WorkerHeartbeatService heartbeatService;

    public WorkerClaimService(
            ExecutionLeaseRepository leaseRepository,
            WorkerRegistrationService registrationService,
            WorkerProperties properties,
            WorkerMetrics metrics,
            MeterRegistry meterRegistry,
            ObjectMapper objectMapper,
            Clock clock,
            @Lazy WorkerHeartbeatService heartbeatService) {
        this.leaseRepository = leaseRepository;
        this.registrationService = registrationService;
        this.properties = properties;
        this.metrics = metrics;
        this.meterRegistry = meterRegistry;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.heartbeatService = heartbeatService;
    }

    @PostConstruct
    public void init() {
        // Register active leases gauge for this worker
        metrics.registerActiveLeasesGauge(meterRegistry, () -> {
            UUID workerId = registrationService.getWorkerPublicId();
            if (workerId == null) {
                return 0L;
            }
            return leaseRepository.countByWorkerPublicId(workerId);
        });
    }

    @KafkaListener(topics = "${flowforge.worker.topic-execution-created:execution-created}", groupId = "${flowforge.worker.group-id:worker-group}")
    public void onExecutionCreated(String message) {
        if (!properties.isEnabled()) {
            return;
        }

        UUID executionPublicId;
        try {
            ExecutionCreatedPayload payload = objectMapper.readValue(message, ExecutionCreatedPayload.class);
            executionPublicId = payload.getExecutionPublicId();
        } catch (Exception e) {
            logger.error("Failed to parse ExecutionCreatedPayload from message: " + message, e);
            return;
        }

        logger.info("Received execution-created event for execution ID: {}", executionPublicId);
        try {
            boolean success = claimExecution(executionPublicId);
            if (success) {
                logger.info("Successfully acquired lease for execution ID: {}", executionPublicId);
            } else {
                logger.info("Skipped/Failed to acquire lease for execution ID: {}", executionPublicId);
            }
        } catch (Exception e) {
            logger.warn("Conflict or transaction rollback when claiming execution ID " + executionPublicId + ": " + e.getMessage());
            metrics.recordClaimFailure();
            metrics.recordLeaseConflict();
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claimExecution(UUID executionPublicId) {
        UUID workerId = registrationService.getWorkerPublicId();
        if (workerId == null) {
            logger.warn("Cannot claim execution: worker is not registered yet");
            metrics.recordClaimFailure();
            return false;
        }

        // Fast path check
        if (leaseRepository.findByExecutionPublicId(executionPublicId).isPresent()) {
            logger.warn("Execution ID {} already leased (fast path check).", executionPublicId);
            metrics.recordClaimFailure();
            return false;
        }

        Instant now = clock.instant();
        Instant expiry = now.plusMillis(properties.getLeaseDurationMs());
        String leaseToken = UUID.randomUUID().toString();

        // Every lease starts at version 1
        ExecutionLease lease = new ExecutionLease(
                executionPublicId,
                workerId,
                leaseToken,
                1L,
                now,
                expiry
        );

        leaseRepository.saveAndFlush(lease);
        metrics.recordSuccessfulClaim();
        // Start tracking the lease for periodic heartbeats
        heartbeatService.trackLease(executionPublicId, leaseToken, 1L);
        return true;
    }
}
