package com.flowforge.worker.dto;

public enum NetworkErrorCategory {
    DNS_FAILURE,
    CONNECTION_REFUSED,
    CONNECTION_TIMEOUT,
    REQUEST_TIMEOUT,
    TLS_FAILURE,
    INVALID_URL,
    INVALID_RESPONSE,
    SSRF_BLOCKED,
    INTERRUPTED,
    UNKNOWN_NETWORK_ERROR
}
