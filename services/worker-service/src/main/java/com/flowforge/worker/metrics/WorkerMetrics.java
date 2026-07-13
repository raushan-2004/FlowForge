package com.flowforge.worker.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Component
public class WorkerMetrics {

    private final Counter expiredLeasesCounter;
    private final Counter renewalsCounter;
    private final Counter claimFailuresCounter;
    private final Counter successfulClaimsCounter;
    private final Counter leaseRecoveryCounter;
    private final Counter leaseConflictsCounter;
    private final Timer heartbeatLatencyTimer;

    public WorkerMetrics(MeterRegistry registry) {
        this.expiredLeasesCounter = Counter.builder("flowforge.worker.leases.expired")
                .description("Total number of expired leases detected")
                .register(registry);

        this.renewalsCounter = Counter.builder("flowforge.worker.leases.renewed")
                .description("Total number of successful lease renewals")
                .register(registry);

        this.claimFailuresCounter = Counter.builder("flowforge.worker.claims.failed")
                .description("Total number of failed lease claims")
                .register(registry);

        this.successfulClaimsCounter = Counter.builder("flowforge.worker.claims.success")
                .description("Total number of successful lease claims")
                .register(registry);

        this.leaseRecoveryCounter = Counter.builder("flowforge.worker.leases.recovered")
                .description("Total number of recovered leases")
                .register(registry);

        this.leaseConflictsCounter = Counter.builder("flowforge.worker.leases.conflicts")
                .description("Total number of lease conflicts detected")
                .register(registry);

        this.heartbeatLatencyTimer = Timer.builder("flowforge.worker.heartbeat.latency")
                .description("Latency of heartbeat updates")
                .register(registry);
    }

    public void registerActiveWorkersGauge(MeterRegistry registry, Supplier<Number> activeWorkersSupplier) {
        Gauge.builder("flowforge.worker.active", activeWorkersSupplier)
                .description("Number of active workers")
                .register(registry);
    }

    public void registerActiveLeasesGauge(MeterRegistry registry, Supplier<Number> activeLeasesSupplier) {
        Gauge.builder("flowforge.worker.leases.active", activeLeasesSupplier)
                .description("Number of active leases owned by this worker")
                .register(registry);
    }

    public void recordExpiredLease() {
        expiredLeasesCounter.increment();
    }

    public void recordRenewal() {
        renewalsCounter.increment();
    }

    public void recordClaimFailure() {
        claimFailuresCounter.increment();
    }

    public void recordSuccessfulClaim() {
        successfulClaimsCounter.increment();
    }

    public void recordLeaseRecovery() {
        leaseRecoveryCounter.increment();
    }

    public void recordLeaseConflict() {
        leaseConflictsCounter.increment();
    }

    public void recordHeartbeatLatency(long durationMs) {
        heartbeatLatencyTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }
}
