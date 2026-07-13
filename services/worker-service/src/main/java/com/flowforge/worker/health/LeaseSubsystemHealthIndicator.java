package com.flowforge.worker.health;

import com.flowforge.worker.repository.ExecutionLeaseRepository;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class LeaseSubsystemHealthIndicator implements HealthIndicator {

    private final ExecutionLeaseRepository leaseRepository;

    public LeaseSubsystemHealthIndicator(ExecutionLeaseRepository leaseRepository) {
        this.leaseRepository = leaseRepository;
    }

    @Override
    public Health health() {
        try {
            long count = leaseRepository.count();
            return Health.up()
                    .withDetail("status", "Healthy")
                    .withDetail("totalLeasesInDb", count)
                    .build();
        } catch (Exception e) {
            return Health.down(e).withDetail("error", "Failed to query execution leases table").build();
        }
    }
}
