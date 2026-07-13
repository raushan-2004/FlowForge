package com.flowforge.worker.dto;

import java.util.Collections;
import java.util.Map;

public class JobDefinitionDto {

    private final String httpMethod;
    private final String targetUrl;
    private final Map<String, String> requestHeaders;
    private final String requestBody;
    private final int timeoutSeconds;

    public JobDefinitionDto(String httpMethod, String targetUrl, Map<String, String> requestHeaders, String requestBody, int timeoutSeconds) {
        this.httpMethod = httpMethod;
        this.targetUrl = targetUrl;
        this.requestHeaders = requestHeaders != null ? Collections.unmodifiableMap(requestHeaders) : Collections.emptyMap();
        this.requestBody = requestBody;
        this.timeoutSeconds = timeoutSeconds;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public Map<String, String> getRequestHeaders() {
        return requestHeaders;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }
}
