package com.flowforge.scheduler;

import com.flowforge.scheduler.config.SchedulerProperties;
import com.flowforge.scheduler.metrics.SchedulerMetrics;
import com.flowforge.scheduler.model.*;
import com.flowforge.scheduler.repository.*;
import com.flowforge.scheduler.service.SchedulerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
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
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {
        SchedulerServiceApplication.class,
        SchedulerServiceIT.TestConfig.class
})
@Testcontainers
@ActiveProfiles("test")
class SchedulerServiceIT {

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
            return new MutableClock(Instant.parse("2026-07-11T10:00:00Z"));
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
    private TenantRepository tenantRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private ExecutionRepository executionRepository;

    @Autowired
    private ExecutionAttemptRepository executionAttemptRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private SchedulerService schedulerService;

    @Autowired
    private SchedulerProperties schedulerProperties;

    @Autowired
    private Clock clock;

    private Tenant activeTenant;
    private Project activeProject;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
        executionAttemptRepository.deleteAll();
        executionRepository.deleteAll();
        jobRepository.deleteAll();
        projectRepository.deleteAll();
        tenantRepository.deleteAll();

        // Default configs
        schedulerProperties.setEnabled(true);
        schedulerProperties.setBatchSize(10);
        schedulerProperties.setMaxMissedSchedulesLimit(5);

        // Reset Clock to baseline
        ((MutableClock) clock).setInstant(Instant.parse("2026-07-11T10:00:00Z"));

