package com.flowforge.event.dto;

import java.time.Instant;
import java.util.UUID;

public class WorkflowStartedPayload {
    private UUID workflowRunPublicId;
    private UUID workflowDefinitionPublicId;
    private Instant startedAt;

    public WorkflowStartedPayload() {}

    public WorkflowStartedPayload(UUID workflowRunPublicId, UUID workflowDefinitionPublicId, Instant startedAt) {
        this.workflowRunPublicId = workflowRunPublicId;
        this.workflowDefinitionPublicId = workflowDefinitionPublicId;
        this.startedAt = startedAt;
    }

    public UUID getWorkflowRunPublicId() {
        return workflowRunPublicId;
    }

    public UUID getWorkflowDefinitionPublicId() {
        return workflowDefinitionPublicId;
    }

    public Instant getStartedAt() {
        return startedAt;
    }
}
