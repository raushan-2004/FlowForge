# Security Rules

## SEC-001 — Public System Assumption

Assume all public input is untrusted.

This includes:

* URLs,
* headers,
* payloads,
* workflow graphs,
* job configuration,
* API keys,
* query parameters,
* pagination parameters,
* SSE connection attempts.

---

## SEC-002 — Password Storage

Passwords must use an adaptive password hashing algorithm through approved password encoding abstraction.

Never use plain hashing such as raw SHA-256 for password storage.

Never log passwords.

---

## SEC-003 — API Key Storage

Full API key secret is shown only at creation time.

Persist:

* public key identifier,
* verification hash,
* metadata.

Do not persist retrievable plaintext API key secrets.

---

## SEC-004 — Tenant Authorization

Role checks alone are insufficient.

Authorization requires:

* authenticated identity,
* validated tenant membership,
* required permission,
* tenant-scoped resource access.

---

## SEC-005 — SSRF Protection

HTTP execution must validate:

* URI parsing,
* allowed scheme,
* embedded credentials,
* hostname,
* DNS resolution,
* all resolved addresses,
* blocked address ranges,
* restricted ports where configured,
* redirect destinations,
* redirect count.

Validation must address:

* IPv4,
* IPv6,
* loopback,
* private ranges,
* link-local ranges,
* prohibited infrastructure destinations.

Redirects must repeat destination validation.

---

## SEC-006 — Safe HTTP Client Boundary

All user-controlled outbound HTTP execution must use SafeHttpClient.

Bypass paths are architecture violations.

---

## SEC-007 — Worker Network Isolation

Execution workers should have restricted access to the control plane.

Workers must not receive PostgreSQL credentials.

Production target architecture should enforce network restrictions in addition to application validation.

---

## SEC-008 — Secret Management

Never commit:

* database passwords,
* JWT signing private keys,
* API keys,
* SMTP credentials,
* Kafka credentials,
* encryption keys.

Never print them at startup.

Never expose them through public diagnostics.

---

## SEC-009 — Logging Redaction

Do not log by default:

* Authorization header,
* Cookie header,
* Set-Cookie header,
* passwords,
* API key secrets,
* access tokens,
* refresh tokens,
* request bodies,
* response bodies,
* full sensitive query strings.

Prefer metadata logging.

---

## SEC-010 — Public Error Safety

Public error responses must not expose:

* stack traces,
* SQL,
* internal paths,
* broker addresses,
* database hosts,
* secrets,
* raw internal exception details.

---

## SEC-011 — CORS

Production CORS origins must be explicit.

Do not combine wildcard origins with credentialed browser APIs.

Configuration must be environment-specific.

---

## SEC-012 — CSRF

CSRF policy depends on credential transport.

Cookie-authenticated unsafe operations require explicit CSRF protection.

Do not globally disable CSRF merely to simplify configuration.

---

## SEC-013 — Payload Limits

Bound:

* API request body size,
* execution input size,
* workflow node count,
* workflow edge count,
* workflow graph size,
* HTTP response capture size,
* headers,
* batch operations,
* page size.

---

## SEC-014 — Abuse Prevention

Public deployment requires:

* authentication,
* email verification when implemented,
* rate limits,
* daily quotas,
* concurrent execution limits,
* destination-level limits,
* tenant suspension capability.

FlowForge must not become an unrestricted traffic relay or load generator.

---

## SEC-015 — SSE Isolation

SSE connections must be tenant-scoped.

Never expose raw cross-tenant event streams to browser clients.

---

## SEC-016 — Operational Endpoint Protection

Public health endpoint should expose minimal information.

Sensitive operational endpoints such as:

* environment,
* configuration properties,
* loggers,
* thread dumps,
* heap dumps,
* detailed metrics

must not be publicly accessible.

---

## SEC-017 — Dependency Security

CI should eventually include:

* dependency vulnerability scanning,
* secret scanning,
* container scanning.

Do not add unnecessary or unmaintained dependencies.

---

## Required Security Tests

Test:

* expired access credential rejection,
* revoked API key rejection,
* cross-tenant execution read rejection,
* cross-tenant cancellation rejection,
* cross-tenant SSE rejection,
* cross-tenant DLQ replay rejection,
* loopback SSRF rejection,
* private IPv4 rejection,
* IPv6 loopback rejection,
* link-local rejection,
* prohibited scheme rejection,
* redirect-to-blocked-target rejection,
* embedded-credential URL rejection,
* oversized payload rejection,
* oversized response rejection,
* public stack trace absence,
* secret absence from logs.

---
