package com.flowforge.worker.dto;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ExecutionResult {

    private final UUID executionPublicId;
    private final Instant startedAt;
    private final Instant finishedAt;
    private final Duration duration;
    private final Integer statusCode;
    private final Map<String, List<String>> responseHeaders;
    private final String responseBody;
    private final long responseSize;
    private final boolean bodyTruncated;
    private final String contentType;
    private final String protocolVersion;
    private final int redirectCount;
    private final boolean timeoutFlag;
    private final NetworkErrorCategory networkErrorCategory;

    public ExecutionResult(
            UUID executionPublicId,
            Instant startedAt,
            Instant finishedAt,
            Duration duration,
            Integer statusCode,
            Map<String, List<String>> responseHeaders,
            String responseBody,
            long responseSize,
            boolean bodyTruncated,
            String contentType,
            String protocolVersion,
            int redirectCount,
            boolean timeoutFlag,
            NetworkErrorCategory networkErrorCategory) {
        this.executionPublicId = executionPublicId;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.duration = duration;
        this.statusCode = statusCode;
        this.responseHeaders = responseHeaders != null ? Collections.unmodifiableMap(responseHeaders) : Collections.emptyMap();
        this.responseBody = responseBody;
        this.responseSize = responseSize;
        this.bodyTruncated = bodyTruncated;
        this.contentType = contentType;
        this.protocolVersion = protocolVersion;
        this.redirectCount = redirectCount;
        this.timeoutFlag = timeoutFlag;
        this.networkErrorCategory = networkErrorCategory;
    }

    public UUID getExecutionPublicId() {
        return executionPublicId;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public Duration getDuration() {
        return duration;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public Map<String, List<String>> getResponseHeaders() {
        return responseHeaders;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public long getResponseSize() {
        return responseSize;
    }

    public boolean isBodyTruncated() {
        return bodyTruncated;
    }

    public String getContentType() {
        return contentType;
    }

    public String getProtocolVersion() {
        return protocolVersion;
    }

    public int getRedirectCount() {
        return redirectCount;
    }

    public boolean isTimeoutFlag() {
        return timeoutFlag;
    }

    public NetworkErrorCategory getNetworkErrorCategory() {
        return networkErrorCategory;
    }
}
