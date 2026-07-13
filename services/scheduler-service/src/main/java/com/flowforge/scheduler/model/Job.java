package com.flowforge.scheduler.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "jobs")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", unique = true, nullable = false, updatable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type", nullable = false)
    private JobScheduleType scheduleType;

    @Column(name = "cron_expression")
    private String cronExpression;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private JobStatus status;

    @Column(name = "next_fire_at")
    private Instant nextFireAt;

    @Column(name = "last_scheduled_at")
    private Instant lastScheduledAt;

    @Column(name = "schedule_version", nullable = false)
    private Long scheduleVersion = 0L;

    @Column(name = "retry_max_attempts")
    private Integer retryMaxAttempts;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected Job() {}

    public Job(UUID publicId, Project project, String name, boolean enabled,
               JobScheduleType scheduleType, String cronExpression, JobStatus status,
               Instant nextFireAt, Integer retryMaxAttempts) {
        this.publicId = publicId;
        this.project = project;
        this.name = name;
        this.enabled = enabled;
        this.scheduleType = scheduleType;
        this.cronExpression = cronExpression;
        this.status = status;
        this.nextFireAt = nextFireAt;
        this.retryMaxAttempts = retryMaxAttempts;
        this.scheduleVersion = 0L;
    }

    public Long getId() {
        return id;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public Project getProject() {
        return project;
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public JobScheduleType getScheduleType() {
        return scheduleType;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public JobStatus getStatus() {
        return status;
    }

    public Instant getNextFireAt() {
        return nextFireAt;
    }

    public void setNextFireAt(Instant nextFireAt) {
        this.nextFireAt = nextFireAt;
    }

    public Instant getLastScheduledAt() {
        return lastScheduledAt;
    }

    public void setLastScheduledAt(Instant lastScheduledAt) {
        this.lastScheduledAt = lastScheduledAt;
    }

    public Long getScheduleVersion() {
        return scheduleVersion;
    }

    public void setScheduleVersion(Long scheduleVersion) {
        this.scheduleVersion = scheduleVersion;
    }

    public Integer getRetryMaxAttempts() {
        return retryMaxAttempts;
    }

    public Long getVersion() {
        return version;
    }
}
