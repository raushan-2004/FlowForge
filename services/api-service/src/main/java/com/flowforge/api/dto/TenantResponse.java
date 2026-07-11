package com.flowforge.api.dto;

import com.flowforge.api.model.TenantStatus;
import java.time.Instant;
import java.util.UUID;

public class TenantResponse {

    private UUID publicId;
    private String name;
    private TenantStatus status;
    private UUID createdBy;
    private UUID updatedBy;
    private Instant createdAt;
    private Instant updatedAt;

    public TenantResponse() {}

    public TenantResponse(UUID publicId, String name, TenantStatus status, UUID createdBy, UUID updatedBy, Instant createdAt, Instant updatedAt) {
        this.publicId = publicId;
        this.name = name;
        this.status = status;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public void setPublicId(UUID publicId) {
        this.publicId = publicId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TenantStatus getStatus() {
        return status;
    }

    public void setStatus(TenantStatus status) {
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
}
