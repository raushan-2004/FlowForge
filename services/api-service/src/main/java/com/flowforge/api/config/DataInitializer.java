package com.flowforge.api.config;

import com.flowforge.api.model.*;
import com.flowforge.api.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final TenantMembershipRepository tenantMembershipRepository;
    private final ProjectRepository projectRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            UserRepository userRepository,
            TenantRepository tenantRepository,
            TenantMembershipRepository tenantMembershipRepository,
            ProjectRepository projectRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.tenantMembershipRepository = tenantMembershipRepository;
        this.projectRepository = projectRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        initializeUser("login@flowforge.com", UUID.fromString("00000000-0000-0000-0000-000000000002"));
        initializeUser("admin@flowforge.com", UUID.fromString("38f46f65-ae6a-4cd0-b995-222fcfb7fdec"));
    }

    private void initializeUser(String email, UUID userPublicId) {
        // Find or create the user
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = new User(userPublicId, email, passwordEncoder.encode("supersecret123"), UserStatus.ACTIVE);
                    return userRepository.saveAndFlush(newUser);
                });

        // Always update/reset password to ensure it is encoded with the active BCrypt encoder and matches supersecret123
        user.updatePassword(passwordEncoder.encode("supersecret123"));
        userRepository.saveAndFlush(user);

        // Find default tenant or create if not exists
        UUID tenantPublicId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        Tenant tenant = tenantRepository.findByPublicId(tenantPublicId)
                .orElseGet(() -> {
                    Tenant newTenant = new Tenant(tenantPublicId, "Default Tenant", TenantStatus.ACTIVE, user.getPublicId());
                    return tenantRepository.saveAndFlush(newTenant);
                });

        // Ensure user membership in this tenant
        if (!tenantMembershipRepository.findByTenantPublicIdAndUserPublicId(tenantPublicId, user.getPublicId()).isPresent()) {
            TenantMembership membership = new TenantMembership(tenant, user, TenantRole.OWNER);
            tenantMembershipRepository.saveAndFlush(membership);
        }

        // Ensure project exists under tenant
        UUID projectPublicId = UUID.fromString("00000000-0000-0000-0000-000000000003");
        if (!projectRepository.findByPublicId(projectPublicId).isPresent()) {
            Project project = new Project(projectPublicId, tenant, "Main Workspace", ProjectStatus.ACTIVE, user.getPublicId());
            projectRepository.saveAndFlush(project);
        }
    }
}
