package com.flowforge.api.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workflow_runs")
public class WorkflowRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", unique = true, nullable = false, updatable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_definition_id", nullable = false)
    private WorkflowDefinition workflowDefinition;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WorkflowRunStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected WorkflowRun() {}

    public WorkflowRun(UUID publicId, WorkflowDefinition workflowDefinition, Instant startedAt) {
        this.publicId = publicId;
        this.workflowDefinition = workflowDefinition;
        this.status = WorkflowRunStatus.RUNNING;
        this.startedAt = startedAt;
    }

    public Long getId() {
        return id;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public WorkflowDefinition getWorkflowDefinition() {
        return workflowDefinition;
    }

    public WorkflowRunStatus getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void succeed(Instant finishedAt) {
        if (this.status != WorkflowRunStatus.RUNNING) {
            throw new IllegalStateException("Cannot transition workflow run to SUCCEEDED from " + this.status);
        }
        this.status = WorkflowRunStatus.SUCCEEDED;
        this.finishedAt = finishedAt;
    }

    public void fail(Instant finishedAt) {
        if (this.status != WorkflowRunStatus.RUNNING) {
            throw new IllegalStateException("Cannot transition workflow run to FAILED from " + this.status);
        }
        this.status = WorkflowRunStatus.FAILED;
        this.finishedAt = finishedAt;
    }
}
