# Coding Standards

## 1. Java Version

Use Java 21.

Use modern language features where they improve clarity.

Avoid cleverness that reduces maintainability.

---

## 2. Dependency Injection

Use constructor injection.

Do not use field injection.

---

## 3. DTOs and Immutable Data

Prefer records for immutable:

* commands,
* queries,
* API request/response DTOs,
* event payloads,
* value objects

where framework requirements permit.

---

## 4. Domain Encapsulation

Do not expose public setters on domain aggregates merely for convenience.

State changes should occur through meaningful methods or transition policies.

Avoid:

execution.setStatus(COMPLETED)

Prefer intent-revealing operations and conditional persistence semantics.

---

## 5. Package Organization

Prefer feature-oriented organization.

Example:

execution/
├── api/
├── application/
├── domain/
└── infrastructure/

Avoid one global project-wide structure containing huge folders such as:

controller/
service/
repository/
entity/

when feature-oriented organization gives clearer ownership.

---

## 6. Naming

Names must express business meaning.

Avoid generic names such as:

* Utils
* Helper
* Manager
* CommonService
* DataProcessor

unless the name is genuinely precise in context.

---

## 7. Service Size

Avoid God services.

A class should have one coherent reason to change.

Examples of preferable focused application services:

* SubmitExecutionUseCase
* CancelExecutionUseCase
* ClaimEligibleExecutions
* AcceptExecutionResult
* EvaluateRetry
* ProgressWorkflowNode

rather than one enormous JobService.

---

## 8. Transaction Boundaries

Transactions belong at explicit application operation boundaries.

Do not spread nested transactional behavior casually across unrelated methods.

Document important transaction boundaries in code comments or architecture docs where non-obvious.

Never perform external HTTP calls inside a database transaction.

Never wait for Kafka acknowledgement while holding scheduler row locks.

---

## 9. Exceptions

Use domain/application exceptions intentionally.

Map them to stable public error codes.

Do not return raw exception messages directly to clients.

Do not catch Exception broadly unless performing boundary-level translation, logging, or cleanup with correct rethrow behavior.

---

## 10. Logging

Use structured, contextual logging.

Useful fields include:

* requestId,
* correlationId,
* tenantId,
* projectId,
* executionId,
* attemptId,
* workflowExecutionId,
* eventId.

Do not log sensitive payloads.

---

## 11. Configuration

Configuration values must be externalizable.

Use typed configuration properties for related settings.

Avoid scattered environment-variable reads throughout business logic.

---

## 12. Time

Inject Clock where business correctness depends on time.

Do not call Instant.now() throughout domain logic.

---

## 13. Randomness

Inject randomness abstractions where deterministic tests are required.

---

## 14. Collections and Limits

Do not load unbounded result sets.

Repository methods must use:

* pagination,
* bounded batches,
* explicit limits

for potentially large data.

---

## 15. Kafka Consumers

Consumers must clearly define:

* message validation,
* supported schema versions,
* idempotency behavior,
* transaction boundary,
* acknowledgement behavior,
* retry/error handling.

---

## 16. Database Access

Use JPA for normal persistence where appropriate.

Use explicit native PostgreSQL queries where correctness or performance requires database-specific features such as:

* FOR UPDATE SKIP LOCKED,
* conditional atomic mutations,
* RETURNING,
* specialized indexing.

Do not force every query through ORM abstractions if doing so weakens correctness.

---

## 17. Comments

Comments should explain:

* why a concurrency mechanism exists,
* why a transaction boundary is important,
* why a security restriction exists,
* why an unusual query is necessary.

Do not add comments that merely restate obvious code.

---

## 18. TODO Policy

Do not generate placeholder TODO implementations and call the stage complete.

If a required part cannot be implemented:

* report it,
* explain the blocker,
* leave the build state accurate.

---

## 19. Formatting and Build

The root build must provide consistent:

* Java version,
* dependency management,
* compiler settings,
* test execution,
* integration-test conventions.

Every stage must run the affected build before reporting completion.

---

## 20. AI Agent Workflow

Before coding:

1. read relevant ai-context documents,
2. inspect existing code,
3. identify affected invariants,
4. list files to change,
5. identify transaction boundaries,
6. identify security implications,
7. identify concurrency implications.

During coding:

1. implement smallest coherent scope,
2. avoid unrelated refactoring,
3. preserve module boundaries,
4. add required tests.

After coding:

1. compile,
2. run tests,
3. report exact results,
4. summarize changed files,
5. explain transaction boundaries,
6. explain concurrency behavior,
7. identify assumptions,
8. identify unresolved issues.

Never silently redesign FlowForge.
