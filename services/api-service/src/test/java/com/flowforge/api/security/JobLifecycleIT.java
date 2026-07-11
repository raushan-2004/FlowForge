package com.flowforge.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowforge.api.BasePersistenceIT;
import com.flowforge.api.dto.JobRequest;
import com.flowforge.api.dto.JobResponse;
import com.flowforge.api.model.*;
import com.flowforge.api.repository.*;
import com.flowforge.api.shared.identity.PublicIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class JobLifecycleIT extends BasePersistenceIT {

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
    private JobRepository jobRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PublicIdGenerator publicIdGenerator;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private User ownerUser;
    private User viewerUser;

    private String ownerToken;
    private String viewerToken;

    private Tenant tenantA;
    private Tenant tenantB;

    private Project projectA1;
    private Project projectA2;
    private Project projectB;

    @BeforeEach
    void setUp() {
        jobRepository.deleteAllInBatch();
        projectRepository.deleteAllInBatch();
        membershipRepository.deleteAllInBatch();
        tenantRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        // Create Users
        ownerUser = userRepository.save(new User(publicIdGenerator.generate(), "owner@flowforge.com", "hash", UserStatus.ACTIVE));
        viewerUser = userRepository.save(new User(publicIdGenerator.generate(), "viewer@flowforge.com", "hash", UserStatus.ACTIVE));

        ownerToken = jwtService.generateToken(ownerUser.getPublicId());
        viewerToken = jwtService.generateToken(viewerUser.getPublicId());

        // Create Tenants
        tenantA = tenantRepository.save(new Tenant(publicIdGenerator.generate(), "Tenant A", TenantStatus.ACTIVE, ownerUser.getPublicId()));
        tenantB = tenantRepository.save(new Tenant(publicIdGenerator.generate(), "Tenant B", TenantStatus.ACTIVE, ownerUser.getPublicId()));

        // Create Memberships
        membershipRepository.save(new TenantMembership(tenantA, ownerUser, TenantRole.OWNER));
        membershipRepository.save(new TenantMembership(tenantA, viewerUser, TenantRole.VIEWER));

        // Create Projects
        projectA1 = projectRepository.save(new Project(publicIdGenerator.generate(), tenantA, "Project A1", ProjectStatus.ACTIVE, ownerUser.getPublicId()));
        projectA2 = projectRepository.save(new Project(publicIdGenerator.generate(), tenantA, "Project A2", ProjectStatus.ACTIVE, ownerUser.getPublicId()));
        projectB = projectRepository.save(new Project(publicIdGenerator.generate(), tenantB, "Project B", ProjectStatus.ACTIVE, ownerUser.getPublicId()));
    }

    @Test
    void testJobCreationAndValidation() throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "Value");

        JobRequest request = new JobRequest(
                projectA1.getPublicId(),
                "Backup Job",
                "Performs night backup",
                true,
                "POST",
                "https://api.internal/backup",
                headers,
                "{\"force\": true}",
                60,
                3,
                "EXPONENTIAL",
                5,
                "CRON",
                "0 0 2 * * ?" // Valid daily 2 AM cron
        );

        // 1. Successful Job Creation
        String response = mockMvc.perform(post("/api/v1/jobs")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-FlowForge-Tenant", tenantA.getPublicId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.publicId").exists())
                .andExpect(jsonPath("$.name", is("Backup Job")))
                .andExpect(jsonPath("$.status", is("ACTIVE")))
                .andExpect(jsonPath("$.httpMethod", is("POST")))
                .andExpect(jsonPath("$.targetUrl", is("https://api.internal/backup")))
                .andExpect(jsonPath("$.requestHeaders['content-type']", is("application/json"))) // normalized
                .andExpect(jsonPath("$.timeoutSeconds", is(60)))
                .andReturn().getResponse().getContentAsString();

        UUID jobPublicId = UUID.fromString(objectMapper.readTree(response).get("publicId").asText());

        // 2. Duplicate Job Name in same Project -> Rejected
        mockMvc.perform(post("/api/v1/jobs")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-FlowForge-Tenant", tenantA.getPublicId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")));

        // 3. Same Job Name in DIFFERENT Project -> Allowed
        request.setProjectPublicId(projectA2.getPublicId());
        mockMvc.perform(post("/api/v1/jobs")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-FlowForge-Tenant", tenantA.getPublicId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Backup Job")));

        // 4. Invalid Target URL Scheme -> Rejected
        request.setTargetUrl("ftp://invalid-scheme.com/backup");
        mockMvc.perform(post("/api/v1/jobs")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-FlowForge-Tenant", tenantA.getPublicId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")));

        // 5. Unsupported HTTP Method -> Rejected
        request.setTargetUrl("https://api.internal/backup");
        request.setHttpMethod("CONNECT");
        mockMvc.perform(post("/api/v1/jobs")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-FlowForge-Tenant", tenantA.getPublicId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")));

        // 6. Timeout out of bounds -> Rejected
        request.setHttpMethod("POST");
        request.setTimeoutSeconds(400);
        mockMvc.perform(post("/api/v1/jobs")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-FlowForge-Tenant", tenantA.getPublicId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")));

        // 7. Invalid Cron Expression syntax -> Rejected
        request.setTimeoutSeconds(60);
        request.setCronExpression("invalid-cron-exp");
        mockMvc.perform(post("/api/v1/jobs")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-FlowForge-Tenant", tenantA.getPublicId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")));
    }

    @Test
    void testJobGetListUpdateAndArchive() throws Exception {
        // Create standard active job
        JobRequest createReq = new JobRequest(
                projectA1.getPublicId(),
                "Report Generation",
                "Builds weekly stats report",
                true,
                "GET",
                "https://api.internal/report",
                null,
                null,
                30,
                null,
                null,
                null,
                "MANUAL",
                null
        );

        String createRes = mockMvc.perform(post("/api/v1/jobs")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-FlowForge-Tenant", tenantA.getPublicId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andReturn().getResponse().getContentAsString();

        UUID jobId = UUID.fromString(objectMapper.readTree(createRes).get("publicId").asText());

        // 1. Get Job by ID
        mockMvc.perform(get("/api/v1/jobs/" + jobId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-FlowForge-Tenant", tenantA.getPublicId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Report Generation")))
                .andExpect(jsonPath("$.status", is("DRAFT"))); // Manual type defaults to DRAFT

        // 2. List Jobs
        mockMvc.perform(get("/api/v1/jobs")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-FlowForge-Tenant", tenantA.getPublicId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Report Generation")));

        // 3. Update Job Details
        JobRequest updateReq = new JobRequest();
        updateReq.setName("Report Generation Updated");
        updateReq.setTimeoutSeconds(45);

        mockMvc.perform(patch("/api/v1/jobs/" + jobId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-FlowForge-Tenant", tenantA.getPublicId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Report Generation Updated")))
                .andExpect(jsonPath("$.timeoutSeconds", is(45)));

        // 4. Archive Job (DELETE)
        mockMvc.perform(delete("/api/v1/jobs/" + jobId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-FlowForge-Tenant", tenantA.getPublicId().toString()))
                .andExpect(status().isNoContent());

        // Verify it is excluded from normal list
        mockMvc.perform(get("/api/v1/jobs")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-FlowForge-Tenant", tenantA.getPublicId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void testJobTenantAndProjectIsolation() throws Exception {
        // Create job in Tenant B (Project B)
        Job jobInTenantB = jobRepository.save(new Job(
                publicIdGenerator.generate(),
                projectB,
                "Tenant B Job",
                "Internal task",
                true,
                JobHttpMethod.GET,
                "https://api.internal/task",
                null,
                null,
                30,
                null,
                null,
                null,
                JobScheduleType.MANUAL,
                null,
                JobStatus.DRAFT,
                ownerUser.getPublicId()
        ));

        // 1. Cross-tenant access denied via header matching (User has no membership in Tenant B)
        mockMvc.perform(get("/api/v1/jobs/" + jobInTenantB.getPublicId())
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-FlowForge-Tenant", tenantB.getPublicId().toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("MEMBERSHIP_DENIED")));

        // 2. Cross-tenant data isolation via repository query bounds (Active tenant is Tenant A)
        mockMvc.perform(get("/api/v1/jobs/" + jobInTenantB.getPublicId())
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-FlowForge-Tenant", tenantA.getPublicId().toString()))
                .andExpect(status().isNotFound()) // ResourceNotFoundException - query scopes strictly
                .andExpect(jsonPath("$.code", is("RESOURCE_NOT_FOUND")));
    }

    @Test
    void testJobAuthorizationViewerLimits() throws Exception {
        JobRequest createReq = new JobRequest(
                projectA1.getPublicId(),
                "Viewer Job",
                "Metadata only",
                true,
                "GET",
                "https://api.internal/task",
                null,
                null,
                30,
                null,
                null,
                null,
                "MANUAL",
                null
        );

        // Viewer not allowed to POST new job -> gets 403 Forbidden
        mockMvc.perform(post("/api/v1/jobs")
                        .header("Authorization", "Bearer " + viewerToken)
                        .header("X-FlowForge-Tenant", tenantA.getPublicId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("MEMBERSHIP_DENIED")));
    }

    @Test
    void testJobOptimisticLockingBehavior() {
        Job job = jobRepository.save(new Job(
                publicIdGenerator.generate(),
                projectA1,
                "Concurrent Job",
                "Tests locking",
                true,
                JobHttpMethod.GET,
                "https://api.internal/task",
                null,
                null,
                30,
                null,
                null,
                null,
                JobScheduleType.MANUAL,
                null,
                JobStatus.DRAFT,
                ownerUser.getPublicId()
        ));

        // Load two instances of the same database row
        Job firstInstance = jobRepository.findById(job.getId()).orElseThrow();
        entityManager.clear(); // Detach firstInstance

        Job secondInstance = jobRepository.findById(job.getId()).orElseThrow();
        entityManager.clear(); // Detach secondInstance

        // Update details and save first instance (increments version)
        firstInstance.updateDetails("Job Updated Once", null, true, null, null, null, null, 30, null, null, null, null, null, null, ownerUser.getPublicId(), firstInstance.getCreatedAt());
        jobRepository.saveAndFlush(firstInstance);

        // Update details and save second instance -> fails on version mismatch
        secondInstance.updateDetails("Job Updated Twice", null, true, null, null, null, null, 30, null, null, null, null, null, null, ownerUser.getPublicId(), secondInstance.getCreatedAt());
        
        assertThatThrownBy(() -> {
            jobRepository.saveAndFlush(secondInstance);
        }).isInstanceOf(org.springframework.orm.ObjectOptimisticLockingFailureException.class);
    }
}
