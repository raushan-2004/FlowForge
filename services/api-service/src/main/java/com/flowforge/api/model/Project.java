package com.flowforge.api.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "projects", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"tenant_id", "name"}),
    @UniqueConstraint(columnNames = {"id", "tenant_id"})
})
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", unique = true, nullable = false, updatable = false)
    private UUID publicId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProjectStatus status;

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "updated_by", nullable = false)
    private UUID updatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Project() {}

    public Project(UUID publicId, Tenant tenant, String name, ProjectStatus status) {
        this(publicId, tenant, name, status, UUID.fromString("00000000-0000-0000-0000-000000000000"));
    }

    public Project(UUID publicId, Tenant tenant, String name, ProjectStatus status, UUID createdBy) {
        if (publicId == null) throw new IllegalArgumentException("publicId cannot be null");
        if (tenant == null) throw new IllegalArgumentException("tenant cannot be null");
        if (name == null) throw new IllegalArgumentException("name cannot be null");
        if (status == null) throw new IllegalArgumentException("status cannot be null");
        if (createdBy == null) throw new IllegalArgumentException("createdBy cannot be null");

        this.publicId = publicId;
        this.tenant = tenant;
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

    public Tenant getTenant() {
        return tenant;
    }

    public String getName() {
        return name;
    }

    public ProjectStatus getStatus() {
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
        this.status = ProjectStatus.SUSPENDED;
        this.updatedBy = updatedBy;
        this.updatedAt = Instant.now();
    }

    public void activate() {
        activate(UUID.fromString("00000000-0000-0000-0000-000000000000"));
    }

    public void activate(UUID updatedBy) {
        if (updatedBy == null) throw new IllegalArgumentException("updatedBy cannot be null");
        this.status = ProjectStatus.ACTIVE;
        this.updatedBy = updatedBy;
        this.updatedAt = Instant.now();
    }

    public void rename(String name) {
        rename(name, UUID.fromString("00000000-0000-0000-0000-000000000000"));
    }

    public void rename(String name, UUID updatedBy) {
        if (name == null) throw new IllegalArgumentException("name cannot be null");
        if (updatedBy == null) throw new IllegalArgumentException("updatedBy cannot be null");
        this.name = name;
        this.updatedBy = updatedBy;
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Project)) return false;
        Project project = (Project) o;
        return publicId != null && project.getPublicId() != null && publicId.equals(project.getPublicId());
    }

    @Override
    public int hashCode() {
        return publicId != null ? publicId.hashCode() : 0;
    }
}
