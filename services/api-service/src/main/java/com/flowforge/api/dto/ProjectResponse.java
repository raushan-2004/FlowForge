package com.flowforge.api.dto;

import com.flowforge.api.model.ProjectStatus;
import java.time.Instant;
import java.util.UUID;

public class ProjectResponse {

    private UUID publicId;
    private String name;
    private ProjectStatus status;
    private UUID tenantPublicId;
    private UUID createdBy;
    private UUID updatedBy;
    private Instant createdAt;
    private Instant updatedAt;

    public ProjectResponse() {}

    public ProjectResponse(UUID publicId, String name, ProjectStatus status, UUID tenantPublicId, UUID createdBy, UUID updatedBy, Instant createdAt, Instant updatedAt) {
        this.publicId = publicId;
        this.name = name;
        this.status = status;
        this.tenantPublicId = tenantPublicId;
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

    public ProjectStatus getStatus() {
        return status;
    }

    public void setStatus(ProjectStatus status) {
        this.status = status;
    }

    public UUID getTenantPublicId() {
        return tenantPublicId;
    }

    public void setTenantPublicId(UUID tenantPublicId) {
        this.tenantPublicId = tenantPublicId;
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
