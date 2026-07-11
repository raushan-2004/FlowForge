package com.flowforge.api.dto;

import java.util.UUID;

public class ExecutionRequest {

    private UUID jobPublicId;
    private String triggerType;
    private String triggerSource;

    public ExecutionRequest() {}

    public ExecutionRequest(UUID jobPublicId, String triggerType, String triggerSource) {
        this.jobPublicId = jobPublicId;
        this.triggerType = triggerType;
        this.triggerSource = triggerSource;
    }

    public UUID getJobPublicId() {
        return jobPublicId;
    }

    public void setJobPublicId(UUID jobPublicId) {
        this.jobPublicId = jobPublicId;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public String getTriggerSource() {
        return triggerSource;
    }

    public void setTriggerSource(String triggerSource) {
        this.triggerSource = triggerSource;
    }
}
