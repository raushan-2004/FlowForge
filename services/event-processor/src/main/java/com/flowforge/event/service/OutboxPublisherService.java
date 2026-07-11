package com.flowforge.event.service;

import com.flowforge.event.model.OutboxEvent;
import com.flowforge.event.model.OutboxStatus;
import com.flowforge.event.repository.OutboxEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class OutboxPublisherService {

    private final OutboxEventRepository outboxEventRepository;

    public OutboxPublisherService(OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<OutboxEvent> claimBatch(Instant now, int batchSize) {
        List<OutboxEvent> batch = outboxEventRepository.claimNextBatchForPublishing(now, batchSize);
        for (OutboxEvent event : batch) {
            event.setStatus(OutboxStatus.PUBLISHING);
        }
        return outboxEventRepository.saveAllAndFlush(batch);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPublished(Long id, Instant publishedAt) {
        outboxEventRepository.findById(id).ifPresent(event -> {
            event.setStatus(OutboxStatus.PUBLISHED);
            event.setPublishedAt(publishedAt);
            event.setLastError(null);
            event.setNextAttemptAt(null);
            outboxEventRepository.saveAndFlush(event);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long id, int attemptCount, Instant nextAttemptAt, String error, OutboxStatus nextStatus) {
        outboxEventRepository.findById(id).ifPresent(event -> {
            event.setStatus(nextStatus);
            event.setAttemptCount(attemptCount);
            event.setNextAttemptAt(nextAttemptAt);
            event.setLastError(error);
            outboxEventRepository.saveAndFlush(event);
        });
    }
}
