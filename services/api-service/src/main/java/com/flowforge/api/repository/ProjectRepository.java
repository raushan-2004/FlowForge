package com.flowforge.api.repository;

import com.flowforge.api.model.Project;
import com.flowforge.api.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    Optional<Project> findByPublicIdAndTenant(UUID publicId, Tenant tenant);
    Optional<Project> findByTenantAndNameIgnoreCase(Tenant tenant, String name);
}
