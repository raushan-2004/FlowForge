package com.flowforge.api.dto;

import java.time.Instant;
import java.util.UUID;

public class WorkflowRunResponse {
    private UUID publicId;
    private UUID workflowDefinitionPublicId;
    private String status;
    private Instant startedAt;
    private Instant finishedAt;

    public WorkflowRunResponse() {}

    public WorkflowRunResponse(UUID publicId, UUID workflowDefinitionPublicId, String status, Instant startedAt, Instant finishedAt) {
        this.publicId = publicId;
        this.workflowDefinitionPublicId = workflowDefinitionPublicId;
        this.status = status;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public UUID getWorkflowDefinitionPublicId() {
        return workflowDefinitionPublicId;
    }

    public String getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }
}
