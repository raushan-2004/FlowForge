package com.flowforge.worker.service;

import com.flowforge.worker.config.WorkerProperties;
import com.flowforge.worker.dto.ExecutionResult;
import com.flowforge.worker.dto.JobDefinitionDto;
import com.flowforge.worker.dto.NetworkErrorCategory;
import com.flowforge.worker.repository.ExecutionLeaseRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class HttpExecutionEngine {

    private static final Logger logger = LoggerFactory.getLogger(HttpExecutionEngine.class);

    private final HttpClient httpClient;
    private final SsrfValidator ssrfValidator;
    private final ExecutionLeaseRepository leaseRepository;
    private final WorkerRegistrationService registrationService;
    private final Clock clock;

    @Value("${flowforge.worker.max-response-body-size-bytes:1048576}")
    private long maxResponseBodySize;

    // Micrometer Metrics
    private final Counter requestCounter;
    private final Counter successCounter;
    private final Counter failureCounter;
    private final Counter timeoutCounter;
    private final Counter ssrfRejectionsCounter;
    private final DistributionSummary responseSizeSummary;
    private final DistributionSummary redirectSummary;
    private final Timer durationTimer;

    public HttpExecutionEngine(
            HttpClient httpClient,
            SsrfValidator ssrfValidator,
            ExecutionLeaseRepository leaseRepository,
            WorkerRegistrationService registrationService,
            Clock clock,
            MeterRegistry meterRegistry) {
        this.httpClient = httpClient;
        this.ssrfValidator = ssrfValidator;
        this.leaseRepository = leaseRepository;
        this.registrationService = registrationService;
        this.clock = clock;

        // Initialize Metrics
        this.requestCounter = meterRegistry.counter("flowforge.worker.execution.requests");
        this.successCounter = meterRegistry.counter("flowforge.worker.execution.success");
        this.failureCounter = meterRegistry.counter("flowforge.worker.execution.failure");
        this.timeoutCounter = meterRegistry.counter("flowforge.worker.execution.timeout");
        this.ssrfRejectionsCounter = meterRegistry.counter("flowforge.worker.execution.ssrf_rejections");
        this.responseSizeSummary = meterRegistry.summary("flowforge.worker.execution.response_size");
        this.redirectSummary = meterRegistry.summary("flowforge.worker.execution.redirects");
        this.durationTimer = meterRegistry.timer("flowforge.worker.execution.duration");
    }

    public ExecutionResult execute(UUID executionPublicId, JobDefinitionDto job, String leaseToken) {
        requestCounter.increment();
        logger.info("Starting execution for ID: {} targeting URL: {}", executionPublicId, job.getTargetUrl());

        Instant startedAt = clock.instant();

        // 1. Validate Lease before dispatch
        if (!verifyLeaseStillValid(executionPublicId, leaseToken)) {
            logger.warn("Aborting HTTP execution: Lease ownership lost immediately before dispatch.");
            return createErrorResult(executionPublicId, startedAt, NetworkErrorCategory.INTERRUPTED, false, 0);
        }

        // 2. Perform validation and HTTP execution loop
        String method = job.getHttpMethod();
        String currentUrl = job.getTargetUrl();
        String body = job.getRequestBody();
        int redirectCount = 0;
        Set<URI> visitedUris = new HashSet<>();

        URI currentUri;
        try {
            currentUri = URI.create(currentUrl);
            visitedUris.add(currentUri);
        } catch (Exception e) {
            logger.error("Malformed target URL: " + currentUrl, e);
            return createErrorResult(executionPublicId, startedAt, NetworkErrorCategory.INVALID_URL, false, 0);
        }

        HttpResponse<InputStream> response = null;
        Instant finishedAt;
        NetworkErrorCategory networkError = null;
        boolean timeoutFlag = false;

        // Start timing the client execution
        Timer.Sample sample = Timer.start();

        while (true) {
            // Validate scheme and resolve/check SSRF
            try {
                ssrfValidator.validateUri(currentUri);
            } catch (UnknownHostException e) {
                logger.error("SSRF DNS failure resolving " + currentUri.getHost(), e);
                networkError = NetworkErrorCategory.DNS_FAILURE;
                break;
            } catch (SsrfValidator.SsrfValidationException e) {
                logger.error("SSRF validation blocked request to " + currentUri, e);
                ssrfRejectionsCounter.increment();
                networkError = NetworkErrorCategory.SSRF_BLOCKED;
                break;
            }

            // Dispatch HTTP request
            try {
                HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                        .uri(currentUri)
                        .timeout(Duration.ofSeconds(job.getTimeoutSeconds()));

                // Set headers (normalizing and logging masked values)
                if (job.getRequestHeaders() != null) {
                    job.getRequestHeaders().forEach((k, v) -> {
                        reqBuilder.header(k, v);
                        if (logger.isDebugEnabled()) {
                            logger.debug("Header: {} = {}", k, maskSensitiveHeader(k, v));
                        }
                    });
                }

                // Set method and body
                if (method.equalsIgnoreCase("GET")) {
                    reqBuilder.GET();
                } else if (method.equalsIgnoreCase("DELETE")) {
                    reqBuilder.DELETE();
                } else {
                    HttpRequest.BodyPublisher bodyPublisher = body != null ?
                            HttpRequest.BodyPublishers.ofString(body) :
                            HttpRequest.BodyPublishers.noBody();
                    reqBuilder.method(method, bodyPublisher);
                }

                HttpRequest request = reqBuilder.build();

                // Check lease immediately before socket dispatch
                if (!verifyLeaseStillValid(executionPublicId, leaseToken)) {
                    logger.warn("Aborting HTTP execution: Lease ownership lost before socket dispatch.");
                    networkError = NetworkErrorCategory.INTERRUPTED;
                    break;
                }

                logger.info("Executing HTTP {} request to {}", method, currentUri);
                response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

                // Check if redirect
                int statusCode = response.statusCode();
                if (statusCode == 301 || statusCode == 302 || statusCode == 303 || statusCode == 307 || statusCode == 308) {
                    redirectCount++;
                    if (redirectCount > 5) {
                        logger.error("Redirect limit exceeded (max 5)");
                        networkError = NetworkErrorCategory.INVALID_RESPONSE;
                        break;
                    }

                    Optional<String> locationOpt = response.headers().firstValue("Location");
                    if (locationOpt.isEmpty() || locationOpt.get().isBlank()) {
                        logger.error("Redirect status returned with no Location header");
                        networkError = NetworkErrorCategory.INVALID_RESPONSE;
                        break;
                    }

                    URI nextUri;
                    try {
                        nextUri = currentUri.resolve(locationOpt.get());
                    } catch (Exception e) {
                        logger.error("Failed to parse redirect location: " + locationOpt.get(), e);
                        networkError = NetworkErrorCategory.INVALID_URL;
                        break;
                    }

                    if (visitedUris.contains(nextUri)) {
                        logger.error("Redirect loop detected for URI: " + nextUri);
                        networkError = NetworkErrorCategory.INVALID_RESPONSE;
                        break;
                    }

                    visitedUris.add(nextUri);
                    currentUri = nextUri;

                    // Map HTTP redirect methods
                    if (statusCode == 303 || statusCode == 301 || statusCode == 302) {
                        method = "GET";
                        body = null;
                    }
                    logger.info("Following redirect to {}", currentUri);
                    continue; // Loop to next request
                }

                break; // No redirect, break loop

            } catch (HttpConnectTimeoutException e) {
                logger.error("Connection timeout executing request: " + e.getMessage());
                networkError = NetworkErrorCategory.CONNECTION_TIMEOUT;
                timeoutFlag = true;
                timeoutCounter.increment();
                break;
            } catch (HttpTimeoutException | SocketTimeoutException e) {
                logger.error("Request timeout executing request: " + e.getMessage());
                networkError = NetworkErrorCategory.REQUEST_TIMEOUT;
                timeoutFlag = true;
                timeoutCounter.increment();
                break;
            } catch (ConnectException e) {
                logger.error("Connection refused executing request: " + e.getMessage());
                networkError = NetworkErrorCategory.CONNECTION_REFUSED;
                break;
            } catch (java.net.UnknownHostException e) {
                logger.error("DNS lookup failed: " + e.getMessage());
                networkError = NetworkErrorCategory.DNS_FAILURE;
                break;
            } catch (javax.net.ssl.SSLException e) {
                logger.error("TLS failure during request: " + e.getMessage());
                networkError = NetworkErrorCategory.TLS_FAILURE;
                break;
            } catch (InterruptedException e) {
                logger.error("HTTP execution interrupted", e);
                networkError = NetworkErrorCategory.INTERRUPTED;
                Thread.currentThread().interrupt();
                break;
            } catch (IOException e) {
                logger.error("I/O error during HTTP execution: " + e.getMessage());
                networkError = NetworkErrorCategory.UNKNOWN_NETWORK_ERROR;
                break;
            }
        }

        // Stop timer
        sample.stop(durationTimer);
        finishedAt = clock.instant();
        Duration duration = Duration.between(startedAt, finishedAt);

        // 3. Verify Lease after response
        if (!verifyLeaseStillValid(executionPublicId, leaseToken)) {
            logger.warn("Aborting HTTP execution processing: Lease ownership lost after response received.");
            return createErrorResult(executionPublicId, startedAt, NetworkErrorCategory.INTERRUPTED, false, redirectCount);
        }

        // 4. Map HTTP responses to ExecutionResult
        if (networkError != null) {
            failureCounter.increment();
            return createErrorResult(executionPublicId, startedAt, networkError, timeoutFlag, redirectCount);
        }

        // Read body and size metrics
        long responseSize = 0;
        boolean bodyTruncated = false;
        String responseBodyStr = "";

        if (response != null) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try (InputStream is = response.body()) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    responseSize += read;
                    if (bos.size() < maxResponseBodySize) {
                        int toWrite = (int) Math.min(read, maxResponseBodySize - bos.size());
                        bos.write(buffer, 0, toWrite);
                    }
                }
            } catch (IOException e) {
                logger.error("Failed to read response body stream: " + e.getMessage());
                failureCounter.increment();
                return createErrorResult(executionPublicId, startedAt, NetworkErrorCategory.UNKNOWN_NETWORK_ERROR, false, redirectCount);
            }
            bodyTruncated = responseSize > maxResponseBodySize;
            responseBodyStr = bos.toString(StandardCharsets.UTF_8);
        }

        // Update successful metrics
        successCounter.increment();
        responseSizeSummary.record(responseSize);
        redirectSummary.record(redirectCount);

        String contentType = response.headers().firstValue("Content-Type").orElse("application/octet-stream");
        String protocol = response.version().name();

        logger.info("Completed HTTP execution. Status code: {}, Response size: {}, Truncated: {}",
                response.statusCode(), responseSize, bodyTruncated);

        return new ExecutionResult(
                executionPublicId,
                startedAt,
                finishedAt,
                duration,
                response.statusCode(),
                response.headers().map(),
                responseBodyStr,
                responseSize,
                bodyTruncated,
                contentType,
                protocol,
                redirectCount,
                false,
                null
        );
    }

    private ExecutionResult createErrorResult(UUID executionPublicId, Instant startedAt, NetworkErrorCategory category, boolean timeoutFlag, int redirectCount) {
        Instant finished = clock.instant();
        return new ExecutionResult(
                executionPublicId,
                startedAt,
                finished,
                Duration.between(startedAt, finished),
                null,
                Collections.emptyMap(),
                null,
                0,
                false,
                null,
                null,
                redirectCount,
                timeoutFlag,
                category
        );
    }

    private boolean verifyLeaseStillValid(UUID executionPublicId, String leaseToken) {
        UUID workerId = registrationService.getWorkerPublicId();
        if (workerId == null) {
            return false;
        }
        return leaseRepository.findByExecutionPublicId(executionPublicId)
                .map(lease -> lease.getWorkerPublicId().equals(workerId) && lease.getLeaseToken().equals(leaseToken))
                .orElse(false);
    }

    private String maskSensitiveHeader(String key, String value) {
        String lower = key.toLowerCase();
        if (lower.contains("auth") || lower.contains("key") || lower.contains("token") || lower.contains("cookie") || lower.contains("pass")) {
            return "[MASKED]";
        }
        return value;
    }

    public long getMaxResponseBodySize() {
        return maxResponseBodySize;
    }

    public void setMaxResponseBodySize(long maxResponseBodySize) {
        this.maxResponseBodySize = maxResponseBodySize;
    }
}
