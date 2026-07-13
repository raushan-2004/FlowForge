package com.flowforge.scheduler.repository;

import com.flowforge.scheduler.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;

public interface JobRepository extends JpaRepository<Job, Long> {
    
    @Query(value = "SELECT * FROM jobs j " +
                   "WHERE j.status = 'ACTIVE' " +
                   "  AND j.enabled = true " +
                   "  AND j.schedule_type = 'CRON' " +
                   "  AND j.next_fire_at <= :now " +
                   "ORDER BY j.next_fire_at ASC " +
                   "LIMIT :batchSize " +
                   "FOR UPDATE SKIP LOCKED", nativeQuery = true)
    List<Job> claimNextBatchForScheduling(@Param("now") Instant now, @Param("batchSize") int batchSize);

    @Query(value = "SELECT * FROM jobs j " +
                   "WHERE j.status = 'ACTIVE' " +
                   "  AND j.enabled = true " +
                   "  AND j.schedule_type = 'CRON' " +
                   "  AND j.next_fire_at <= :now " +
                   "ORDER BY j.next_fire_at ASC " +
                   "LIMIT 1 " +
                   "FOR UPDATE SKIP LOCKED", nativeQuery = true)
    java.util.Optional<Job> claimNextJobForScheduling(@Param("now") Instant now);

    @Query(value = "SELECT COUNT(*) FROM jobs j " +
                   "WHERE j.status = 'ACTIVE' " +
                   "  AND j.enabled = true " +
                   "  AND j.schedule_type = 'CRON' " +
                   "  AND j.next_fire_at <= :now", nativeQuery = true)
    long countActiveEnabledDueJobs(@Param("now") Instant now);
}
