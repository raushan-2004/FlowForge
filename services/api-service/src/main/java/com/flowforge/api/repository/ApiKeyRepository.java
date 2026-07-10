package com.flowforge.api.repository;

import com.flowforge.api.model.ApiKey;
import com.flowforge.api.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    Optional<ApiKey> findByKeyId(String keyId);
    Optional<ApiKey> findByPublicIdAndTenant(UUID publicId, Tenant tenant);
}
