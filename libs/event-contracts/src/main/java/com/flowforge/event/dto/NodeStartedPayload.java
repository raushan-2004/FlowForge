package com.flowforge.event.dto;

import java.time.Instant;
import java.util.UUID;

public class NodeStartedPayload {
    private UUID workflowRunPublicId;
    private String nodeId;
    private Instant startedAt;

    public NodeStartedPayload() {}

    public NodeStartedPayload(UUID workflowRunPublicId, String nodeId, Instant startedAt) {
        this.workflowRunPublicId = workflowRunPublicId;
        this.nodeId = nodeId;
        this.startedAt = startedAt;
    }

    public UUID getWorkflowRunPublicId() {
        return workflowRunPublicId;
    }

    public String getNodeId() {
        return nodeId;
    }

    public Instant getStartedAt() {
        return startedAt;
    }
}
