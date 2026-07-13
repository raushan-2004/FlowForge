package com.flowforge.api.dto;

import java.util.UUID;

public class InternalWorkflowNodeCompleteRequest {
    private UUID executionPublicId;
    private String finalStatus;
    private Integer httpStatus;
    private String networkError;

    public InternalWorkflowNodeCompleteRequest() {}

    public InternalWorkflowNodeCompleteRequest(UUID executionPublicId, String finalStatus, Integer httpStatus, String networkError) {
        this.executionPublicId = executionPublicId;
        this.finalStatus = finalStatus;
        this.httpStatus = httpStatus;
        this.networkError = networkError;
    }

    public UUID getExecutionPublicId() {
        return executionPublicId;
    }

    public void setExecutionPublicId(UUID executionPublicId) {
        this.executionPublicId = executionPublicId;
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
}
