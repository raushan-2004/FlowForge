package com.flowforge.event.repository;

import com.flowforge.event.model.OutboxEvent;
import com.flowforge.event.model.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Query(value = "SELECT * FROM outbox_events " +
            "WHERE (status = 'PENDING' OR (status = 'FAILED' AND next_attempt_at <= :now)) " +
            "ORDER BY created_at ASC " +
            "LIMIT :batchSize " +
            "FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<OutboxEvent> claimNextBatchForPublishing(@Param("now") Instant now, @Param("batchSize") int batchSize);

    long countByStatus(OutboxStatus status);
}
