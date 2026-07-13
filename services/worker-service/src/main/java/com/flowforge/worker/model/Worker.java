package com.flowforge.worker.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "workers")
public class Worker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", unique = true, nullable = false, updatable = false)
    private UUID publicId;

    @Column(name = "worker_name", nullable = false)
    private String workerName;

    @Column(name = "instance_id", unique = true, nullable = false)
    private String instanceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private WorkerStatus status;

    @Column(name = "registered_at", nullable = false, updatable = false)
    private Instant registeredAt;

    @Column(name = "last_heartbeat_at", nullable = false)
    private Instant lastHeartbeatAt;

    @ElementCollection(targetClass = WorkerCapability.class, fetch = FetchType.EAGER)
    @CollectionTable(name = "worker_capabilities", joinColumns = @JoinColumn(name = "worker_id"))
    @Column(name = "capability", nullable = false)
    @Enumerated(EnumType.STRING)
    private Set<WorkerCapability> capabilities = new HashSet<>();

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected Worker() {}

    public Worker(UUID publicId, String workerName, String instanceId, WorkerStatus status, Instant registeredAt, Instant lastHeartbeatAt) {
        this.publicId = publicId;
        this.workerName = workerName;
        this.instanceId = instanceId;
        this.status = status;
        this.registeredAt = registeredAt;
        this.lastHeartbeatAt = lastHeartbeatAt;
    }

    public Long getId() {
        return id;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public String getWorkerName() {
        return workerName;
    }

    public void setWorkerName(String workerName) {
        this.workerName = workerName;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public WorkerStatus getStatus() {
        return status;
    }

    public void setStatus(WorkerStatus status) {
        this.status = status;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public Instant getLastHeartbeatAt() {
        return lastHeartbeatAt;
    }

    public void setLastHeartbeatAt(Instant lastHeartbeatAt) {
        this.lastHeartbeatAt = lastHeartbeatAt;
    }

    public Set<WorkerCapability> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(Set<WorkerCapability> capabilities) {
        this.capabilities = capabilities;
    }

    public Long getVersion() {
        return version;
    }
}
