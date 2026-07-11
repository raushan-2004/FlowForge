package com.flowforge.api.dto;

import com.flowforge.api.model.ApiKeyStatus;
import java.time.Instant;
import java.util.UUID;

public class ApiKeyResponse {

    private UUID publicId;
    private String keyId;
    private String displayPrefix;
    private ApiKeyStatus status;
    private UUID createdBy;
    private UUID updatedBy;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant expiryAt;
    private Instant lastUsedAt;

    public ApiKeyResponse() {}

    public ApiKeyResponse(UUID publicId, String keyId, String displayPrefix, ApiKeyStatus status, UUID createdBy, UUID updatedBy, Instant createdAt, Instant updatedAt, Instant expiryAt, Instant lastUsedAt) {
        this.publicId = publicId;
        this.keyId = keyId;
        this.displayPrefix = displayPrefix;
        this.status = status;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.expiryAt = expiryAt;
        this.lastUsedAt = lastUsedAt;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public void setPublicId(UUID publicId) {
        this.publicId = publicId;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public String getDisplayPrefix() {
        return displayPrefix;
    }

    public void setDisplayPrefix(String displayPrefix) {
        this.displayPrefix = displayPrefix;
    }

    public ApiKeyStatus getStatus() {
        return status;
    }

    public void setStatus(ApiKeyStatus status) {
        this.status = status;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getExpiryAt() {
        return expiryAt;
    }

    public void setExpiryAt(Instant expiryAt) {
        this.expiryAt = expiryAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(Instant lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }
}
