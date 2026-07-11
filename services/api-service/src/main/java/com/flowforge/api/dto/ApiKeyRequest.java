package com.flowforge.api.dto;

public class ApiKeyRequest {

    private Long expirySeconds;

    public ApiKeyRequest() {}

    public ApiKeyRequest(Long expirySeconds) {
        this.expirySeconds = expirySeconds;
    }

    public Long getExpirySeconds() {
        return expirySeconds;
    }

    public void setExpirySeconds(Long expirySeconds) {
        this.expirySeconds = expirySeconds;
    }
}
