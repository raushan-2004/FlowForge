# Event Contract Rules

## 1. General Envelope

Business events should use a consistent envelope.

Conceptual fields:

* eventId
* eventType
* schemaVersion
* occurredAt
* producer
* tenantId where appropriate
* aggregateType
* aggregateId
* correlationId
* causationId
* payload

The exact serialized contract must be versioned.

---

## 2. Stable Event ID

eventId identifies the logical event.

If an outbox event is published five times because of retry, all five deliveries use the same eventId.

---

## 3. Correlation and Causation

correlationId tracks a broader operation chain.

causationId identifies the event or command that directly caused another event.

Do not confuse event identity with correlation identity.

---

## 4. Contract Ownership

Event contracts belong in the narrow event-contracts module.

Do not publish:

* JPA entities,
* internal repository models,
* arbitrary Java exception serialization.

Event payloads must be deliberate contracts.

---

## 5. Schema Evolution

Event contracts require schemaVersion.

Evolution rules:

* prefer additive changes,
* consumers should tolerate documented optional additions,
* breaking changes require explicit new version strategy,
* do not silently reinterpret an existing field.

---

## 6. Initial Logical Events

The platform may use events equivalent to:

* EXECUTION_SCHEDULED
* EXECUTION_DISPATCH_REQUESTED
* EXECUTION_STARTED
* EXECUTION_COMPLETED
* EXECUTION_FAILED
* EXECUTION_CANCELLED
* EXECUTION_DEAD_LETTERED
* EXECUTION_RETRY_SCHEDULED
* WORKFLOW_STARTED
* WORKFLOW_NODE_COMPLETED
* WORKFLOW_NODE_FAILED
* WORKFLOW_COMPLETED
* WORKFLOW_FAILED

Names may follow code conventions, but semantics must remain explicit.

---

## 7. Execution Command Contract

Execution command must contain only the data required by the worker.

It should include:

* execution identity,
* attempt identity,
* ownership protocol information or reference,
* handler type,
* validated bounded handler configuration,
* bounded execution input,
* timeout configuration,
* correlation metadata.

Do not expose unrelated tenant data.

Do not include database credentials.

---

## 8. Ownership and Lease Messages

Because workers do not directly access PostgreSQL, narrow messages are used for ownership operations.

Logical message types may include:

* EXECUTION_OWNERSHIP_REQUESTED
* EXECUTION_OWNERSHIP_GRANTED
* EXECUTION_OWNERSHIP_REJECTED
* EXECUTION_LEASE_RENEWAL_REQUESTED
* EXECUTION_LEASE_RENEWED
* EXECUTION_LEASE_RENEWAL_REJECTED

The exact request/reply or asynchronous coordination pattern must be finalized before Stage 10 implementation.

Do not invent multiple competing ownership protocols in different services.

---

## 9. Execution Result Contract

Worker result message includes logically:

* result message ID,
* execution ID,
* attempt ID,
* worker ID,
* lease token/fencing token,
* outcome,
* bounded response metadata,
* failure classification,
* timing metadata,
* correlation metadata.

The Result Processor must not trust the result without validating current ownership.

---

## 10. Consumer Idempotency

Every consumer that mutates durable business state must:

1. begin database transaction,
2. insert processed-event identity,
3. detect duplicate,
4. perform business mutation,
5. create required outbox event,
6. commit,
7. acknowledge message.

If duplicate:
business mutation must not be repeated.

---

## 11. Offset Acknowledgement

Conceptual ordering:

receive event
→ perform DB transaction
→ commit DB
→ acknowledge/advance message position

If process crashes after DB commit but before acknowledgement:

* event is redelivered,
* processed-event uniqueness detects duplicate,
* processing becomes safe no-op.

---

## 12. No Sensitive Event Payloads

Do not place secrets into durable event payloads unless explicitly designed and encrypted.

Avoid publishing:

* API key secrets,
* refresh tokens,
* authorization headers,
* cookies,
* database credentials,
* unrestricted request/response bodies.

---

## 13. Topic Permission Principle

Production architecture should support least-privilege messaging credentials.

A worker capable of publishing execution results must not automatically have permission to publish unrelated administrative domain events.

Topic-level permissions are deployment concerns but contract ownership should support them.

---

## 14. Event Test Requirements

Every business consumer requires tests for:

* normal event,
* duplicate event,
* malformed event behavior,
* unsupported schema version behavior,
* transaction rollback,
* crash-equivalent redelivery behavior,
* concurrency where relevant.
