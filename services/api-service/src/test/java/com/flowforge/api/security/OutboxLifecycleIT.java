package com.flowforge.api.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowforge.api.BasePersistenceIT;
import com.flowforge.api.dto.ExecutionRequest;
import com.flowforge.event.dto.ExecutionCreatedPayload;
import com.flowforge.api.model.*;
import com.flowforge.api.repository.*;
import com.flowforge.api.shared.identity.PublicIdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class OutboxLifecycleIT extends BasePersistenceIT {

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

    @SpyBean
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PublicIdGenerator publicIdGenerator;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private Clock clock;

    @Autowired
    private com.flowforge.api.service.ExecutionService executionService;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private User ownerUser;
    private String ownerToken;
    private Tenant tenantA;
    private Project projectA;
    private Job activeJob;

    @BeforeEach
    void setUp() {
        Mockito.reset(outboxEventRepository);
        outboxEventRepository.deleteAllInBatch();
        attemptRepository.deleteAllInBatch();
        executionRepository.deleteAllInBatch();
        jobRepository.deleteAllInBatch();
        projectRepository.deleteAllInBatch();
        membershipRepository.deleteAllInBatch();
        tenantRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        // Setup base entities
        ownerUser = userRepository.save(new User(publicIdGenerator.generate(), "owner@flowforge.com", "hash", UserStatus.ACTIVE));
        ownerToken = jwtService.generateToken(ownerUser.getPublicId());
        tenantA = tenantRepository.save(new Tenant(publicIdGenerator.generate(), "Tenant A", TenantStatus.ACTIVE, ownerUser.getPublicId()));
        membershipRepository.save(new TenantMembership(tenantA, ownerUser, TenantRole.OWNER));
        projectA = projectRepository.save(new Project(publicIdGenerator.generate(), tenantA, "Project A", ProjectStatus.ACTIVE, ownerUser.getPublicId()));

        activeJob = jobRepository.save(new Job(
                publicIdGenerator.generate(), projectA, "Active Job", "backup task", true,
                JobHttpMethod.POST, "https://api.internal/run", null, null, 30,
                3, "FIXED", 10, JobScheduleType.MANUAL, null, JobStatus.ACTIVE, ownerUser.getPublicId()
        ));
    }

    @Test
    void testExecutionAtomicallyCreatesOutboxEvent() throws Exception {
        ExecutionRequest request = new ExecutionRequest(activeJob.getPublicId(), "MANUAL", null);

        // 1. Trigger execution
        mockMvc.perform(post("/api/v1/executions")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-FlowForge-Tenant", tenantA.getPublicId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // 2. Assert parent and child structures
        assertThat(executionRepository.count()).isEqualTo(1);
        assertThat(attemptRepository.count()).isEqualTo(1);
        assertThat(outboxEventRepository.count()).isEqualTo(1);

        OutboxEvent event = outboxEventRepository.findAll().get(0);
        assertThat(event.getPublicId()).isNotNull();
        assertThat(event.getAggregateType()).isEqualTo(OutboxAggregateType.EXECUTION);
        assertThat(event.getEventType()).isEqualTo("EXECUTION_CREATED");
        assertThat(event.getPayloadVersion()).isEqualTo(1);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);

        // Verify JSON payload deserialization values
        ExecutionCreatedPayload payload = objectMapper.readValue(event.getPayload(), ExecutionCreatedPayload.class);
        assertThat(payload.getJobPublicId()).isEqualTo(activeJob.getPublicId());
        assertThat(payload.getProjectPublicId()).isEqualTo(projectA.getPublicId());
        assertThat(payload.getTenantPublicId()).isEqualTo(tenantA.getPublicId());
        assertThat(payload.getTriggerType()).isEqualTo("MANUAL");
    }

    @Test
    @org.springframework.transaction.annotation.Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void testOutboxRollbackOnServiceFailure() {
        // Configure generator to throw exception on the 3rd call (OutboxEvent creation)
        java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger(0);
        publicIdGenerator.setUuidSupplier(() -> {
            int val = counter.incrementAndGet();
            if (val == 3) {
                throw new RuntimeException("Simulated outbox event generation failure");
            }
            return UUID.randomUUID();
        });

        ExecutionRequest request = new ExecutionRequest(activeJob.getPublicId(), "MANUAL", null);

        // Establish security context
        TenantSecurityContext context = new TenantSecurityContext(
                ownerUser.getPublicId(),
                tenantA.getPublicId(),
                tenantA.getId(),
                TenantRole.OWNER
        );
        TenantSecurityContextHolder.setContext(context);

        try {
            // Trigger execution -> should fail and trigger a rollback
            assertThatThrownBy(() -> {
                executionService.createExecution(request);
            }).isInstanceOf(RuntimeException.class)
              .hasMessageContaining("Simulated outbox event generation failure");
        } finally {
            TenantSecurityContextHolder.clear();
            publicIdGenerator.reset();
            entityManager.clear();
        }

        // Verify TRANSACTION ROLLBACK: parent execution, attempts, and outbox event must be rolled back!
        assertThat(executionRepository.count()).isEqualTo(0);
        assertThat(attemptRepository.count()).isEqualTo(0);
        assertThat(outboxEventRepository.count()).isEqualTo(0);
    }

    @Test
    void testOutboxRepositoryQueries() {
        // Create multiple entries
        OutboxEvent event1 = new OutboxEvent(publicIdGenerator.generate(), OutboxAggregateType.EXECUTION, UUID.randomUUID(), "EXECUTION_CREATED", "{}", 1, clock.instant());
        OutboxEvent event2 = new OutboxEvent(publicIdGenerator.generate(), OutboxAggregateType.EXECUTION, UUID.randomUUID(), "EXECUTION_CREATED", "{}", 1, clock.instant().plusSeconds(1));
        OutboxEvent event3 = new OutboxEvent(publicIdGenerator.generate(), OutboxAggregateType.EXECUTION, UUID.randomUUID(), "EXECUTION_CREATED", "{}", 1, clock.instant().plusSeconds(2));

        event1.markPublished(clock.instant()); // published

        outboxEventRepository.saveAll(List.of(event1, event2, event3));

        // Query PENDING outbox events
        List<OutboxEvent> pending = outboxEventRepository.findTop100ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);
        assertThat(pending).hasSize(2);
        assertThat(pending.get(0).getPublicId()).isEqualTo(event2.getPublicId()); // ordered by createdAt asc
        assertThat(pending.get(1).getPublicId()).isEqualTo(event3.getPublicId());
    }

    @Test
    void testOutboxOptimisticLocking() {
        OutboxEvent event = outboxEventRepository.save(new OutboxEvent(
                publicIdGenerator.generate(), OutboxAggregateType.EXECUTION, UUID.randomUUID(), "EXECUTION_CREATED", "{}", 1, clock.instant()
        ));

        OutboxEvent firstInstance = outboxEventRepository.findById(event.getId()).orElseThrow();
        entityManager.clear();

        OutboxEvent secondInstance = outboxEventRepository.findById(event.getId()).orElseThrow();
        entityManager.clear();

        firstInstance.startPublishing();
        outboxEventRepository.saveAndFlush(firstInstance);

        secondInstance.startPublishing();
        assertThatThrownBy(() -> {
            outboxEventRepository.saveAndFlush(secondInstance);
        }).isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }
}
