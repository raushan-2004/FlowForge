package com.flowforge.worker.repository;

import com.flowforge.worker.model.ExecutionLease;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExecutionLeaseRepository extends JpaRepository<ExecutionLease, Long> {
    Optional<ExecutionLease> findByExecutionPublicId(UUID executionPublicId);
    List<ExecutionLease> findAllByWorkerPublicId(UUID workerPublicId);
    long countByWorkerPublicId(UUID workerPublicId);

    @Query("SELECT l FROM ExecutionLease l WHERE l.leaseExpiresAt < :now")
    List<ExecutionLease> findExpiredLeases(@Param("now") Instant now);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE ExecutionLease l SET l.leaseExpiresAt = :newExpiry, l.version = l.version + 1 " +
           "WHERE l.executionPublicId = :execId AND l.leaseToken = :token AND l.leaseVersion = :leaseVersion")
    int renewLease(@Param("execId") UUID execId,
                   @Param("token") String token,
                   @Param("leaseVersion") Long leaseVersion,
                   @Param("newExpiry") Instant newExpiry);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE ExecutionLease l SET l.workerPublicId = :recoveryWorkerId, l.leaseToken = :newToken, " +
           "l.leaseVersion = l.leaseVersion + 1, l.leasedAt = :now, l.leaseExpiresAt = :newExpiry, l.version = l.version + 1 " +
           "WHERE l.executionPublicId = :execId AND l.leaseExpiresAt < :now")
    int reclaimExpiredLease(@Param("execId") UUID execId,
                            @Param("recoveryWorkerId") UUID recoveryWorkerId,
                            @Param("newToken") String newToken,
                            @Param("now") Instant now,
                            @Param("newExpiry") Instant newExpiry);
}
