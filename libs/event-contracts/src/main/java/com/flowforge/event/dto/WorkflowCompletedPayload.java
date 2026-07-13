package com.flowforge.event.dto;

import java.time.Instant;
import java.util.UUID;

public class WorkflowCompletedPayload {
    private UUID workflowRunPublicId;
    private String status;
    private Instant finishedAt;

    public WorkflowCompletedPayload() {}

    public WorkflowCompletedPayload(UUID workflowRunPublicId, String status, Instant finishedAt) {
        this.workflowRunPublicId = workflowRunPublicId;
        this.status = status;
        this.finishedAt = finishedAt;
    }

    public UUID getWorkflowRunPublicId() {
        return workflowRunPublicId;
    }

    public String getStatus() {
        return status;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }
}
