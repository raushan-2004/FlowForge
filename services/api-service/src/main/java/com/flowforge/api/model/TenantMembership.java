package com.flowforge.api.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "tenant_memberships", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"tenant_id", "user_id"})
})
public class TenantMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private TenantRole role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected TenantMembership() {}

    public TenantMembership(Tenant tenant, User user, TenantRole role) {
        if (tenant == null) throw new IllegalArgumentException("tenant cannot be null");
        if (user == null) throw new IllegalArgumentException("user cannot be null");
        if (role == null) throw new IllegalArgumentException("role cannot be null");

        this.tenant = tenant;
        this.user = user;
        this.role = role;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public User getUser() {
        return user;
    }

    public TenantRole getRole() {
        return role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void updateRole(TenantRole role) {
        if (role == null) throw new IllegalArgumentException("role cannot be null");
        this.role = role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TenantMembership)) return false;
        TenantMembership that = (TenantMembership) o;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 17;
    }
}
