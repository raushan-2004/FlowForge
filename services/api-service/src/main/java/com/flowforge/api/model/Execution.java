package com.flowforge.api.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "executions")
public class Execution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", unique = true, nullable = false, updatable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false)
    private ExecutionTriggerType triggerType;

    @Column(name = "trigger_source")
    private String triggerSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_status", nullable = false)
    private ExecutionStatus currentStatus;

    @Column(name = "queued_at", nullable = false)
    private Instant queuedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "current_attempt_number", nullable = false)
    private int currentAttemptNumber;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "execution", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ExecutionAttempt> attempts = new ArrayList<>();

    protected Execution() {}

    public Execution(UUID publicId, Job job, ExecutionTriggerType triggerType, String triggerSource,
                     Instant queuedAt, int maxAttempts) {
        if (publicId == null) throw new IllegalArgumentException("publicId cannot be null");
        if (job == null) throw new IllegalArgumentException("job cannot be null");
        if (triggerType == null) throw new IllegalArgumentException("triggerType cannot be null");
        if (queuedAt == null) throw new IllegalArgumentException("queuedAt cannot be null");
        if (maxAttempts < 1) throw new IllegalArgumentException("maxAttempts must be at least 1");

        this.publicId = publicId;
        this.job = job;
        this.project = job.getProject();
        this.tenant = job.getProject().getTenant();
        this.triggerType = triggerType;
        this.triggerSource = triggerSource;
        this.currentStatus = ExecutionStatus.QUEUED;
        this.queuedAt = queuedAt;
        this.currentAttemptNumber = 1;
        this.maxAttempts = maxAttempts;
        this.createdAt = queuedAt;
        this.updatedAt = queuedAt;
    }

    public Long getId() {
        return id;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public Job getJob() {
        return job;
    }

    public Project getProject() {
        return project;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public ExecutionTriggerType getTriggerType() {
        return triggerType;
    }

    public String getTriggerSource() {
        return triggerSource;
    }

    public ExecutionStatus getCurrentStatus() {
        return currentStatus;
    }

    public Instant getQueuedAt() {
        return queuedAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public int getCurrentAttemptNumber() {
        return currentAttemptNumber;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public void setNextAttemptAt(Instant nextAttemptAt) {
        this.nextAttemptAt = nextAttemptAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<ExecutionAttempt> getAttempts() {
        return attempts;
    }

    public void addAttempt(ExecutionAttempt attempt) {
        this.attempts.add(attempt);
        attempt.setExecution(this);
    }

    // --- State Machine Transitions ---

    public void start(Instant startedAt) {
        if (this.currentStatus != ExecutionStatus.QUEUED) {
            throw new IllegalStateException("Cannot transition from " + this.currentStatus + " to RUNNING");
        }
        this.currentStatus = ExecutionStatus.RUNNING;
        this.startedAt = startedAt;
        this.updatedAt = startedAt;
        this.nextAttemptAt = null;
    }

    public void succeed(Instant finishedAt) {
        if (this.currentStatus != ExecutionStatus.RUNNING) {
            throw new IllegalStateException("Cannot transition from " + this.currentStatus + " to SUCCEEDED");
        }
        this.currentStatus = ExecutionStatus.SUCCEEDED;
        this.finishedAt = finishedAt;
        this.updatedAt = finishedAt;
    }

    public void fail(Instant finishedAt) {
        if (this.currentStatus != ExecutionStatus.RUNNING) {
            throw new IllegalStateException("Cannot transition from " + this.currentStatus + " to FAILED");
        }
        this.currentStatus = ExecutionStatus.FAILED;
        this.finishedAt = finishedAt;
        this.updatedAt = finishedAt;
    }

    public void cancel(Instant finishedAt) {
        if (this.currentStatus != ExecutionStatus.QUEUED && this.currentStatus != ExecutionStatus.RUNNING) {
            throw new IllegalStateException("Cannot transition from " + this.currentStatus + " to CANCELLED");
        }
        this.currentStatus = ExecutionStatus.CANCELLED;
        this.finishedAt = finishedAt;
        this.updatedAt = finishedAt;
    }

    public void incrementAttempt(int nextAttemptNumber) {
        if (nextAttemptNumber <= this.currentAttemptNumber) {
            throw new IllegalArgumentException("Next attempt number must be strictly greater than current");
        }
        this.currentAttemptNumber = nextAttemptNumber;
        this.updatedAt = Instant.now();
    }

    public void scheduleRetry(Instant nextAttemptAt) {
        if (this.currentStatus != ExecutionStatus.FAILED) {
            throw new IllegalStateException("Cannot transition from " + this.currentStatus + " to QUEUED for retry");
        }
        this.currentStatus = ExecutionStatus.QUEUED;
        this.finishedAt = null;
        this.nextAttemptAt = nextAttemptAt;
        this.updatedAt = Instant.now();
    }
}
