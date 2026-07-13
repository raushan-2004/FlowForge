package com.flowforge.worker;

import com.flowforge.worker.config.WorkerProperties;
import com.flowforge.worker.metrics.WorkerMetrics;
import com.flowforge.worker.model.*;
import com.flowforge.worker.repository.*;
import com.flowforge.worker.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
        WorkerServiceApplication.class,
        WorkerServiceIT.TestConfig.class
})
@Testcontainers
@ActiveProfiles("test")
class WorkerServiceIT {

    private static final Logger logger = LoggerFactory.getLogger(WorkerServiceIT.class);

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("flowforge")
            .withUsername("postgres")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9092");
        registry.add("spring.kafka.listener.auto-startup", () -> "false");
    }

    @org.springframework.boot.test.context.TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        public Clock testClock() {
            return new MutableClock(Instant.parse("2026-07-11T12:00:00Z"));
        }
    }

    static class MutableClock extends Clock {
        private Instant instant;

        public MutableClock(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }

        public void advanceBy(Duration duration) {
            this.instant = this.instant.plus(duration);
        }

        public void setInstant(Instant instant) {
            this.instant = instant;
        }
    }

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private ExecutionLeaseRepository leaseRepository;

    @Autowired
    private WorkerRegistrationService registrationService;

    @Autowired
    private WorkerClaimService claimService;

    @Autowired
    private WorkerHeartbeatService heartbeatService;

    @Autowired
    private LeaseRecoveryService recoveryService;

    @Autowired
    private WorkerProperties properties;

    @Autowired
    private Clock clock;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // Stop tracking any leases to prevent test cross-contamination
        heartbeatService.getActiveLeaseTrackers().clear();

        leaseRepository.deleteAll();
        workerRepository.deleteAll();

        // Clear tables we don't own but use for validation (simulating Flyway setup)
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS executions (public_id UUID PRIMARY KEY, current_status VARCHAR(50))");
        jdbcTemplate.execute("DELETE FROM executions");

        properties.setEnabled(true);
        properties.setHeartbeatIntervalMs(1000);
        properties.setLeaseDurationMs(3000);
        properties.setRecoveryIntervalMs(2000);

        // Reset Clock to baseline
        ((MutableClock) clock).setInstant(Instant.parse("2026-07-11T12:00:00Z"));
    }

    @Test
    void testWorkerRegistrationAndShutdown() throws Exception {
        // Run CommandLineRunner to register the worker
        registrationService.run();

        UUID workerId = registrationService.getWorkerPublicId();
        assertThat(workerId).isNotNull();

        Worker worker = workerRepository.findByPublicId(workerId).orElseThrow();
        assertThat(worker.getStatus()).isEqualTo(WorkerStatus.ACTIVE);
        assertThat(worker.getCapabilities()).containsExactlyInAnyOrder(
                WorkerCapability.HTTP,
                WorkerCapability.PLAYWRIGHT,
                WorkerCapability.SHELL,
                WorkerCapability.GPU,
                WorkerCapability.HIGH_MEMORY
        );

        // Shutdown worker
        registrationService.shutdown();

        Worker shutdownWorker = workerRepository.findByPublicId(workerId).orElseThrow();
        assertThat(shutdownWorker.getStatus()).isEqualTo(WorkerStatus.OFFLINE);
    }

    @Test
    void testHeartbeatUpdatesLastHeartbeatAt() throws Exception {
        registrationService.run();
        UUID workerId = registrationService.getWorkerPublicId();

        Worker workerBefore = workerRepository.findByPublicId(workerId).orElseThrow();
        Instant originalHeartbeat = workerBefore.getLastHeartbeatAt();

        // Advance clock by 5 seconds
        ((MutableClock) clock).advanceBy(Duration.ofSeconds(5));

        // Trigger manual heartbeat
        heartbeatService.runHeartbeat();

        Worker workerAfter = workerRepository.findByPublicId(workerId).orElseThrow();
        assertThat(workerAfter.getLastHeartbeatAt()).isAfter(originalHeartbeat);
    }

    @Test
    void testLeaseAcquisitionAndExecutionStatus() throws Exception {
        registrationService.run();
        UUID workerId = registrationService.getWorkerPublicId();

        UUID execId = UUID.randomUUID();
        // Insert a mock execution in the executions table (simulating api-service state)
        jdbcTemplate.update("INSERT INTO executions (public_id, current_status) VALUES (?, ?)", execId, "QUEUED");

        // Attempt claim
        boolean claimed = claimService.claimExecution(execId);
        assertThat(claimed).isTrue();

        // Verify lease record exists
        Optional<ExecutionLease> leaseOpt = leaseRepository.findByExecutionPublicId(execId);
        assertThat(leaseOpt).isPresent();
        ExecutionLease lease = leaseOpt.get();
        assertThat(lease.getWorkerPublicId()).isEqualTo(workerId);
        assertThat(lease.getLeaseVersion()).isEqualTo(1L);

        // Verify execution remains QUEUED (worker-service does not transition it to RUNNING)
        String currentStatus = jdbcTemplate.queryForObject(
                "SELECT current_status FROM executions WHERE public_id = ?", String.class, execId);
        assertThat(currentStatus).isEqualTo("QUEUED");
    }

    @Test
    void testDuplicateLeasePrevention() throws Exception {
        registrationService.run();

        UUID execId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO executions (public_id, current_status) VALUES (?, ?)", execId, "QUEUED");

        // Claim 1 succeeds
        boolean claimed1 = claimService.claimExecution(execId);
        assertThat(claimed1).isTrue();

        // Claim 2 fails due to unique constraint or isPresent check
        try {
            boolean claimed2 = claimService.claimExecution(execId);
            assertThat(claimed2).isFalse();
        } catch (Exception e) {
            logger.info("Claim 2 correctly rolled back with exception: {}", e.getMessage());
        }

        // Verify only 1 lease row exists in db
        assertThat(leaseRepository.findAll()).hasSize(1);
    }

    @Test
    void testConcurrentWorkerClaims() throws Exception {
        registrationService.run();

        UUID execId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO executions (public_id, current_status) VALUES (?, ?)", execId, "QUEUED");

        int threadCount = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);
        AtomicInteger successCounter = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                    boolean ok = claimService.claimExecution(execId);
                    if (ok) {
                        successCounter.incrementAndGet();
                    }
                } catch (Exception e) {
                    logger.debug("Concurrent claim correctly rejected by database unique constraint: {}", e.getMessage());
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        latch.countDown();
        finishLatch.await();
        executor.shutdown();

        // Assert exactly one claim succeeded
        assertThat(successCounter.get()).isEqualTo(1);
        assertThat(leaseRepository.findAll()).hasSize(1);
    }

    @Test
    void testLeaseExpirationAndRecovery() throws Exception {
        registrationService.run();
        UUID workerId1 = registrationService.getWorkerPublicId();

        UUID execId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO executions (public_id, current_status) VALUES (?, ?)", execId, "QUEUED");

        // Worker 1 claims
        claimService.claimExecution(execId);

        // Verify lease version starts at 1
        ExecutionLease leaseBefore = leaseRepository.findByExecutionPublicId(execId).orElseThrow();
        String originalToken = leaseBefore.getLeaseToken();
        assertThat(leaseBefore.getLeaseVersion()).isEqualTo(1L);

        // Advance clock past lease expiry duration (leaseDuration is 3000ms)
        ((MutableClock) clock).advanceBy(Duration.ofMillis(4000));

        // Set up a second worker (simulating recovery worker)
        UUID workerId2 = UUID.randomUUID();
        workerRepository.save(new Worker(
                workerId2, "recovery-worker", "worker2@host", WorkerStatus.ACTIVE, clock.instant(), clock.instant()
        ));

        // Trigger manual recovery using second worker
        boolean reclaimed = recoveryService.reclaimLeaseTransactionally(execId, workerId2, clock.instant());
        assertThat(reclaimed).isTrue();

        // Verify lease is reassigned to Worker 2 and leaseVersion is incremented
        ExecutionLease leaseAfter = leaseRepository.findByExecutionPublicId(execId).orElseThrow();
        assertThat(leaseAfter.getWorkerPublicId()).isEqualTo(workerId2);
        assertThat(leaseAfter.getLeaseVersion()).isEqualTo(2L);
        assertThat(leaseAfter.getLeaseToken()).isNotEqualTo(originalToken);
    }

    @Test
    void testStaleWorkerRenewalRejection() throws Exception {
        registrationService.run();
        UUID workerId = registrationService.getWorkerPublicId();

        UUID execId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO executions (public_id, current_status) VALUES (?, ?)", execId, "QUEUED");

        // Worker claims execution
        claimService.claimExecution(execId);
        ExecutionLease lease = leaseRepository.findByExecutionPublicId(execId).orElseThrow();

        // Set up stale version/token renewal attempt
        boolean renewResult = heartbeatService.renewLeaseTransactionally(
                execId, "invalid-token", lease.getLeaseVersion(), clock.instant().plus(Duration.ofSeconds(10))
        );
        assertThat(renewResult).isFalse();

        boolean renewResult2 = heartbeatService.renewLeaseTransactionally(
                execId, lease.getLeaseToken(), 999L, clock.instant().plus(Duration.ofSeconds(10))
        );
        assertThat(renewResult2).isFalse();
    }
}
