package com.flowforge.worker.service;

import com.flowforge.worker.config.WorkerProperties;
import com.flowforge.worker.metrics.WorkerMetrics;
import com.flowforge.worker.model.ExecutionLease;
import com.flowforge.worker.repository.ExecutionLeaseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class LeaseRecoveryService {

    private static final Logger logger = LoggerFactory.getLogger(LeaseRecoveryService.class);

    private final ExecutionLeaseRepository leaseRepository;
    private final WorkerRegistrationService registrationService;
    private final WorkerProperties properties;
    private final WorkerMetrics metrics;
    private final Clock clock;
    private final WorkerHeartbeatService heartbeatService;

    public LeaseRecoveryService(
            ExecutionLeaseRepository leaseRepository,
            WorkerRegistrationService registrationService,
            WorkerProperties properties,
            WorkerMetrics metrics,
            Clock clock,
            WorkerHeartbeatService heartbeatService) {
        this.leaseRepository = leaseRepository;
        this.registrationService = registrationService;
        this.properties = properties;
        this.metrics = metrics;
        this.clock = clock;
        this.heartbeatService = heartbeatService;
    }

    @Scheduled(fixedDelayString = "${flowforge.worker.recovery-interval-ms:10000}")
    public void runRecovery() {
        if (!properties.isEnabled()) {
            return;
        }

        UUID workerId = registrationService.getWorkerPublicId();
        if (workerId == null) {
            return; // Worker not registered yet
        }

        try {
            Instant now = clock.instant();
            List<ExecutionLease> expired = leaseRepository.findExpiredLeases(now);
            if (expired.isEmpty()) {
                return;
            }

            logger.info("Found {} expired execution leases. Initiating recovery...", expired.size());
            for (ExecutionLease lease : expired) {
                try {
                    boolean success = reclaimLeaseTransactionally(lease.getExecutionPublicId(), workerId, now);
                    if (success) {
                        metrics.recordExpiredLease();
                        metrics.recordLeaseRecovery();
                        logger.info("Successfully recovered expired lease for execution ID: {}", lease.getExecutionPublicId());
                    } else {
                        metrics.recordLeaseConflict();
                        logger.debug("Failed to recover lease for execution ID: {} (reclaimed by another instance)", lease.getExecutionPublicId());
                    }
                } catch (Exception e) {
                    logger.error("Error recovering lease for execution ID: " + lease.getExecutionPublicId(), e);
                }
            }

        } catch (Exception e) {
            logger.error("Lease recovery cycle failed", e);
        }
    }

    @Transactional
    public boolean reclaimLeaseTransactionally(UUID execId, UUID recoveryWorkerId, Instant now) {
        String newToken = UUID.randomUUID().toString();
        Instant newExpiry = now.plusMillis(properties.getLeaseDurationMs());

        int updated = leaseRepository.reclaimExpiredLease(execId, recoveryWorkerId, newToken, now, newExpiry);
        if (updated > 0) {
            // Load the updated lease to get the new leaseVersion
            leaseRepository.findByExecutionPublicId(execId).ifPresent(l -> {
                // Track the newly acquired lease in heartbeat service
                heartbeatService.trackLease(execId, l.getLeaseToken(), l.getLeaseVersion());
            });
            return true;
        }
        return false;
    }
}
