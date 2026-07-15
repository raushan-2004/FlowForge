<![CDATA[<div align="center">

<!-- Logo -->
<img src="https://img.shields.io/badge/FlowForge-v1.0.0-7c3aed?style=for-the-badge&logo=data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIyNCIgaGVpZ2h0PSIyNCIgdmlld0JveD0iMCAwIDI0IDI0IiBmaWxsPSJ3aGl0ZSI+PHBhdGggZD0iTTEzIDJMMyAxNGgxMGwtMSA4IDEwLTEySDEybDEtOHoiLz48L3N2Zz4=" alt="FlowForge" />

# FlowForge

### Distributed Job Orchestration & Workflow Engine

**A resilient, multi-tenant execution platform built for high-throughput distributed workloads,
conditional DAG routing, heartbeated worker pools, and real-time operational observability.**

---

[![Build](https://img.shields.io/badge/build-passing-4ade80?style=flat-square&logo=github-actions)](.)
[![TypeScript](https://img.shields.io/badge/TypeScript-clean-3178c6?style=flat-square&logo=typescript)](.)
[![Java](https://img.shields.io/badge/Java-21-f89820?style=flat-square&logo=openjdk)](.)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-6db33f?style=flat-square&logo=springboot)](.)
[![Next.js](https://img.shields.io/badge/Next.js-16.2-000000?style=flat-square&logo=nextdotjs)](.)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169e1?style=flat-square&logo=postgresql)](.)
[![Kafka](https://img.shields.io/badge/Kafka-Redpanda%20v24.1-ff4f00?style=flat-square&logo=apachekafka)](.)
[![License](https://img.shields.io/badge/license-MIT-a855f7?style=flat-square)](LICENSE)

---

[**Live Console**](http://localhost:3000) · [**Documentation**](http://localhost:3000/docs) · [**API Reference**](#api-reference) · [**Architecture**](#architecture)

</div>

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Environment Variables](#environment-variables)
- [Database Schema](#database-schema)
- [API Reference](#api-reference)
- [Frontend Application](#frontend-application)
- [Running Tests](#running-tests)
- [Development Workflow](#development-workflow)
- [Deployment](#deployment)
- [Contributing](#contributing)
- [Changelog](#changelog)

---

## Overview

FlowForge is a production-grade, multi-tenant distributed job scheduling and workflow orchestration platform. It enables engineering teams to define jobs (HTTP-dispatched units of work), compose them into conditional DAG workflows, schedule them with cron expressions or manual triggers, and observe every execution in real time.

Built on **Spring Boot 3**, **Apache Kafka (Redpanda)**, and **PostgreSQL 16**, FlowForge implements the transactional outbox pattern, heartbeated worker leases, and optimistic concurrency control to provide exactly-once-semantics guarantees without relying on distributed locks.

The frontend is a full-featured **Next.js 16 / React 19** single-page application with a 10-page console, a visual ReactFlow workflow builder, real-time SSE monitoring, Recharts analytics dashboards, and a complete administration and settings panel.

---

## Features

### Core Platform

| Capability | Description |
|---|---|
| **Multi-Tenancy** | Full tenant isolation — projects, jobs, executions, workflows, and API keys are strictly scoped per tenant |
| **Job Scheduling** | Cron-based, one-shot, and API-triggered job dispatch with timezone support |
| **HTTP Dispatching** | Execute any HTTP endpoint with configurable method, headers, body, timeout, and response validation |
| **DAG Workflows** | Fan-out / fan-in pipeline orchestration with cycle detection and reachability validation |
| **Worker Pool** | Distributed worker nodes with heartbeated lease protocol and automatic stale-lease recovery |
| **Retry Engine** | Configurable exponential backoff, linear backoff, fixed delay, and no-retry policies with dead-letter queues |
| **RBAC** | Four-tier role model: `OWNER` → `ADMIN` → `DEVELOPER` → `VIEWER` with enforced capability matrix |
| **API Keys** | Project-scoped bcrypt-hashed API keys with rotation, revocation, prefix display, and last-used tracking |
| **Audit Logs** | Comprehensive event trail for all user and system actions |

### Reliability Guarantees

- **Exactly-once execution** via PostgreSQL conditional state mutations and execution fencing tokens
- **Transactional outbox pattern** for reliable Kafka event publication without 2PC
- **Optimistic concurrency control** with `version` columns on all mutable entities
- **Heartbeat lease protocol** — workers claim execution leases and renew them every N seconds; expired leases are automatically reclaimed by the scheduler
- **Dead-letter queue** — executions exhausting all retry attempts move to `DEAD` state for manual intervention
- **Idempotent job dispatch** — duplicate API trigger calls produce a single execution

### Observability

- Real-time SSE event stream (`/api/v1/monitoring/stream`)
- Live worker heartbeat, queue depth, and Kafka consumer lag feeds
- Full execution timeline with per-attempt details, error categories, and duration
- Recharts-powered analytics: trend lines, heatmaps, retry analysis, distributed traces

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                          FlowForge Platform                         │
│                                                                     │
│   ┌──────────────────┐        REST / SSE         ┌──────────────┐  │
│   │  Next.js 16 UI   │ ◄────────────────────────► │  API Service │  │
│   │  (React 19)      │                            │ (Spring Boot)│  │
│   │  Port: 3000      │                            │  Port: 8080  │  │
│   └──────────────────┘                            └──────┬───────┘  │
│                                                          │           │
│                                         ┌────────────────▼────────┐ │
│                                         │       PostgreSQL 16      │ │
│                                         │   - Users & Tenants      │ │
│                                         │   - Projects & Jobs      │ │
│                                         │   - Executions & Leases  │ │
│                                         │   - Outbox Events        │ │
│                                         │   - Workflows            │ │
│                                         │   Port: 5432             │ │
│                                         └────────────────┬────────┘ │
│                                                          │           │
│                     ┌────────────────────────────────────▼─────────┐│
│                     │              Kafka (Redpanda v24.1)           ││
│                     │   Topics: execution-events, result-events     ││
│                     │   Port: 9092 (external) / 29092 (internal)    ││
│                     └────────────┬──────────────────────────────────┘│
│                                  │                                    │
│           ┌──────────────────────▼──────────────────────┐            │
│           │           Worker Service Pool                │            │
│           │  ┌────────┐  ┌────────┐  ┌────────┐         │            │
│           │  │Worker 1│  │Worker 2│  │Worker N│         │            │
│           │  │        │  │        │  │        │         │            │
│           │  └────────┘  └────────┘  └────────┘         │            │
│           │  - Claims execution leases                   │            │
│           │  - Sends heartbeats every 10s               │            │
│           │  - Dispatches HTTP requests to target URLs   │            │
│           │  - Publishes result events to Kafka          │            │
│           └──────────────────────────────────────────────┘            │
│                                                                       │
│   ┌────────────────────┐       ┌────────────────────────────────────┐│
│   │  Scheduler Service │       │        Event Processor             ││
│   │  - Polls due jobs  │       │  - Consumes Kafka result events    ││
│   │  - Triggers DAG    │       │  - Persists execution outcomes     ││
│   │    workflow nodes  │       │  - Updates execution attempts      ││
│   │  - Reclaims stale  │       │  - Emits downstream workflow steps ││
│   │    leases          │       │                                    ││
│   └────────────────────┘       └────────────────────────────────────┘│
│                                                                       │
│   ┌────────────────────┐                                             │
│   │  Valkey (Redis)    │   Rate limiting · Fast counters             │
│   │  Port: 6379        │   Worker liveness projections               │
│   └────────────────────┘   NOT the correctness authority             │
└─────────────────────────────────────────────────────────────────────┘
```

### Data Flow: Job Execution Lifecycle

```
User triggers job (API / cron)
        │
        ▼
API Service creates EXECUTION (status: PENDING)
        │
        ▼
Scheduler polls PENDING executions
        │
        ▼
Worker claims EXECUTION_LEASE (with fencing token)
        │
        ▼
Worker dispatches HTTP request to job.targetUrl
        │
        ▼
Worker publishes ResultEvent → Kafka
        │
        ▼
Event Processor consumes event → updates EXECUTION to SUCCESS/FAILED
        │
        ├─── FAILED + retries remaining → re-enqueue (PENDING)
        └─── FAILED + no retries remaining → status: DEAD
```

### DAG Workflow Execution

```
Workflow Trigger
        │
        ▼
workflow_runs record created (status: RUNNING)
        │
        ▼
Root nodes (no predecessors) → PENDING executions created
        │
        ▼
Workers execute root nodes
        │
        ▼
Event Processor processes results → checks all predecessors complete
        │
        ▼
Unblocked downstream nodes → new PENDING executions created
        │
        ▼
Fan-out: multiple parallel children executed simultaneously
        │
        ▼
Fan-in: waits for ALL predecessors to succeed before triggering
        │
        ▼
workflow_run → SUCCESS when all node_executions complete
```

---

## Technology Stack

### Backend

| Layer | Technology | Version | Purpose |
|---|---|---|---|
| Language | Java | 21 | Backend runtime |
| Framework | Spring Boot | 3.3.4 | Application framework, DI, REST |
| Security | Spring Security + JWT | 6.x | Authentication & authorization |
| Data | Spring Data JPA + Hibernate | 3.x | ORM & repository layer |
| Messaging | Apache Kafka (Redpanda) | v24.1.2 | Async execution event streaming |
| Database | PostgreSQL | 16-alpine | Durable state storage |
| Migrations | Flyway | 10.x | Schema versioning |
| Cache | Valkey (Redis-compatible) | 7.2-alpine | Rate limiting, fast projections |
| Testing | JUnit 5 + Testcontainers | 1.21.4 | Unit & integration tests |
| Architecture | ArchUnit | 1.3.0 | Package dependency enforcement |
| Build | Maven | 3.x | Multi-module build system |

### Frontend

| Layer | Technology | Version | Purpose |
|---|---|---|---|
| Framework | Next.js | 16.2.10 | React meta-framework, routing, SSR/SSG |
| UI | React | 19 | Component model |
| Language | TypeScript | 5.x | Type safety |
| Styling | Tailwind CSS v4 | 4.x | Utility-first CSS |
| Data Fetching | TanStack Query | 5.x | Server state management, caching |
| Workflow Editor | ReactFlow | 11.x | Visual DAG builder |
| Charts | Recharts | 2.x | Analytics & metrics visualizations |
| Code Editor | Monaco Editor | 0.52.x | Job request body / JSON editing |
| Forms | React Hook Form | 7.x | Form state management |
| Validation | Zod | 3.x | Schema validation |
| Icons | Lucide React | latest | Icon library |
| Fonts | Geist (Variable) | — | Typography |

### Infrastructure

| Service | Image | Port | Role |
|---|---|---|---|
| PostgreSQL | `postgres:16-alpine` | 5432 | Primary database |
| Redpanda | `redpandadata/redpanda:v24.1.2` | 9092 | Kafka-compatible message broker |
| Valkey | `valkey/valkey:7.2-alpine` | 6379 | Redis-compatible cache |

---

## Project Structure

```
FlowForge/
├── docker-compose.yml              # Local infrastructure stack
├── .env                            # Environment variable template
├── pom.xml                         # Root Maven parent POM
│
├── libs/                           # Shared internal libraries
│   ├── event-contracts/            # Kafka event payload contracts (shared DTOs)
│   ├── messaging-contracts/        # Kafka topic & key constants
│   └── test-support/               # Shared test helpers, fixtures, and base classes
│
├── services/
│   ├── api-service/                # Primary REST API (Spring Boot)
│   │   └── src/main/
│   │       ├── java/com/flowforge/api/
│   │       │   ├── controller/     # REST controllers (9 controllers)
│   │       │   │   ├── AuthController.java
│   │       │   │   ├── TenantController.java
│   │       │   │   ├── ProjectController.java
│   │       │   │   ├── JobController.java
│   │       │   │   ├── ExecutionController.java
│   │       │   │   ├── WorkflowController.java
│   │       │   │   ├── ApiKeyController.java
│   │       │   │   ├── MembershipController.java
│   │       │   │   └── InternalExecutionController.java
│   │       │   ├── service/        # Business logic layer
│   │       │   ├── repository/     # Spring Data JPA repositories
│   │       │   ├── model/          # JPA entities
│   │       │   ├── dto/            # Request/Response DTOs
│   │       │   ├── security/       # JWT filter, token provider
│   │       │   ├── config/         # Kafka, security, CORS config
│   │       │   ├── exception/      # Global exception handler
│   │       │   └── shared/         # Outbox publisher, lease recovery
│   │       └── resources/
│   │           ├── application.yml
│   │           └── db/migration/   # 12 Flyway migrations (V1–V12)
│   │
│   ├── scheduler-service/          # Cron & lease scheduler
│   ├── worker-service/             # HTTP-dispatching worker nodes
│   ├── event-processor/            # Kafka consumer — result persistence
│   └── result-processor/           # Supplementary result handling
│
└── frontend/                       # Next.js 16 dashboard
    └── src/
        ├── app/
        │   ├── page.tsx            # Marketing landing page (/)
        │   ├── layout.tsx          # Root layout with SEO metadata
        │   ├── global-error.tsx    # React error boundary
        │   ├── not-found.tsx       # 404 page
        │   ├── sitemap.ts          # Auto-generated sitemap
        │   ├── globals.css         # Global styles + prose-doc
        │   ├── docs/               # Documentation portal (/docs)
        │   └── (dashboard)/        # Authenticated app routes
        │       ├── layout.tsx      # Dashboard layout + providers
        │       ├── dashboard/      # Main console (/dashboard)
        │       ├── projects/       # Project management (/projects)
        │       ├── jobs/           # Job CRUD + editor (/jobs)
        │       ├── workflows/      # DAG workflow builder (/workflows)
        │       ├── executions/     # Execution explorer (/executions)
        │       ├── workers/        # Live monitoring (/workers)
        │       ├── analytics/      # Analytics & observability (/analytics)
        │       ├── settings/       # Administration (/settings)
        │       └── apikeys/        # API key management (/apikeys)
        ├── components/
        │   ├── ui/                 # Design system components
        │   ├── layout/             # Sidebar, TopNav, CommandPalette
        │   ├── crud/               # EntityTable, EntityToolbar
        │   ├── workflow/           # ReactFlow nodes & edges
        │   ├── auth/               # ProtectedRoute
        │   └── onboarding/         # Guided onboarding banner
        ├── providers/              # React context providers
        │   ├── AuthProvider.tsx
        │   ├── ThemeProvider.tsx
        │   ├── QueryProvider.tsx
        │   ├── SessionProvider.tsx
        │   ├── PermissionProvider.tsx
        │   └── UserPreferencesProvider.tsx
        ├── services/               # API service layer (17 files)
        │   ├── auth.ts, jobs.ts, projects.ts, workflows.ts
        │   ├── executions.ts, apikeys.ts, analytics.ts
        │   ├── metrics.ts, health.ts, activity.ts
        │   ├── settings.ts, monitoring/live-events.ts
        │   └── ...
        └── lib/
            ├── api.ts              # Axios-based API client
            └── utils.ts            # cn() utility
```

---

## Prerequisites

| Requirement | Version | Notes |
|---|---|---|
| Java | 21+ | Required for backend services |
| Maven | 3.9+ | Or use the included `mvnw` / `mvnw.cmd` wrapper |
| Node.js | 20+ | Required for frontend |
| npm | 10+ | Package manager |
| Docker | 24+ | Required for PostgreSQL, Redpanda, Valkey |
| Docker Compose | 2.x | For infrastructure stack |

---

## Quick Start

### 1. Clone the Repository

```bash
git clone https://github.com/raushan-2004/FlowForge.git
cd FlowForge
```

### 2. Start Infrastructure Services

```bash
# Starts PostgreSQL, Valkey, and Redpanda (Kafka)
docker compose up -d postgres valkey redpanda

# Verify all services are healthy
docker compose ps
```

### 3. Start the API Service

**Linux / macOS:**
```bash
export FLOWFORGE_DB_URL=jdbc:postgresql://localhost:5432/flowforge
export FLOWFORGE_DB_USERNAME=postgres
export FLOWFORGE_DB_PASSWORD=local_dev_password_change_me
export FLOWFORGE_KAFKA_BOOTSTRAP_SERVERS=localhost:9092
export FLOWFORGE_VALKEY_HOST=localhost
export FLOWFORGE_VALKEY_PORT=6379
export SPRING_PROFILES_ACTIVE=local

./mvnw spring-boot:run -pl services/api-service
```

**Windows (PowerShell):**
```powershell
$env:FLOWFORGE_DB_URL = "jdbc:postgresql://localhost:5432/flowforge"
$env:FLOWFORGE_DB_USERNAME = "postgres"
$env:FLOWFORGE_DB_PASSWORD = "local_dev_password_change_me"
$env:FLOWFORGE_KAFKA_BOOTSTRAP_SERVERS = "localhost:9092"
$env:FLOWFORGE_VALKEY_HOST = "localhost"
$env:FLOWFORGE_VALKEY_PORT = "6379"
$env:SPRING_PROFILES_ACTIVE = "local"

.\mvnw.cmd spring-boot:run -pl services/api-service
```

The API service will start on **http://localhost:8080**.

Flyway runs automatically on startup and applies all 12 schema migrations. The seed user (V12) is created automatically.

### 4. Start the Frontend

```bash
cd frontend
npm install
npm run dev
```

The dashboard opens at **http://localhost:3000**.

### 5. Log In

| Field | Value |
|---|---|
| Email | `login@flowforge.com` |
| Password | `supersecret123` |

> The default credentials are seeded by Flyway migration `V12__Seed_Default_User.sql`. Change these before any production deployment.

---

## Environment Variables

All environment variables are documented in [`.env`](.env).

| Variable | Default | Description |
|---|---|---|
| `FLOWFORGE_DB_URL` | `jdbc:postgresql://localhost:5432/flowforge` | PostgreSQL JDBC URL |
| `FLOWFORGE_DB_USERNAME` | `postgres` | Database username |
| `FLOWFORGE_DB_PASSWORD` | `local_dev_password_change_me` | Database password |
| `FLOWFORGE_KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka bootstrap servers |
| `FLOWFORGE_VALKEY_HOST` | `localhost` | Valkey (Redis-compatible) host |
| `FLOWFORGE_VALKEY_PORT` | `6379` | Valkey port |
| `SPRING_PROFILES_ACTIVE` | `local` | Spring active profile |

> **Note:** Automated tests (`./mvnw clean verify`) do **not** require these variables — Testcontainers provisions ephemeral containers dynamically for each test run.

### Frontend Environment

The frontend reads its API base URL from `NEXT_PUBLIC_API_BASE_URL`. In development, it defaults to `http://localhost:8080`. Create `frontend/.env.local` for local overrides:

```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

---

## Database Schema

FlowForge uses **Flyway** for schema management. All migrations live in:
`services/api-service/src/main/resources/db/migration/`

| Migration | Description |
|---|---|
| `V1` | Users, Tenants, Tenant Memberships, Projects, API Keys |
| `V2` | Audit fields (`created_by`, `updated_by`) on core tables |
| `V3` | API Key audit fields (`name`, `last_used_at`) |
| `V4` | Jobs — HTTP config, schedule, retry policy, cron expression |
| `V5` | Executions — lifecycle state machine; Execution Attempts — per-attempt tracking |
| `V6` | Outbox Events — transactional outbox table for Kafka publishing |
| `V7` | Jobs scheduler metadata — `next_run_at`, `last_run_at` |
| `V8` | Workers, Worker Capabilities, Execution Leases |
| `V9` | Execution Attempts — `http_status_code`, `response_body`, `error_message` |
| `V10` | Retry metadata on executions — `retry_reason`, `retry_count` |
| `V11` | Workflow Definitions, Workflow Runs, Node Executions |
| `V12` | Seed default tenant, user (`login@flowforge.com`), and project |

### Core Entity Relationships

```
tenants ──< tenant_memberships >── users
   │
   └──< projects ──< jobs ──< executions ──< execution_attempts
                │                │
                │                └──< execution_leases
                │
                └──< api_keys
                │
                └──< workflow_definitions ──< workflow_runs ──< node_executions

outbox_events  (decoupled — published by API service, consumed by event-processor)
workers ──< worker_capabilities
```

---

## API Reference

Base URL: `http://localhost:8080/api/v1`

All endpoints require authentication unless noted. Pass the JWT as:
```
Authorization: Bearer <token>
```

Or use a project API key:
```
X-API-Key: <key>
```

### Authentication

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/auth/login` | None | Authenticate; returns JWT token |
| `POST` | `/auth/refresh` | JWT | Refresh access token |
| `POST` | `/auth/logout` | JWT | Invalidate current session |

**Login example:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "login@flowforge.com", "password": "supersecret123"}'
```

### Tenants

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/tenants/me` | Get current tenant |
| `PUT` | `/tenants/me` | Update tenant settings |
| `GET` | `/tenants/me/members` | List tenant memberships |

### Projects

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/projects` | List all projects in tenant |
| `POST` | `/projects` | Create a new project |
| `GET` | `/projects/{id}` | Get project details |
| `PUT` | `/projects/{id}` | Update project |
| `POST` | `/projects/{id}/archive` | Archive a project |

### Jobs

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/projects/{projectId}/jobs` | List jobs in project |
| `POST` | `/projects/{projectId}/jobs` | Create a job |
| `GET` | `/projects/{projectId}/jobs/{id}` | Get job details |
| `PUT` | `/projects/{projectId}/jobs/{id}` | Update job |
| `DELETE` | `/projects/{projectId}/jobs/{id}` | Delete job |
| `POST` | `/projects/{projectId}/jobs/{id}/trigger` | Manually trigger job |
| `POST` | `/projects/{projectId}/jobs/{id}/enable` | Enable a disabled job |
| `POST` | `/projects/{projectId}/jobs/{id}/disable` | Disable a job |

**Create job example:**
```bash
curl -X POST http://localhost:8080/api/v1/projects/{projectId}/jobs \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Daily ETL",
    "targetUrl": "https://api.example.com/etl/run",
    "httpMethod": "POST",
    "scheduleType": "CRON",
    "cronExpression": "0 2 * * *",
    "timeoutSeconds": 60,
    "retryMaxAttempts": 3,
    "retryStrategy": "EXPONENTIAL_BACKOFF"
  }'
```

### Executions

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/executions` | List executions (filterable by status, job, date) |
| `GET` | `/executions/{id}` | Get execution with all attempts |
| `POST` | `/executions/{id}/retry` | Manually retry a failed/dead execution |
| `GET` | `/executions/{id}/logs` | Streaming execution logs |

### Workflows

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/projects/{projectId}/workflows` | List workflow definitions |
| `POST` | `/projects/{projectId}/workflows` | Create a workflow |
| `GET` | `/projects/{projectId}/workflows/{id}` | Get workflow with definition JSON |
| `PUT` | `/projects/{projectId}/workflows/{id}` | Update workflow definition |
| `DELETE` | `/projects/{projectId}/workflows/{id}` | Delete workflow |
| `POST` | `/projects/{projectId}/workflows/{id}/trigger` | Trigger a workflow run |
| `GET` | `/projects/{projectId}/workflows/{id}/runs` | List workflow runs |

### API Keys

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/projects/{projectId}/keys` | List API keys |
| `POST` | `/projects/{projectId}/keys` | Create API key (returns secret once) |
| `POST` | `/projects/{projectId}/keys/{keyId}/rotate` | Rotate key (new secret, deprecate old) |
| `DELETE` | `/projects/{projectId}/keys/{keyId}` | Revoke API key |

### Monitoring

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/monitoring/stream` | SSE live event stream |
| `GET` | `/monitoring/workers` | Worker health and heartbeat status |
| `GET` | `/monitoring/queues` | Queue depth metrics |

---

## Frontend Application

The frontend is a **Next.js 16 App Router** application with **18 compiled routes**.

### Application Routes

| Route | Description |
|---|---|
| `/` | Marketing landing page with hero, features, architecture, FAQ |
| `/docs` | Searchable documentation portal (13 sections) |
| `/login` | Authentication page |
| `/dashboard` | Main console — metrics, activity feed, system health |
| `/projects` | Project CRUD — list, create, archive |
| `/projects/[id]` | Project detail — members, jobs, keys, settings |
| `/jobs` | Job management — list, filter, search |
| `/jobs/editor` | Job editor with Monaco-powered request body |
| `/workflows` | Workflow list + visual ReactFlow DAG builder |
| `/executions` | Execution explorer with timeline and retry details |
| `/workers` | Live SSE monitoring — workers, queues, Kafka lag, alerts |
| `/analytics` | Analytics dashboards — trends, traces, metrics, reports |
| `/settings` | Administration — profile, org, members, roles, audit, security |
| `/apikeys` | API key administration — create, rotate, revoke |
| `/sitemap.xml` | Auto-generated sitemap |
| `/unauthorized` | 403 page |

### Key UI Components

| Component | Description |
|---|---|
| `Sidebar` | Collapsible navigation with keyboard shortcut hints |
| `CommandPalette` | ⌘K global search and navigation |
| `EntityTable` | Generic sortable, paginated data table |
| `WorkflowCanvas` | ReactFlow DAG editor with validation |
| `ExecutionTimeline` | Attempt-by-attempt execution history |
| `LiveEventFeed` | SSE-powered real-time execution feed |
| `OnboardingBanner` | 6-step guided onboarding for first-time users |
| `AuditLogViewer` | Searchable, filterable, paginated audit log table |

### Design System

The UI uses a custom Tailwind CSS v4 design system built around:
- **Palette:** Slate dark background + Violet/Indigo accent
- **Typography:** Geist Sans (variable) + Geist Mono
- **Components:** `Button`, `Card`, `Badge`, `Dialog`, `StatusBadge`, `Skeleton`, `Spinner`, `MetricCard`, `SearchInput`

---

## Running Tests

### Unit Tests

Run all unit tests (`*Test.java`) across all Maven modules:

```bash
./mvnw test
# or on Windows
.\mvnw.cmd test
```

### Integration Tests

Integration tests (`*IT.java`) use **Testcontainers** — Docker must be running. They provision real PostgreSQL, Kafka, and Valkey containers:

```bash
./mvnw verify
# or on Windows
.\mvnw.cmd verify
```

### Individual Service Tests

```bash
# Run tests only for the API service
./mvnw test -pl services/api-service

# Run integration tests only for the API service
./mvnw verify -pl services/api-service -Dit.test="*IT"
```

### Architecture Tests (ArchUnit)

ArchUnit tests enforce package dependency rules and layered architecture constraints. They run as part of `./mvnw test`.

### Frontend Type Checking

```bash
cd frontend
npx tsc --noEmit --skipLibCheck
```

### Frontend Production Build (Full Verification)

```bash
cd frontend
npm run build
```

Expected output: 18 routes compiled with `✓ Compiled successfully`.

---

## Development Workflow

### Adding a New API Endpoint

1. Create the controller method in the appropriate `*Controller.java`
2. Add request/response DTOs in `dto/`
3. Implement business logic in the `service/` layer
4. Add repository methods if needed in `repository/`
5. Write unit tests in `*Test.java` and integration tests in `*IT.java`

### Adding a New Frontend Page

1. Create the route folder in `frontend/src/app/(dashboard)/`
2. Add `page.tsx` with the `"use client"` directive
3. Add the route to the Sidebar navigation in `components/layout/Sidebar.tsx`
4. Add the route to the CommandPalette in `components/layout/CommandPalette.tsx`
5. Create or extend the matching service file in `services/`

### Adding a Database Migration

1. Create a new `V{N+1}__Description.sql` file in:
   `services/api-service/src/main/resources/db/migration/`
2. Migrations are applied automatically on the next service startup
3. Never modify existing migration files

### Code Style

- **Java:** Follow standard Spring Boot conventions. Services handle business logic; controllers handle HTTP concerns only.
- **TypeScript:** Strict mode. No `any`. Extract reusable logic into services (`src/services/`) and hooks.
- **CSS:** Use Tailwind utilities. No inline styles. Design system tokens only.

---

## Deployment

### Docker (Recommended for Production)

Build the API service JAR:
```bash
./mvnw clean package -pl services/api-service -DskipTests
```

Build a Docker image:
```bash
docker build -t flowforge-api:latest services/api-service/
```

Build the frontend:
```bash
cd frontend && npm run build
```

### Environment Checklist (Production)

| Item | Action Required |
|---|---|
| `FLOWFORGE_DB_PASSWORD` | Change from default `local_dev_password_change_me` |
| JWT Secret | Set a strong random secret (min 256 bits) |
| Default user password | Change `supersecret123` immediately after first login |
| CORS origins | Restrict `ALLOWED_ORIGINS` to your production domain |
| Database | Use a managed PostgreSQL service (e.g., RDS, Cloud SQL) |
| Kafka | Use a managed Kafka cluster or Redpanda Cloud |
| TLS | Terminate TLS at your load balancer or reverse proxy |
| Rate limiting | Verify Valkey rate limiting is configured for your expected load |

### Health Checks

| Service | Endpoint |
|---|---|
| API Service | `GET http://localhost:8080/actuator/health` |
| PostgreSQL | `pg_isready -U postgres -d flowforge` |
| Valkey | `valkey-cli ping` → `PONG` |
| Redpanda | `rpk cluster info` |

---

## Contributing

1. **Fork** the repository
2. **Branch** from `main`: `git checkout -b feat/your-feature`
3. **Implement** your changes with tests
4. **Verify** the build: `./mvnw verify` and `npm run build` (frontend)
5. **Commit** with conventional commits: `feat:`, `fix:`, `docs:`, `refactor:`
6. **Open a Pull Request** against `main`

### Commit Convention

```
feat: add bulk job trigger endpoint
fix: prevent stale lease reclaim race condition
docs: update API reference with workflow endpoints
refactor: extract execution state machine into dedicated service
test: add integration test for workflow DAG fan-out
```

---

## Changelog

### v1.0.0 — July 2026 · *Initial Production Release*

#### Backend
- Multi-tenant platform with full JWT authentication and project-scoped API keys
- 9 REST controllers covering all platform entities
- Transactional outbox pattern for reliable Kafka event publishing
- DAG workflow execution with fan-out/fan-in support and cycle detection
- Heartbeated worker pool with automatic stale-lease recovery
- Configurable retry engine: exponential backoff, linear backoff, fixed delay
- Dead-letter queue management
- Optimistic concurrency control on all mutable entities
- 12 Flyway database migrations covering the complete schema
- ArchUnit architecture enforcement tests
- Testcontainers-based integration test suite

#### Frontend
- 18-route Next.js 16 App Router application
- Marketing landing page with sticky nav, hero, features, architecture, FAQ
- Searchable documentation portal (13 sections)
- Visual ReactFlow DAG workflow builder with live validation
- Execution Explorer with timeline, attempt history, and retry context
- Live SSE Operations Center — workers, queue depths, Kafka lag, system alerts
- Recharts analytics — execution trends, heatmaps, retry analysis, distributed traces
- Full settings administration — profile, organization, members, roles, audit logs, security, preferences, feature flags
- Guided 6-step onboarding banner
- Global React error boundary
- SEO: full Open Graph, Twitter Cards, sitemap, robots.txt
- PWA Web App Manifest
- Zero TypeScript errors · Zero build warnings

---

<div align="center">

**Built with ♥ for engineering teams who take reliability seriously.**

[Documentation](http://localhost:3000/docs) · [Launch Console](http://localhost:3000) · [GitHub](https://github.com/raushan-2004/FlowForge)

</div>
]]>
