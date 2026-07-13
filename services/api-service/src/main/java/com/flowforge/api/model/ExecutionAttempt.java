package com.flowforge.api.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "execution_attempts")
public class ExecutionAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", unique = true, nullable = false, updatable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "execution_id", nullable = false)
    private Execution execution;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AttemptStatus status;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "worker_id")
    private String workerId;

    @Column(name = "duration")
    private Long duration; // in milliseconds

    @Column(name = "error_category")
    private String errorCategory;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "response_size")
    private Long responseSize;

    @Column(name = "body_truncated")
    private Boolean bodyTruncated;

    @Column(name = "network_error")
    private String networkError;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ExecutionAttempt() {}

    public ExecutionAttempt(UUID publicId, int attemptNumber, AttemptStatus status, Instant createdAt) {
        if (publicId == null) throw new IllegalArgumentException("publicId cannot be null");
        if (attemptNumber < 1) throw new IllegalArgumentException("attemptNumber must be at least 1");
        if (status == null) throw new IllegalArgumentException("status cannot be null");
        if (createdAt == null) throw new IllegalArgumentException("createdAt cannot be null");

        this.publicId = publicId;
        this.attemptNumber = attemptNumber;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public Execution getExecution() {
        return execution;
    }

    void setExecution(Execution execution) {
        this.execution = execution;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public AttemptStatus getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public String getWorkerId() {
        return workerId;
    }

    public Long getDuration() {
        return duration;
    }

    public String getErrorCategory() {
        return errorCategory;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public Long getResponseSize() {
        return responseSize;
    }

    public Boolean getBodyTruncated() {
        return bodyTruncated;
    }

    public String getNetworkError() {
        return networkError;
    }

    public String getContentType() {
        return contentType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void start(String workerId, Instant startedAt) {
        this.status = AttemptStatus.RUNNING;
        this.workerId = workerId;
        this.startedAt = startedAt;
    }

    public void succeed(Instant finishedAt) {
        succeed(finishedAt, null, null, null, null);
    }

    public void succeed(Instant finishedAt, Integer httpStatus, Long responseSize, Boolean bodyTruncated, String contentType) {
        this.status = AttemptStatus.SUCCEEDED;
        this.finishedAt = finishedAt;
        this.httpStatus = httpStatus;
        this.responseSize = responseSize;
        this.bodyTruncated = bodyTruncated;
        this.contentType = contentType;
        if (this.startedAt != null) {
            this.duration = java.time.Duration.between(this.startedAt, finishedAt).toMillis();
        }
    }

    public void fail(String errorCategory, Instant finishedAt) {
        fail(errorCategory, null, null, null, null, null, finishedAt);
    }

    public void fail(String errorCategory, String networkError, Integer httpStatus, Long responseSize, Boolean bodyTruncated, String contentType, Instant finishedAt) {
        this.status = AttemptStatus.FAILED;
        this.errorCategory = errorCategory;
        this.networkError = networkError;
        this.httpStatus = httpStatus;
        this.responseSize = responseSize;
        this.bodyTruncated = bodyTruncated;
        this.contentType = contentType;
        this.finishedAt = finishedAt;
        if (this.startedAt != null) {
            this.duration = java.time.Duration.between(this.startedAt, finishedAt).toMillis();
        }
    }

    public void cancel(Instant finishedAt) {
        this.status = AttemptStatus.CANCELLED;
        this.finishedAt = finishedAt;
        if (this.startedAt != null) {
            this.duration = java.time.Duration.between(this.startedAt, finishedAt).toMillis();
        }
    }
}
