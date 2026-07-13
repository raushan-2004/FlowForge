package com.flowforge.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowforge.api.dto.ExecutionAttemptResponse;
import com.flowforge.event.dto.ExecutionCreatedPayload;
import com.flowforge.api.dto.ExecutionRequest;
import com.flowforge.api.dto.ExecutionResponse;
import com.flowforge.api.exception.InvalidRequestException;
import com.flowforge.api.exception.MembershipDeniedException;
import com.flowforge.api.exception.ResourceNotFoundException;
import com.flowforge.api.model.*;
import com.flowforge.api.repository.ExecutionAttemptRepository;
import com.flowforge.api.repository.ExecutionRepository;
import com.flowforge.api.repository.JobRepository;
import com.flowforge.api.repository.OutboxEventRepository;
import com.flowforge.api.security.TenantSecurityContext;
import com.flowforge.api.security.TenantSecurityContextHolder;
import com.flowforge.api.shared.identity.PublicIdGenerator;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ExecutionService {

    private static final Logger logger = LoggerFactory.getLogger(ExecutionService.class);

    private final ExecutionRepository executionRepository;
    private final ExecutionAttemptRepository attemptRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final JobRepository jobRepository;
    private final TenantAuthorizationService authorizationService;
    private final PublicIdGenerator publicIdGenerator;
    private final Clock clock;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    private final Counter finalizedCounter;
    private final Counter failedCounter;
    private final Counter rollbackCounter;
    private final Counter leaseReleasedCounter;

    public ExecutionService(
            ExecutionRepository executionRepository,
            ExecutionAttemptRepository attemptRepository,
            OutboxEventRepository outboxEventRepository,
            JobRepository jobRepository,
            TenantAuthorizationService authorizationService,
            PublicIdGenerator publicIdGenerator,
            Clock clock,
            ObjectMapper objectMapper,
            JdbcTemplate jdbcTemplate,
            MeterRegistry meterRegistry) {
        this.executionRepository = executionRepository;
        this.attemptRepository = attemptRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.jobRepository = jobRepository;
        this.authorizationService = authorizationService;
        this.publicIdGenerator = publicIdGenerator;
        this.clock = clock;
        this.objectMapper = objectMapper;
        this.jdbcTemplate = jdbcTemplate;

        this.finalizedCounter = meterRegistry.counter("flowforge.execution.finalized");
        this.failedCounter = meterRegistry.counter("flowforge.execution.finalization.failed");
        this.rollbackCounter = meterRegistry.counter("flowforge.execution.finalization.rollback");
        this.leaseReleasedCounter = meterRegistry.counter("flowforge.execution.lease.released");
    }

    private TenantSecurityContext getActiveTenantContext() {
        TenantSecurityContext context = TenantSecurityContextHolder.getContext();
        if (context == null) {
            throw new MembershipDeniedException("Active tenant context is required");
        }
        return context;
    }

    private String getActorTriggerSource(TenantSecurityContext context) {
        if (context.isHuman()) {
            return context.getUserPublicId().toString();
        }
        return "project_key_" + context.getProjectPublicId().toString().substring(0, 8);
    }

    @Transactional
    public ExecutionResponse createExecution(ExecutionRequest request) {
        TenantSecurityContext context = getActiveTenantContext();

        // Authorization check
        if (context.isAutomation()) {
            if (!authorizationService.canSubmitJobs()) {
                throw new MembershipDeniedException("Automation key is not authorized to submit jobs");
            }
        } else {
            if (!authorizationService.canCreateProjects()) {
                throw new MembershipDeniedException("Only OWNER, ADMIN, and DEVELOPER can trigger executions");
            }
        }

        if (request.getJobPublicId() == null) {
            throw new InvalidRequestException("Job public ID is required");
        }

        // Job ownership validation (strictly bounded to tenant/project)
        Job job;
        if (context.isAutomation()) {
            job = jobRepository.findByPublicIdAndProjectPublicIdAndTenantPublicId(request.getJobPublicId(), context.getProjectPublicId(), context.getTenantPublicId())
                    .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        } else {
            job = jobRepository.findByPublicIdAndTenantPublicId(request.getJobPublicId(), context.getTenantPublicId())
                    .orElseThrow(() -> new ResourceNotFoundException("Job not found"));
        }

        // Job status validation
        if (job.getStatus() == JobStatus.ARCHIVED) {
            throw new InvalidRequestException("Cannot trigger execution: Job is archived");
        }
        if (job.getStatus() == JobStatus.PAUSED) {
            throw new InvalidRequestException("Cannot trigger execution: Job is paused");
        }

        // Parse trigger type
        ExecutionTriggerType triggerType;
        if (request.getTriggerType() == null || request.getTriggerType().trim().isEmpty()) {
            triggerType = context.isAutomation() ? ExecutionTriggerType.API_KEY : ExecutionTriggerType.MANUAL;
        } else {
            try {
                triggerType = ExecutionTriggerType.valueOf(request.getTriggerType().trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new InvalidRequestException("Invalid trigger type: " + request.getTriggerType());
            }
        }

        String triggerSource = request.getTriggerSource() != null && !request.getTriggerSource().trim().isEmpty()
                ? request.getTriggerSource().trim()
                : getActorTriggerSource(context);

        int maxAttempts = job.getRetryMaxAttempts() != null ? job.getRetryMaxAttempts() + 1 : 3;

        Instant now = clock.instant();

        // Create aggregate parent
        Execution execution = new Execution(
                publicIdGenerator.generate(),
                job,
                triggerType,
                triggerSource,
                now,
                maxAttempts
        );

        // Create attempt #1 (PENDING status)
        ExecutionAttempt firstAttempt = new ExecutionAttempt(
                publicIdGenerator.generate(),
                1,
                AttemptStatus.PENDING,
                now
        );

        execution.addAttempt(firstAttempt);

        // Persist parent (which cascades to child)
        execution = executionRepository.save(execution);

        // Construct outbox event payload DTO
        ExecutionCreatedPayload payloadDto = new ExecutionCreatedPayload(
                execution.getPublicId(),
                execution.getJob().getPublicId(),
                execution.getProject().getPublicId(),
                execution.getTenant().getPublicId(),
                execution.getTriggerType().name(),
                execution.getQueuedAt()
        );

        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(payloadDto);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize execution payload to JSON", e);
        }

        // Create OutboxEvent
        OutboxEvent outboxEvent = new OutboxEvent(
                publicIdGenerator.generate(),
                OutboxAggregateType.EXECUTION,
                execution.getPublicId(),
                "EXECUTION_CREATED",
                jsonPayload,
                1, // payloadVersion
                now
        );

        outboxEventRepository.save(outboxEvent);

        return mapToResponse(execution);
    }

    @Transactional
    public Execution triggerExecutionInternal(Job job, ExecutionTriggerType triggerType, String triggerSource, Instant now) {
        int maxAttempts = job.getRetryMaxAttempts() != null ? job.getRetryMaxAttempts() + 1 : 3;

        Execution execution = new Execution(
                publicIdGenerator.generate(),
                job,
                triggerType,
                triggerSource,
                now,
                maxAttempts
        );

        ExecutionAttempt firstAttempt = new ExecutionAttempt(
                publicIdGenerator.generate(),
                1,
                AttemptStatus.PENDING,
                now
        );

        execution.addAttempt(firstAttempt);

        execution = executionRepository.saveAndFlush(execution);
        attemptRepository.saveAndFlush(firstAttempt);

        // Emit EXECUTION_CREATED event with payload version 2
        ExecutionCreatedPayload payloadDto = new ExecutionCreatedPayload(
                execution.getPublicId(),
                execution.getJob().getPublicId(),
                execution.getProject().getPublicId(),
                execution.getTenant().getPublicId(),
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
                publicIdGenerator.generate(),
                OutboxAggregateType.EXECUTION,
                execution.getPublicId(),
                "EXECUTION_CREATED",
                jsonPayload,
                1, // payloadVersion
                now
        );

        outboxEventRepository.saveAndFlush(outboxEvent);

        return execution;
    }

    @Transactional(readOnly = true)
    public List<ExecutionResponse> getExecutions(UUID projectId) {
        TenantSecurityContext context = getActiveTenantContext();

        if (projectId != null) {
            if (context.isAutomation() && !context.getProjectPublicId().equals(projectId)) {
                throw new ResourceNotFoundException("Project not found");
            }
            return executionRepository.findAllByProjectPublicIdAndTenantPublicId(projectId, context.getTenantPublicId()).stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }

        if (context.isAutomation()) {
            return executionRepository.findAllByProjectPublicIdAndTenantPublicId(context.getProjectPublicId(), context.getTenantPublicId()).stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }

        return executionRepository.findAllByTenantPublicId(context.getTenantPublicId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ExecutionResponse getExecution(UUID executionId) {
        TenantSecurityContext context = getActiveTenantContext();

        Execution execution;
        if (context.isAutomation()) {
            execution = executionRepository.findByPublicIdAndProjectPublicIdAndTenantPublicId(executionId, context.getProjectPublicId(), context.getTenantPublicId())
                    .orElseThrow(() -> new ResourceNotFoundException("Execution not found"));
        } else {
            execution = executionRepository.findByPublicIdAndTenantPublicId(executionId, context.getTenantPublicId())
                    .orElseThrow(() -> new ResourceNotFoundException("Execution not found"));
        }

        return mapToResponse(execution);
    }

    @Transactional(readOnly = true)
    public List<ExecutionAttemptResponse> getAttempts(UUID executionId) {
        TenantSecurityContext context = getActiveTenantContext();

        if (context.isAutomation()) {
            executionRepository.findByPublicIdAndProjectPublicIdAndTenantPublicId(executionId, context.getProjectPublicId(), context.getTenantPublicId())
                    .orElseThrow(() -> new ResourceNotFoundException("Execution not found"));
            return attemptRepository.findAllByExecutionPublicIdAndProjectPublicIdAndTenantPublicId(executionId, context.getProjectPublicId(), context.getTenantPublicId()).stream()
                    .map(this::mapToAttemptResponse)
                    .collect(Collectors.toList());
        } else {
            executionRepository.findByPublicIdAndTenantPublicId(executionId, context.getTenantPublicId())
                    .orElseThrow(() -> new ResourceNotFoundException("Execution not found"));
            return attemptRepository.findAllByExecutionPublicIdAndTenantPublicId(executionId, context.getTenantPublicId()).stream()
                    .map(this::mapToAttemptResponse)
                    .collect(Collectors.toList());
        }
    }

    private ExecutionResponse mapToResponse(Execution execution) {
        return new ExecutionResponse(
                execution.getPublicId(),
                execution.getJob().getPublicId(),
                execution.getProject().getPublicId(),
                execution.getTenant().getPublicId(),
                execution.getTriggerType(),
                execution.getTriggerSource(),
                execution.getCurrentStatus(),
                execution.getQueuedAt(),
                execution.getStartedAt(),
                execution.getFinishedAt(),
                execution.getCurrentAttemptNumber(),
                execution.getMaxAttempts(),
                execution.getCreatedAt(),
                execution.getUpdatedAt(),
                execution.getVersion()
        );
    }

    private ExecutionAttemptResponse mapToAttemptResponse(ExecutionAttempt attempt) {
        return new ExecutionAttemptResponse(
                attempt.getPublicId(),
                attempt.getAttemptNumber(),
                attempt.getStatus(),
                attempt.getStartedAt(),
                attempt.getFinishedAt(),
                attempt.getWorkerId(),
                attempt.getDuration(),
                attempt.getErrorCategory(),
                attempt.getHttpStatus(),
                attempt.getResponseSize(),
                attempt.getBodyTruncated(),
                attempt.getNetworkError(),
                attempt.getContentType(),
                attempt.getCreatedAt()
        );
    }

    @Transactional
    public void finalizeExecution(UUID executionPublicId, com.flowforge.api.dto.InternalExecutionFinalizeRequest request) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int status) {
                    if (status == STATUS_COMMITTED) {
                        finalizedCounter.increment();
                        leaseReleasedCounter.increment();
                    } else if (status == STATUS_ROLLED_BACK) {
                        rollbackCounter.increment();
                        failedCounter.increment();
                    }
                }
            });
        }

        // 1. Validate execution ownership
        Execution execution = executionRepository.findByPublicId(executionPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Execution not found"));

        if (execution.getCurrentStatus() != ExecutionStatus.RUNNING) {
            throw new IllegalStateException("Execution is not in RUNNING state. Current state: " + execution.getCurrentStatus());
        }

        // 2. Validate lease token in database
        String leaseToken = request.getLeaseToken();
        Integer leaseCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM execution_leases WHERE execution_public_id = ? AND lease_token = ?",
                Integer.class,
                executionPublicId,
                leaseToken
        );

        if (leaseCount == null || leaseCount == 0) {
            throw new InvalidRequestException("Invalid lease token or lease not found for execution: " + executionPublicId);
        }

        // Determine final statuses
        ExecutionStatus finalExecutionStatus;
        AttemptStatus finalAttemptStatus;

        if (request.getNetworkErrorCategory() != null) {
            finalExecutionStatus = ExecutionStatus.FAILED;
            finalAttemptStatus = AttemptStatus.FAILED;
        } else {
            Integer statusCode = request.getStatusCode();
            if (statusCode != null && statusCode >= 200 && statusCode < 300) {
                finalExecutionStatus = ExecutionStatus.SUCCEEDED;
                finalAttemptStatus = AttemptStatus.SUCCEEDED;
            } else {
                finalExecutionStatus = ExecutionStatus.FAILED;
                finalAttemptStatus = AttemptStatus.FAILED;
            }
        }

        Instant finishedAt = request.getFinishedAt();

        // 3. Update Execution
        if (finalExecutionStatus == ExecutionStatus.SUCCEEDED) {
            execution.succeed(finishedAt);
        } else {
            execution.fail(finishedAt);
        }
        executionRepository.saveAndFlush(execution);

        // 4. Update the current ExecutionAttempt
        int currentAttemptNum = execution.getCurrentAttemptNumber();
        ExecutionAttempt attempt = attemptRepository.findByExecutionIdAndAttemptNumber(execution.getId(), currentAttemptNum)
                .orElseThrow(() -> new ResourceNotFoundException("Active attempt not found"));

        if (finalAttemptStatus == AttemptStatus.SUCCEEDED) {
            attempt.succeed(finishedAt, request.getStatusCode(), request.getResponseSize(), request.getBodyTruncated(), request.getContentType());
        } else {
            String errorCategory = request.getNetworkErrorCategory() != null ? "NETWORK_ERROR" : "HTTP_ERROR";
            attempt.fail(
                    errorCategory,
                    request.getNetworkErrorCategory(),
                    request.getStatusCode(),
                    request.getResponseSize(),
                    request.getBodyTruncated(),
                    request.getContentType(),
                    finishedAt
            );
        }
        attemptRepository.saveAndFlush(attempt);

        // 5. Release ExecutionLease
        jdbcTemplate.update(
                "DELETE FROM execution_leases WHERE execution_public_id = ? AND lease_token = ?",
                executionPublicId,
                leaseToken
        );

        // 6. Create EXECUTION_COMPLETED OutboxEvent (payload version 2)
        com.flowforge.event.dto.ExecutionCompletedPayload outboxPayload = new com.flowforge.event.dto.ExecutionCompletedPayload(
                execution.getPublicId(),
                execution.getTenant().getPublicId(),
                execution.getProject().getPublicId(),
                execution.getJob().getPublicId(),
                finalExecutionStatus.name(),
                request.getStatusCode(),
                request.getNetworkErrorCategory(),
                attempt.getDuration(),
                finishedAt
        );

        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(outboxPayload);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize execution completed payload to JSON", e);
        }

        OutboxEvent outboxEvent = new OutboxEvent(
                publicIdGenerator.generate(),
                OutboxAggregateType.EXECUTION,
                execution.getPublicId(),
                "EXECUTION_COMPLETED",
                jsonPayload,
                2, // payloadVersion
                clock.instant()
        );
        outboxEventRepository.saveAndFlush(outboxEvent);

        logger.info("Successfully finalized execution: {}. Status: {}", executionPublicId, finalExecutionStatus);
    }

    @Transactional
    public void scheduleRetry(UUID executionPublicId, com.flowforge.api.dto.InternalExecutionRetryRequest request) {
        // 1. Fetch Execution
        Execution execution = executionRepository.findByPublicId(executionPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Execution not found"));

        if (execution.getCurrentStatus() != ExecutionStatus.FAILED) {
            throw new IllegalStateException("Execution is not in FAILED state. Current state: " + execution.getCurrentStatus());
        }

        // 2. Advance execution state
        execution.scheduleRetry(request.getNextAttemptAt());
        int nextAttemptNumber = execution.getCurrentAttemptNumber() + 1;
        execution.incrementAttempt(nextAttemptNumber);
        executionRepository.saveAndFlush(execution);

        // 3. Create a new ExecutionAttempt in PENDING state
        ExecutionAttempt nextAttempt = new ExecutionAttempt(
                publicIdGenerator.generate(),
                nextAttemptNumber,
                AttemptStatus.PENDING,
                clock.instant()
        );
        execution.addAttempt(nextAttempt);
        attemptRepository.saveAndFlush(nextAttempt);

        // 4. Create EXECUTION_RETRY_SCHEDULED OutboxEvent (payload version 1)
        com.flowforge.event.dto.ExecutionRetryScheduledPayload outboxPayload = new com.flowforge.event.dto.ExecutionRetryScheduledPayload(
                execution.getPublicId(),
                nextAttemptNumber,
                request.getNextAttemptAt(),
                request.getRetryStrategy(),
                request.getDelaySeconds()
        );

        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(outboxPayload);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize execution retry scheduled payload to JSON", e);
        }

        OutboxEvent outboxEvent = new OutboxEvent(
                publicIdGenerator.generate(),
                OutboxAggregateType.EXECUTION,
                execution.getPublicId(),
                "EXECUTION_RETRY_SCHEDULED",
                jsonPayload,
                1, // payloadVersion
                clock.instant()
        );
        outboxEventRepository.saveAndFlush(outboxEvent);

        logger.info("Successfully scheduled retry for execution: {}, attempt: {}, nextAttemptAt: {}", 
                executionPublicId, nextAttemptNumber, request.getNextAttemptAt());
    }
}
