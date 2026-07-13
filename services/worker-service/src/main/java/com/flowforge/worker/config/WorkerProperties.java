package com.flowforge.worker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "flowforge.worker")
public class WorkerProperties {

    private String workerName = "flowforge-worker";
    private long heartbeatIntervalMs = 5000;
    private long leaseDurationMs = 15000;
    private long recoveryIntervalMs = 10000;
    private boolean enabled = true;
    private String apiServiceUrl = "http://localhost:8080";
    private String internalToken = "default-internal-token-secret-key-12345";

    public String getWorkerName() {
        return workerName;
    }

    public void setWorkerName(String workerName) {
        this.workerName = workerName;
    }

    public long getHeartbeatIntervalMs() {
        return heartbeatIntervalMs;
    }

    public void setHeartbeatIntervalMs(long heartbeatIntervalMs) {
        this.heartbeatIntervalMs = heartbeatIntervalMs;
    }

    public long getLeaseDurationMs() {
        return leaseDurationMs;
    }

    public void setLeaseDurationMs(long leaseDurationMs) {
        this.leaseDurationMs = leaseDurationMs;
    }

    public long getRecoveryIntervalMs() {
        return recoveryIntervalMs;
    }

    public void setRecoveryIntervalMs(long recoveryIntervalMs) {
        this.recoveryIntervalMs = recoveryIntervalMs;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getApiServiceUrl() {
        return apiServiceUrl;
    }

    public void setApiServiceUrl(String apiServiceUrl) {
        this.apiServiceUrl = apiServiceUrl;
    }

    public String getInternalToken() {
        return internalToken;
    }

    public void setInternalToken(String internalToken) {
        this.internalToken = internalToken;
    }
}
