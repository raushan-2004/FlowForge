# FlowForge Production Hardening & Operations Guide

This guide documents the final production hardening, distributed tracing, backpressure thresholds, DLQ topics, scaling guides, disaster recovery, and chaos testing scenarios configured for FlowForge v1.0.

---

## 1. Distributed Tracing & Logging

### OpenTelemetry Propagation
OpenTelemetry is configured across all services to propagate context using standard W3C Trace Context headers:
- `traceparent`: Propagates Trace ID and Span ID across REST calls and Redpanda/Kafka message headers.
- `tracestate`: Carries vendor-specific state information.
- **Trace Context Map**:
  ```
  [HTTP Request / REST] -> X-Trace-Id header -> MDC Log Context
  [Kafka Messages]      -> Trace headers    -> MDC Log Context
  ```

### Structured JSON Logging
Every microservice writes structured JSON to standard console output.
- **Log Format Schema**:
  ```json
  {
    "timestamp": "2026-07-13T16:30:00.123Z",
    "level": "INFO",
    "service": "api-service",
    "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
    "spanId": "00f067aa0ba902b7",
    "executionPublicId": "550e8400-e29b-44d4-a716-446655440000",
    "workflowRunPublicId": "550e8400-e29b-44d4-a716-446655441111",
    "workerId": "worker-f47ac10b-58cc-4372-a567-0e02b2c3d479",
    "message": "Successfully finalized execution",
    "logger": "com.flowforge.api.service.ExecutionService",
    "thread": "http-nio-8080-exec-1"
  }
  ```
- **Masking Rules**: Logback regex pattern filters mask headers matching `Authorization`, `Bearer`, `X-API-Key`, `password`, and `secret`.

---

## 2. Dead Letter Queue (DLQ) & Replay Strategy

### DLQ Redirection
Kafka consumer configuration defines explicit Dead Letter Queue error handlers. If a message fails consumption after 3 attempts (due to serialization issues, corrupt payloads, or database connection losses), it is routed to the corresponding DLQ topic:
- `execution-completed` $\rightarrow$ `execution-dead`
- `execution-retry-scheduled` $\rightarrow$ `retry-dead`
- `workflow-started` $\rightarrow$ `workflow-dead`

### Replay Strategy
1. **Analyze**: Operations team queries the DLQ topic metrics and inspects payload structures.
2. **Resolve**: Database index blockages, network partitions, or schema changes are applied.
3. **Replay Job**: An operational Spring Batch job or custom script reads messages from the `*-dead` topic, wraps them back into the active queue, and commits the DLQ offset.

---

## 3. Backpressure & Capacity Safeguards

- **Concurrent Execution Limit**: Workers restrict concurrent job runs using a semaphores-backed task executor. Default limit is 100 concurrent jobs per worker instance.
- **Queue Depth Limit**: Redpanda listeners stop polling (backpressure pause) when internal thread queue depth exceeds 500 pending tasks.
- **HTTP Concurrency**: Tomcat connection threads are restricted in `api-service` via `server.tomcat.max-threads=200` to prevent database connection pool starvation.

---

## 4. Graceful Shutdown Protocol

### Worker Service
1. Receive SIGTERM.
2. Transition state to `DRAINING` (reject new claim events from Kafka).
3. Finish active HTTP executions (maximum wait: 30 seconds).
4. Release active execution leases in PostgreSQL.
5. Transition status to `OFFLINE` and exit.

### Scheduler & Kafka Listeners
1. Stop polling Kafka topics.
2. Allow active partition threads to complete current execution finalization tasks.
3. Commit final offsets to Redpanda.
4. Close connection pools and terminate.

---

## 5. Security Hardening Assumptions
- **TLS Enforced**: All traffic between ingress, API controllers, and workers uses TLS v1.3.
- **Internal Authentication**: API endpoints starting with `/internal/v1/**` reject calls lacking header `X-Internal-Service-Token`.
- **Secrets Management**: DB credentials and signing keys are mounted dynamically in Kubernetes using CSI Secrets Driver (never hardcoded in templates).

---

## 6. Disaster Recovery & Operations Runbook

### Scenario A: Postgres Connection Pool Starvation
1. **Symptom**: HTTP 500 / Timeout errors in `api-service`, Prometheus metric `hikaricp_pending_threads > 0`.
2. **Action**:
   - Check current database connections: `SELECT count(*), state FROM pg_stat_activity GROUP BY state;`.
   - Scale down scheduler-service replica counts to reduce database lock contention.
   - Force terminate stalled database connections using `pg_terminate_backend`.

### Scenario B: Kafka Consumer Lag Spike
1. **Symptom**: `flowforge_kafka_consumer_lag > 1000` on the `execution-completed` topic.
2. **Action**:
   - Verify result-processor health.
   - Check if `result-processor` pods are cycling due to OOM issues.
   - Trigger horizontal scale-out of result-processor replicas up to 5 partitions.
