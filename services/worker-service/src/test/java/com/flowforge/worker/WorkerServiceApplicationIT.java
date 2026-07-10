package com.flowforge.worker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.redpanda.RedpandaContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class WorkerServiceApplicationIT {

    @Container
    static RedpandaContainer redpanda = new RedpandaContainer(
            DockerImageName.parse("docker.io/redpandadata/redpanda:v24.1.2")
                    .asCompatibleSubstituteFor("docker.redpanda.com/redpandadata/redpanda")
    );

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", redpanda::getBootstrapServers);
        registry.add("spring.kafka.listener.auto-startup", () -> "false");
    }

    @Test
    void contextLoads() {
        // Verifies the worker context boots successfully with only Redpanda container, and no Postgres
    }
}
