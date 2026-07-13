package com.flowforge.api.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "node_executions")
public class NodeExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_run_id", nullable = false)
    private WorkflowRun workflowRun;

    @Column(name = "node_id", nullable = false)
    private String nodeId;

    @Column(name = "execution_public_id", unique = true)
    private UUID executionPublicId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private NodeExecutionStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    protected NodeExecution() {}

    public NodeExecution(WorkflowRun workflowRun, String nodeId, NodeExecutionStatus status, Instant startedAt) {
        this.workflowRun = workflowRun;
        this.nodeId = nodeId;
        this.status = status;
        this.startedAt = startedAt;
    }

    public Long getId() {
        return id;
    }

    public WorkflowRun getWorkflowRun() {
        return workflowRun;
    }

    public String getNodeId() {
        return nodeId;
    }

    public UUID getExecutionPublicId() {
        return executionPublicId;
    }

    public void setExecutionPublicId(UUID executionPublicId) {
        this.executionPublicId = executionPublicId;
    }

    public NodeExecutionStatus getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void start(Instant startedAt) {
        this.status = NodeExecutionStatus.RUNNING;
        this.startedAt = startedAt;
    }

    public void succeed(Instant finishedAt) {
        this.status = NodeExecutionStatus.SUCCEEDED;
        this.finishedAt = finishedAt;
    }

    public void fail(Instant finishedAt) {
        this.status = NodeExecutionStatus.FAILED;
        this.finishedAt = finishedAt;
    }
}
