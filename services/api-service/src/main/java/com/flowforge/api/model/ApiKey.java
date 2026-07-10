package com.flowforge.api.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "api_keys")
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", unique = true, nullable = false, updatable = false)
    private UUID publicId;

    @Column(name = "key_id", unique = true, nullable = false, updatable = false)
    private String keyId;

    @Column(name = "display_prefix", nullable = false, updatable = false)
    private String displayPrefix;

    @Column(name = "secret_hash", nullable = false)
    private String secretHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ApiKeyStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expiry_at")
    private Instant expiryAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    protected ApiKey() {}

    public ApiKey(UUID publicId, String keyId, String displayPrefix, String secretHash, Project project, Tenant tenant, ApiKeyStatus status, Instant expiryAt) {
        if (publicId == null) throw new IllegalArgumentException("publicId cannot be null");
        if (keyId == null) throw new IllegalArgumentException("keyId cannot be null");
        if (displayPrefix == null) throw new IllegalArgumentException("displayPrefix cannot be null");
        if (secretHash == null) throw new IllegalArgumentException("secretHash cannot be null");
        if (project == null) throw new IllegalArgumentException("project cannot be null");
        if (tenant == null) throw new IllegalArgumentException("tenant cannot be null");
        if (status == null) throw new IllegalArgumentException("status cannot be null");

        this.publicId = publicId;
        this.keyId = keyId;
        this.displayPrefix = displayPrefix;
        this.secretHash = secretHash;
        this.project = project;
        this.tenant = tenant;
        this.status = status;
        this.expiryAt = expiryAt;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public String getKeyId() {
        return keyId;
    }

    public String getDisplayPrefix() {
        return displayPrefix;
    }

    public String getSecretHash() {
        return secretHash;
    }

    public Project getProject() {
        return project;
    }

    public Tenant getTenant() {
        return tenant;
    }

    public ApiKeyStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiryAt() {
        return expiryAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public void revoke() {
        this.status = ApiKeyStatus.REVOKED;
    }

    public void expire() {
        this.status = ApiKeyStatus.EXPIRED;
    }

    public void recordUsage(Instant usedAt) {
        this.lastUsedAt = usedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ApiKey)) return false;
        ApiKey apiKey = (ApiKey) o;
        return publicId != null && publicId.equals(apiKey.getPublicId());
    }

    @Override
    public int hashCode() {
        return publicId != null ? publicId.hashCode() : 0;
    }
}
