# FlowForge AI Context Pack — Core Architecture

# FlowForge Project Overview

## 1. Product Definition

FlowForge is a multi-tenant distributed job orchestration and workflow execution platform.

It allows users and external applications to:

* create projects,
* define executable jobs,
* submit immediate and delayed executions,
* assign priorities,
* configure retry policies,
* execute outbound HTTP jobs,
* monitor execution status,
* inspect execution attempts,
* cancel executions,
* replay dead-lettered executions,
* create DAG-based workflows,
* execute independent workflow branches in parallel,
* observe execution progress through REST APIs and Server-Sent Events.

FlowForge is designed as a public multi-tenant platform.

The system must be correct under:

* concurrent requests,
* duplicate client requests,
* duplicate event delivery,
* process crashes,
* worker crashes,
* network failures,
* delayed messages,
* stale worker results,
* concurrent scheduler instances,
* concurrent workflow parent completion,
* cancellation/completion races.

The architecture must favor explicit failure handling over assumptions of perfect delivery.

---

## 2. Core Product Goals

FlowForge should demonstrate:

1. distributed job scheduling,
2. reliable execution dispatch,
3. transactional outbox usage,
4. idempotent event processing,
5. worker leasing,
6. fencing-token protection,
7. retry scheduling,
8. dead-letter handling,
9. DAG workflow execution,
10. tenant isolation,
11. public API security,
12. SSRF-resistant HTTP execution,
13. observability,
14. horizontal scaling.

---

## 3. Non-Goals for Initial Release

The first public versions do not require:

* Kubernetes,
* service mesh,
* multi-region deployment,
* database sharding,
* full event sourcing,
* CQRS everywhere,
* arbitrary user code execution,
* shell command execution,
* arbitrary container execution,
* arbitrary TCP proxying,
* arbitrary protocol support,
* complex billing,
* payment gateway integration,
* exactly-once message delivery claims.

---

## 4. Technology Direction

Backend:

* Java 21
* Spring Boot
* Spring Security
* Spring Data JPA where appropriate
* Maven

Persistence:

* PostgreSQL
* Flyway migrations

Messaging:

* Kafka protocol
* Spring Kafka
* broker implementation must remain replaceable

Hot-path ephemeral state:

* Valkey

Frontend:

* React
* TypeScript

Testing:

* JUnit 5
* Testcontainers
* ArchUnit where useful
* k6 for load testing

Observability:

* Micrometer
* OpenTelemetry
* structured logs
* Grafana-compatible telemetry backend

Deployment:

* Docker
* Docker Compose for local development
* portable container-based public deployment

---

## 5. Logical Services

### API Service

Responsibilities:

* authentication,
* authorization,
* tenant context resolution,
* project management,
* API key management,
* job definition management,
* execution submission,
* execution query APIs,
* cancellation requests,
* workflow management APIs,
* DLQ management APIs,
* quota admission checks,
* idempotency handling for submission APIs.

The API service does not execute jobs.

---

### Scheduler Service

Responsibilities:

* find eligible executions,
* atomically claim execution batches,
* apply scheduling policy,
* respect dispatch admission rules,
* move executions into dispatch-pending state,
* create dispatch outbox messages.

The scheduler must support multiple concurrent replicas safely.

The scheduler must not hold database row locks while waiting for Kafka acknowledgements.

---

### Worker Service

Responsibilities:

* consume execution commands,
* participate in execution ownership protocol,
* execute HTTP jobs,
* renew execution ownership leases through the trusted coordination boundary,
* perform cooperative cancellation where supported,
* publish execution-result messages.

The HTTP worker must not directly update core PostgreSQL execution state.

The worker must not have direct PostgreSQL credentials.

The worker must use SafeHttpClient for all user-configured outbound HTTP requests.

---

### Result Processor

Responsibilities:

* handle execution ownership operations,
* validate lease ownership,
* validate fencing tokens,
* process lease renewal requests,
* process execution result messages,
* reject stale results,
* update attempt state,
* update execution state,
* create domain outbox events.

The Result Processor is part of the trusted control plane.

---

### Event Processor

Responsibilities:

* consume domain events,
* deduplicate events,
* evaluate retries,
* progress workflows,
* update read projections where required,
* generate tenant-scoped live update events,
* support SSE projection flow.

Business mutations caused by event consumption must be idempotent.

---

## 6. Trust Zones

FlowForge has three conceptual security zones.

### Edge Zone

Contains:

* public frontend,
* public API ingress,
* TLS termination,
* public rate limiting.

### Control Plane

Contains:

* API Service,
* Scheduler Service,
* Result Processor,
* Event Processor,
* PostgreSQL,
* Kafka,
* Valkey.

### Execution Plane

Contains:

* HTTP workers,
* restricted outbound execution capabilities.

Target architecture requires strong network isolation between execution and control planes.

Initial beta deployment may use container-level separation and egress rules, but code architecture must preserve the stronger logical boundary.

---

## 7. Source of Truth

PostgreSQL is the durable source of truth for:

* tenants,
* users,
* memberships,
* projects,
* API key metadata,
* job definitions,
* job executions,
* execution attempts,
* execution leases,
* workflows,
* workflow executions,
* workflow node executions,
* outbox records,
* processed event records,
* dead-letter records,
* durable usage records.

Kafka is transport and event history within configured retention.

Valkey is used for hot-path ephemeral concerns such as:

* rate limiting,
* fast counters,
* worker liveness projection,
* temporary operational state.

Valkey must not be the only source of truth for durable execution state.

---

## 8. Delivery Semantics

The system assumes:

* Kafka messages may be delivered more than once.
* Outbox events may be published more than once.
* Consumers may crash after database commit and before offset acknowledgement.
* Workers may crash during execution.
* Clients may retry requests.
* Network timeouts do not prove that an operation did not happen.

The system therefore uses:

* idempotency keys,
* database uniqueness constraints,
* transactional outbox,
* stable event IDs,
* processed-event deduplication,
* conditional state transitions,
* execution leases,
* fencing tokens.

The system does not claim end-to-end exactly-once execution of arbitrary external side effects.

---

## 9. Main Execution Flow

The intended execution flow is:

Client
→ API Service
→ validate authentication and authorization
→ quota admission
→ idempotency handling
→ create execution
→ create outbox event
→ commit transaction

Outbox Relay
→ publish execution event

Scheduler
→ identify eligible execution
→ atomically claim
→ mark DISPATCH_PENDING
→ create dispatch outbox event
→ commit transaction

Outbox Relay
→ publish execution command

Worker
→ receive execution command
→ establish valid ownership through trusted coordination protocol
→ execute job
→ renew ownership when needed
→ publish execution result

Result Processor
→ validate ownership and fencing token
→ accept or reject result
→ update attempt
→ update execution
→ create domain event in outbox
→ commit

Event Processor
→ consume domain event
→ deduplicate
→ evaluate retry or workflow progression
→ perform idempotent mutation
→ commit
→ acknowledge message

---

## 10. Initial MVP

MVP 1 includes:

* user authentication,
* tenants,
* projects,
* project API keys,
* HTTP job definitions,
* immediate execution submission,
* delayed execution,
* priorities,
* retry policies,
* execution attempts,
* execution history,
* cancellation request,
* basic DLQ,
* basic dashboard.

MVP 2 adds:

* recurring scheduling,
* DAG workflows,
* parallel branches,
* workflow visualization,
* SSE updates,
* advanced DLQ replay,
* priority aging,
* recovery dashboards.

---

