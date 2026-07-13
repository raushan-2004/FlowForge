package com.flowforge.worker.service;

import com.flowforge.worker.config.WorkerProperties;
import com.flowforge.worker.metrics.WorkerMetrics;
import com.flowforge.worker.model.ExecutionLease;
import com.flowforge.worker.model.Worker;
import com.flowforge.worker.repository.ExecutionLeaseRepository;
import com.flowforge.worker.repository.WorkerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WorkerHeartbeatService {

    private static final Logger logger = LoggerFactory.getLogger(WorkerHeartbeatService.class);

    private final WorkerRepository workerRepository;
    private final ExecutionLeaseRepository leaseRepository;
    private final WorkerRegistrationService registrationService;
    private final WorkerProperties properties;
    private final WorkerMetrics metrics;
    private final Clock clock;

    // Track active leases that this worker instance is currently execution-responsible for.
    // If a lease renewal fails (returns 0 rows updated), we remove it from this map.
    private final ConcurrentHashMap<UUID, LeaseTracker> activeLeaseTrackers = new ConcurrentHashMap<>();

    private Instant lastHeartbeatTime = Instant.MIN;

    public WorkerHeartbeatService(
            WorkerRepository workerRepository,
            ExecutionLeaseRepository leaseRepository,
            WorkerRegistrationService registrationService,
            WorkerProperties properties,
            WorkerMetrics metrics,
            Clock clock) {
        this.workerRepository = workerRepository;
        this.leaseRepository = leaseRepository;
        this.registrationService = registrationService;
        this.properties = properties;
        this.metrics = metrics;
        this.clock = clock;
    }

    public static class LeaseTracker {
        private final UUID executionPublicId;
        private final String leaseToken;
        private final Long leaseVersion;

        public LeaseTracker(UUID executionPublicId, String leaseToken, Long leaseVersion) {
            this.executionPublicId = executionPublicId;
            this.leaseToken = leaseToken;
            this.leaseVersion = leaseVersion;
        }

        public UUID getExecutionPublicId() {
            return executionPublicId;
        }

        public String getLeaseToken() {
            return leaseToken;
        }

        public Long getLeaseVersion() {
            return leaseVersion;
        }
    }

    public void trackLease(UUID executionPublicId, String leaseToken, Long leaseVersion) {
        activeLeaseTrackers.put(executionPublicId, new LeaseTracker(executionPublicId, leaseToken, leaseVersion));
    }

    public void untrackLease(UUID executionPublicId) {
        activeLeaseTrackers.remove(executionPublicId);
    }

    public boolean isTracking(UUID executionPublicId) {
        return activeLeaseTrackers.containsKey(executionPublicId);
    }

    public ConcurrentHashMap<UUID, LeaseTracker> getActiveLeaseTrackers() {
        return activeLeaseTrackers;
    }

    @Scheduled(fixedDelayString = "${flowforge.worker.heartbeat-interval-ms:5000}")
    public void runHeartbeat() {
        if (!properties.isEnabled()) {
            return;
        }

        UUID workerId = registrationService.getWorkerPublicId();
        if (workerId == null) {
            return; // Worker not registered yet
        }

        long start = clock.millis();
        try {
            // 1. Update Worker lastHeartbeatAt
            updateWorkerHeartbeat(workerId);

            // 2. Renew all actively tracked leases
            Instant now = clock.instant();
            Instant newExpiry = now.plusMillis(properties.getLeaseDurationMs());

            activeLeaseTrackers.forEach((execId, tracker) -> {
                try {
                    boolean success = renewLeaseTransactionally(execId, tracker.getLeaseToken(), tracker.getLeaseVersion(), newExpiry);
                    if (success) {
                        metrics.recordRenewal();
                        logger.debug("Successfully renewed lease for execution ID: {}", execId);
                    } else {
                        // Lost ownership
                        metrics.recordLeaseConflict();
                        activeLeaseTrackers.remove(execId);
                        logger.warn("Lost lease ownership for execution ID: {}. Abandoning execution.", execId);
                    }
                } catch (Exception e) {
                    logger.error("Failed to renew lease for execution ID: " + execId, e);
                }
            });

            this.lastHeartbeatTime = clock.instant();
            long latency = clock.millis() - start;
            metrics.recordHeartbeatLatency(latency);

        } catch (Exception e) {
            metrics.recordClaimFailure();
            logger.error("Worker heartbeat cycle failed", e);
        }
    }

    @Transactional
    public void updateWorkerHeartbeat(UUID workerId) {
        workerRepository.findByPublicId(workerId).ifPresent(worker -> {
            worker.setLastHeartbeatAt(clock.instant());
            workerRepository.saveAndFlush(worker);
        });
    }

    @Transactional
    public boolean renewLeaseTransactionally(UUID execId, String token, Long leaseVersion, Instant newExpiry) {
        int updated = leaseRepository.renewLease(execId, token, leaseVersion, newExpiry);
        return updated > 0;
    }

    public Instant getLastHeartbeatTime() {
        return lastHeartbeatTime;
    }
}
