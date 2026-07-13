package com.flowforge.scheduler.metrics;

import com.flowforge.scheduler.repository.JobRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Component
public class SchedulerMetrics {

    private final JobRepository jobRepository;
    private final Clock clock;
    private final Counter scheduledJobsCounter;
    private final Counter skippedSchedulesCounter;
    private final Counter schedulingFailuresCounter;
    private final MeterRegistry registry;

    public SchedulerMetrics(JobRepository jobRepository, MeterRegistry registry, Clock clock) {
        this.jobRepository = jobRepository;
        this.clock = clock;
        this.registry = registry;

        // Register due jobs gauge
        registry.gauge("flowforge.scheduler.jobs.due", this, SchedulerMetrics::getDueJobsCount);

        this.scheduledJobsCounter = registry.counter("flowforge.scheduler.jobs.scheduled");
        this.skippedSchedulesCounter = registry.counter("flowforge.scheduler.skipped");
        this.schedulingFailuresCounter = registry.counter("flowforge.scheduler.failures");
    }

    private double getDueJobsCount() {
        try {
            return jobRepository.countActiveEnabledDueJobs(clock.instant());
        } catch (Exception e) {
            return 0.0;
        }
    }

    public void recordScheduled() {
        scheduledJobsCounter.increment();
    }

    public void recordSkipped(int count) {
        skippedSchedulesCounter.increment(count);
    }

    public void recordFailure() {
        schedulingFailuresCounter.increment();
    }

    public void recordLag(Instant nextFireAt) {
        Instant now = clock.instant();
        if (nextFireAt != null && now.isAfter(nextFireAt)) {
            Timer.builder("flowforge.scheduler.lag")
                    .description("Lag between scheduled nextFireAt and actual execution trigger")
                    .register(registry)
                    .record(Duration.between(nextFireAt, now));
        }
    }
}
