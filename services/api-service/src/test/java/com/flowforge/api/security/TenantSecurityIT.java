package com.flowforge.api.security;

import com.flowforge.api.BasePersistenceIT;
import com.flowforge.api.model.*;
import com.flowforge.api.repository.TenantMembershipRepository;
import com.flowforge.api.repository.TenantRepository;
import com.flowforge.api.repository.UserRepository;
import com.flowforge.api.shared.identity.PublicIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class TenantSecurityIT extends BasePersistenceIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private TenantMembershipRepository membershipRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PublicIdGenerator publicIdGenerator;

    private User user;
    private Tenant tenant;
    private String token;

    @BeforeEach
    void setUp() {
        membershipRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        tenantRepository.deleteAllInBatch();

        user = userRepository.save(new User(publicIdGenerator.generate(), "user@flowforge.com", "hash", UserStatus.ACTIVE));
        token = jwtService.generateToken(user.getPublicId());
        tenant = tenantRepository.save(new Tenant(publicIdGenerator.generate(), "Acme Corp", TenantStatus.ACTIVE));
    }

    @Test
    void testTenantContextMissingHeaderRejectedOnlyOnTenantScopedRoutes() throws Exception {
        mockMvc.perform(get("/api/v1/test-tenant-scoped/projects")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("MISSING_TENANT_HEADER")));

        mockMvc.perform(get("/api/v1/test-authenticated/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("authenticated")));
    }

    @Test
    void testTenantContextMalformedHeaderRejectedSafely() throws Exception {
        mockMvc.perform(get("/api/v1/test-tenant-scoped/projects")
                        .header("Authorization", "Bearer " + token)
                        .header("X-FlowForge-Tenant", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("MALFORMED_TENANT_IDENTIFIER")));
    }

    @Test
    void testNonMemberDenied() throws Exception {
        mockMvc.perform(get("/api/v1/test-tenant-scoped/projects")
                        .header("Authorization", "Bearer " + token)
                        .header("X-FlowForge-Tenant", tenant.getPublicId().toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("MEMBERSHIP_DENIED")));
    }

    @Test
    void testValidMembershipAcceptedWithCapabilities() throws Exception {
        membershipRepository.saveAndFlush(new TenantMembership(tenant, user, TenantRole.DEVELOPER));

        mockMvc.perform(get("/api/v1/test-tenant-scoped/projects")
                        .header("Authorization", "Bearer " + token)
                        .header("X-FlowForge-Tenant", tenant.getPublicId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userPublicId", is(user.getPublicId().toString())))
                .andExpect(jsonPath("$.tenantPublicId", is(tenant.getPublicId().toString())))
                .andExpect(jsonPath("$.role", is("DEVELOPER")))
                .andExpect(jsonPath("$.canManageProjects", is(true)))
                .andExpect(jsonPath("$.canCreateJobs", is(true)))
                .andExpect(jsonPath("$.canViewExecutions", is(true)));
    }

    @Test
    void testSuspendedTenantDenied() throws Exception {
        membershipRepository.saveAndFlush(new TenantMembership(tenant, user, TenantRole.ADMIN));

        tenant.suspend();
        tenantRepository.saveAndFlush(tenant);

        mockMvc.perform(get("/api/v1/test-tenant-scoped/projects")
                        .header("Authorization", "Bearer " + token)
                        .header("X-FlowForge-Tenant", tenant.getPublicId().toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("TENANT_NOT_FOUND"))); 
    }

    @Test
    void testTenantContextCleanupBetweenSequentialRequests() throws Exception {
        membershipRepository.saveAndFlush(new TenantMembership(tenant, user, TenantRole.VIEWER));

        mockMvc.perform(get("/api/v1/test-tenant-scoped/projects")
                        .header("Authorization", "Bearer " + token)
                        .header("X-FlowForge-Tenant", tenant.getPublicId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role", is("VIEWER")));

        mockMvc.perform(get("/api/v1/test-tenant-scoped/projects")
                        .header("X-FlowForge-Tenant", tenant.getPublicId().toString()))
                .andExpect(status().isUnauthorized());
    }
}
