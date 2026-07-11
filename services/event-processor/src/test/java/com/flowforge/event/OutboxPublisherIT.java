package com.flowforge.event;

import com.flowforge.event.config.OutboxProperties;
import com.flowforge.event.model.OutboxAggregateType;
import com.flowforge.event.model.OutboxEvent;
import com.flowforge.event.model.OutboxStatus;
import com.flowforge.event.repository.OutboxEventRepository;
import com.flowforge.event.scheduler.OutboxEventScheduler;
import com.flowforge.event.service.OutboxPublisherService;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.redpanda.RedpandaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Clock;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class OutboxPublisherIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("flowforge")
            .withUsername("test")
            .withPassword("test");

    @Container
    static RedpandaContainer redpanda = new RedpandaContainer(
            DockerImageName.parse("docker.io/redpandadata/redpanda:v24.1.2")
                    .asCompatibleSubstituteFor("docker.redpanda.com/redpandadata/redpanda")
    );

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");

        registry.add("spring.kafka.bootstrap-servers", redpanda::getBootstrapServers);
        registry.add("spring.kafka.listener.auto-startup", () -> "false");
    }

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private OutboxPublisherService outboxPublisherService;

    @Autowired
    private OutboxEventScheduler outboxEventScheduler;

    @Autowired
    private OutboxProperties outboxProperties;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @org.springframework.boot.test.context.TestConfiguration
    static class TestConfig {
        @org.springframework.context.annotation.Bean
        @org.springframework.context.annotation.Primary
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public KafkaTemplate<String, String> testKafkaTemplate(org.springframework.kafka.core.ProducerFactory<?, ?> pf) {
            return Mockito.spy(new KafkaTemplate((org.springframework.kafka.core.ProducerFactory) pf));
        }
    }

    @Autowired
    private Clock clock;

    private Consumer<String, String> consumer;

    @BeforeEach
    void setUp() {
        Mockito.reset(kafkaTemplate);
        outboxEventRepository.deleteAllInBatch();
        outboxProperties.setEnabled(false); // Disable scheduler auto-run

        // Configure Kafka Consumer
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(redpanda.getBootstrapServers(), "test-group", "true");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        DefaultKafkaConsumerFactory<String, String> cf = new DefaultKafkaConsumerFactory<>(consumerProps);
        consumer = cf.createConsumer();
        consumer.subscribe(Collections.singletonList("execution-created"));
    }

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    void testSuccessfulEventPublishing() throws Exception {
        UUID executionId = UUID.randomUUID();
        UUID outboxId = UUID.randomUUID();
        String payload = "{\"executionPublicId\":\"" + executionId + "\"}";

        OutboxEvent event = new OutboxEvent(
                outboxId, OutboxAggregateType.EXECUTION, executionId,
                "EXECUTION_CREATED", payload, 1, clock.instant()
        );
        outboxEventRepository.saveAndFlush(event);

        // Run publisher poll manually
        outboxProperties.setEnabled(true);
        outboxEventScheduler.pollAndPublish();
        outboxProperties.setEnabled(false);

        // Assert DB status
        OutboxEvent updated = outboxEventRepository.findById(event.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(updated.getPublishedAt()).isNotNull();
        assertThat(updated.getAttemptCount()).isEqualTo(0);

        // Assert Kafka record received
        ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(consumer, "execution-created", java.time.Duration.ofSeconds(5));
        assertThat(record.key()).isEqualTo(executionId.toString());
        assertThat(record.value()).isEqualTo(payload);
    }

    @Test
    void testFailedPublishingIncrementsAttemptAndAppliesBackoff() {
        UUID executionId = UUID.randomUUID();
        String payload = "{\"executionPublicId\":\"" + executionId + "\"}";

        OutboxEvent event = new OutboxEvent(
                UUID.randomUUID(), OutboxAggregateType.EXECUTION, executionId,
                "EXECUTION_CREATED", payload, 1, clock.instant()
        );
        outboxEventRepository.saveAndFlush(event);

        // Mock KafkaTemplate send to fail
        doReturn(CompletableFuture.failedFuture(new RuntimeException("Simulated broker network failure")))
                .when(kafkaTemplate).send(anyString(), anyString(), anyString());

        // Run publisher poll manually
        outboxProperties.setEnabled(true);
        outboxEventScheduler.pollAndPublish();
        outboxProperties.setEnabled(false);

        // Assert DB status is FAILED, attempt is incremented, and nextAttemptAt is set
        OutboxEvent updated = outboxEventRepository.findById(event.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(updated.getAttemptCount()).isEqualTo(1);
        assertThat(updated.getLastError()).contains("Simulated broker network failure");
        assertThat(updated.getNextAttemptAt()).isNotNull();

        // Check delay: base backoff (2s) * 2^1 = 4 seconds
        long diffSeconds = updated.getNextAttemptAt().getEpochSecond() - clock.instant().getEpochSecond();
        assertThat(diffSeconds).isEqualTo(4);
    }

    @Test
    void testDeadTransitionAfterMaxRetries() {
        UUID executionId = UUID.randomUUID();
        String payload = "{\"executionPublicId\":\"" + executionId + "\"}";

        // Setup max retries properties to 3
        outboxProperties.setMaxRetries(3);

        // Event already failed twice
        OutboxEvent event = new OutboxEvent(
                UUID.randomUUID(), OutboxAggregateType.EXECUTION, executionId,
                "EXECUTION_CREATED", payload, 1, clock.instant()
        );
        event.setStatus(OutboxStatus.FAILED);
        event.setAttemptCount(2);
        event.setNextAttemptAt(clock.instant().minusSeconds(10)); // Set past timestamp to trigger claim
        outboxEventRepository.saveAndFlush(event);

        // Mock KafkaTemplate send to fail
        doReturn(CompletableFuture.failedFuture(new RuntimeException("Simulated broker network failure")))
                .when(kafkaTemplate).send(anyString(), anyString(), anyString());

        // Run publisher poll manually
        outboxProperties.setEnabled(true);
        outboxEventScheduler.pollAndPublish();
        outboxProperties.setEnabled(false);

        // Assert DB status becomes DEAD
        OutboxEvent updated = outboxEventRepository.findById(event.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(OutboxStatus.DEAD);
        assertThat(updated.getAttemptCount()).isEqualTo(3); // reaches limit
        assertThat(updated.getNextAttemptAt()).isNull();
    }

    @Test
    void testConcurrentClaimingPreventsDuplicatePublishing() throws Exception {
        // Insert 10 events
        for (int i = 0; i < 10; i++) {
            outboxEventRepository.save(new OutboxEvent(
                    UUID.randomUUID(), OutboxAggregateType.EXECUTION, UUID.randomUUID(),
                    "EXECUTION_CREATED", "{}", 1, clock.instant()
            ));
        }
        outboxEventRepository.flush();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch latch = new CountDownLatch(1);

        Callable<List<OutboxEvent>> task = () -> {
            latch.await(); // Synchronized start
            return outboxPublisherService.claimBatch(clock.instant(), 5);
        };

        Future<List<OutboxEvent>> future1 = executor.submit(task);
        Future<List<OutboxEvent>> future2 = executor.submit(task);

        latch.countDown(); // Go!

        List<OutboxEvent> claimed1 = future1.get();
        List<OutboxEvent> claimed2 = future2.get();

        executor.shutdown();

        // Verify total claimed is exactly 10 and they are completely mutually exclusive (no overlap!)
        assertThat(claimed1).hasSize(5);
        assertThat(claimed2).hasSize(5);

        Set<Long> ids1 = new HashSet<>();
        for (OutboxEvent e : claimed1) ids1.add(e.getId());

        Set<Long> ids2 = new HashSet<>();
        for (OutboxEvent e : claimed2) ids2.add(e.getId());

        // Set intersection must be empty
        ids1.retainAll(ids2);
        assertThat(ids1).isEmpty();
    }
}
