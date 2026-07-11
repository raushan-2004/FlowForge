package com.flowforge.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowforge.api.BasePersistenceIT;
import com.flowforge.api.dto.*;
import com.flowforge.api.model.*;
import com.flowforge.api.repository.*;
import com.flowforge.api.shared.identity.PublicIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class TenantProjectMembershipIT extends BasePersistenceIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private TenantMembershipRepository membershipRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PublicIdGenerator publicIdGenerator;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private User ownerUser;
    private User adminUser;
    private User developerUser;
    private User viewerUser;
    private User nonMemberUser;
    private User suspendedUser;

    private String ownerToken;
    private String adminToken;
    private String devToken;
    private String viewerToken;
    private String nonMemberToken;
    private String suspendedToken;

    private Tenant tenantA;
    private Tenant tenantB;

    @BeforeEach
    void setUp() {
        membershipRepository.deleteAllInBatch();
        projectRepository.deleteAllInBatch();
        tenantRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        // Create Users
        ownerUser = userRepository.save(new User(publicIdGenerator.generate(), "owner@flowforge.com", "hash", UserStatus.ACTIVE));
        adminUser = userRepository.save(new User(publicIdGenerator.generate(), "admin@flowforge.com", "hash", UserStatus.ACTIVE));
        developerUser = userRepository.save(new User(publicIdGenerator.generate(), "dev@flowforge.com", "hash", UserStatus.ACTIVE));
        viewerUser = userRepository.save(new User(publicIdGenerator.generate(), "viewer@flowforge.com", "hash", UserStatus.ACTIVE));
        nonMemberUser = userRepository.save(new User(publicIdGenerator.generate(), "nonmember@flowforge.com", "hash", UserStatus.ACTIVE));
        suspendedUser = userRepository.save(new User(publicIdGenerator.generate(), "suspended@flowforge.com", "hash", UserStatus.SUSPENDED));

        // Generate Tokens
        ownerToken = jwtService.generateToken(ownerUser.getPublicId());
        adminToken = jwtService.generateToken(adminUser.getPublicId());
        devToken = jwtService.generateToken(developerUser.getPublicId());
        viewerToken = jwtService.generateToken(viewerUser.getPublicId());
        nonMemberToken = jwtService.generateToken(nonMemberUser.getPublicId());
        suspendedToken = jwtService.generateToken(suspendedUser.getPublicId());

        // Create Tenants
        tenantA = tenantRepository.save(new Tenant(publicIdGenerator.generate(), "Tenant A", TenantStatus.ACTIVE, ownerUser.getPublicId()));
        tenantB = tenantRepository.save(new Tenant(publicIdGenerator.generate(), "Tenant B", TenantStatus.ACTIVE, nonMemberUser.getPublicId()));

        // Create Memberships in Tenant A
        membershipRepository.save(new TenantMembership(tenantA, ownerUser, TenantRole.OWNER));
        membershipRepository.save(new TenantMembership(tenantA, adminUser, TenantRole.ADMIN));
        membershipRepository.save(new TenantMembership(tenantA, developerUser, TenantRole.DEVELOPER));
        membershipRepository.save(new TenantMembership(tenantA, viewerUser, TenantRole.VIEWER));

        // Create Membership in Tenant B
        membershipRepository.save(new TenantMembership(tenantB, nonMemberUser, TenantRole.OWNER));
    }

    // --- Tenant APIs Tests ---

    @Test
    void testCreateTenantSuccessAndOwnerMembership() throws Exception {
        TenantRequest request = new TenantRequest("New Enterprise Tenant");

        String response = mockMvc.perform(post("/api/v1/tenants")
                        .header("Authorization", "Bearer " + devToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.publicId").exists())
                .andExpect(jsonPath("$.name", is("New Enterprise Tenant")))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.createdBy", is(developerUser.getPublicId().toString())))
                .andReturn().getResponse().getContentAsString();

        UUID tenantPublicId = UUID.fromString(objectMapper.readTree(response).get("publicId").asText());

        // Verify creator automatically becomes OWNER
        TenantMembership membership = membershipRepository
                .findByTenantPublicIdAndUserPublicId(tenantPublicId, developerUser.getPublicId())
                .orElseThrow();
        assertThat(membership.getRole()).isEqualTo(TenantRole.OWNER);
    }

    @Test
    void testCreateTenantDuplicateNameRejected() throws Exception {
        TenantRequest request = new TenantRequest("Tenant A"); // Case sensitive name duplication check

        mockMvc.perform(post("/api/v1/tenants")
                        .header("Authorization", "Bearer " + devToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("DUPLICATE_TENANT_NAME")));
    }

    @Test
    void testCreateTenantSuspendedUserRejected() throws Exception {
        TenantRequest request = new TenantRequest("Suspended User Tenant");

        mockMvc.perform(post("/api/v1/tenants")
                        .header("Authorization", "Bearer " + suspendedToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("SUSPENDED_ACCOUNT")));
    }

    @Test
    void testTenantGetAndList() throws Exception {
        mockMvc.perform(get("/api/v1/tenants/" + tenantA.getPublicId())
                        .header("Authorization", "Bearer " + devToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Tenant A")));

        mockMvc.perform(get("/api/v1/tenants")
                        .header("Authorization", "Bearer " + devToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void testTenantPatchDetails() throws Exception {
        TenantRequest update = new TenantRequest("Tenant A Renamed", "SUSPENDED");

        // Non-admin/owner developer patch -> gets 403
        mockMvc.perform(patch("/api/v1/tenants/" + tenantA.getPublicId())
                        .header("Authorization", "Bearer " + devToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("MEMBERSHIP_DENIED")));

        // Owner patch -> succeeds
        mockMvc.perform(patch("/api/v1/tenants/" + tenantA.getPublicId())
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Tenant A Renamed")))
                .andExpect(jsonPath("$.status", is("SUSPENDED")))
                .andExpect(jsonPath("$.updatedBy", is(ownerUser.getPublicId().toString())));
    }

    // --- Membership APIs Tests ---

    @Test
    void testMembershipAddAndDuplicateRejection() throws Exception {
        // Dev is not allowed to add members
        MembershipRequest request = new MembershipRequest(nonMemberUser.getPublicId(), TenantRole.DEVELOPER);

        mockMvc.perform(post("/api/v1/tenants/" + tenantA.getPublicId() + "/members")
                        .header("Authorization", "Bearer " + devToken)
                        .header("X-FlowForge-Tenant", tenantA.getPublicId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("MEMBERSHIP_DENIED")));

        // Admin adds member -> succeeds
        mockMvc.perform(post("/api/v1/tenants/" + tenantA.getPublicId() + "/members")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("X-FlowForge-Tenant", tenantA.getPublicId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role", is("DEVELOPER")))
                .andExpect(jsonPath("$.userPublicId", is(nonMemberUser.getPublicId().toString())));

        // Duplicate membership add rejected -> gets 400
        mockMvc.perform(post("/api/v1/tenants/" + tenantA.getPublicId() + "/members")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("X-FlowForge-Tenant", tenantA.getPublicId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")));
    }

    @Test
    void testMembershipRoleUpdateAndLastOwnerProtection() throws Exception {
        // Demote developer role to VIEWER -> succeeds
        MembershipRequest demoteDev = new MembershipRequest(null, TenantRole.VIEWER);
        mockMvc.perform(patch("/api/v1/tenants/" + tenantA.getPublicId() + "/members/" + developerUser.getPublicId())
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-FlowForge-Tenant", tenantA.getPublicId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(demoteDev)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role", is("VIEWER")));

        // Demoting the last OWNER to ADMIN -> blocked
        MembershipRequest demoteOwner = new MembershipRequest(null, TenantRole.ADMIN);
        mockMvc.perform(patch("/api/v1/tenants/" + tenantA.getPublicId() + "/members/" + ownerUser.getPublicId())
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-FlowForge-Tenant", tenantA.getPublicId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(demoteOwner)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")));
    }

    @Test
    void testRemoveMemberAndLastOwnerProtection() throws Exception {
        // Admin removes developer -> succeeds
        mockMvc.perform(delete("/api/v1/tenants/" + tenantA.getPublicId() + "/members/" + developerUser.getPublicId())
                        .header("Authorization", "Bearer " + adminToken)
                        .header("X-FlowForge-Tenant", tenantA.getPublicId().toString()))
                .andExpect(status().isNoContent());

        // Attempting to delete the last owner of tenantA -> blocked
        mockMvc.perform(delete("/api/v1/tenants/" + tenantA.getPublicId() + "/members/" + ownerUser.getPublicId())
                        .header("Authorization", "Bearer " + adminToken)
                        .header("X-FlowForge-Tenant", tenantA.getPublicId().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")));
    }

    // --- Project APIs and Isolation Tests ---

    @Test
    void testProjectCreateRenameArchiveAndScopedLookup() throws Exception {
        ProjectRequest createReq = new ProjectRequest("Alpha Project");

        // Viewer not allowed to create project -> gets 403
        mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + viewerToken)
                        .header("X-FlowForge-Tenant", tenantA.getPublicId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("MEMBERSHIP_DENIED")));

        // Developer creates project -> succeeds
        String response = mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + devToken)
                        .header("X-FlowForge-Tenant", tenantA.getPublicId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Alpha Project")))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.createdBy", is(developerUser.getPublicId().toString())))
                .andReturn().getResponse().getContentAsString();

        UUID projectPublicId = UUID.fromString(objectMapper.readTree(response).get("publicId").asText());

        // Duplicate project name within same tenant -> rejected
        mockMvc.perform(post("/api/v1/projects")
                        .header("Authorization", "Bearer " + devToken)
                        .header("X-FlowForge-Tenant", tenantA.getPublicId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")));

        // Rename project -> succeeds
        ProjectRequest renameReq = new ProjectRequest("Alpha Project Updated");
        mockMvc.perform(patch("/api/v1/projects/" + projectPublicId)
                        .header("Authorization", "Bearer " + devToken)
                        .header("X-FlowForge-Tenant", tenantA.getPublicId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(renameReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Alpha Project Updated")))
                .andExpect(jsonPath("$.updatedBy", is(developerUser.getPublicId().toString())));

        // Tenant-scoped lookup -> project list is size 1
        mockMvc.perform(get("/api/v1/projects")
                        .header("Authorization", "Bearer " + devToken)
                        .header("X-FlowForge-Tenant", tenantA.getPublicId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Alpha Project Updated")));

        // Archive project (DELETE) -> soft deletes
        mockMvc.perform(delete("/api/v1/projects/" + projectPublicId)
                        .header("Authorization", "Bearer " + devToken)
                        .header("X-FlowForge-Tenant", tenantA.getPublicId().toString()))
                .andExpect(status().isNoContent());

        // Verify status transitioned to SUSPENDED (Archived)
        mockMvc.perform(get("/api/v1/projects/" + projectPublicId)
                        .header("Authorization", "Bearer " + devToken)
                        .header("X-FlowForge-Tenant", tenantA.getPublicId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SUSPENDED")));
    }

    @Test
    void testTenantIsolationCrossAccessDenied() throws Exception {
        // Create project in Tenant B
        Project projectInTenantB = projectRepository.save(new Project(
                publicIdGenerator.generate(),
                tenantB,
                "Tenant B Project",
                ProjectStatus.ACTIVE,
                nonMemberUser.getPublicId()
        ));

        // 1. Cross-tenant access denied at API Filter level (User of Tenant A requests Tenant B header)
        mockMvc.perform(get("/api/v1/projects")
                        .header("Authorization", "Bearer " + devToken)
                        .header("X-FlowForge-Tenant", tenantB.getPublicId().toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("MEMBERSHIP_DENIED")));

        // 2. Cross-tenant lookup isolation at Database Query Level (User of Tenant A requests Tenant A header but Tenant B project UUID)
        mockMvc.perform(get("/api/v1/projects/" + projectInTenantB.getPublicId())
                        .header("Authorization", "Bearer " + devToken)
                        .header("X-FlowForge-Tenant", tenantA.getPublicId().toString()))
                .andExpect(status().isNotFound()) // ResourceNotFoundException - enforces isolation
                .andExpect(jsonPath("$.code", is("RESOURCE_NOT_FOUND")));
    }
}
