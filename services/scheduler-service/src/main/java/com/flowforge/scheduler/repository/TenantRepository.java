package com.flowforge.scheduler.repository;

import com.flowforge.scheduler.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, Long> {
}
