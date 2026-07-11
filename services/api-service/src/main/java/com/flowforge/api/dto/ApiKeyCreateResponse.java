package com.flowforge.api.dto;

public class ApiKeyCreateResponse {

    private ApiKeyResponse metadata;
    private String token;

    public ApiKeyCreateResponse() {}

    public ApiKeyCreateResponse(ApiKeyResponse metadata, String token) {
        this.metadata = metadata;
        this.token = token;
    }

    public ApiKeyResponse getMetadata() {
        return metadata;
    }

    public void setMetadata(ApiKeyResponse metadata) {
        this.metadata = metadata;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
