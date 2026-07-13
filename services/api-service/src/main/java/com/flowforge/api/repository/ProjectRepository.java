package com.flowforge.api.repository;

import com.flowforge.api.model.Project;
import com.flowforge.api.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    Optional<Project> findByPublicId(UUID publicId);
    Optional<Project> findByPublicIdAndTenant(UUID publicId, Tenant tenant);
    Optional<Project> findByTenantAndNameIgnoreCase(Tenant tenant, String name);

    @Query("SELECT p FROM Project p WHERE p.publicId = :publicId AND p.tenant.publicId = :tenantPublicId")
    Optional<Project> findByPublicIdAndTenantPublicId(
            @Param("publicId") UUID publicId,
            @Param("tenantPublicId") UUID tenantPublicId
    );

    @Query("SELECT p FROM Project p WHERE p.tenant.publicId = :tenantPublicId")
    List<Project> findAllByTenantPublicId(@Param("tenantPublicId") UUID tenantPublicId);
}
