package com.flowforge.api.dto;

import com.flowforge.api.model.ExecutionStatus;
import com.flowforge.api.model.ExecutionTriggerType;

import java.time.Instant;
import java.util.UUID;

public class ExecutionResponse {

    private UUID publicId;
    private UUID jobPublicId;
    private UUID projectPublicId;
    private UUID tenantPublicId;
    private ExecutionTriggerType triggerType;
    private String triggerSource;
    private ExecutionStatus currentStatus;
    private Instant queuedAt;
    private Instant startedAt;
    private Instant finishedAt;
    private int currentAttemptNumber;
    private int maxAttempts;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;

    public ExecutionResponse() {}

    public ExecutionResponse(UUID publicId, UUID jobPublicId, UUID projectPublicId, UUID tenantPublicId, ExecutionTriggerType triggerType, String triggerSource, ExecutionStatus currentStatus, Instant queuedAt, Instant startedAt, Instant finishedAt, int currentAttemptNumber, int maxAttempts, Instant createdAt, Instant updatedAt, Long version) {
        this.publicId = publicId;
        this.jobPublicId = jobPublicId;
        this.projectPublicId = projectPublicId;
        this.tenantPublicId = tenantPublicId;
        this.triggerType = triggerType;
        this.triggerSource = triggerSource;
        this.currentStatus = currentStatus;
        this.queuedAt = queuedAt;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.currentAttemptNumber = currentAttemptNumber;
        this.maxAttempts = maxAttempts;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public void setPublicId(UUID publicId) {
        this.publicId = publicId;
    }

    public UUID getJobPublicId() {
        return jobPublicId;
    }

    public void setJobPublicId(UUID jobPublicId) {
        this.jobPublicId = jobPublicId;
    }

    public UUID getProjectPublicId() {
        return projectPublicId;
    }

    public void setProjectPublicId(UUID projectPublicId) {
        this.projectPublicId = projectPublicId;
    }

    public UUID getTenantPublicId() {
        return tenantPublicId;
    }

    public void setTenantPublicId(UUID tenantPublicId) {
        this.tenantPublicId = tenantPublicId;
    }

    public ExecutionTriggerType getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(ExecutionTriggerType triggerType) {
        this.triggerType = triggerType;
    }

    public String getTriggerSource() {
        return triggerSource;
    }

    public void setTriggerSource(String triggerSource) {
        this.triggerSource = triggerSource;
    }

    public ExecutionStatus getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(ExecutionStatus currentStatus) {
        this.currentStatus = currentStatus;
    }

    public Instant getQueuedAt() {
        return queuedAt;
    }

    public void setQueuedAt(Instant queuedAt) {
        this.queuedAt = queuedAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public int getCurrentAttemptNumber() {
        return currentAttemptNumber;
    }

    public void setCurrentAttemptNumber(int currentAttemptNumber) {
        this.currentAttemptNumber = currentAttemptNumber;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
