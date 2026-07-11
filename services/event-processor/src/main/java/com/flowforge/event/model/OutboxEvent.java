package com.flowforge.event.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", unique = true, nullable = false, updatable = false)
    private UUID publicId;

    @Enumerated(EnumType.STRING)
    @Column(name = "aggregate_type", nullable = false)
    private OutboxAggregateType aggregateType;

    @Column(name = "aggregate_public_id", nullable = false)
    private UUID aggregatePublicId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "payload", nullable = false)
    private String payload;

    @Column(name = "payload_version", nullable = false)
    private int payloadVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OutboxStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "next_attempt_at")
    private Instant nextAttemptAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected OutboxEvent() {}

    public OutboxEvent(UUID publicId, OutboxAggregateType aggregateType, UUID aggregatePublicId,
                       String eventType, String payload, int payloadVersion, Instant createdAt) {
        if (publicId == null) throw new IllegalArgumentException("publicId cannot be null");
        if (aggregateType == null) throw new IllegalArgumentException("aggregateType cannot be null");
        if (aggregatePublicId == null) throw new IllegalArgumentException("aggregatePublicId cannot be null");
        if (eventType == null) throw new IllegalArgumentException("eventType cannot be null");
        if (payload == null) throw new IllegalArgumentException("payload cannot be null");
        if (createdAt == null) throw new IllegalArgumentException("createdAt cannot be null");

        this.publicId = publicId;
        this.aggregateType = aggregateType;
        this.aggregatePublicId = aggregatePublicId;
        this.eventType = eventType;
        this.payload = payload;
        this.payloadVersion = payloadVersion;
        this.status = OutboxStatus.PENDING;
        this.createdAt = createdAt;
        this.attemptCount = 0;
    }

    public Long getId() {
        return id;
    }

    public UUID getPublicId() {
        return publicId;
    }

    public OutboxAggregateType getAggregateType() {
        return aggregateType;
    }

    public UUID getAggregatePublicId() {
        return aggregatePublicId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public int getPayloadVersion() {
        return payloadVersion;
    }

    public OutboxStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public String getLastError() {
        return lastError;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setStatus(OutboxStatus status) {
        this.status = status;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public void setNextAttemptAt(Instant nextAttemptAt) {
        this.nextAttemptAt = nextAttemptAt;
    }
}
