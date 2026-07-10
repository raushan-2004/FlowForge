# Testing Rules

## 1. Testing Philosophy

Tests must verify architecture and correctness, not only line coverage.

Do not weaken tests merely to make the build pass.

Do not replace meaningful integration tests with mocks when correctness depends on actual database or messaging behavior.

---

## 2. Test Layers

### Unit Tests

Use for:

* state transition policy,
* retry calculation,
* jitter boundaries,
* priority calculation,
* graph validation,
* cycle detection,
* failure classification,
* pure authorization rules.

### Repository Integration Tests

Use Testcontainers PostgreSQL for:

* constraints,
* native queries,
* SKIP LOCKED behavior,
* conditional updates,
* transaction behavior,
* index-sensitive query behavior where relevant.

Do not use H2 as proof that PostgreSQL-specific concurrency SQL works.

### Messaging Integration Tests

Test:

* serialization,
* deserialization,
* duplicate delivery,
* consumer transaction behavior,
* unsupported event versions,
* malformed messages,
* redelivery.

### End-to-End Tests

Test critical flows:

submission
→ scheduling
→ dispatch
→ worker execution
→ result processing
→ final state

and:

workflow start
→ parallel nodes
→ parent completion
→ child readiness
→ workflow completion.

---

## 3. Deterministic Time

Use injected Clock abstractions for:

* scheduling,
* retry calculation,
* lease expiry,
* claim expiry,
* idempotency expiry,
* timeout logic.

Avoid tests that depend on arbitrary sleep calls.

---

## 4. Deterministic Randomness

Retry jitter logic must allow deterministic testing through injected randomness abstraction or deterministic strategy.

---

## 5. Concurrency Tests

Concurrency tests must:

* start operations concurrently using proper synchronization barriers,
* assert database end state,
* assert uniqueness properties,
* repeat enough times to expose races where practical.

Do not claim concurrency safety based only on sequential tests.

---

## 6. Transaction Tests

For transactional outbox flows, test rollback.

Example:

If execution creation transaction fails:

Expected:

* execution absent,
* outbox event absent.

If transaction succeeds:

Expected:

* execution present,
* outbox event present.

---

## 7. Duplicate Delivery Tests

Every idempotent business consumer must be tested with repeated delivery of the same event ID.

Expected:
business effect occurs once.

---

## 8. Architecture Tests

Use ArchUnit or Maven module boundaries where appropriate to verify rules such as:

* worker module does not depend on JPA,
* worker module does not depend on PostgreSQL driver,
* API controllers do not return persistence entities,
* HTTP job handlers cannot bypass SafeHttpClient package boundary,
* forbidden module dependencies fail tests.

---

## 9. Security Tests

Security tests must include both positive and negative cases.

Do not test only that an authorized user succeeds.

Also test that:

* wrong tenant fails,
* insufficient role fails,
* revoked credential fails,
* malformed credential fails.

---

## 10. Load Tests

k6 scenarios should eventually include:

* burst execution submission,
* sustained execution submission,
* execution-history reads,
* SSE connection load,
* mixed tenant traffic.

Load tests must report:

* throughput,
* latency percentiles,
* error rate,
* queue age,
* database saturation,
* consumer lag.

---

## 11. Chaos/Failure Tests

Required scenarios include:

* worker killed during execution,
* result redelivery,
* outbox relay crash after publish before status update,
* scheduler process termination,
* event consumer crash after DB commit before acknowledgement,
* temporary broker unavailability,
* temporary Valkey unavailability.

---

## 12. Test Completion Rule

An implementation stage is not complete until:

* affected modules compile,
* relevant unit tests pass,
* relevant integration tests pass,
* migration tests pass when schema changed,
* architecture tests pass when boundaries changed.

Report exact failing tests rather than claiming general success.

---
