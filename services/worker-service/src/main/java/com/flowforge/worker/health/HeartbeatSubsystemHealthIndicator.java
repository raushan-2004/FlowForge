package com.flowforge.worker.health;

import com.flowforge.worker.config.WorkerProperties;
import com.flowforge.worker.service.WorkerHeartbeatService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;

@Component
public class HeartbeatSubsystemHealthIndicator implements HealthIndicator {

    private final WorkerHeartbeatService heartbeatService;
    private final WorkerProperties properties;
    private final Clock clock;

    public HeartbeatSubsystemHealthIndicator(WorkerHeartbeatService heartbeatService, WorkerProperties properties, Clock clock) {
        this.heartbeatService = heartbeatService;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public Health health() {
        if (!properties.isEnabled()) {
            return Health.up().withDetail("status", "Disabled by configuration").build();
        }

        Instant lastTime = heartbeatService.getLastHeartbeatTime();
        if (lastTime == Instant.MIN) {
            return Health.down().withDetail("error", "Heartbeat has not run yet").build();
        }

        // Allow up to 3x the interval before calling it unhealthy
        long maxDelayMs = properties.getHeartbeatIntervalMs() * 3;
        long elapsedMs = clock.millis() - lastTime.toEpochMilli();

        if (elapsedMs > maxDelayMs) {
            return Health.down()
                    .withDetail("error", "Heartbeat task is delayed")
                    .withDetail("elapsedMs", elapsedMs)
                    .withDetail("maxDelayMs", maxDelayMs)
                    .build();
        }

        return Health.up()
                .withDetail("lastHeartbeatTime", lastTime)
                .withDetail("elapsedMs", elapsedMs)
                .build();
    }
}
