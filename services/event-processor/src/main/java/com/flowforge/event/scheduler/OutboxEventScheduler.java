package com.flowforge.event.scheduler;

import com.flowforge.event.config.OutboxProperties;
import com.flowforge.event.model.OutboxEvent;
import com.flowforge.event.model.OutboxStatus;
import com.flowforge.event.repository.OutboxEventRepository;
import com.flowforge.event.service.OutboxPublisherService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
public class OutboxEventScheduler {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventScheduler.class);

    private final OutboxProperties outboxProperties;
    private final OutboxPublisherService outboxPublisherService;
    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Clock clock;

    // Micrometer Metrics
    private final Counter publishedCounter;
    private final Counter failedCounter;
    private final Counter retryCounter;
    private final Timer publishLatencyTimer;

    public OutboxEventScheduler(
            OutboxProperties outboxProperties,
            OutboxPublisherService outboxPublisherService,
            OutboxEventRepository outboxEventRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            Clock clock,
            MeterRegistry meterRegistry) {
        this.outboxProperties = outboxProperties;
        this.outboxPublisherService = outboxPublisherService;
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.clock = clock;

        // Initialize Metrics
        this.publishedCounter = meterRegistry.counter("flowforge.outbox.events.published");
        this.failedCounter = meterRegistry.counter("flowforge.outbox.events.failed");
        this.retryCounter = meterRegistry.counter("flowforge.outbox.events.retried");
        this.publishLatencyTimer = meterRegistry.timer("flowforge.outbox.publish.latency");

        meterRegistry.gauge("flowforge.outbox.events.pending", this,
                s -> s.outboxEventRepository.countByStatus(OutboxStatus.PENDING));
        meterRegistry.gauge("flowforge.outbox.events.dead", this,
                s -> s.outboxEventRepository.countByStatus(OutboxStatus.DEAD));
    }

    @Scheduled(fixedDelayString = "${flowforge.outbox.poll-interval-ms:1000}")
    public void pollAndPublish() {
        if (!outboxProperties.isEnabled()) {
            return;
        }

        int batchSize = outboxProperties.getBatchSize();
        Instant now = clock.instant();

        // 1. Claim pending/failed outbox events atomically
        List<OutboxEvent> batch = outboxPublisherService.claimBatch(now, batchSize);
        if (batch.isEmpty()) {
            return;
        }

        log.debug("Claimed {} outbox events for publishing", batch.size());

        // 2. Process each event (outside claim transaction to keep DB lock window short)
        for (OutboxEvent event : batch) {
            processEvent(event);
        }
    }

    private void processEvent(OutboxEvent event) {
        String topic = outboxProperties.getTopicExecutionCreated();
        // The aggregatePublicId is the executionPublicId (provides strict in-order routing key)
        String key = event.getAggregatePublicId().toString();
        String payload = event.getPayload();

        Timer.Sample sample = Timer.start();

        try {
            log.info("Publishing outbox event {} (execution: {}) to topic {}", event.getPublicId(), key, topic);

            // Synchronously block until broker acknowledges receipt (acks=all)
            CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, key, payload);
            future.get(5, TimeUnit.SECONDS);

            // Stop timer & mark published in DB
            sample.stop(publishLatencyTimer);
            outboxPublisherService.markPublished(event.getId(), clock.instant());
            publishedCounter.increment();

            log.info("Successfully published outbox event {}", event.getPublicId());

        } catch (Exception e) {
            log.error("Failed to publish outbox event {}: {}", event.getPublicId(), e.getMessage());
            sample.stop(publishLatencyTimer);

            int nextAttempts = event.getAttemptCount() + 1;
            boolean dead = nextAttempts >= outboxProperties.getMaxRetries();
            OutboxStatus nextStatus = dead ? OutboxStatus.DEAD : OutboxStatus.FAILED;

            Instant nextAttemptAt = null;
            if (!dead) {
                // Exponential backoff math: delay = base * 2^(attempts)
                long delaySeconds = outboxProperties.getBackoffBaseSeconds() * (long) Math.pow(2, nextAttempts);
                nextAttemptAt = clock.instant().plusSeconds(delaySeconds);
            }

            // Save status mutation to DB
            outboxPublisherService.markFailed(event.getId(), nextAttempts, nextAttemptAt, e.getMessage(), nextStatus);
            failedCounter.increment();
            if (!dead) {
                retryCounter.increment();
            }
        }
    }
}
