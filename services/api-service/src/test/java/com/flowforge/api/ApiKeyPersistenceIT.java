package com.flowforge.api;

import com.flowforge.api.model.*;
import com.flowforge.api.repository.ApiKeyRepository;
import com.flowforge.api.repository.ProjectRepository;
import com.flowforge.api.repository.TenantRepository;
import com.flowforge.api.shared.identity.PublicIdGenerator;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApiKeyPersistenceIT extends BasePersistenceIT {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private PublicIdGenerator publicIdGenerator;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void testApiKeyCreationAndKeyIdUniqueness() {
        // Given
        Tenant tenant = tenantRepository.save(new Tenant(publicIdGenerator.generate(), "Tenant A", TenantStatus.ACTIVE));
        Project project = projectRepository.save(new Project(publicIdGenerator.generate(), tenant, "Project Alpha", ProjectStatus.ACTIVE));

        UUID apiPublicId = publicIdGenerator.generate();
        String keyId = "ff_lookup_123456";
        ApiKey apiKey = new ApiKey(apiPublicId, keyId, "ff_key", "algorithm_neutral_hash", project, tenant, ApiKeyStatus.ACTIVE, null);

        // When
        apiKeyRepository.saveAndFlush(apiKey);

        // Then
        Optional<ApiKey> found = apiKeyRepository.findByKeyId(keyId);
        assertThat(found).isPresent();
        assertThat(found.get().getDisplayPrefix()).isEqualTo("ff_key");
        assertThat(found.get().getProject().getId()).isEqualTo(project.getId());
        assertThat(found.get().getTenant().getId()).isEqualTo(tenant.getId());

        // Verify duplicate key_id is rejected
        ApiKey duplicate = new ApiKey(publicIdGenerator.generate(), keyId, "ff_key2", "hash2", project, tenant, ApiKeyStatus.ACTIVE, null);
        assertThatThrownBy(() -> {
            apiKeyRepository.saveAndFlush(duplicate);
        }).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }

    @Test
    void testCrossTenantApiKeyProjectOwnershipMismatchRejection() {
        // Given
        Tenant tenantA = tenantRepository.save(new Tenant(publicIdGenerator.generate(), "Tenant A", TenantStatus.ACTIVE));
        Tenant tenantB = tenantRepository.save(new Tenant(publicIdGenerator.generate(), "Tenant B", TenantStatus.ACTIVE));

        Project projectA = projectRepository.save(new Project(publicIdGenerator.generate(), tenantA, "Project A", ProjectStatus.ACTIVE));

        UUID apiPublicId = publicIdGenerator.generate();
        Timestamp now = Timestamp.from(Instant.now());

        // Attempting to insert an API key referencing Project A but setting tenant_id to Tenant B fails on composite FK
        assertThatThrownBy(() -> {
            entityManager.createNativeQuery("INSERT INTO api_keys (public_id, key_id, display_prefix, secret_hash, project_id, tenant_id, status, created_by, updated_by, created_at, updated_at) " +
                            "VALUES (:publicId, 'ff_bad_key', 'ff_prefix', 'hash', :projectId, :tenantId, 'ACTIVE', '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', :createdAt, :createdAt)")
                    .setParameter("publicId", apiPublicId)
                    .setParameter("projectId", projectA.getId())
                    .setParameter("tenantId", tenantB.getId()) 
                    .setParameter("createdAt", now)
                    .executeUpdate();
            entityManager.flush();
        }).isInstanceOf(jakarta.persistence.PersistenceException.class);
    }
}
