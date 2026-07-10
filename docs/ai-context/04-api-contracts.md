# API Contract Rules

## 1. API Style

Use REST for:

* commands,
* resource creation,
* resource queries,
* filtering,
* pagination,
* administrative actions.

Use Server-Sent Events for incremental live dashboard updates.

SSE is not the source of truth.

Clients must use REST to obtain current state and SSE to receive incremental changes.

---

## 2. Versioning

Public API routes use explicit versioning.

Example:

/api/v1/...

Breaking contract changes require versioning strategy review.

---

## 3. Authentication Modes

Dashboard users:

* browser authentication flow,
* short-lived access credential,
* secure refresh/session mechanism according to security design.

External applications:

* project-scoped API keys.

Do not use project API keys as human dashboard sessions.

---

## 4. Tenant Resolution

Tenant scope must come from authenticated identity and authorization context.

Do not accept arbitrary tenantId from a public request and trust it.

If an API supports switching between a user's authorized tenants, selected tenant context must be validated against membership.

---

## 5. Error Format

Use one consistent public error structure.

Recommended fields:

* code
* message
* requestId
* fieldErrors when applicable

Example concept:

{
"code": "IDEMPOTENCY_KEY_REUSED",
"message": "The idempotency key was already used with a different request.",
"requestId": "..."
}

Do not expose internal exception names or stack traces.

---

## 6. Pagination

List APIs must be bounded.

Do not allow unbounded page sizes.

Execution history APIs should support stable pagination suitable for frequently changing datasets.

Cursor-based pagination is preferred for high-volume execution history where practical.

---

## 7. Execution Submission

Conceptual endpoint:

POST /api/v1/projects/{projectId}/executions

Request may contain:

* job definition reference,
* bounded input,
* priority,
* scheduled time where supported.

Idempotency key should be supplied through the documented request mechanism.

Behavior:

New valid request:
202 Accepted or documented creation response.

Same idempotency key and equivalent request:
return original logical submission result.

Same idempotency key with different fingerprint:
409 Conflict.

Same key currently being processed:
409 with a stable in-progress error code, unless later architecture explicitly adopts another behavior.

---

## 8. Job Definition APIs

Support:

* create,
* get,
* list,
* update mutable draft/configuration,
* disable,
* archive where required.

Do not return stored secret values.

HTTP handler configuration must be validated before activation.

---

## 9. Execution Query APIs

Support filtering by useful fields such as:

* status,
* job definition,
* creation time range,
* scheduling time range,
* workflow execution where applicable.

Every query is tenant-scoped.

---

## 10. Cancellation

Cancellation endpoint represents a request for cancellation.

Semantics:

Before RUNNING:
execution may transition directly to CANCELLED.

While RUNNING:
execution transitions to CANCELLATION_REQUESTED.

Cancellation of external HTTP work is cooperative and cannot guarantee that an external side effect did not already happen.

Public documentation must not promise otherwise.

---

## 11. DLQ Replay

Replay is an explicit command endpoint.

Concurrent replay requests must not create multiple replay executions.

Response must clearly indicate:

* replay accepted,
* replay already in progress,
* entry already replayed,
* entry unavailable.

---

## 12. Workflow APIs

Workflow definition creation and update must distinguish:

* editable draft,
* activated immutable/versioned definition.

Workflow execution must reference an exact definition version.

Graph validation errors should be structured and understandable.

---

## 13. SSE Contract

SSE events must be tenant-scoped.

Browser clients must never receive raw cross-tenant Kafka streams.

Live event payloads should be bounded and UI-oriented.

A live event should include enough identity for the client to update or refetch the relevant resource.

Do not put secrets or full execution payloads into SSE events.

---

## 14. Request Limits

All public request bodies are bounded.

Limits must exist for:

* execution input,
* workflow graph size,
* workflow node count,
* edge count,
* batch operation size,
* pagination size,
* headers.

Exact limits are configuration values documented separately.

---
