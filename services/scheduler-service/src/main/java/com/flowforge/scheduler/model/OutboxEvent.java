package com.flowforge.scheduler.model;

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

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
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
}
