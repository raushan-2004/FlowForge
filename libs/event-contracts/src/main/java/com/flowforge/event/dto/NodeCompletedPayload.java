package com.flowforge.event.dto;

import java.time.Instant;
import java.util.UUID;

public class NodeCompletedPayload {
    private UUID workflowRunPublicId;
    private String nodeId;
    private String status;
    private Instant finishedAt;

    public NodeCompletedPayload() {}

    public NodeCompletedPayload(UUID workflowRunPublicId, String nodeId, String status, Instant finishedAt) {
        this.workflowRunPublicId = workflowRunPublicId;
        this.nodeId = nodeId;
        this.status = status;
        this.finishedAt = finishedAt;
    }

    public UUID getWorkflowRunPublicId() {
        return workflowRunPublicId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getStatus() {
        return status;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }
}
