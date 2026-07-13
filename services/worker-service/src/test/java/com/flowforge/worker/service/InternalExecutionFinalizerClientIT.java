package com.flowforge.worker.service;

import com.flowforge.worker.WorkerServiceApplication;
import com.flowforge.worker.config.WorkerProperties;
import com.flowforge.worker.dto.ExecutionResult;
import com.flowforge.worker.dto.InternalExecutionFinalizeRequest;
import com.flowforge.worker.dto.NetworkErrorCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.bind.annotation.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(
        classes = {
                WorkerServiceApplication.class,
                InternalExecutionFinalizerClientIT.TestFinalizerController.class
        },
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Testcontainers
@ActiveProfiles("test")
class InternalExecutionFinalizerClientIT {

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("flowforge_worker")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @LocalServerPort
    private int port;

    @Autowired
    private WorkerProperties properties;

    @Autowired
    private InternalExecutionFinalizerClient client;

    @BeforeEach
    void setup() {
        properties.setApiServiceUrl("http://localhost:" + port);
        properties.setInternalToken("test-service-token-123");
        TestFinalizerController.receivedRequest = null;
        TestFinalizerController.receivedHeader = null;
        TestFinalizerController.shouldFail = false;
    }

    @Test
    void testSuccessfulFinalizationCall() {
        UUID execId = UUID.randomUUID();
        ExecutionResult result = new ExecutionResult(
                execId,
                Instant.now(),
                Instant.now().plusMillis(200),
                Duration.ofMillis(200),
                200,
                Collections.emptyMap(),
                "response",
                512L,
                false,
                "application/json",
                "HTTP/1.1",
                0,
                false,
                null
        );

        client.finalizeExecution(execId, result, "lease-token-xyz");

        assertThat(TestFinalizerController.receivedRequest).isNotNull();
        assertThat(TestFinalizerController.receivedRequest.getLeaseToken()).isEqualTo("lease-token-xyz");
        assertThat(TestFinalizerController.receivedRequest.getStatusCode()).isEqualTo(200);
        assertThat(TestFinalizerController.receivedRequest.getResponseSize()).isEqualTo(512L);
        assertThat(TestFinalizerController.receivedRequest.getBodyTruncated()).isFalse();
        assertThat(TestFinalizerController.receivedRequest.getContentType()).isEqualTo("application/json");
        assertThat(TestFinalizerController.receivedRequest.getNetworkErrorCategory()).isNull();
        assertThat(TestFinalizerController.receivedHeader).isEqualTo("test-service-token-123");
    }

    @Test
    void testNetworkErrorFinalizationCall() {
        UUID execId = UUID.randomUUID();
        ExecutionResult result = new ExecutionResult(
                execId,
                Instant.now(),
                Instant.now().plusMillis(50),
                Duration.ofMillis(50),
                null,
                Collections.emptyMap(),
                null,
                0L,
                false,
                null,
                null,
                0,
                false,
                NetworkErrorCategory.CONNECTION_TIMEOUT
        );

        client.finalizeExecution(execId, result, "lease-token-xyz");

        assertThat(TestFinalizerController.receivedRequest).isNotNull();
        assertThat(TestFinalizerController.receivedRequest.getNetworkErrorCategory()).isEqualTo("CONNECTION_TIMEOUT");
    }

    @Test
    void testFailedResponseThrowsException() {
        TestFinalizerController.shouldFail = true;

        UUID execId = UUID.randomUUID();
        ExecutionResult result = new ExecutionResult(
                execId,
                Instant.now(),
                Instant.now().plusMillis(200),
                Duration.ofMillis(200),
                200,
                Collections.emptyMap(),
                "response",
                512L,
                false,
                "application/json",
                "HTTP/1.1",
                0,
                false,
                null
        );

        assertThatThrownBy(() -> client.finalizeExecution(execId, result, "lease-token-xyz"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to call execution finalization API");
    }

    @RestController
    @RequestMapping("/internal/v1/executions")
    public static class TestFinalizerController {

        static InternalExecutionFinalizeRequest receivedRequest;
        static String receivedHeader;
        static boolean shouldFail;

        @PostMapping("/{executionPublicId}/finalize")
        public ResponseEntity<Void> finalizeExecution(
                @PathVariable("executionPublicId") UUID executionPublicId,
                @RequestHeader("X-Internal-Service-Token") String token,
                @RequestBody InternalExecutionFinalizeRequest request) {

            receivedHeader = token;
            receivedRequest = request;

            if (shouldFail) {
                return ResponseEntity.badRequest().build();
            }

            return ResponseEntity.ok().build();
        }
    }
}
