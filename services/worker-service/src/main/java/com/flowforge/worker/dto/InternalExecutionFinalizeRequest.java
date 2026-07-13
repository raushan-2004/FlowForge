package com.flowforge.worker.dto;

import java.time.Instant;

public class InternalExecutionFinalizeRequest {

    private String leaseToken;
    private Instant startedAt;
    private Instant finishedAt;
    private Integer statusCode;
    private Long responseSize;
    private Boolean bodyTruncated;
    private String contentType;
    private String networkErrorCategory;

    public InternalExecutionFinalizeRequest() {}

    public InternalExecutionFinalizeRequest(String leaseToken, Instant startedAt, Instant finishedAt, Integer statusCode, Long responseSize, Boolean bodyTruncated, String contentType, String networkErrorCategory) {
        this.leaseToken = leaseToken;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.statusCode = statusCode;
        this.responseSize = responseSize;
        this.bodyTruncated = bodyTruncated;
        this.contentType = contentType;
        this.networkErrorCategory = networkErrorCategory;
    }

    public String getLeaseToken() {
        return leaseToken;
    }

    public void setLeaseToken(String leaseToken) {
        this.leaseToken = leaseToken;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public Long getResponseSize() {
        return responseSize;
    }

    public void setResponseSize(Long responseSize) {
        this.responseSize = responseSize;
    }

    public Boolean getBodyTruncated() {
        return bodyTruncated;
    }

    public void setBodyTruncated(Boolean bodyTruncated) {
        this.bodyTruncated = bodyTruncated;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getNetworkErrorCategory() {
        return networkErrorCategory;
    }

    public void setNetworkErrorCategory(String networkErrorCategory) {
        this.networkErrorCategory = networkErrorCategory;
    }
}
