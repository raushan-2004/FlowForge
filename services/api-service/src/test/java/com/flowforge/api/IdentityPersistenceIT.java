package com.flowforge.api;

import com.flowforge.api.model.Tenant;
import com.flowforge.api.model.TenantStatus;
import com.flowforge.api.model.User;
import com.flowforge.api.model.UserStatus;
import com.flowforge.api.repository.TenantRepository;
import com.flowforge.api.repository.UserRepository;
import com.flowforge.api.shared.identity.PublicIdGenerator;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdentityPersistenceIT extends BasePersistenceIT {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private PublicIdGenerator publicIdGenerator;

    @Test
    void testUserCreationAndCaseInsensitiveUniqueEmailConstraint() {
        // Given
        UUID publicId = publicIdGenerator.generate();
        String originalEmail = "  User@FlowForge.com  ";
        // Normalize email: trim and lowercase at application boundary
        String normalizedEmail = originalEmail.trim().toLowerCase(Locale.ROOT);
        User user = new User(publicId, normalizedEmail, "hashed_password", UserStatus.ACTIVE);

        // When
        userRepository.save(user);

        // Then
        Optional<User> found = userRepository.findByEmail(normalizedEmail);
        assertThat(found).isPresent();
        assertThat(found.get().getPublicId()).isEqualTo(publicId);

        // Verify case-insensitive constraint: attempting to register with duplicate email fails
        UUID secondPublicId = publicIdGenerator.generate();
        String duplicateEmail = "USER@FLOWFORGE.COM"; 
        User secondUser = new User(secondPublicId, duplicateEmail.trim().toLowerCase(Locale.ROOT), "different_hash", UserStatus.ACTIVE);

        assertThatThrownBy(() -> {
            userRepository.saveAndFlush(secondUser);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void testTenantCreationAndStatusTransitions() {
        // Given
        UUID tenantPublicId = publicIdGenerator.generate();
        Tenant tenant = new Tenant(tenantPublicId, "Acme Corp", TenantStatus.ACTIVE);

        // When
        tenantRepository.saveAndFlush(tenant);

        // Then
        Optional<Tenant> found = tenantRepository.findByPublicId(tenantPublicId);
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Acme Corp");
        assertThat(found.get().getStatus()).isEqualTo(TenantStatus.ACTIVE);

        // When status is closed
        found.get().close();
        tenantRepository.saveAndFlush(found.get());

        // Then status is closed
        Tenant closedTenant = tenantRepository.findByPublicId(tenantPublicId).orElseThrow();
        assertThat(closedTenant.getStatus()).isEqualTo(TenantStatus.CLOSED);
    }
}
