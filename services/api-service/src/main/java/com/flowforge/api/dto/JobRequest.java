package com.flowforge.api.dto;

import java.util.Map;
import java.util.UUID;

public class JobRequest {

    private UUID projectPublicId;
    private String name;
    private String description;
    private Boolean enabled;
    private String httpMethod;
    private String targetUrl;
    private Map<String, String> requestHeaders;
    private String requestBody;
    private Integer timeoutSeconds;
    private Integer retryMaxAttempts;
    private String retryStrategy;
    private Integer retryBaseDelaySeconds;
    private String scheduleType;
    private String cronExpression;

    public JobRequest() {}

    public JobRequest(UUID projectPublicId, String name, String description, Boolean enabled, String httpMethod, String targetUrl, Map<String, String> requestHeaders, String requestBody, Integer timeoutSeconds, Integer retryMaxAttempts, String retryStrategy, Integer retryBaseDelaySeconds, String scheduleType, String cronExpression) {
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

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
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

    public Integer getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
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

    public String getScheduleType() {
        return scheduleType;
    }

    public void setScheduleType(String scheduleType) {
        this.scheduleType = scheduleType;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public void setCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }
}
