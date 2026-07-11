package com.flowforge.api.dto;

import com.flowforge.api.model.JobHttpMethod;
import com.flowforge.api.model.JobScheduleType;
import com.flowforge.api.model.JobStatus;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class JobResponse {

    private UUID publicId;
    private UUID projectPublicId;
    private String name;
    private String description;
    private boolean enabled;
    private JobHttpMethod httpMethod;
    private String targetUrl;
    private Map<String, String> requestHeaders;
    private String requestBody;
    private int timeoutSeconds;
    private Integer retryMaxAttempts;
    private String retryStrategy;
    private Integer retryBaseDelaySeconds;
    private JobScheduleType scheduleType;
    private String cronExpression;
    private JobStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
    private Long version;

    public JobResponse() {}

    public JobResponse(UUID publicId, UUID projectPublicId, String name, String description, boolean enabled, JobHttpMethod httpMethod, String targetUrl, Map<String, String> requestHeaders, String requestBody, int timeoutSeconds, Integer retryMaxAttempts, String retryStrategy, Integer retryBaseDelaySeconds, JobScheduleType scheduleType, String cronExpression, JobStatus status, Instant createdAt, Instant updatedAt, UUID createdBy, UUID updatedBy, Long version) {
        this.publicId = publicId;
        this.projectPublicId = projectPublicId;
        this.name = name;
        this.description = description;
        this.enabled = enabled;
        this.httpMethod = httpMethod;
        this.targetUrl = targetUrl;
        this.requestHeaders = requestHeaders;
        this.requestBody = requestBody;
        this.timeoutSeconds = timeoutSeconds;
        this.retryMaxAttempts = retryMaxAttempts;
        this.retryStrategy = retryStrategy;
        this.retryBaseDelaySeconds = retryBaseDelaySeconds;
        this.scheduleType = scheduleType;
        this.cronExpression = cronExpression;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.version = version;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public void setPublicId(UUID publicId) {
        this.publicId = publicId;
    }

    public UUID getProjectPublicId() {
        return projectPublicId;
    }

    public void setProjectPublicId(UUID projectPublicId) {
        this.projectPublicId = projectPublicId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public JobHttpMethod getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(JobHttpMethod httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public Map<String, String> getRequestHeaders() {
        return requestHeaders;
    }

    public void setRequestHeaders(Map<String, String> requestHeaders) {
        this.requestHeaders = requestHeaders;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public Integer getRetryMaxAttempts() {
        return retryMaxAttempts;
    }

    public void setRetryMaxAttempts(Integer retryMaxAttempts) {
        this.retryMaxAttempts = retryMaxAttempts;
    }

    public String getRetryStrategy() {
        return retryStrategy;
    }

    public void setRetryStrategy(String retryStrategy) {
        this.retryStrategy = retryStrategy;
    }

    public Integer getRetryBaseDelaySeconds() {
        return retryBaseDelaySeconds;
    }

    public void setRetryBaseDelaySeconds(Integer retryBaseDelaySeconds) {
        this.retryBaseDelaySeconds = retryBaseDelaySeconds;
    }

    public JobScheduleType getScheduleType() {
        return scheduleType;
    }

    public void setScheduleType(JobScheduleType scheduleType) {
        this.scheduleType = scheduleType;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
