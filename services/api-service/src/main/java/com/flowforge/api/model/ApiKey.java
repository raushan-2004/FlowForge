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

    @Column(name = "created_by", nullable = false, updatable = false)
    private UUID createdBy;

    @Column(name = "updated_by", nullable = false)
    private UUID updatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "expiry_at")
    private Instant expiryAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    protected ApiKey() {}

    public ApiKey(UUID publicId, String keyId, String displayPrefix, String secretHash, Project project, Tenant tenant, ApiKeyStatus status, Instant expiryAt) {
        this(publicId, keyId, displayPrefix, secretHash, project, tenant, status, expiryAt, UUID.fromString("00000000-0000-0000-0000-000000000000"));
    }

    public ApiKey(UUID publicId, String keyId, String displayPrefix, String secretHash, Project project, Tenant tenant, ApiKeyStatus status, Instant expiryAt, UUID createdBy) {
        this(publicId, keyId, displayPrefix, secretHash, project, tenant, status, expiryAt, createdBy, Instant.now());
    }

    public ApiKey(UUID publicId, String keyId, String displayPrefix, String secretHash, Project project, Tenant tenant, ApiKeyStatus status, Instant expiryAt, UUID createdBy, Instant createdAt) {
        if (publicId == null) throw new IllegalArgumentException("publicId cannot be null");
        if (keyId == null) throw new IllegalArgumentException("keyId cannot be null");
        if (displayPrefix == null) throw new IllegalArgumentException("displayPrefix cannot be null");
        if (secretHash == null) throw new IllegalArgumentException("secretHash cannot be null");
        if (project == null) throw new IllegalArgumentException("project cannot be null");
        if (tenant == null) throw new IllegalArgumentException("tenant cannot be null");
        if (status == null) throw new IllegalArgumentException("status cannot be null");
        if (createdBy == null) throw new IllegalArgumentException("createdBy cannot be null");
        if (createdAt == null) throw new IllegalArgumentException("createdAt cannot be null");

        this.publicId = publicId;
        this.keyId = keyId;
        this.displayPrefix = displayPrefix;
        this.secretHash = secretHash;
        this.project = project;
        this.tenant = tenant;
        this.status = status;
        this.expiryAt = expiryAt;
        this.createdBy = createdBy;
        this.updatedBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
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

    public Instant getExpiryAt() {
        return expiryAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public void revoke() {
        revoke(UUID.fromString("00000000-0000-0000-0000-000000000000"));
    }

    public void revoke(UUID updatedBy) {
        if (updatedBy == null) throw new IllegalArgumentException("updatedBy cannot be null");
        this.status = ApiKeyStatus.REVOKED;
        this.updatedBy = updatedBy;
        this.updatedAt = Instant.now();
    }

    public void expire() {
        expire(UUID.fromString("00000000-0000-0000-0000-000000000000"));
    }

    public void expire(UUID updatedBy) {
        if (updatedBy == null) throw new IllegalArgumentException("updatedBy cannot be null");
        this.status = ApiKeyStatus.EXPIRED;
        this.updatedBy = updatedBy;
        this.updatedAt = Instant.now();
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
