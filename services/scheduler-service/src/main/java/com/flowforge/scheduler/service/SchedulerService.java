package com.flowforge.scheduler.service;

import com.flowforge.scheduler.config.SchedulerProperties;
import com.flowforge.scheduler.metrics.SchedulerMetrics;
import com.flowforge.scheduler.model.Job;
import com.flowforge.scheduler.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class SchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerService.class);

    private final JobRepository jobRepository;
    private final SchedulerProcessor schedulerProcessor;
    private final SchedulerProperties properties;
    private final SchedulerMetrics metrics;
    private final Clock clock;

    public SchedulerService(
            JobRepository jobRepository,
            SchedulerProcessor schedulerProcessor,
            SchedulerProperties properties,
            SchedulerMetrics metrics,
            Clock clock) {
        this.jobRepository = jobRepository;
        this.schedulerProcessor = schedulerProcessor;
        this.properties = properties;
        this.metrics = metrics;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${flowforge.scheduler.poll-interval-ms:1000}")
    public void pollAndSchedule() {
        if (!properties.isEnabled()) {
            return;
        }

        try {
            Instant now = clock.instant();
            int batchSize = properties.getBatchSize();
            for (int i = 0; i < batchSize; i++) {
                try {
                    boolean processed = schedulerProcessor.claimAndProcessJob(now);
                    if (!processed) {
                        break;
                    }
                } catch (Exception e) {
                    metrics.recordFailure();
                    logger.error("Failed to process scheduled job", e);
                }
            }
        } catch (Exception e) {
            metrics.recordFailure();
            logger.error("Scheduler poll cycle failed", e);
        }
    }
}
