package com.flowforge.api.dto;

import com.flowforge.api.model.AttemptStatus;

import java.time.Instant;
import java.util.UUID;

public class ExecutionAttemptResponse {

    private UUID publicId;
    private int attemptNumber;
    private AttemptStatus status;
    private Instant startedAt;
    private Instant finishedAt;
    private String workerId;
    private Long duration;
    private String errorCategory;
    private Integer httpStatus;
    private Long responseSize;
    private Boolean bodyTruncated;
    private String networkError;
    private String contentType;
    private Instant createdAt;

    public ExecutionAttemptResponse() {}

    public ExecutionAttemptResponse(UUID publicId, int attemptNumber, AttemptStatus status, Instant startedAt, Instant finishedAt, String workerId, Long duration, String errorCategory, Integer httpStatus, Long responseSize, Boolean bodyTruncated, String networkError, String contentType, Instant createdAt) {
        this.publicId = publicId;
        this.attemptNumber = attemptNumber;
        this.status = status;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.workerId = workerId;
        this.duration = duration;
        this.errorCategory = errorCategory;
        this.httpStatus = httpStatus;
        this.responseSize = responseSize;
        this.bodyTruncated = bodyTruncated;
        this.networkError = networkError;
        this.contentType = contentType;
        this.createdAt = createdAt;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public void setPublicId(UUID publicId) {
        this.publicId = publicId;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(int attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public AttemptStatus getStatus() {
        return status;
    }

    public void setStatus(AttemptStatus status) {
        this.status = status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public String getErrorCategory() {
        return errorCategory;
    }

    public void setErrorCategory(String errorCategory) {
        this.errorCategory = errorCategory;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(Integer httpStatus) {
        this.httpStatus = httpStatus;
    }

    public Long getResponseSize() {
        return responseSize;
    }

    public void setResponseSize(Long responseSize) {
        this.responseSize = responseSize;
    }

    public Boolean getBodyTruncated() {
        return bodyTruncated;
    }

    public void setBodyTruncated(Boolean bodyTruncated) {
        this.bodyTruncated = bodyTruncated;
    }

    public String getNetworkError() {
        return networkError;
    }

    public void setNetworkError(String networkError) {
        this.networkError = networkError;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
