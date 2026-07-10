# FlowForge AI Context Pack — Correctness and Engineering Rules

# Concurrency Invariants

These invariants are mandatory correctness properties.

Every concurrency-sensitive implementation prompt must identify which invariants it affects.

---

## INV-001 — Idempotent Submission Uniqueness

A logical idempotent submission creates at most one logical execution.

Concurrent requests with the same tenant, project, and idempotency key must not create duplicate logical executions.

---

## INV-002 — Unique Scheduler Claim

An eligible execution may be claimed by only one scheduler transaction for a given dispatch cycle.

Multiple scheduler replicas may claim different executions concurrently.

---

## INV-003 — Single Valid Execution Ownership

An execution has at most one valid current execution owner according to the lease protocol.

Expired or replaced ownership cannot authorize state mutation.

---

## INV-004 — Stale Worker Rejection

A worker using an expired, replaced, or otherwise stale fencing token cannot mutate execution or attempt state.

A stale success cannot overwrite a newer valid result.

A stale failure cannot overwrite a newer valid success.

---

## INV-005 — Stable Event Identity

Republishing one logical outbox event preserves the same event ID.

---

## INV-006 — Idempotent Business Consumption

Duplicate delivery of the same logical event cannot repeat its business side effect.

Deduplication and business mutation must commit atomically.

---

## INV-007 — Valid State Transitions Only

Every persisted execution state transition must satisfy:

* domain transition policy,
* current database state condition,
* ownership condition where applicable.

---

## INV-008 — Atomic State and Event Persistence

A business state mutation requiring an event and the corresponding outbox event must commit atomically.

The system must not persist one without the other.

---

## INV-009 — Terminal State Protection

Once an execution reaches a terminal state, competing late operations cannot overwrite it.

---

## INV-010 — Workflow Dependency Exactly Once

One logical parent completion may decrement a child's remaining dependency count at most once.

Duplicate parent completion events cannot perform additional decrements.

---

## INV-011 — Workflow Ready Exactly Once

A workflow node transitions to READY at most once for one workflow execution.

Concurrent completion of multiple parents must not create duplicate child executions.

---

## INV-012 — DLQ Replay Ownership

Only one concurrent caller may own replay initiation for an OPEN dead-letter entry.

---

## INV-013 — Tenant Isolation

A tenant-scoped resource cannot be read or mutated by an unauthorized tenant.

Concurrency or caching must not weaken tenant isolation.

---

## INV-014 — Retry Scheduling Uniqueness

One logical failure event cannot schedule the same retry attempt multiple times.

---

## INV-015 — Recovery Revalidation

Recovery logic must revalidate expiry and ownership conditions at mutation time.

A scanner may not act solely on stale previously-read lease state.

---

## INV-016 — First Valid Terminal Transition Wins

When cancellation and completion race, only one valid terminal transition may persist.

The winner is determined by valid conditional state transition semantics, not last-writer-wins behavior.

---

## INV-017 — Outbox Recovery Safety

An expired outbox claim may be reclaimed.

Duplicate publication is allowed.

Loss of a committed outbox event is not allowed under normal supported failure recovery.

---

## INV-018 — Bounded Processing

No public request, worker response, queue, or workflow graph may create unbounded in-memory processing.

---

## Required Concurrency Test Scenarios

### CT-001 Scheduler Contention

10 concurrent claimers.

1000 eligible executions.

Expected:

* 1000 unique claims,
* zero duplicate claims.

### CT-002 Idempotency Contention

100 concurrent equivalent submissions with the same idempotency key.

Expected:

* one logical execution,
* all other responses resolve through documented replay or in-progress behavior,
* no duplicate logical work.

### CT-003 Lease Fencing

Worker A receives ownership token A.

A expires.

Worker B receives token B.

A submits result.

Expected:
rejected.

B submits valid result.

Expected:
accepted.

### CT-004 Duplicate Workflow Event

Same parent completion event delivered repeatedly.

Expected:
dependency count decremented once.

### CT-005 Parallel Parent Completion

Many parents complete concurrently.

Expected:
child becomes READY once and creates one execution.

### CT-006 Cancellation Race

Cancellation and completion are executed concurrently repeatedly.

Expected:
exactly one valid terminal state.

### CT-007 DLQ Replay Contention

Multiple concurrent replay requests.

Expected:
one replay operation owns the transition.

### CT-008 Duplicate Event Consumption

Same event delivered multiple times.

Expected:
business side effect applied once.

---
