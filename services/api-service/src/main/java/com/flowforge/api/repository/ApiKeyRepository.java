package com.flowforge.api.repository;

import com.flowforge.api.model.ApiKey;
import com.flowforge.api.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    Optional<ApiKey> findByKeyId(String keyId);
    Optional<ApiKey> findByPublicIdAndTenant(UUID publicId, Tenant tenant);

    @Query("SELECT k FROM ApiKey k JOIN FETCH k.project JOIN FETCH k.tenant WHERE k.keyId = :keyId")
    Optional<ApiKey> findByKeyIdWithProjectAndTenant(@Param("keyId") String keyId);

    @Query("SELECT k FROM ApiKey k WHERE k.publicId = :publicId AND k.tenant.publicId = :tenantPublicId")
    Optional<ApiKey> findByPublicIdAndTenantPublicId(
            @Param("publicId") UUID publicId,
            @Param("tenantPublicId") UUID tenantPublicId
    );

    @Query("SELECT k FROM ApiKey k WHERE k.project.publicId = :projectPublicId AND k.tenant.publicId = :tenantPublicId")
    List<ApiKey> findAllByProjectPublicIdAndTenantPublicId(
            @Param("projectPublicId") UUID projectPublicId,
            @Param("tenantPublicId") UUID tenantPublicId
    );
}
