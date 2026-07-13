package com.flowforge.scheduler.model;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "projects")
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

    protected Project() {}

    public Project(UUID publicId, Tenant tenant, String name, ProjectStatus status) {
        this.publicId = publicId;
        this.tenant = tenant;
        this.name = name;
        this.status = status;
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

    public ProjectStatus getStatus() {
        return status;
    }
}
