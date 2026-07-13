package com.flowforge.api.repository;

import com.flowforge.api.model.Execution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExecutionRepository extends JpaRepository<Execution, Long> {

    Optional<Execution> findByPublicId(UUID publicId);

    @Query("SELECT e FROM Execution e JOIN FETCH e.job j JOIN FETCH e.project p JOIN FETCH e.tenant t WHERE e.publicId = :executionPublicId AND t.publicId = :tenantPublicId")
    Optional<Execution> findByPublicIdAndTenantPublicId(
            @Param("executionPublicId") UUID executionPublicId,
            @Param("tenantPublicId") UUID tenantPublicId
    );

    @Query("SELECT e FROM Execution e JOIN FETCH e.job j JOIN FETCH e.project p JOIN FETCH e.tenant t WHERE e.publicId = :executionPublicId AND p.publicId = :projectPublicId AND t.publicId = :tenantPublicId")
    Optional<Execution> findByPublicIdAndProjectPublicIdAndTenantPublicId(
            @Param("executionPublicId") UUID executionPublicId,
            @Param("projectPublicId") UUID projectPublicId,
            @Param("tenantPublicId") UUID tenantPublicId
    );

    @Query("SELECT e FROM Execution e JOIN FETCH e.job j JOIN FETCH e.project p JOIN FETCH e.tenant t WHERE t.publicId = :tenantPublicId ORDER BY e.queuedAt DESC")
    List<Execution> findAllByTenantPublicId(@Param("tenantPublicId") UUID tenantPublicId);

    @Query("SELECT e FROM Execution e JOIN FETCH e.job j JOIN FETCH e.project p JOIN FETCH e.tenant t WHERE p.publicId = :projectPublicId AND t.publicId = :tenantPublicId ORDER BY e.queuedAt DESC")
    List<Execution> findAllByProjectPublicIdAndTenantPublicId(
            @Param("projectPublicId") UUID projectPublicId,
            @Param("tenantPublicId") UUID tenantPublicId
    );
}
