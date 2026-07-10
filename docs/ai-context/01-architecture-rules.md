# Architecture Rules

These rules are mandatory unless an explicit architecture decision changes them.

## AR-001 — PostgreSQL Durable Truth

PostgreSQL is the durable source of truth for business state.

Do not make correctness of durable job state depend only on:

* in-memory state,
* local process state,
* Valkey,
* Kafka consumer position.

---

## AR-002 — Transactional Outbox

When a durable business-state mutation requires an event to be published, the business mutation and outbox insertion must occur in the same PostgreSQL transaction.

Never:

1. commit business state,
2. then directly publish Kafka event,
3. assume publication cannot fail.

---

## AR-003 — At-Least-Once Delivery

Assume event publication and event consumption are at least once.

Consumers performing business mutations must be idempotent.

---

## AR-004 — Stable Event Identity

Each logical event has a stable event ID.

Republishing the same outbox event must preserve its original event ID.

Do not generate a new event ID for each publication retry.

---

## AR-005 — Short Database Transactions

Do not hold database transactions open while:

* publishing to Kafka,
* making HTTP calls,
* waiting for external services,
* sleeping,
* performing long-running computation.

---

## AR-006 — Distributed Coordination

Do not use:

* Java synchronized,
* ReentrantLock,
* static maps,
* local semaphores

as correctness mechanisms across service replicas.

Use database concurrency controls, leases, fencing tokens, and atomic infrastructure operations where appropriate.

---

## AR-007 — Scheduler Claiming

Scheduler claiming must use PostgreSQL concurrency primitives.

The intended approach is:

FOR UPDATE SKIP LOCKED

inside a short transaction.

Multiple scheduler replicas must be able to claim different work without duplicate claims.

---

## AR-008 — State Transitions

Execution state changes require both:

1. domain transition validation,
2. database conditional mutation.

Application-level validation alone is insufficient.

---

## AR-009 — Worker Isolation

HTTP workers must not directly mutate core execution state in PostgreSQL.

Workers publish narrow ownership, renewal, and result messages through the trusted coordination boundary.

---

## AR-010 — Result Validation

Execution results are not trusted merely because they came from a worker.

The Result Processor must validate:

* execution identity,
* attempt identity,
* current execution state,
* lease ownership,
* fencing token,
* result compatibility with current state.

---

## AR-011 — Tenant Context

Tenant identity must come from authenticated security context.

Never trust tenantId supplied in a request body as authorization evidence.

Every tenant-owned resource lookup must enforce tenant scope.

---

## AR-012 — Persistence/API Separation

Do not expose JPA entities directly through:

* REST responses,
* REST request models,
* Kafka event contracts.

Use separate contracts where responsibilities differ.

---

## AR-013 — Safe HTTP Execution

All user-configured outbound HTTP execution must pass through SafeHttpClient.

No handler may create a bypass path using:

* raw URLConnection,
* arbitrary WebClient,
* arbitrary RestTemplate,
* generic HTTP client instantiated inside the handler.

---

## AR-014 — Migration Discipline

All schema changes use Flyway.

Never modify a migration that has already been applied to a shared environment.

Create a new migration.

---

## AR-015 — Backpressure

Do not use unbounded in-memory queues.

Execution admission and dispatch must respect bounded system capacity.

---

## AR-016 — Graceful Shutdown

Consumers and workers must support graceful shutdown.

Workers should:

* stop accepting new work,
* continue ownership renewal during draining,
* finish current work within configured grace period,
* publish accepted results,
* exit.

If shutdown exceeds the grace period, lease expiry and recovery mechanisms handle unfinished work.

---

## AR-017 — No False Exactly-Once Claims

Do not describe FlowForge as guaranteeing exactly-once execution of arbitrary external HTTP side effects.

The system provides:

* idempotent internal state processing,
* duplicate suppression where defined,
* stale-result rejection,
* reliable at-least-once event processing.

External targets may require their own idempotency mechanisms.

---

## AR-018 — Module Boundaries

Avoid generic shared modules containing unrelated application logic.

Shared libraries should contain narrow concerns such as:

* event contracts,
* messaging contracts,
* test support.

Domain logic should remain owned by the service or bounded context responsible for it.

---

## AR-019 — Public Error Handling

Public APIs must not expose:

* stack traces,
* SQL details,
* internal service hostnames,
* infrastructure topology,
* secrets,
* raw exception messages containing sensitive values.

---

## AR-020 — Architecture Change Protocol

If implementation requirements conflict with these rules:

1. identify the conflict,
2. explain the affected invariant,
3. propose alternatives,
4. do not silently weaken the architecture.

---
