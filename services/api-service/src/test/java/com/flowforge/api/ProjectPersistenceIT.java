package com.flowforge.api;

import com.flowforge.api.model.*;
import com.flowforge.api.repository.ProjectRepository;
import com.flowforge.api.repository.TenantRepository;
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

class ProjectPersistenceIT extends BasePersistenceIT {

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private PublicIdGenerator publicIdGenerator;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void testProjectCreationAndLookup() {
        // Given
        Tenant tenant = tenantRepository.save(new Tenant(publicIdGenerator.generate(), "Tenant A", TenantStatus.ACTIVE));
        String name = "  My Project  ";
        String normalizedName = name.trim(); 

        Project project = new Project(publicIdGenerator.generate(), tenant, normalizedName, ProjectStatus.ACTIVE);
        projectRepository.saveAndFlush(project);

        // Verify lookup matches normalized name
        Optional<Project> found = projectRepository.findByTenantAndNameIgnoreCase(tenant, "my project");
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("My Project");
    }

    @Test
    void testTenantScopedCaseInsensitiveNameUniquenessRejection() {
        // Given
        Tenant tenant = tenantRepository.save(new Tenant(publicIdGenerator.generate(), "Tenant A", TenantStatus.ACTIVE));
        Project project = new Project(publicIdGenerator.generate(), tenant, "My Project", ProjectStatus.ACTIVE);
        projectRepository.saveAndFlush(project);

        // Verify case-insensitive duplicate in same tenant is rejected
        Project duplicate = new Project(publicIdGenerator.generate(), tenant, "MY PROJECT", ProjectStatus.ACTIVE);
        assertThatThrownBy(() -> {
            projectRepository.saveAndFlush(duplicate);
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void testProjectNameAllowedAcrossDifferentTenants() {
        // Given
        Tenant tenantA = tenantRepository.save(new Tenant(publicIdGenerator.generate(), "Tenant A", TenantStatus.ACTIVE));
        Tenant tenantB = tenantRepository.save(new Tenant(publicIdGenerator.generate(), "Tenant B", TenantStatus.ACTIVE));

        Project projectA = new Project(publicIdGenerator.generate(), tenantA, "My Project", ProjectStatus.ACTIVE);
        projectRepository.saveAndFlush(projectA);

        // Verify same project name allowed in different tenant
        Project projectB = new Project(publicIdGenerator.generate(), tenantB, "My Project", ProjectStatus.ACTIVE);
        projectRepository.saveAndFlush(projectB); 

        assertThat(projectRepository.findByTenantAndNameIgnoreCase(tenantA, "my project")).isPresent();
        assertThat(projectRepository.findByTenantAndNameIgnoreCase(tenantB, "my project")).isPresent();
    }

    @Test
    void testTenantScopedProjectLookupIsolation() {
        // Given
        Tenant tenantA = tenantRepository.save(new Tenant(publicIdGenerator.generate(), "Tenant A", TenantStatus.ACTIVE));
        Tenant tenantB = tenantRepository.save(new Tenant(publicIdGenerator.generate(), "Tenant B", TenantStatus.ACTIVE));

        UUID projectPublicId = publicIdGenerator.generate();
        Project projectA = projectRepository.save(new Project(projectPublicId, tenantA, "Project Alpha", ProjectStatus.ACTIVE));

        // When/Then
        assertThat(projectRepository.findByPublicIdAndTenant(projectPublicId, tenantA)).isPresent();
        assertThat(projectRepository.findByPublicIdAndTenant(projectPublicId, tenantB)).isEmpty();
    }

    @Test
    void testInvalidTenantForeignKeyRejection() {
        // Given
        UUID projectPublicId = publicIdGenerator.generate();
        Timestamp now = Timestamp.from(Instant.now());

        // When/Then
        assertThatThrownBy(() -> {
            entityManager.createNativeQuery("INSERT INTO projects (public_id, tenant_id, name, status, created_by, updated_by, created_at, updated_at) VALUES (:publicId, 999999, 'Bad FK Project', 'ACTIVE', '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', :now, :now)")
                    .setParameter("publicId", projectPublicId)
                    .setParameter("now", now)
                    .executeUpdate();
            entityManager.flush();
        }).isInstanceOf(jakarta.persistence.PersistenceException.class);
    }
}
