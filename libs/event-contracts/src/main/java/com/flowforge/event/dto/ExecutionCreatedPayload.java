package com.flowforge.event.dto;

import java.time.Instant;
import java.util.UUID;

public class ExecutionCreatedPayload {

    private UUID executionPublicId;
    private UUID jobPublicId;
    private UUID projectPublicId;
    private UUID tenantPublicId;
    private String triggerType;
    private Instant queuedAt;

    public ExecutionCreatedPayload() {}

    public ExecutionCreatedPayload(UUID executionPublicId, UUID jobPublicId, UUID projectPublicId, UUID tenantPublicId, String triggerType, Instant queuedAt) {
        this.executionPublicId = executionPublicId;
        this.jobPublicId = jobPublicId;
        this.projectPublicId = projectPublicId;
        this.tenantPublicId = tenantPublicId;
        this.triggerType = triggerType;
        this.queuedAt = queuedAt;
    }

    public UUID getExecutionPublicId() {
        return executionPublicId;
    }

    public void setExecutionPublicId(UUID executionPublicId) {
        this.executionPublicId = executionPublicId;
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

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public Instant getQueuedAt() {
        return queuedAt;
    }

    public void setQueuedAt(Instant queuedAt) {
        this.queuedAt = queuedAt;
    }
}
