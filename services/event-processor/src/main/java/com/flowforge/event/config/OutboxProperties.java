package com.flowforge.event.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "flowforge.outbox")
public class OutboxProperties {

    private int pollIntervalMs = 1000;
    private int batchSize = 10;
    private int maxRetries = 5;
    private int backoffBaseSeconds = 2;
    private String topicExecutionCreated = "execution-created";
    private boolean enabled = true;

    public int getPollIntervalMs() {
        return pollIntervalMs;
    }

    public void setPollIntervalMs(int pollIntervalMs) {
        this.pollIntervalMs = pollIntervalMs;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public int getBackoffBaseSeconds() {
        return backoffBaseSeconds;
    }

    public void setBackoffBaseSeconds(int backoffBaseSeconds) {
        this.backoffBaseSeconds = backoffBaseSeconds;
    }

    public String getTopicExecutionCreated() {
        return topicExecutionCreated;
    }

    public void setTopicExecutionCreated(String topicExecutionCreated) {
        this.topicExecutionCreated = topicExecutionCreated;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
