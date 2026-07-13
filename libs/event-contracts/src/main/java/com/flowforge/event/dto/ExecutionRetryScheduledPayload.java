package com.flowforge.event.dto;

import java.time.Instant;
import java.util.UUID;

public class ExecutionRetryScheduledPayload {

    private UUID executionPublicId;
    private int attemptNumber;
    private Instant nextAttemptAt;
    private String retryStrategy;
    private long delaySeconds;

    public ExecutionRetryScheduledPayload() {}

    public ExecutionRetryScheduledPayload(UUID executionPublicId, int attemptNumber, Instant nextAttemptAt, String retryStrategy, long delaySeconds) {
        this.executionPublicId = executionPublicId;
        this.attemptNumber = attemptNumber;
        this.nextAttemptAt = nextAttemptAt;
        this.retryStrategy = retryStrategy;
        this.delaySeconds = delaySeconds;
    }

    public UUID getExecutionPublicId() {
        return executionPublicId;
    }

    public void setExecutionPublicId(UUID executionPublicId) {
        this.executionPublicId = executionPublicId;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public void setAttemptNumber(int attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public void setNextAttemptAt(Instant nextAttemptAt) {
        this.nextAttemptAt = nextAttemptAt;
    }

    public String getRetryStrategy() {
        return retryStrategy;
    }

    public void setRetryStrategy(String retryStrategy) {
        this.retryStrategy = retryStrategy;
    }

    public long getDelaySeconds() {
        return delaySeconds;
    }

    public void setDelaySeconds(long delaySeconds) {
        this.delaySeconds = delaySeconds;
    }
}
