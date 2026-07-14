package com.flowforge.api.repository;

import com.flowforge.api.model.Tenant;
import com.flowforge.api.model.TenantMembership;
import com.flowforge.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.flowforge.api.model.TenantRole;

@Repository
public interface TenantMembershipRepository extends JpaRepository<TenantMembership, Long> {
    Optional<TenantMembership> findByTenantAndUser(Tenant tenant, User user);

    @Query("SELECT m FROM TenantMembership m JOIN FETCH m.tenant WHERE m.tenant.publicId = :tenantPublicId AND m.user.publicId = :userPublicId")
    Optional<TenantMembership> findByTenantPublicIdAndUserPublicId(
            @Param("tenantPublicId") UUID tenantPublicId, 
            @Param("userPublicId") UUID userPublicId
    );

    @Query("SELECT m FROM TenantMembership m WHERE m.tenant.publicId = :tenantPublicId")
    List<TenantMembership> findAllByTenantPublicId(@Param("tenantPublicId") UUID tenantPublicId);

    @Query("SELECT m FROM TenantMembership m WHERE m.user.publicId = :userPublicId")
    List<TenantMembership> findAllByUserPublicId(@Param("userPublicId") UUID userPublicId);

    @Query("SELECT COUNT(m) FROM TenantMembership m WHERE m.tenant.publicId = :tenantPublicId AND m.role = :role")
    long countByTenantPublicIdAndRole(
            @Param("tenantPublicId") UUID tenantPublicId,
            @Param("role") TenantRole role
    );
}
