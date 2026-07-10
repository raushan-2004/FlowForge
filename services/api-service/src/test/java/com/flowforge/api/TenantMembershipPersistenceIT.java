package com.flowforge.api;

import com.flowforge.api.model.*;
import com.flowforge.api.repository.TenantMembershipRepository;
import com.flowforge.api.repository.TenantRepository;
import com.flowforge.api.repository.UserRepository;
import com.flowforge.api.shared.identity.PublicIdGenerator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantMembershipPersistenceIT extends BasePersistenceIT {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private TenantMembershipRepository membershipRepository;

    @Autowired
    private PublicIdGenerator publicIdGenerator;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void testMembershipCreationAndUniquenessConstraint() {
        // Given
        User user = userRepository.save(new User(publicIdGenerator.generate(), "member@flowforge.com", "hash", UserStatus.ACTIVE));
        Tenant tenant = tenantRepository.save(new Tenant(publicIdGenerator.generate(), "SaaS Group", TenantStatus.ACTIVE));
        TenantMembership membership = new TenantMembership(tenant, user, TenantRole.ADMIN);

        // When
        membershipRepository.saveAndFlush(membership);

        // Then
        Optional<TenantMembership> found = membershipRepository.findByTenantAndUser(tenant, user);
        assertThat(found).isPresent();
        assertThat(found.get().getRole()).isEqualTo(TenantRole.ADMIN);

        // Verify duplicate membership is rejected
        TenantMembership duplicate = new TenantMembership(tenant, user, TenantRole.DEVELOPER);
        assertThatThrownBy(() -> {
            membershipRepository.saveAndFlush(duplicate);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void testRoleCheckConstraintAtSqlLevel() {
        // Given
        User user = userRepository.save(new User(publicIdGenerator.generate(), "member-chk@flowforge.com", "hash", UserStatus.ACTIVE));
        Tenant tenant = tenantRepository.save(new Tenant(publicIdGenerator.generate(), "SaaS Group Chk", TenantStatus.ACTIVE));

        // When/Then
        assertThatThrownBy(() -> {
            entityManager.createNativeQuery("INSERT INTO tenant_memberships (tenant_id, user_id, role, created_at) VALUES (:tenantId, :userId, 'GUEST', :createdAt)")
                    .setParameter("tenantId", tenant.getId())
                    .setParameter("userId", user.getId())
                    .setParameter("createdAt", Timestamp.from(Instant.now()))
                    .executeUpdate();
            entityManager.flush();
        }).isInstanceOf(jakarta.persistence.PersistenceException.class);
    }
}
