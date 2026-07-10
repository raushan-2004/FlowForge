# Domain Model

## 1. Tenant

Represents the primary isolation and ownership boundary.

Core concepts:

* tenant identity,
* tenant status,
* plan/usage policy reference,
* creation timestamp.

Possible statuses:

* ACTIVE
* SUSPENDED
* CLOSED

A suspended tenant cannot submit new work.

---

## 2. User

Represents a human dashboard user.

User authentication identity is separate from tenant membership.

A user may belong to multiple tenants.

---

## 3. Membership

Connects:

User
↔ Tenant

Roles:

* OWNER
* ADMIN
* DEVELOPER
* VIEWER

Authorization rules are permission-based and tenant-scoped.

---

## 4. Project

A project belongs to one tenant.

A project groups:

* API keys,
* job definitions,
* workflows,
* executions,
* usage.

External API keys are project-scoped.

---

## 5. API Key

A project API key contains:

* public identifier or prefix,
* secret verification hash,
* project ID,
* tenant ID,
* status,
* creation time,
* expiry time,
* last-used time.

Statuses:

* ACTIVE
* REVOKED
* EXPIRED

The full secret is returned only at creation time.

---

## 6. Job Definition

A Job Definition describes reusable executable behavior.

Initial handler type:

* HTTP

Future handler types may exist, but must not weaken execution-plane security.

A job definition includes:

* identity,
* tenant ownership,
* project ownership,
* name,
* description,
* handler type,
* validated handler configuration,
* retry policy reference/configuration,
* timeout configuration,
* status,
* version.

Possible statuses:

* ACTIVE
* DISABLED
* ARCHIVED

---

## 7. Job Execution

A Job Execution represents one logical execution request.

Important properties:

* execution ID,
* tenant ID,
* project ID,
* job definition ID,
* status,
* priority,
* scheduled time,
* dispatch-after time,
* current attempt number,
* maximum attempts,
* created time,
* started time,
* completed time,
* cancellation request time,
* execution input reference or bounded payload,
* failure classification where applicable.

Recommended status model:

* SCHEDULED
* RETRY_WAIT
* DISPATCH_PENDING
* DISPATCHED
* RUNNING
* CANCELLATION_REQUESTED
* COMPLETED
* FAILED
* CANCELLED
* DEAD_LETTER

Terminal states:

* COMPLETED
* CANCELLED
* DEAD_LETTER

FAILED may be an intermediate durable state before retry evaluation depending on implementation flow.

Terminal states must not be overwritten by competing results.

---

## 8. Execution Attempt

Represents one physical attempt of a logical execution.

Properties include:

* attempt ID,
* execution ID,
* attempt number,
* worker identity where relevant,
* status,
* start time,
* finish time,
* failure category,
* bounded result metadata.

Statuses:

* CREATED
* RUNNING
* SUCCEEDED
* FAILED
* TIMED_OUT
* ABANDONED
* CANCELLED

Constraint:

(execution_id, attempt_number) must be unique.

ABANDONED means execution ownership was lost without an accepted valid terminal result.

---

## 9. Execution Lease

Represents temporary execution ownership.

Properties:

* execution ID,
* attempt ID,
* owner worker ID,
* lease token,
* fencing version/token,
* acquired time,
* expiry time,
* renewal metadata if required.

Rules:

* only one current valid ownership lease may exist for an execution,
* lease expiry does not authorize stale workers to mutate state,
* result acceptance requires valid fencing information,
* renewal must be conditional on current ownership.

---

## 10. Idempotency Record

Represents deduplication of client submission requests.

Scope:

tenant
+
project
+
idempotency key

Properties:

* idempotency key,
* request fingerprint,
* status,
* created resource ID,
* creation time,
* completion time.

Statuses:

* PROCESSING
* COMPLETED
* FAILED

Behavior:

Same key + equivalent request:
return previous logical result.

Same key + different request fingerprint:
reject with conflict.

Concurrent same-key requests:
at most one logical execution may be created.

---

## 11. Retry Policy

A retry policy determines whether and when a failed execution is retried.

Concepts:

* maximum attempts,
* retryable failure categories,
* strategy,
* initial delay,
* maximum delay,
* jitter policy.

Strategies:

* FIXED
* EXPONENTIAL

Retry delay must support jitter.

Retry scheduling must remain idempotent under duplicate failure events.

---

## 12. Dead Letter Entry

Represents an execution that cannot continue normal retry processing.

Statuses:

* OPEN
* REPLAYING
* REPLAYED
* DISMISSED

Replay must use conditional ownership so concurrent replay requests cannot create duplicate replay executions.

---

## 13. Workflow Definition

A workflow definition describes a directed acyclic graph.

Contains:

* workflow identity,
* tenant ID,
* project ID,
* name,
* version,
* status,
* nodes,
* edges,
* failure policy.

Statuses may include:

* DRAFT
* ACTIVE
* ARCHIVED

An ACTIVE version should be treated as immutable or versioned rather than mutated in place.

---

## 14. Workflow Node

A workflow node references executable behavior.

A node includes:

* node ID,
* workflow definition ID,
* job definition reference,
* node configuration,
* dependency metadata.

---

## 15. Workflow Edge

Represents a dependency:

parent node
→ child node

The workflow graph must be validated as acyclic before activation.

---

## 16. Workflow Execution

Represents one execution of a workflow definition version.

Statuses:

* CREATED
* RUNNING
* COMPLETED
* FAILED
* CANCELLED

The workflow execution tracks the exact definition version used.

---

## 17. Workflow Node Execution

Represents runtime state of one workflow node.

Properties:

* workflow execution ID,
* workflow node ID,
* state,
* remaining dependency count,
* generated job execution ID where applicable,
* completion metadata.

Possible states:

* BLOCKED
* READY
* SCHEDULED
* RUNNING
* COMPLETED
* FAILED
* SKIPPED
* CANCELLED

The transition to READY must occur at most once.

Duplicate parent completion events must not decrement dependencies multiple times.

---

## 18. Workflow Failure Policies

Initial policies:

### FAIL_FAST

When a required node fails permanently:

* workflow becomes FAILED,
* no new downstream nodes are scheduled,
* already-running jobs may complete,
* their execution truth is recorded,
* completion does not continue workflow progression.

### CONTINUE_INDEPENDENT_BRANCHES

Independent branches may continue where graph semantics permit.

This policy can be added after FAIL_FAST is correct.

---

## 19. Failure Categories

Execution failure should be classified.

Examples:

* HTTP_CLIENT_ERROR
* HTTP_SERVER_ERROR
* CONNECTION_TIMEOUT
* READ_TIMEOUT
* DNS_FAILURE
* CIRCUIT_OPEN
* RESPONSE_TOO_LARGE
* CANCELLED
* OWNERSHIP_LOST
* INTERNAL_EXECUTION_ERROR

Retryability must be decided by policy, not by generic exception retry.

---

## 20. Domain Transition Principle

State machines must be explicit.

A transition should be modeled as:

current state
+
command/event
+
validated context
→ next state or rejection

Do not allow arbitrary status setters.

Domain logic validates semantic transitions.

Persistence logic uses conditional writes to protect against concurrent transitions.
