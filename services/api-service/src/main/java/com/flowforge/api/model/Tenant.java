package com.flowforge.api.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenants")
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", unique = true, nullable = false, updatable = false)
    private UUID publicId;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TenantStatus status;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "updated_by", nullable = false)
    private UUID updatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Tenant() {}

    public Tenant(UUID publicId, String name, TenantStatus status) {
        this(publicId, name, status, UUID.fromString("00000000-0000-0000-0000-000000000000"));
    }

    public Tenant(UUID publicId, String name, TenantStatus status, UUID createdBy) {
        if (publicId == null) throw new IllegalArgumentException("publicId cannot be null");
        if (name == null) throw new IllegalArgumentException("name cannot be null");
        if (status == null) throw new IllegalArgumentException("status cannot be null");
        if (createdBy == null) throw new IllegalArgumentException("createdBy cannot be null");

        this.publicId = publicId;
        this.name = name;
        this.status = status;
        this.createdBy = createdBy;
        this.updatedBy = createdBy;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public Long getId() {
        return id;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public String getName() {
        return name;
    }

    public TenantStatus getStatus() {
        return status;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void suspend() {
        suspend(UUID.fromString("00000000-0000-0000-0000-000000000000"));
    }

    public void suspend(UUID updatedBy) {
        if (updatedBy == null) throw new IllegalArgumentException("updatedBy cannot be null");
        this.status = TenantStatus.SUSPENDED;
        this.updatedBy = updatedBy;
        this.updatedAt = Instant.now();
    }

    public void activate() {
        activate(UUID.fromString("00000000-0000-0000-0000-000000000000"));
    }

    public void activate(UUID updatedBy) {
        if (updatedBy == null) throw new IllegalArgumentException("updatedBy cannot be null");
        this.status = TenantStatus.ACTIVE;
        this.updatedBy = updatedBy;
        this.updatedAt = Instant.now();
    }

    public void close() {
        close(UUID.fromString("00000000-0000-0000-0000-000000000000"));
    }

    public void close(UUID updatedBy) {
        if (updatedBy == null) throw new IllegalArgumentException("updatedBy cannot be null");
        this.status = TenantStatus.CLOSED;
        this.updatedBy = updatedBy;
        this.updatedAt = Instant.now();
    }

    public void changeName(String name) {
        changeName(name, UUID.fromString("00000000-0000-0000-0000-000000000000"));
    }

    public void changeName(String name, UUID updatedBy) {
        if (name == null) throw new IllegalArgumentException("name cannot be null");
        if (updatedBy == null) throw new IllegalArgumentException("updatedBy cannot be null");
        this.name = name;
        this.updatedBy = updatedBy;
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Tenant)) return false;
        Tenant tenant = (Tenant) o;
        return publicId != null && publicId.equals(tenant.getPublicId());
    }

    @Override
    public int hashCode() {
        return publicId != null ? publicId.hashCode() : 0;
    }
}
