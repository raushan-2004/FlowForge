package com.flowforge.api.repository;

import com.flowforge.api.model.ExecutionAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExecutionAttemptRepository extends JpaRepository<ExecutionAttempt, Long> {

    Optional<ExecutionAttempt> findByExecutionIdAndAttemptNumber(Long executionId, int attemptNumber);

    @Query("SELECT a FROM ExecutionAttempt a JOIN FETCH a.execution e JOIN FETCH e.project p JOIN FETCH e.tenant t WHERE e.publicId = :executionPublicId AND t.publicId = :tenantPublicId ORDER BY a.attemptNumber ASC")
    List<ExecutionAttempt> findAllByExecutionPublicIdAndTenantPublicId(
            @Param("executionPublicId") UUID executionPublicId,
            @Param("tenantPublicId") UUID tenantPublicId
    );

    @Query("SELECT a FROM ExecutionAttempt a JOIN FETCH a.execution e JOIN FETCH e.project p JOIN FETCH e.tenant t WHERE e.publicId = :executionPublicId AND p.publicId = :projectPublicId AND t.publicId = :tenantPublicId ORDER BY a.attemptNumber ASC")
    List<ExecutionAttempt> findAllByExecutionPublicIdAndProjectPublicIdAndTenantPublicId(
            @Param("executionPublicId") UUID executionPublicId,
            @Param("projectPublicId") UUID projectPublicId,
            @Param("tenantPublicId") UUID tenantPublicId
    );
}
