package com.flowforge.api.repository;

import com.flowforge.api.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {
    Optional<Tenant> findByPublicId(UUID publicId);
    Optional<Tenant> findByNameIgnoreCase(String name);
}
