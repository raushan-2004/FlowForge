# FlowForge AI Context Pack — Persistence and Contracts

# Database Design Rules

## 1. General Principles

PostgreSQL is the durable source of truth.

Use:

* foreign keys,
* unique constraints,
* check constraints,
* conditional updates,
* row locks where required,
* indexes based on actual query patterns.

Do not rely only on application validation for data invariants that PostgreSQL can enforce.

Default transaction isolation:

READ COMMITTED

Use targeted locking and compare-and-set updates rather than globally increasing isolation level.

---

## 2. Identifier Strategy

Externally visible IDs should be difficult to enumerate.

Use one consistent identifier strategy across the project.

Internal database identifiers and public identifiers may be separated if required.

Do not expose sequential numeric IDs publicly without explicit design approval.

---

## 3. Tenant Scoping

Tenant-owned tables must contain or be safely joinable to tenant ownership.

High-frequency resource queries should support direct tenant-scoped lookup where practical.

Example repository intent:

find execution
where public_id = ?
and tenant_id = ?

Do not:

1. load resource globally,
2. return it,
3. perform tenant check later.

Authorization-sensitive filtering belongs in the query path.

---

## 4. Core Logical Tables

Initial schema is expected to include logical equivalents of:

* users
* tenants
* tenant_memberships
* projects
* api_keys
* job_definitions
* job_executions
* execution_attempts
* execution_leases
* idempotency_records
* outbox_events
* processed_events
* dead_letter_entries
* workflow_definitions
* workflow_nodes
* workflow_edges
* workflow_executions
* workflow_node_executions

Exact naming may follow project conventions.

---

## 5. Execution Query Index

Scheduler eligibility query is conceptually:

SELECT ...
FROM job_executions
WHERE status IN ('SCHEDULED', 'RETRY_WAIT')
AND dispatch_after <= NOW()
ORDER BY effective_priority DESC, scheduled_at ASC
FOR UPDATE SKIP LOCKED
LIMIT ?

Indexes must support this access pattern.

Do not add indexes mechanically. Explain the query each index supports.

---

## 6. Scheduler Claim Transaction

The intended transaction is:

BEGIN

select eligible rows
using FOR UPDATE SKIP LOCKED

update selected rows to DISPATCH_PENDING

insert dispatch outbox records

COMMIT

Kafka publication must occur after transaction completion through the outbox relay.

---

## 7. Conditional State Mutation

Do not use:

load entity
check state
set state
save

as the only concurrency protection for contested transitions.

Use conditional mutation where required.

Conceptual example:

UPDATE job_executions
SET status = 'COMPLETED'
WHERE id = ?
AND status IN ('RUNNING', 'CANCELLATION_REQUESTED')
AND ownership condition is valid

Affected-row count must be checked.

---

## 8. Attempt Constraint

Required uniqueness:

UNIQUE(execution_id, attempt_number)

Duplicate processing must not create multiple attempt N records for one execution.

---

## 9. Idempotency Constraint

Required uniqueness scope:

UNIQUE(
tenant_id,
project_id,
idempotency_key
)

Request fingerprint must be stored so key reuse with different logical content can be rejected.

Idempotency acquisition, execution creation, resource mapping, and initial outbox insertion must be transactionally safe.

---

## 10. Outbox Model

Outbox records require fields logically equivalent to:

* id,
* stable event ID,
* aggregate type,
* aggregate ID,
* event type,
* schema version,
* payload,
* status,
* created time,
* next attempt time,
* claimed by,
* claimed until,
* publication time,
* publication attempt count.

Suggested states:

* PENDING
* CLAIMED
* PUBLISHED

Claiming must support multiple relay instances.

Do not hold database locks while waiting for Kafka acknowledgement.

Claim expiry must permit safe recovery.

Duplicate publication remains possible and expected.

---

## 11. Processed Event Deduplication

Consumers performing business mutations need durable deduplication.

Required uniqueness is conceptually:

UNIQUE(
consumer_name,
event_id
)

Deduplication record insertion and the corresponding business mutation must occur in the same database transaction.

Never:

1. insert processed-event marker,
2. commit,
3. perform business mutation in another transaction.

That could lose business processing permanently.

---

## 12. Execution Lease Mutation

Lease acquisition and renewal must be atomic.

Renewal must verify:

* execution identity,
* current owner,
* current lease token/fencing token,
* expected ownership state.

Expired ownership observed by a recovery scanner must be revalidated at mutation time.

Recovery must not act solely on stale previously-read data.

---

## 13. Workflow Dependency Mutation

Workflow child dependency progression must be transactionally safe.

Processing a parent completion should:

1. deduplicate event,
2. verify workflow progression is still allowed,
3. decrement remaining dependency count once,
4. detect transition to zero,
5. mark child READY exactly once,
6. create child execution if required,
7. create outbox event,
8. commit.

The processed-event record and dependency mutation belong in one transaction.

---

## 14. Workflow Constraints

Required conceptual uniqueness:

UNIQUE(
workflow_execution_id,
workflow_node_id
)

Additional uniqueness should prevent multiple generated executions for a single node execution where semantics require exactly one.

Workflow graph activation must validate:

* node references exist,
* edge endpoints exist,
* no self-edge unless explicitly supported,
* no duplicate edge,
* graph is acyclic.

---

## 15. Dead Letter Replay

DLQ replay uses conditional state ownership.

Conceptual mutation:

UPDATE dead_letter_entries
SET status = 'REPLAYING'
WHERE id = ?
AND status = 'OPEN'

Affected rows:

1:
caller owns replay operation.

0:
entry is not available for replay.

Replay execution creation and DLQ state transition must be transactionally coordinated.

---

## 16. Timestamp Rules

Store timestamps consistently.

Prefer UTC storage semantics.

Application code must not depend on local server timezone for scheduling correctness.

Scheduling tests must use injected clock abstractions where appropriate.

---

## 17. Migration Rules

Flyway migration rules:

* migrations are append-only,
* do not edit applied migrations,
* migrations must work from empty database,
* migrations must work sequentially from previous version,
* dangerous data migrations should be isolated,
* production schema changes should favor backward-compatible expansion/contraction patterns.

---

