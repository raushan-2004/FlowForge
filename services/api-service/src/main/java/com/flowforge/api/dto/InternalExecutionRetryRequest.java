package com.flowforge.api.dto;

import java.time.Instant;

public class InternalExecutionRetryRequest {

    private Instant nextAttemptAt;
    private long delaySeconds;
    private String retryStrategy;

    public InternalExecutionRetryRequest() {}

    public InternalExecutionRetryRequest(Instant nextAttemptAt, long delaySeconds, String retryStrategy) {
        this.nextAttemptAt = nextAttemptAt;
        this.delaySeconds = delaySeconds;
        this.retryStrategy = retryStrategy;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public void setNextAttemptAt(Instant nextAttemptAt) {
        this.nextAttemptAt = nextAttemptAt;
    }

    public long getDelaySeconds() {
        return delaySeconds;
    }

    public void setDelaySeconds(long delaySeconds) {
        this.delaySeconds = delaySeconds;
    }

    public String getRetryStrategy() {
        return retryStrategy;
    }

    public void setRetryStrategy(String retryStrategy) {
        this.retryStrategy = retryStrategy;
    }
}
