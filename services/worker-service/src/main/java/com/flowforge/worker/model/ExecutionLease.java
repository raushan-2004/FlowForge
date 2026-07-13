package com.flowforge.worker.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "execution_leases")
public class ExecutionLease {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "execution_public_id", unique = true, nullable = false, updatable = false)
    private UUID executionPublicId;

    @Column(name = "worker_public_id", nullable = false)
    private UUID workerPublicId;

    @Column(name = "lease_token", nullable = false)
    private String leaseToken;

    @Column(name = "lease_version", nullable = false)
    private Long leaseVersion;

    @Column(name = "leased_at", nullable = false)
    private Instant leasedAt;

    @Column(name = "lease_expires_at", nullable = false)
    private Instant leaseExpiresAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected ExecutionLease() {}

    public ExecutionLease(UUID executionPublicId, UUID workerPublicId, String leaseToken, Long leaseVersion, Instant leasedAt, Instant leaseExpiresAt) {
        this.executionPublicId = executionPublicId;
        this.workerPublicId = workerPublicId;
        this.leaseToken = leaseToken;
        this.leaseVersion = leaseVersion;
        this.leasedAt = leasedAt;
        this.leaseExpiresAt = leaseExpiresAt;
    }

    public Long getId() {
        return id;
    }

    public UUID getExecutionPublicId() {
        return executionPublicId;
    }

    public UUID getWorkerPublicId() {
        return workerPublicId;
    }

    public void setWorkerPublicId(UUID workerPublicId) {
        this.workerPublicId = workerPublicId;
    }

    public String getLeaseToken() {
        return leaseToken;
    }

    public void setLeaseToken(String leaseToken) {
        this.leaseToken = leaseToken;
    }

    public Long getLeaseVersion() {
        return leaseVersion;
    }

    public void setLeaseVersion(Long leaseVersion) {
        this.leaseVersion = leaseVersion;
    }

    public Instant getLeasedAt() {
        return leasedAt;
    }

    public void setLeasedAt(Instant leasedAt) {
        this.leasedAt = leasedAt;
    }

    public Instant getLeaseExpiresAt() {
        return leaseExpiresAt;
    }

    public void setLeaseExpiresAt(Instant leaseExpiresAt) {
        this.leaseExpiresAt = leaseExpiresAt;
    }

    public Long getVersion() {
        return version;
    }
}