        // Setup base active Tenant and Project
        activeTenant = tenantRepository.save(new Tenant(UUID.randomUUID(), "Test Tenant", TenantStatus.ACTIVE));
        activeProject = projectRepository.save(new Project(UUID.randomUUID(), activeTenant, "Test Project", ProjectStatus.ACTIVE));
    }

    @Test
    void testManualJobsIgnored() {
        Job job = jobRepository.save(new Job(
                UUID.randomUUID(), activeProject, "Manual Job", true,
                JobScheduleType.MANUAL, null, JobStatus.DRAFT, null, 1
        ));

        schedulerService.pollAndSchedule();

        List<Execution> executions = executionRepository.findAll();
        assertThat(executions).isEmpty();
    }

    @Test
    void testCronJobsScheduled() {
        // Cron expression: runs every 5 minutes (e.g. 10:05, 10:10, etc.)
        Job job = jobRepository.save(new Job(
                UUID.randomUUID(), activeProject, "Cron Job", true,
                JobScheduleType.CRON, "0 */5 * * * *", JobStatus.ACTIVE,
                Instant.parse("2026-07-11T10:00:00Z"), 2
        ));

        // Advance clock past fire time to 10:01:00
        ((MutableClock) clock).advanceBy(Duration.ofMinutes(1));

        schedulerService.pollAndSchedule();

        // Verify nextFireAt is advanced to 10:05:00
        Job updatedJob = jobRepository.findById(job.getId()).orElseThrow();
        assertThat(updatedJob.getNextFireAt()).isEqualTo(Instant.parse("2026-07-11T10:05:00Z"));
        assertThat(updatedJob.getLastScheduledAt()).isEqualTo(Instant.parse("2026-07-11T10:00:00Z"));
        assertThat(updatedJob.getScheduleVersion()).isEqualTo(1L);

        // Verify Execution and Attempt #1
        List<Execution> executions = executionRepository.findAll();
        assertThat(executions).hasSize(1);
        Execution exec = executions.get(0);
        assertThat(exec.getJob().getId()).isEqualTo(job.getId());
        assertThat(exec.getCurrentStatus()).isEqualTo(ExecutionStatus.QUEUED);
        assertThat(exec.getQueuedAt()).isEqualTo(Instant.parse("2026-07-11T10:00:00Z"));
        assertThat(exec.getMaxAttempts()).isEqualTo(2);

        List<ExecutionAttempt> attempts = executionAttemptRepository.findAll();
        assertThat(attempts).hasSize(1);
        ExecutionAttempt attempt = attempts.get(0);
        assertThat(attempt.getAttemptNumber()).isEqualTo(1);
        assertThat(attempt.getStatus()).isEqualTo(AttemptStatus.PENDING);

        // Verify Outbox Event created
        List<OutboxEvent> outboxEvents = outboxEventRepository.findAll();
        assertThat(outboxEvents).hasSize(1);
    }

    @Test
    void testDuplicateSchedulerInstancesDoNotDuplicateExecutions() throws Exception {
        Job job = jobRepository.save(new Job(
                UUID.randomUUID(), activeProject, "Concurrent Job", true,
                JobScheduleType.CRON, "0 */5 * * * *", JobStatus.ACTIVE,
                Instant.parse("2026-07-11T10:00:00Z"), 1
        ));

        ((MutableClock) clock).advanceBy(Duration.ofMinutes(1));

        // Execute concurrent runs using threads
        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);
        AtomicInteger successCounter = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                    schedulerService.pollAndSchedule();
                    successCounter.incrementAndGet();
                } catch (Exception ignored) {
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        // Release latch to trigger parallel scheduler claims
        latch.countDown();
        finishLatch.await();
        executor.shutdown();

        // Verify only 1 Execution is created
        List<Execution> executions = executionRepository.findAll();
        assertThat(executions).hasSize(1);

        // Verify nextFireAt advanced exactly once
        Job updatedJob = jobRepository.findById(job.getId()).orElseThrow();
        assertThat(updatedJob.getNextFireAt()).isEqualTo(Instant.parse("2026-07-11T10:05:00Z"));
        assertThat(updatedJob.getScheduleVersion()).isEqualTo(1L);
    }

    @Test
    void testMissedScheduleHandling() {
        // Cron expression: runs every 1 minute
        Job job = jobRepository.save(new Job(
                UUID.randomUUID(), activeProject, "Missed Job", true,
                JobScheduleType.CRON, "0 */1 * * * *", JobStatus.ACTIVE,
                Instant.parse("2026-07-11T10:00:00Z"), 1
        ));

        // Advance clock by 3 minutes (meaning 3 occurrences missed: 10:00, 10:01, 10:02)
        ((MutableClock) clock).advanceBy(Duration.ofMinutes(3));

        // Polling cycle: catches up all missed occurrences sequentially since limit is 5
        schedulerService.pollAndSchedule();
        Job updatedJob = jobRepository.findById(job.getId()).orElseThrow();
        assertThat(updatedJob.getNextFireAt()).isEqualTo(Instant.parse("2026-07-11T10:04:00Z"));
        assertThat(executionRepository.findAll()).hasSize(4);
    }

    @Test
    void testMissedScheduleOverLimitFastForwards() {
        // Limit is set to 5
        schedulerProperties.setMaxMissedSchedulesLimit(5);

        Job job = jobRepository.save(new Job(
                UUID.randomUUID(), activeProject, "OverLimit Job", true,
                JobScheduleType.CRON, "0 */1 * * * *", JobStatus.ACTIVE,
                Instant.parse("2026-07-11T10:00:00Z"), 1
        ));

        // Advance clock by 10 minutes (10 occurrences missed)
        ((MutableClock) clock).advanceBy(Duration.ofMinutes(10));

        schedulerService.pollAndSchedule();

        // Verify nextFireAt was fast-forwarded to next future tick (10:11:00)
        Job updatedJob = jobRepository.findById(job.getId()).orElseThrow();
        assertThat(updatedJob.getNextFireAt()).isEqualTo(Instant.parse("2026-07-11T10:11:00Z"));
        assertThat(updatedJob.getLastScheduledAt()).isEqualTo(Instant.parse("2026-07-11T10:00:00Z"));

        // Verify no executions created (or only future ones expected)
        assertThat(executionRepository.findAll()).isEmpty();
    }

    @Test
    void testPausedJobsIgnored() {
        Job job = jobRepository.save(new Job(
                UUID.randomUUID(), activeProject, "Paused Job", true,
                JobScheduleType.CRON, "0 */5 * * * *", JobStatus.PAUSED,
                Instant.parse("2026-07-11T10:00:00Z"), 1
        ));

        ((MutableClock) clock).advanceBy(Duration.ofMinutes(6));

        schedulerService.pollAndSchedule();

        assertThat(executionRepository.findAll()).isEmpty();
    }

    @Test
    void testArchivedJobsIgnored() {
        Job job = jobRepository.save(new Job(
                UUID.randomUUID(), activeProject, "Archived Job", true,
                JobScheduleType.CRON, "0 */5 * * * *", JobStatus.ARCHIVED,
                Instant.parse("2026-07-11T10:00:00Z"), 1
        ));

        ((MutableClock) clock).advanceBy(Duration.ofMinutes(6));

        schedulerService.pollAndSchedule();

        assertThat(executionRepository.findAll()).isEmpty();
    }

    @Test
    void testTenantIsolation() {
        // Suspended Tenant and Project
        Tenant suspendedTenant = tenantRepository.save(new Tenant(UUID.randomUUID(), "Suspended Tenant", TenantStatus.SUSPENDED));
        Project suspendedProject = projectRepository.save(new Project(UUID.randomUUID(), suspendedTenant, "Suspended Project", ProjectStatus.ACTIVE));

        Job jobSuspended = jobRepository.save(new Job(
                UUID.randomUUID(), suspendedProject, "Suspended Tenant Job", true,
                JobScheduleType.CRON, "0 */5 * * * *", JobStatus.ACTIVE,
                Instant.parse("2026-07-11T10:00:00Z"), 1
        ));

        // Active Tenant and Project
        Job jobActive = jobRepository.save(new Job(
                UUID.randomUUID(), activeProject, "Active Tenant Job", true,
                JobScheduleType.CRON, "0 */5 * * * *", JobStatus.ACTIVE,
                Instant.parse("2026-07-11T10:00:00Z"), 1
        ));

        ((MutableClock) clock).advanceBy(Duration.ofMinutes(6));

        schedulerService.pollAndSchedule();

        // Suspended Tenant Job must be advanced (so it doesn't get stuck) but NO execution created for it
        Job updatedSuspended = jobRepository.findById(jobSuspended.getId()).orElseThrow();
        assertThat(updatedSuspended.getNextFireAt()).isEqualTo(Instant.parse("2026-07-11T10:10:00Z"));

        // Active Job advanced and HAS execution
        Job updatedActive = jobRepository.findById(jobActive.getId()).orElseThrow();
        assertThat(updatedActive.getNextFireAt()).isEqualTo(Instant.parse("2026-07-11T10:10:00Z"));

        List<Execution> executions = executionRepository.findAll();
        assertThat(executions).hasSize(2);
        for (Execution exec : executions) {
            assertThat(exec.getJob().getId()).isEqualTo(jobActive.getId());
        }
    }
}
