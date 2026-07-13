package com.flowforge.scheduler.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "flowforge.scheduler")
public class SchedulerProperties {

    private int pollIntervalMs = 1000;
    private int batchSize = 10;
    private boolean enabled = true;
    private int maxMissedSchedulesLimit = 5;

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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxMissedSchedulesLimit() {
        return maxMissedSchedulesLimit;
    }

    public void setMaxMissedSchedulesLimit(int maxMissedSchedulesLimit) {
        this.maxMissedSchedulesLimit = maxMissedSchedulesLimit;
    }
}
