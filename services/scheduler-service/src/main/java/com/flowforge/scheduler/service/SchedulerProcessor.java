package com.flowforge.scheduler.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowforge.event.dto.ExecutionCreatedPayload;
import com.flowforge.scheduler.config.SchedulerProperties;
import com.flowforge.scheduler.metrics.SchedulerMetrics;
import com.flowforge.scheduler.model.*;
import com.flowforge.scheduler.repository.ExecutionRepository;
import com.flowforge.scheduler.repository.JobRepository;
import com.flowforge.scheduler.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Component
public class SchedulerProcessor {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerProcessor.class);

    private final JobRepository jobRepository;
    private final ExecutionRepository executionRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final SchedulerProperties properties;
    private final SchedulerMetrics metrics;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public SchedulerProcessor(
            JobRepository jobRepository,
            ExecutionRepository executionRepository,
            OutboxEventRepository outboxEventRepository,
            SchedulerProperties properties,
            SchedulerMetrics metrics,
            ObjectMapper objectMapper,
            Clock clock) {
        this.jobRepository = jobRepository;
        this.executionRepository = executionRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.properties = properties;
        this.metrics = metrics;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean claimAndProcessJob(Instant now) {
        java.util.Optional<Job> jobOpt = jobRepository.claimNextJobForScheduling(now);
        if (jobOpt.isEmpty()) {
            return false;
        }
        Job job = jobOpt.get();

        // 1. Validation check
        Tenant tenant = job.getProject().getTenant();
        Project project = job.getProject();

        if (tenant.getStatus() != TenantStatus.ACTIVE ||
                project.getStatus() != ProjectStatus.ACTIVE ||
                job.getStatus() != JobStatus.ACTIVE ||
                !job.isEnabled()) {
            logger.warn("Skipping job {} (publicId: {}) because tenant/project/job status is not active or job is disabled",
                    job.getName(), job.getPublicId());
            skipAndAdvance(job, now);
            return true;
        }

        // 2. Schedule evaluation
        String cronExprStr = job.getCronExpression();
        if (cronExprStr == null || cronExprStr.trim().isEmpty()) {
            logger.error("Job {} has ACTIVE CRON status but cron expression is null or empty. Disabling job.", job.getId());
            job.setNextFireAt(null);
            jobRepository.save(job);
            return true;
        }

        CronExpression cron = CronExpression.parse(cronExprStr);
        Instant currentFireAt = job.getNextFireAt();
        if (currentFireAt == null) {
            currentFireAt = nextOccurrence(cron, now);
            job.setNextFireAt(currentFireAt);
            jobRepository.save(job);
            return true;
        }

        // Check missed schedules
        Instant temp = nextOccurrence(cron, currentFireAt);
        int missedCount = 0;
        while (temp != null && temp.isBefore(now)) {
            missedCount++;
            temp = nextOccurrence(cron, temp);
        }

        if (missedCount > properties.getMaxMissedSchedulesLimit()) {
            logger.warn("Job {} missed {} schedules (limit: {}). Fast-forwarding nextFireAt.",
                    job.getId(), missedCount, properties.getMaxMissedSchedulesLimit());
            metrics.recordSkipped(missedCount);
            // Fast-forward to the next future execution time
            job.setNextFireAt(nextOccurrence(cron, now));
            job.setLastScheduledAt(currentFireAt);
            job.setScheduleVersion(job.getScheduleVersion() + 1);
            jobRepository.save(job);
            return true;
        }

        // Advance nextFireAt by exactly one step
        Instant nextFireAt = nextOccurrence(cron, currentFireAt);
        job.setNextFireAt(nextFireAt);
        job.setLastScheduledAt(currentFireAt);
        job.setScheduleVersion(job.getScheduleVersion() + 1);
        jobRepository.save(job);

        // Record metrics
        metrics.recordScheduled();
        metrics.recordLag(currentFireAt);

        // Create Execution
        UUID executionPublicId = UUID.randomUUID();
        int maxAttempts = job.getRetryMaxAttempts() != null ? job.getRetryMaxAttempts() : 1;
        Execution execution = new Execution(
                executionPublicId,
                job,
                ExecutionTriggerType.SCHEDULED,
                null,
                currentFireAt,
                maxAttempts
        );

        // Create Attempt #1
        ExecutionAttempt attempt = new ExecutionAttempt(
                UUID.randomUUID(),
                1,
                AttemptStatus.PENDING,
                currentFireAt
        );
        execution.addAttempt(attempt);
        executionRepository.save(execution);

        // Create Outbox Event
        ExecutionCreatedPayload payloadDto = new ExecutionCreatedPayload(
                execution.getPublicId(),
                job.getPublicId(),
                job.getProject().getPublicId(),
                job.getProject().getTenant().getPublicId(),
                execution.getTriggerType().name(),
                execution.getQueuedAt()
        );

        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(payloadDto);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize execution payload to JSON", e);
        }

        OutboxEvent outboxEvent = new OutboxEvent(
                UUID.randomUUID(),
                OutboxAggregateType.EXECUTION,
                execution.getPublicId(),
                "EXECUTION_CREATED",
                jsonPayload,
                1,
                clock.instant()
        );
        outboxEventRepository.save(outboxEvent);

        logger.info("Successfully scheduled job {} (fire time: {}, executionId: {})",
                job.getId(), currentFireAt, executionPublicId);
        return true;
    }

    private void skipAndAdvance(Job job, Instant now) {
        String cronExprStr = job.getCronExpression();
        if (cronExprStr != null && !cronExprStr.trim().isEmpty()) {
            CronExpression cron = CronExpression.parse(cronExprStr);
            Instant currentFireAt = job.getNextFireAt();
            if (currentFireAt != null) {
                job.setNextFireAt(nextOccurrence(cron, currentFireAt));
                job.setLastScheduledAt(currentFireAt);
                job.setScheduleVersion(job.getScheduleVersion() + 1);
            } else {
                job.setNextFireAt(nextOccurrence(cron, now));
            }
        } else {
            job.setNextFireAt(null);
        }
        jobRepository.save(job);
    }

    private Instant nextOccurrence(CronExpression cron, Instant from) {
        if (from == null) return null;
        java.time.ZonedDateTime zdt = from.atZone(java.time.ZoneId.of("UTC"));
        java.time.ZonedDateTime nextZdt = cron.next(zdt);
        return nextZdt != null ? nextZdt.toInstant() : null;
    }
}
