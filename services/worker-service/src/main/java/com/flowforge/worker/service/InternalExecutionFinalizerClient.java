package com.flowforge.worker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flowforge.worker.config.WorkerProperties;
import com.flowforge.worker.dto.ExecutionResult;
import com.flowforge.worker.dto.InternalExecutionFinalizeRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

@Service
public class InternalExecutionFinalizerClient {

    private static final Logger logger = LoggerFactory.getLogger(InternalExecutionFinalizerClient.class);

    private final HttpClient httpClient;
    private final WorkerProperties properties;
    private final ObjectMapper objectMapper;

    public InternalExecutionFinalizerClient(HttpClient httpClient, WorkerProperties properties, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public void finalizeExecution(UUID executionPublicId, ExecutionResult result, String leaseToken) {
        String url = properties.getApiServiceUrl() + "/internal/v1/executions/" + executionPublicId + "/finalize";
        logger.info("Calling api-service to finalize execution {} at URL: {}", executionPublicId, url);

        InternalExecutionFinalizeRequest requestDto = new InternalExecutionFinalizeRequest(
                leaseToken,
                result.getStartedAt(),
                result.getFinishedAt(),
                result.getStatusCode(),
                result.getResponseSize(),
                result.isBodyTruncated(),
                result.getContentType(),
                result.getNetworkErrorCategory() != null ? result.getNetworkErrorCategory().name() : null
        );

        try {
            String jsonBody = objectMapper.writeValueAsString(requestDto);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .header("X-Internal-Service-Token", properties.getInternalToken())
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                logger.error("Failed to finalize execution {} inside api-service. Status: {}, Response: {}",
                        executionPublicId, response.statusCode(), response.body());
                throw new IllegalStateException("Finalization API returned status " + response.statusCode());
            }

            logger.info("Successfully finalized execution {} in api-service.", executionPublicId);
        } catch (Exception e) {
            logger.error("Error calling execution finalization API for " + executionPublicId, e);
            throw new RuntimeException("Failed to call execution finalization API", e);
        }
    }
}
