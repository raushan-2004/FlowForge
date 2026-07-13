package com.flowforge.worker.service;

import com.flowforge.worker.WorkerServiceApplication;
import com.flowforge.worker.config.WorkerProperties;
import com.flowforge.worker.dto.ExecutionResult;
import com.flowforge.worker.dto.JobDefinitionDto;
import com.flowforge.worker.dto.NetworkErrorCategory;
import com.flowforge.worker.model.ExecutionLease;
import com.flowforge.worker.repository.ExecutionLeaseRepository;
import com.flowforge.worker.repository.WorkerRepository;
import com.flowforge.worker.service.WorkerRegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.bind.annotation.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.OutputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.time.Clock;
import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = {
                WorkerServiceApplication.class,
                HttpExecutionEngineIT.TestConfig.class,
                HttpExecutionEngineIT.TestHttpController.class
        },
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Testcontainers
@ActiveProfiles("test")
class HttpExecutionEngineIT {

    private static final Logger logger = LoggerFactory.getLogger(HttpExecutionEngineIT.class);

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
        public SsrfValidator spySsrfValidator(com.flowforge.worker.config.SsrfProperties props) {
            return new SsrfValidator(props) {
                @Override
                public void validateUri(URI uri) throws UnknownHostException, SsrfValidationException {
                    // Bypass SSRF loopback check strictly for localhost to allow local mock controller traffic
                    if (uri.getHost() != null && uri.getHost().equalsIgnoreCase("localhost")) {
                        return;
                    }
                    super.validateUri(uri);
                }

                @Override
                protected InetAddress[] resolveAddresses(String host) throws UnknownHostException {
                    if (host.equals("multi-dns.example")) {
                        return new InetAddress[] {
                                InetAddress.getByName("8.8.8.8"), // Public safe
                                InetAddress.getByName("127.0.0.1") // Private unsafe
                        };
                    }
                    if (host.equals("dns-fail.example")) {
                        throw new UnknownHostException("Host dns-fail.example not found");
                    }
                    return super.resolveAddresses(host);
                }
            };
        }
    }

    @RestController
    @RequestMapping("/mock-http")
    public static class TestHttpController {

        @GetMapping("/get")
        public ResponseEntity<String> handleGet(@RequestHeader HttpHeaders headers, @RequestParam Map<String, String> params) {
            return ResponseEntity.ok("GET SUCCESS: " + params.getOrDefault("q", ""));
        }

        @PostMapping("/post")
        public ResponseEntity<String> handlePost(@RequestBody String body) {
            return ResponseEntity.ok("POST SUCCESS: " + body);
        }

        @PutMapping("/put")
        public ResponseEntity<String> handlePut(@RequestBody String body) {
            return ResponseEntity.ok("PUT SUCCESS: " + body);
        }

        @DeleteMapping("/delete")
        public ResponseEntity<String> handleDelete() {
            return ResponseEntity.ok("DELETE SUCCESS");
        }

        @GetMapping("/redirect-loop")
        public ResponseEntity<Void> handleRedirectLoop() {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, "/mock-http/redirect-loop")
                    .build();
        }

        @GetMapping("/redirect-limit")
        public ResponseEntity<Void> handleRedirectLimit(@RequestParam("count") int count) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, "/mock-http/redirect-limit?count=" + (count + 1))
                    .build();
        }

        @GetMapping("/redirect-private")
        public ResponseEntity<Void> handleRedirectPrivate() {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, "http://127.0.0.1:8080/mock-http/get")
                    .build();
        }

        @GetMapping("/http-500")
        public ResponseEntity<String> handle500() {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("INTERNAL SERVER ERROR BODY");
        }

        @GetMapping("/http-404")
        public ResponseEntity<String> handle404() {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("NOT FOUND BODY");
        }

        @GetMapping("/empty")
        public ResponseEntity<Void> handleEmpty() {
            return ResponseEntity.ok().build();
        }

        @GetMapping("/large")
        public ResponseEntity<String> handleLarge() {
            char[] chars = new char[5000];
            Arrays.fill(chars, 'A');
            return ResponseEntity.ok(new String(chars));
        }

        @GetMapping("/delay")
        public ResponseEntity<String> handleDelay() throws InterruptedException {
            Thread.sleep(5000);
            return ResponseEntity.ok("DELAY SUCCESS");
        }

        @GetMapping("/chunked")
        public void handleChunked(jakarta.servlet.http.HttpServletResponse response) throws Exception {
            response.setStatus(200);
            response.setHeader("Transfer-Encoding", "chunked");
            OutputStream os = response.getOutputStream();
            os.write("Chunk 1\n".getBytes());
            os.flush();
            Thread.sleep(100);
            os.write("Chunk 2\n".getBytes());
            os.flush();
            os.close();
        }
    }

    @LocalServerPort
    private int port;

    @Autowired
    private HttpExecutionEngine executionEngine;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private ExecutionLeaseRepository leaseRepository;

    @Autowired
    private WorkerRegistrationService registrationService;

    @Autowired
    private WorkerProperties workerProperties;

    @Autowired
    private Clock clock;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID executionPublicId;
    private String leaseToken;

    @BeforeEach
    void setUp() throws Exception {
        workerProperties.setEnabled(true); // Enable worker registration runner for testing

        leaseRepository.deleteAll();
        workerRepository.deleteAll();

        // Register worker in db
        registrationService.run();

        UUID workerId = registrationService.getWorkerPublicId();
        assertThat(workerId).isNotNull();

        executionPublicId = UUID.randomUUID();
        leaseToken = UUID.randomUUID().toString();

        // Re-create the lease
        ExecutionLease lease = new ExecutionLease(
                executionPublicId,
                workerId,
                leaseToken,
                1L,
                clock.instant(),
                clock.instant().plusSeconds(60)
        );
        leaseRepository.saveAndFlush(lease);
    }

    private String getBaseUrl() {
        return "http://localhost:" + port;
    }

    @Test
    void testGetMethod() {
        JobDefinitionDto job = new JobDefinitionDto("GET", getBaseUrl() + "/mock-http/get?q=test", null, null, 10);
        ExecutionResult result = executionEngine.execute(executionPublicId, job, leaseToken);

        assertThat(result.getStatusCode()).isEqualTo(200);
        assertThat(result.getResponseBody()).contains("GET SUCCESS: test");
        assertThat(result.getNetworkErrorCategory()).isNull();
        assertThat(result.isBodyTruncated()).isFalse();
    }

    @Test
    void testPostMethod() {
        JobDefinitionDto job = new JobDefinitionDto("POST", getBaseUrl() + "/mock-http/post", null, "HELLO POST", 10);
        ExecutionResult result = executionEngine.execute(executionPublicId, job, leaseToken);

        assertThat(result.getStatusCode()).isEqualTo(200);
        assertThat(result.getResponseBody()).contains("POST SUCCESS: HELLO POST");
    }

    @Test
    void testPutMethod() {
        JobDefinitionDto job = new JobDefinitionDto("PUT", getBaseUrl() + "/mock-http/put", null, "HELLO PUT", 10);
        ExecutionResult result = executionEngine.execute(executionPublicId, job, leaseToken);

        assertThat(result.getStatusCode()).isEqualTo(200);
        assertThat(result.getResponseBody()).contains("PUT SUCCESS: HELLO PUT");
    }

    @Test
    void testDeleteMethod() {
        JobDefinitionDto job = new JobDefinitionDto("DELETE", getBaseUrl() + "/mock-http/delete", null, null, 10);
        ExecutionResult result = executionEngine.execute(executionPublicId, job, leaseToken);

        assertThat(result.getStatusCode()).isEqualTo(200);
        assertThat(result.getResponseBody()).contains("DELETE SUCCESS");
    }

    @Test
    void testRedirectLoopDetection() {
        JobDefinitionDto job = new JobDefinitionDto("GET", getBaseUrl() + "/mock-http/redirect-loop", null, null, 10);
        ExecutionResult result = executionEngine.execute(executionPublicId, job, leaseToken);

        assertThat(result.getNetworkErrorCategory()).isEqualTo(NetworkErrorCategory.INVALID_RESPONSE);
        assertThat(result.getStatusCode()).isNull();
    }

    @Test
    void testRedirectLimitExceeded() {
        JobDefinitionDto job = new JobDefinitionDto("GET", getBaseUrl() + "/mock-http/redirect-limit?count=1", null, null, 10);
        ExecutionResult result = executionEngine.execute(executionPublicId, job, leaseToken);

        assertThat(result.getNetworkErrorCategory()).isEqualTo(NetworkErrorCategory.INVALID_RESPONSE);
        assertThat(result.getStatusCode()).isNull();
    }

    @Test
    void testRedirectToPrivateIpBlockedBySsrf() {
        JobDefinitionDto job = new JobDefinitionDto("GET", getBaseUrl() + "/mock-http/redirect-private", null, null, 10);
        ExecutionResult result = executionEngine.execute(executionPublicId, job, leaseToken);

        assertThat(result.getNetworkErrorCategory()).isEqualTo(NetworkErrorCategory.SSRF_BLOCKED);
        assertThat(result.getStatusCode()).isNull();
    }

    @Test
    void testMultipleDnsAnswersOneForbidden() {
        JobDefinitionDto job = new JobDefinitionDto("GET", "http://multi-dns.example/mock-http/get", null, null, 10);
        ExecutionResult result = executionEngine.execute(executionPublicId, job, leaseToken);

        assertThat(result.getNetworkErrorCategory()).isEqualTo(NetworkErrorCategory.SSRF_BLOCKED);
    }

    @Test
    void testHttp500IsCapturedAsValidResponse() {
        JobDefinitionDto job = new JobDefinitionDto("GET", getBaseUrl() + "/mock-http/http-500", null, null, 10);
        ExecutionResult result = executionEngine.execute(executionPublicId, job, leaseToken);

        assertThat(result.getStatusCode()).isEqualTo(500);
        assertThat(result.getResponseBody()).contains("INTERNAL SERVER ERROR BODY");
        assertThat(result.getNetworkErrorCategory()).isNull();
    }

    @Test
    void testHttp404IsCapturedAsValidResponse() {
        JobDefinitionDto job = new JobDefinitionDto("GET", getBaseUrl() + "/mock-http/http-404", null, null, 10);
        ExecutionResult result = executionEngine.execute(executionPublicId, job, leaseToken);

        assertThat(result.getStatusCode()).isEqualTo(404);
        assertThat(result.getResponseBody()).contains("NOT FOUND BODY");
        assertThat(result.getNetworkErrorCategory()).isNull();
    }

    @Test
    void testChunkedResponse() {
        JobDefinitionDto job = new JobDefinitionDto("GET", getBaseUrl() + "/mock-http/chunked", null, null, 10);
        ExecutionResult result = executionEngine.execute(executionPublicId, job, leaseToken);

        assertThat(result.getStatusCode()).isEqualTo(200);
        assertThat(result.getResponseBody()).contains("Chunk 1", "Chunk 2");
    }

    @Test
    void testEmptyResponseBody() {
        JobDefinitionDto job = new JobDefinitionDto("GET", getBaseUrl() + "/mock-http/empty", null, null, 10);
        ExecutionResult result = executionEngine.execute(executionPublicId, job, leaseToken);

        assertThat(result.getStatusCode()).isEqualTo(200);
        assertThat(result.getResponseBody()).isEmpty();
    }

    @Test
    void testResponseTruncation() {
        long originalLimit = executionEngine.getMaxResponseBodySize();
        executionEngine.setMaxResponseBodySize(100);
        try {
            JobDefinitionDto job = new JobDefinitionDto("GET", getBaseUrl() + "/mock-http/large", null, null, 10);
            ExecutionResult result = executionEngine.execute(executionPublicId, job, leaseToken);

            assertThat(result.getStatusCode()).isEqualTo(200);
            assertThat(result.isBodyTruncated()).isTrue();
            assertThat(result.getResponseBody().length()).isEqualTo(100);
            assertThat(result.getResponseSize()).isEqualTo(5000L);
        } finally {
            executionEngine.setMaxResponseBodySize(originalLimit);
        }
    }

    @Test
    void testTlsFailure() {
        // Request HTTPS but hit HTTP port
        JobDefinitionDto job = new JobDefinitionDto("GET", "https://localhost:" + port + "/mock-http/get", null, null, 10);
        ExecutionResult result = executionEngine.execute(executionPublicId, job, leaseToken);

        assertThat(result.getNetworkErrorCategory()).isEqualTo(NetworkErrorCategory.TLS_FAILURE);
    }

    @Test
    void testDnsFailure() {
        JobDefinitionDto job = new JobDefinitionDto("GET", "http://dns-fail.example/mock-http/get", null, null, 10);
        ExecutionResult result = executionEngine.execute(executionPublicId, job, leaseToken);

        assertThat(result.getNetworkErrorCategory()).isEqualTo(NetworkErrorCategory.DNS_FAILURE);
    }

    @Test
    void testConnectionRefused() {
        // Hit an unused local port
        JobDefinitionDto job = new JobDefinitionDto("GET", "http://localhost:59999/mock-http/get", null, null, 10);
        ExecutionResult result = executionEngine.execute(executionPublicId, job, leaseToken);

        assertThat(result.getNetworkErrorCategory()).isEqualTo(NetworkErrorCategory.CONNECTION_REFUSED);
    }

    @Test
    void testRequestTimeout() {
        // Timeout set to 1 second, server takes 5 seconds
        JobDefinitionDto job = new JobDefinitionDto("GET", getBaseUrl() + "/mock-http/delay", null, null, 1);
        ExecutionResult result = executionEngine.execute(executionPublicId, job, leaseToken);

        assertThat(result.getNetworkErrorCategory()).isEqualTo(NetworkErrorCategory.REQUEST_TIMEOUT);
        assertThat(result.isTimeoutFlag()).isTrue();
    }

    @Test
    void testLeaseLossBeforeDispatch() {
        // Invalidate lease by deleting it
        leaseRepository.deleteAll();

        JobDefinitionDto job = new JobDefinitionDto("GET", getBaseUrl() + "/mock-http/get", null, null, 10);
        ExecutionResult result = executionEngine.execute(executionPublicId, job, leaseToken);

        assertThat(result.getNetworkErrorCategory()).isEqualTo(NetworkErrorCategory.INTERRUPTED);
        assertThat(result.getStatusCode()).isNull();
    }
}
