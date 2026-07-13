package com.flowforge.scheduler.model;

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

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "execution", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ExecutionAttempt> attempts = new ArrayList<>();

    protected Execution() {}

    public Execution(UUID publicId, Job job, ExecutionTriggerType triggerType, String triggerSource,
                     Instant queuedAt, int maxAttempts) {
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

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public List<ExecutionAttempt> getAttempts() {
        return attempts;
    }

    public void addAttempt(ExecutionAttempt attempt) {
        this.attempts.add(attempt);
        attempt.setExecution(this);
    }
}
