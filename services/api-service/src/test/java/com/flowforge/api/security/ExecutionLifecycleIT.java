package com.flowforge.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowforge.api.BasePersistenceIT;
import com.flowforge.api.dto.ApiKeyRequest;
import com.flowforge.api.dto.ApiKeyCreateResponse;
import com.flowforge.api.dto.ExecutionRequest;
import com.flowforge.api.dto.ExecutionResponse;
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

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class ExecutionLifecycleIT extends BasePersistenceIT {

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
    private ExecutionRepository executionRepository;

    @Autowired
    private ExecutionAttemptRepository attemptRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PublicIdGenerator publicIdGenerator;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Clock clock;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private User ownerUser;
    private User viewerUser;

    private String ownerToken;
    private String viewerToken;

    private Tenant tenantA;
    private Tenant tenantB;

    private Project projectA;
    private Project projectB;

    private Job activeJob;
    private Job pausedJob;
    private Job archivedJob;
    private Job jobInTenantB;

    @BeforeEach
    void setUp() {
        attemptRepository.deleteAllInBatch();
        executionRepository.deleteAllInBatch();
        jobRepository.deleteAllInBatch();
        projectRepository.deleteAllInBatch();
        membershipRepository.deleteAllInBatch();
        tenantRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        // Users
        ownerUser = userRepository.save(new User(publicIdGenerator.generate(), "owner@flowforge.com", "hash", UserStatus.ACTIVE));
        viewerUser = userRepository.save(new User(publicIdGenerator.generate(), "viewer@flowforge.com", "hash", UserStatus.ACTIVE));

        ownerToken = jwtService.generateToken(ownerUser.getPublicId());
        viewerToken = jwtService.generateToken(viewerUser.getPublicId());

        // Tenants
        tenantA = tenantRepository.save(new Tenant(publicIdGenerator.generate(), "Tenant A", TenantStatus.ACTIVE, ownerUser.getPublicId()));
        tenantB = tenantRepository.save(new Tenant(publicIdGenerator.generate(), "Tenant B", TenantStatus.ACTIVE, ownerUser.getPublicId()));

        // Memberships
        membershipRepository.save(new TenantMembership(tenantA, ownerUser, TenantRole.OWNER));
        membershipRepository.save(new TenantMembership(tenantA, viewerUser, TenantRole.VIEWER));

        // Projects
        projectA = projectRepository.save(new Project(publicIdGenerator.generate(), tenantA, "Project A", ProjectStatus.ACTIVE, ownerUser.getPublicId()));
        projectB = projectRepository.save(new Project(publicIdGenerator.generate(), tenantB, "Project B", ProjectStatus.ACTIVE, ownerUser.getPublicId()));

        // Jobs in Tenant A
        activeJob = jobRepository.save(new Job(
                publicIdGenerator.generate(), projectA, "Active Job", "runs regularly", true,
                JobHttpMethod.POST, "https://api.internal/run", null, null, 30,
                3, "FIXED", 10, JobScheduleType.MANUAL, null, JobStatus.ACTIVE, ownerUser.getPublicId()
        ));

        pausedJob = jobRepository.save(new Job(
                publicIdGenerator.generate(), projectA, "Paused Job", "stopped for now", false,
                JobHttpMethod.POST, "https://api.internal/run", null, null, 30,
                null, null, null, JobScheduleType.MANUAL, null, JobStatus.PAUSED, ownerUser.getPublicId()
        ));

        archivedJob = jobRepository.save(new Job(
                publicIdGenerator.generate(), projectA, "Archived Job", "old definition", false,
                JobHttpMethod.POST, "https://api.internal/run", null, null, 30,
                null, null, null, JobScheduleType.MANUAL, null, JobStatus.ARCHIVED, ownerUser.getPublicId()
        ));

        // Job in Tenant B
        jobInTenantB = jobRepository.save(new Job(
                publicIdGenerator.generate(), projectB, "Tenant B Job", "runs in B", true,
                JobHttpMethod.POST, "https://api.internal/run", null, null, 30,
                null, null, null, JobScheduleType.MANUAL, null, JobStatus.ACTIVE, ownerUser.getPublicId()
        ));
    }

    @Test
    void testExecutionSubmissionAndValidation() throws Exception {
        ExecutionRequest request = new ExecutionRequest(activeJob.getPublicId(), "MANUAL", null);

        // 1. Successful creation
        String response = mockMvc.perform(post("/api/v1/executions")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-FlowForge-Tenant", tenantA.getPublicId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.publicId").exists())
                .andExpect(jsonPath("$.jobPublicId", is(activeJob.getPublicId().toString())))
                .andExpect(jsonPath("$.currentStatus", is("QUEUED")))
                .andExpect(jsonPath("$.triggerType", is("MANUAL")))
                .andExpect(jsonPath("$.currentAttemptNumber", is(1)))
                .andExpect(jsonPath("$.maxAttempts", is(4))) // 3 retries + 1 primary attempt = 4 max attempts
                .andReturn().getResponse().getContentAsString();

        UUID executionId = UUID.fromString(objectMapper.readTree(response).get("publicId").asText());

        // 2. Fetch Attempts list
        mockMvc.perform(get("/api/v1/executions/" + executionId + "/attempts")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-FlowForge-Tenant", tenantA.getPublicId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].attemptNumber", is(1)))
                .andExpect(jsonPath("$[0].status", is("PENDING")));

        // 3. Rejected: Paused Job execution
        request.setJobPublicId(pausedJob.getPublicId());
        mockMvc.perform(post("/api/v1/executions")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-FlowForge-Tenant", tenantA.getPublicId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")));

        // 4. Rejected: Archived Job execution
        request.setJobPublicId(archivedJob.getPublicId());
        mockMvc.perform(post("/api/v1/executions")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-FlowForge-Tenant", tenantA.getPublicId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")));
    }

    @Test
    void testExecutionStateTransitions() {
        Execution execution = new Execution(publicIdGenerator.generate(), activeJob, ExecutionTriggerType.MANUAL, "source", clock.instant(), 3);

        // Transition: QUEUED -> RUNNING
        execution.start(clock.instant());
        assertThat(execution.getCurrentStatus()).isEqualTo(ExecutionStatus.RUNNING);

        // Transition: RUNNING -> SUCCEEDED
        execution.succeed(clock.instant());
        assertThat(execution.getCurrentStatus()).isEqualTo(ExecutionStatus.SUCCEEDED);

        // Illegal: SUCCEEDED -> RUNNING
        assertThatThrownBy(() -> {
            execution.start(clock.instant());
        }).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void testExecutionTenantAndProjectIsolation() throws Exception {
        // Attempting to execute Tenant B's job under Tenant A context -> rejected with 404 (job not found)
        ExecutionRequest request = new ExecutionRequest(jobInTenantB.getPublicId(), "MANUAL", null);
        mockMvc.perform(post("/api/v1/executions")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-FlowForge-Tenant", tenantA.getPublicId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("RESOURCE_NOT_FOUND")));
    }

    @Test
    void testExecutionImmutableHistory() throws Exception {
        ExecutionRequest createReq = new ExecutionRequest(activeJob.getPublicId(), "MANUAL", null);
        String response = mockMvc.perform(post("/api/v1/executions")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-FlowForge-Tenant", tenantA.getPublicId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andReturn().getResponse().getContentAsString();

        UUID executionId = UUID.fromString(objectMapper.readTree(response).get("publicId").asText());

        // Attempting to modify (PATCH) -> 405 Method Not Allowed (no route defined)
        mockMvc.perform(patch("/api/v1/executions/" + executionId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-FlowForge-Tenant", tenantA.getPublicId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isMethodNotAllowed());

        // Attempting to delete (DELETE) -> 405 Method Not Allowed (no route defined)
        mockMvc.perform(delete("/api/v1/executions/" + executionId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-FlowForge-Tenant", tenantA.getPublicId().toString()))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void testExecutionAuthorizationViewerLimits() throws Exception {
        ExecutionRequest request = new ExecutionRequest(activeJob.getPublicId(), "MANUAL", null);

        // Viewer token trying to create execution gets 403 Forbidden (MEMBERSHIP_DENIED)
        mockMvc.perform(post("/api/v1/executions")
                        .header("Authorization", "Bearer " + viewerToken)
                        .header("X-FlowForge-Tenant", tenantA.getPublicId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code", is("MEMBERSHIP_DENIED")));
    }

    @Test
    void testExecutionAutomationAuthorization() throws Exception {
        // 1. Create API key for Project A
        String keyRes = mockMvc.perform(post("/api/v1/projects/" + projectA.getPublicId() + "/keys")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-FlowForge-Tenant", tenantA.getPublicId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andReturn().getResponse().getContentAsString();

        String plaintextKey = objectMapper.readValue(keyRes, ApiKeyCreateResponse.class).getToken();

        // 2. Submit execution using API Key header -> must succeed
        ExecutionRequest request = new ExecutionRequest(activeJob.getPublicId(), "API_KEY", null);
        mockMvc.perform(post("/api/v1/executions")
                        .header("X-FlowForge-Api-Key", plaintextKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.triggerType", is("API_KEY")))
                .andExpect(jsonPath("$.triggerSource").exists());
    }

    @Test
    void testExecutionOptimisticLocking() {
        Execution execution = executionRepository.save(new Execution(
                publicIdGenerator.generate(), activeJob, ExecutionTriggerType.MANUAL, "source", clock.instant(), 3
        ));

        Execution firstInstance = executionRepository.findById(execution.getId()).orElseThrow();
        entityManager.clear();

        Execution secondInstance = executionRepository.findById(execution.getId()).orElseThrow();
        entityManager.clear();

        firstInstance.start(clock.instant());
        executionRepository.saveAndFlush(firstInstance);

        secondInstance.start(clock.instant());
        assertThatThrownBy(() -> {
            executionRepository.saveAndFlush(secondInstance);
        }).isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }
}
