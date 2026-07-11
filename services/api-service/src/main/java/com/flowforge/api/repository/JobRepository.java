package com.flowforge.api.repository;

import com.flowforge.api.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    @Query("SELECT j FROM Job j JOIN FETCH j.project p JOIN FETCH p.tenant t WHERE j.publicId = :jobPublicId AND p.publicId = :projectPublicId AND t.publicId = :tenantPublicId")
    Optional<Job> findByPublicIdAndProjectPublicIdAndTenantPublicId(
            @Param("jobPublicId") UUID jobPublicId,
            @Param("projectPublicId") UUID projectPublicId,
            @Param("tenantPublicId") UUID tenantPublicId
    );

    @Query("SELECT j FROM Job j JOIN FETCH j.project p JOIN FETCH p.tenant t WHERE p.publicId = :projectPublicId AND t.publicId = :tenantPublicId AND j.status <> 'ARCHIVED'")
    List<Job> findAllByProjectPublicIdAndTenantPublicId(
            @Param("projectPublicId") UUID projectPublicId,
            @Param("tenantPublicId") UUID tenantPublicId
    );

    @Query("SELECT j FROM Job j JOIN FETCH j.project p JOIN FETCH p.tenant t WHERE j.publicId = :jobPublicId AND t.publicId = :tenantPublicId")
    Optional<Job> findByPublicIdAndTenantPublicId(
            @Param("jobPublicId") UUID jobPublicId,
            @Param("tenantPublicId") UUID tenantPublicId
    );

    @Query("SELECT j FROM Job j JOIN FETCH j.project p JOIN FETCH p.tenant t WHERE t.publicId = :tenantPublicId AND j.status <> 'ARCHIVED'")
    List<Job> findAllByTenantPublicId(@Param("tenantPublicId") UUID tenantPublicId);

    @Query("SELECT j FROM Job j WHERE j.project.id = :projectId AND LOWER(j.name) = LOWER(:name) AND j.status <> 'ARCHIVED'")
    Optional<Job> findByProjectIdAndNameIgnoreCase(
            @Param("projectId") Long projectId,
            @Param("name") String name
    );
}
