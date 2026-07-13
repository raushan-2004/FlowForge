package com.flowforge.event.dto;

import java.time.Instant;
import java.util.UUID;

public class ExecutionCompletedPayload {

    private UUID executionPublicId;
    private UUID tenantPublicId;
    private UUID projectPublicId;
    private UUID jobPublicId;
    private String finalStatus;
    private Integer httpStatus;
    private String networkError;
    private Long duration;
    private Instant finishedAt;

    public ExecutionCompletedPayload() {}

    public ExecutionCompletedPayload(UUID executionPublicId, UUID tenantPublicId, UUID projectPublicId, UUID jobPublicId, String finalStatus, Integer httpStatus, String networkError, Long duration, Instant finishedAt) {
        this.executionPublicId = executionPublicId;
        this.tenantPublicId = tenantPublicId;
        this.projectPublicId = projectPublicId;
        this.jobPublicId = jobPublicId;
        this.finalStatus = finalStatus;
        this.httpStatus = httpStatus;
        this.networkError = networkError;
        this.duration = duration;
        this.finishedAt = finishedAt;
    }

    public UUID getExecutionPublicId() {
        return executionPublicId;
    }

    public void setExecutionPublicId(UUID executionPublicId) {
        this.executionPublicId = executionPublicId;
    }

    public UUID getTenantPublicId() {
        return tenantPublicId;
    }

    public void setTenantPublicId(UUID tenantPublicId) {
        this.tenantPublicId = tenantPublicId;
    }

    public UUID getProjectPublicId() {
        return projectPublicId;
    }

    public void setProjectPublicId(UUID projectPublicId) {
        this.projectPublicId = projectPublicId;
    }

    public UUID getJobPublicId() {
        return jobPublicId;
    }

    public void setJobPublicId(UUID jobPublicId) {
        this.jobPublicId = jobPublicId;
    }

    public String getFinalStatus() {
        return finalStatus;
    }

    public void setFinalStatus(String finalStatus) {
        this.finalStatus = finalStatus;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(Integer httpStatus) {
        this.httpStatus = httpStatus;
    }

    public String getNetworkError() {
        return networkError;
    }

    public void setNetworkError(String networkError) {
        this.networkError = networkError;
    }

    public Long getDuration() {
        return duration;
    }

    public void setDuration(Long duration) {
        this.duration = duration;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }
}
