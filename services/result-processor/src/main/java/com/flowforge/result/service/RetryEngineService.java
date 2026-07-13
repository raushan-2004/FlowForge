package com.flowforge.result.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowforge.event.dto.ExecutionCompletedPayload;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class RetryEngineService {

    private static final Logger logger = LoggerFactory.getLogger(RetryEngineService.class);

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    @Value("${flowforge.security.internal-token:default-internal-token-secret-key-12345}")
    private String internalToken;

    @Value("${flowforge.result.api-service-url:http://localhost:8080}")
    private String apiServiceUrl;

    public void setApiServiceUrl(String apiServiceUrl) {
        this.apiServiceUrl = apiServiceUrl;
    }

    public void setInternalToken(String internalToken) {
        this.internalToken = internalToken;
    }

    // Metrics
    private final Counter retriesScheduledCounter;
    private final Counter retriesCompletedCounter; 
    private final Counter retriesExhaustedCounter;
    private final Counter retryFailuresCounter;    
    private final Timer retryEvaluationTimer;

    public RetryEngineService(
            JdbcTemplate jdbcTemplate,
            Clock clock,
            HttpClient httpClient,
            ObjectMapper objectMapper,
            MeterRegistry meterRegistry) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;

        this.retriesScheduledCounter = meterRegistry.counter("flowforge.retry.scheduled");
        this.retriesCompletedCounter = meterRegistry.counter("flowforge.retry.completed");
        this.retriesExhaustedCounter = meterRegistry.counter("flowforge.retry.exhausted");
        this.retryFailuresCounter = meterRegistry.counter("flowforge.retry.failed");
        this.retryEvaluationTimer = meterRegistry.timer("flowforge.retry.evaluation.latency");
    }

    @Transactional
    public void processCompletedEvent(ExecutionCompletedPayload event) {
        Timer.Sample sample = Timer.start();
        UUID execId = event.getExecutionPublicId();

        try {
            // 1. Fetch current attempt number and execution status from DB to ensure correct policy context
            Map<String, Object> execMap = jdbcTemplate.queryForMap(
                    "SELECT current_attempt_number, max_attempts, current_status FROM executions WHERE public_id = ?",
                    execId
            );

            int completedAttemptNumber = ((Number) execMap.get("current_attempt_number")).intValue();

            // 2. Idempotency Check: insert into processed_completed_events
            try {
                jdbcTemplate.update(
                        "INSERT INTO processed_completed_events (execution_public_id, attempt_number, processed_at) VALUES (?, ?, ?)",
                        execId,
                        completedAttemptNumber,
                        clock.instant()
                );
            } catch (org.springframework.dao.DuplicateKeyException e) {
                logger.warn("Idempotency check: duplicate completed event received for execution: {}, attempt: {}. Ignoring.",
                        execId, completedAttemptNumber);
                return; // Duplicate, skip processing
            }

            // Notify Workflow Engine
            notifyWorkflowEngine(execId, event.getFinalStatus(), event.getHttpStatus(), event.getNetworkError());

            // 3. Evaluate if retry is needed
            if (!"FAILED".equalsIgnoreCase(event.getFinalStatus())) {
                logger.info("Execution {} completed with terminal status: {}. No retry evaluation needed.", execId, event.getFinalStatus());
                if ("SUCCEEDED".equalsIgnoreCase(event.getFinalStatus()) && completedAttemptNumber > 1) {
                    retriesCompletedCounter.increment(); 
                }
                return;
            }

            // Fetch Job definition details
            Map<String, Object> jobMap = jdbcTemplate.queryForMap(
                    "SELECT j.retry_max_attempts, j.retry_strategy, j.retry_base_delay_seconds " +
                            "FROM jobs j JOIN executions e ON e.job_id = j.id WHERE e.public_id = ?",
                    execId
            );

            Integer retryMaxAttempts = (Integer) jobMap.get("retry_max_attempts");
            String retryStrategy = (String) jobMap.get("retry_strategy");
            Integer retryBaseDelaySeconds = (Integer) jobMap.get("retry_base_delay_seconds");

            if (retryMaxAttempts == null || retryMaxAttempts <= 1) {
                logger.info("Execution {} has no retries configured (max attempts = {}).", execId, retryMaxAttempts);
                return;
            }

            // Check if retry is exhausted
            if (completedAttemptNumber >= retryMaxAttempts) {
                logger.info("Execution {} retry attempts exhausted (completed: {}, max: {}).", execId, completedAttemptNumber, retryMaxAttempts);
                retriesExhaustedCounter.increment();
                return;
            }

            // Check if failure category or status code is retryable
            boolean isRetryable = isFailureRetryable(event.getNetworkError(), event.getHttpStatus());
            if (!isRetryable) {
                logger.info("Execution {} failure is non-retryable (networkError: {}, httpStatus: {}).",
                        execId, event.getNetworkError(), event.getHttpStatus());
                return;
            }

            // Calculate delay
            long baseDelay = retryBaseDelaySeconds != null ? retryBaseDelaySeconds : 5;
            long delaySeconds;
            if ("EXPONENTIAL".equalsIgnoreCase(retryStrategy)) {
                delaySeconds = baseDelay * (long) Math.pow(2, completedAttemptNumber - 1);
            } else {
                delaySeconds = baseDelay;
            }

            Instant nextAttemptAt = clock.instant().plusSeconds(delaySeconds);
            logger.info("Execution {} scheduled for retry attempt {} at {} (delay: {}s, strategy: {})",
                    execId, completedAttemptNumber + 1, nextAttemptAt, delaySeconds, retryStrategy);

            // Call api-service to schedule retry
            callScheduleRetryApi(execId, nextAttemptAt, delaySeconds, retryStrategy);

            retriesScheduledCounter.increment();
            // Record delay metric
            meterRegistry.summary("flowforge.retry.delay.seconds").record(delaySeconds);

        } catch (Exception e) {
            retryFailuresCounter.increment();
            logger.error("Error evaluating retry policy for execution " + execId, e);
            throw e; // Trigger rollback
        } finally {
            sample.stop(retryEvaluationTimer);
        }
    }

    private boolean isFailureRetryable(String networkError, Integer httpStatus) {
        if (networkError != null) {
            return "CONNECTION_TIMEOUT".equals(networkError)
                    || "REQUEST_TIMEOUT".equals(networkError)
                    || "DNS_FAILURE".equals(networkError)
                    || "CONNECTION_REFUSED".equals(networkError);
        }
        if (httpStatus != null) {
            return httpStatus == 408
                    || httpStatus == 429
                    || httpStatus == 500
                    || httpStatus == 502
                    || httpStatus == 503
                    || httpStatus == 504;
        }
        return false;
    }

    private void callScheduleRetryApi(UUID executionPublicId, Instant nextAttemptAt, long delaySeconds, String retryStrategy) {
        String url = apiServiceUrl + "/internal/v1/executions/" + executionPublicId + "/schedule-retry";

        Map<String, Object> body = Map.of(
                "nextAttemptAt", nextAttemptAt.toString(),
                "delaySeconds", delaySeconds,
                "retryStrategy", retryStrategy != null ? retryStrategy : "FIXED"
        );

        try {
            String jsonBody = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .header("X-Internal-Service-Token", internalToken)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IllegalStateException("Schedule retry API returned status " + response.statusCode() + ": " + response.body());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke schedule retry API in api-service", e);
        }
    }

    private void notifyWorkflowEngine(UUID executionPublicId, String finalStatus, Integer httpStatus, String networkError) {
        String url = apiServiceUrl + "/internal/v1/workflows/process-node-completion";

        Map<String, Object> body = new HashMap<>();
        body.put("executionPublicId", executionPublicId.toString());
        body.put("finalStatus", finalStatus);
        body.put("httpStatus", httpStatus);
        body.put("networkError", networkError);

        try {
            String jsonBody = objectMapper.writeValueAsString(body);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .header("X-Internal-Service-Token", internalToken)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IllegalStateException("Process node completion API returned status " + response.statusCode() + ": " + response.body());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke process node completion API in api-service", e);
        }
    }
}
