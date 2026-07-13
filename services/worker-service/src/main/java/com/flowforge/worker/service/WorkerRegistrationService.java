package com.flowforge.worker.service;

import com.flowforge.worker.config.WorkerProperties;
import com.flowforge.worker.metrics.WorkerMetrics;
import com.flowforge.worker.model.Worker;
import com.flowforge.worker.model.WorkerStatus;
import com.flowforge.worker.model.WorkerCapability;
import com.flowforge.worker.repository.WorkerRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PreDestroy;
import java.lang.management.ManagementFactory;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.UUID;

@Service
public class WorkerRegistrationService implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(WorkerRegistrationService.class);

    private final WorkerRepository workerRepository;
    private final WorkerProperties properties;
    private final WorkerMetrics metrics;
    private final MeterRegistry meterRegistry;
    private final Clock clock;

    private UUID workerPublicId;
    private String instanceId;

    public WorkerRegistrationService(
            WorkerRepository workerRepository,
            WorkerProperties properties,
            WorkerMetrics metrics,
            MeterRegistry meterRegistry,
            Clock clock) {
        this.workerRepository = workerRepository;
        this.properties = properties;
        this.metrics = metrics;
        this.meterRegistry = meterRegistry;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (!properties.isEnabled()) {
            return;
        }

        this.instanceId = properties.getWorkerName() + "@" + ManagementFactory.getRuntimeMXBean().getName();
        this.workerPublicId = UUID.randomUUID();

        // STARTING
        Worker worker = new Worker(
                workerPublicId,
                properties.getWorkerName(),
                instanceId,
                WorkerStatus.STARTING,
                clock.instant(),
                clock.instant()
        );
        worker.getCapabilities().addAll(EnumSet.allOf(WorkerCapability.class)); // Default to all capabilities for this stage
        workerRepository.save(worker);
        logger.info("Worker registered with STARTING status. Instance ID: {}, Public ID: {}", instanceId, workerPublicId);

        // Transition to ACTIVE
        worker.setStatus(WorkerStatus.ACTIVE);
        workerRepository.save(worker);
        logger.info("Worker status transitioned to ACTIVE. Ready to accept claims.");

        // Register active workers gauge
        metrics.registerActiveWorkersGauge(meterRegistry, () -> workerRepository.countByStatus(WorkerStatus.ACTIVE));
    }

    @PreDestroy
    @Transactional
    public void shutdown() {
        if (workerPublicId == null) {
            return;
        }

        try {
            Worker worker = workerRepository.findByPublicId(workerPublicId).orElseThrow();
            logger.info("Shutting down worker {}. Transitioning to DRAINING...", workerPublicId);
            worker.setStatus(WorkerStatus.DRAINING);
            workerRepository.save(worker);

            // In this stage, we have no active HTTP tasks, so draining completes immediately.
            // Transition to OFFLINE
            worker.setStatus(WorkerStatus.OFFLINE);
            workerRepository.save(worker);
            logger.info("Worker {} transitioned to OFFLINE.", workerPublicId);
        } catch (Exception e) {
            logger.error("Failed to cleanly transition worker " + workerPublicId + " to OFFLINE status", e);
        }
    }

    public UUID getWorkerPublicId() {
        return workerPublicId;
    }

    public String getInstanceId() {
        return instanceId;
    }
}
