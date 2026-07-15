"use client";

import * as React from "react";
import Link from "next/link";
import {
  BookOpen, Search, ChevronRight, ArrowLeft, Menu, X,
  Rocket, Lock, FolderKanban, Cpu, GitBranch, Activity,
  BarChart3, Settings, Key, Keyboard, Code2, Clock, FileText,
  Server, Zap, CheckCircle2, AlertTriangle, Info,
} from "lucide-react";
import { cn } from "@/lib/utils";

// ─── Doc sections ─────────────────────────────────────────────────────────────

interface DocSection {
  id: string;
  label: string;
  icon: React.ReactNode;
  content: React.ReactNode;
}

const DOC_SECTIONS: DocSection[] = [
  {
    id: "getting-started",
    label: "Getting Started",
    icon: <Rocket className="h-4 w-4" />,
    content: (
      <div className="prose-doc">
        <h1>Getting Started</h1>
        <p>FlowForge is a distributed job orchestration and workflow execution engine. Get up and running in under 5 minutes.</p>
        <h2>Prerequisites</h2>
        <ul>
          <li>Docker &amp; Docker Compose</li>
          <li>Java 21+ (for backend development)</li>
          <li>Node.js 20+ (for frontend development)</li>
          <li>PostgreSQL 16 (via Docker)</li>
        </ul>
        <h2>Quick Start</h2>
        <p>1. Clone the repository and start the infrastructure:</p>
        <CodeBlock code={`git clone https://github.com/raushan-2004/FlowForge\ncd FlowForge\ndocker compose up -d postgres valkey`} />
        <p>2. Start the API service:</p>
        <CodeBlock code={`$env:FLOWFORGE_DB_URL="jdbc:postgresql://localhost:5432/flowforge"\n$env:FLOWFORGE_DB_USERNAME="postgres"\n$env:FLOWFORGE_DB_PASSWORD="local_dev_password_change_me"\n.\\mvnw.cmd spring-boot:run -pl services/api-service`} />
        <p>3. Start the frontend:</p>
        <CodeBlock code={`cd frontend && npm install && npm run dev`} />
        <p>4. Open <a href="http://localhost:3000">http://localhost:3000</a> and sign in with:</p>
        <ul>
          <li><strong>Email:</strong> login@flowforge.com</li>
          <li><strong>Password:</strong> supersecret123</li>
        </ul>
        <CallOut type="tip">Your data is stored in a persistent Docker volume — stopping and restarting containers will not lose your work.</CallOut>
      </div>
    ),
  },
  {
    id: "architecture",
    label: "Architecture",
    icon: <Server className="h-4 w-4" />,
    content: (
      <div className="prose-doc">
        <h1>Architecture Overview</h1>
        <p>FlowForge is a microservices-based platform built for horizontal scalability, fault tolerance, and operational clarity.</p>
        <h2>Components</h2>
        <h3>API Service (Spring Boot 3.3)</h3>
        <p>The central control plane. Handles all REST API requests, tenant authentication, job/workflow management, and publishes execution events to Kafka.</p>
        <h3>Scheduler Service</h3>
        <p>Polls PostgreSQL for due executions based on cron expressions and triggers. Claims execution leases to prevent duplicate dispatch.</p>
        <h3>Worker Pool</h3>
        <p>Distributed HTTP-dispatching workers that claim leases, execute jobs, and send heartbeats. Stale leases are automatically recovered by the scheduler.</p>
        <h3>Event Processor</h3>
        <p>Consumes Kafka execution result events and persists them to PostgreSQL. Implements the transactional outbox pattern for reliable event delivery.</p>
        <h3>PostgreSQL 16</h3>
        <p>The durable state authority. All executions, attempts, projects, jobs, and workflows are stored here. Flyway manages schema migrations.</p>
        <h3>Kafka (Redpanda)</h3>
        <p>Used for asynchronous execution result streaming between workers and the event processor. Enables decoupled, at-least-once delivery.</p>
        <h3>Valkey</h3>
        <p>Redis-compatible cache for rate limiting, fast counters, and worker liveness projections. Not used as a correctness authority.</p>
        <CallOut type="important">Correctness (idempotency, exactly-once job execution) is guaranteed by PostgreSQL conditional state mutations and fencing tokens — not by Kafka or Valkey.</CallOut>
      </div>
    ),
  },
  {
    id: "authentication",
    label: "Authentication",
    icon: <Lock className="h-4 w-4" />,
    content: (
      <div className="prose-doc">
        <h1>Authentication</h1>
        <p>FlowForge uses JWT bearer tokens for dashboard authentication and hashed API keys for programmatic access.</p>
        <h2>Login</h2>
        <CodeBlock code={`POST /api/v1/auth/login\nContent-Type: application/json\n\n{\n  "email": "login@flowforge.com",\n  "password": "supersecret123"\n}`} />
        <p>Returns a JWT token valid for 24 hours. Include it as:</p>
        <CodeBlock code={`Authorization: Bearer <jwt-token>`} />
        <h2>API Keys</h2>
        <p>For programmatic job dispatch, create an API key per project. Keys are only displayed once at creation time and are stored as bcrypt hashes.</p>
        <CodeBlock code={`POST /api/v1/projects/{projectId}/keys\nAuthorization: Bearer <token>\n\n{ "name": "CI-CD Deployer" }`} />
        <p>Use the returned key as:</p>
        <CodeBlock code={`X-API-Key: ff_live_abc123...`} />
        <CallOut type="warning">API key secrets are shown only once. Store them securely in your secrets manager.</CallOut>
      </div>
    ),
  },
  {
    id: "projects",
    label: "Projects",
    icon: <FolderKanban className="h-4 w-4" />,
    content: (
      <div className="prose-doc">
        <h1>Projects</h1>
        <p>Projects are the top-level namespace for organizing jobs, workflows, and API keys within a tenant.</p>
        <h2>Create a Project</h2>
        <CodeBlock code={`POST /api/v1/projects\n\n{\n  "name": "Data Pipeline",\n  "description": "ETL and reporting pipelines"\n}`} />
        <h2>List Projects</h2>
        <CodeBlock code={`GET /api/v1/projects`} />
        <h2>Archive a Project</h2>
        <p>Archiving a project suspends all its jobs and prevents new executions. Existing data is preserved.</p>
        <CodeBlock code={`POST /api/v1/projects/{projectId}/archive`} />
      </div>
    ),
  },
  {
    id: "jobs",
    label: "Jobs",
    icon: <Cpu className="h-4 w-4" />,
    content: (
      <div className="prose-doc">
        <h1>Jobs</h1>
        <p>A Job defines an HTTP endpoint to call, a schedule (cron or one-shot), a retry policy, and a timeout.</p>
        <h2>Create a Job</h2>
        <CodeBlock code={`POST /api/v1/projects/{projectId}/jobs\n\n{\n  "name": "Daily Report",\n  "targetUrl": "https://api.example.com/reports/generate",\n  "httpMethod": "POST",\n  "cronExpression": "0 2 * * *",\n  "maxRetries": 3,\n  "retryPolicy": "EXPONENTIAL_BACKOFF",\n  "timeoutSeconds": 30\n}`} />
        <h2>Trigger Manually</h2>
        <CodeBlock code={`POST /api/v1/projects/{projectId}/jobs/{jobId}/trigger`} />
        <h2>Retry Policies</h2>
        <ul>
          <li><code>EXPONENTIAL_BACKOFF</code> — 1s, 2s, 4s, 8s...</li>
          <li><code>LINEAR_BACKOFF</code> — fixed interval between retries</li>
          <li><code>FIXED_DELAY</code> — constant delay</li>
          <li><code>NONE</code> — no retries</li>
        </ul>
      </div>
    ),
  },
  {
    id: "workflows",
    label: "Workflows",
    icon: <GitBranch className="h-4 w-4" />,
    content: (
      <div className="prose-doc">
        <h1>Workflows</h1>
        <p>Workflows chain multiple jobs into directed acyclic graphs (DAGs). Each node in the DAG is a job; edges define execution order.</p>
        <h2>Create a Workflow</h2>
        <CodeBlock code={`POST /api/v1/projects/{projectId}/workflows\n\n{\n  "name": "ETL Pipeline",\n  "description": "Extract, transform, and load data"\n}`} />
        <h2>Add Nodes &amp; Edges</h2>
        <p>Use the visual Workflow Builder at <code>/workflows</code> to drag and connect job nodes. The builder validates for cycles and unreachable nodes in real-time.</p>
        <h2>Trigger a Workflow</h2>
        <CodeBlock code={`POST /api/v1/projects/{projectId}/workflows/{workflowId}/trigger`} />
        <CallOut type="info">Fan-out is supported: a single job node can fan out to multiple downstream jobs, all of which execute in parallel.</CallOut>
      </div>
    ),
  },
  {
    id: "executions",
    label: "Executions",
    icon: <Activity className="h-4 w-4" />,
    content: (
      <div className="prose-doc">
        <h1>Executions</h1>
        <p>An Execution represents a single run of a job or workflow. Each execution has a lifecycle: PENDING → PROCESSING → SUCCESS | FAILED | DEAD.</p>
        <h2>Execution States</h2>
        <ul>
          <li><strong>PENDING</strong> — queued, waiting for a worker</li>
          <li><strong>PROCESSING</strong> — claimed by a worker, running</li>
          <li><strong>SUCCESS</strong> — completed successfully</li>
          <li><strong>FAILED</strong> — failed, eligible for retry</li>
          <li><strong>DEAD</strong> — all retries exhausted</li>
        </ul>
        <h2>View Execution Details</h2>
        <CodeBlock code={`GET /api/v1/executions/{executionId}`} />
        <h2>Manual Retry</h2>
        <CodeBlock code={`POST /api/v1/executions/{executionId}/retry`} />
      </div>
    ),
  },
  {
    id: "monitoring",
    label: "Monitoring",
    icon: <Activity className="h-4 w-4" />,
    content: (
      <div className="prose-doc">
        <h1>Live Monitoring</h1>
        <p>The Operations Center at <code>/workers</code> provides a real-time view of your system using Server-Sent Events (SSE).</p>
        <h2>What is monitored</h2>
        <ul>
          <li>Worker heartbeat status and utilization</li>
          <li>Queue depths (pending, processing, dead-letter)</li>
          <li>Kafka consumer lag per topic</li>
          <li>Active executions feed</li>
          <li>Scheduler health</li>
          <li>Database connection pool</li>
          <li>System alerts</li>
        </ul>
        <h2>SSE Endpoint</h2>
        <CodeBlock code={`GET /api/v1/monitoring/stream\nAccept: text/event-stream`} />
      </div>
    ),
  },
  {
    id: "analytics",
    label: "Analytics",
    icon: <BarChart3 className="h-4 w-4" />,
    content: (
      <div className="prose-doc">
        <h1>Analytics &amp; Observability</h1>
        <p>The Analytics section at <code>/analytics</code> provides historical insights across all dimensions.</p>
        <h2>Available views</h2>
        <ul>
          <li><strong>Overview</strong> — execution trends, retry trends, queue growth, HTTP status distribution</li>
          <li><strong>Workflows</strong> — per-workflow success rates, durations, and retry patterns</li>
          <li><strong>Workers</strong> — per-node utilization, failure rates, execution counts</li>
          <li><strong>Retries</strong> — retry frequency, exhaustion trends, error categories</li>
          <li><strong>Traces</strong> — distributed trace viewer with span timelines</li>
          <li><strong>Metrics</strong> — live metric explorer with filtering</li>
          <li><strong>Reports</strong> — export data as CSV or JSON</li>
        </ul>
      </div>
    ),
  },
  {
    id: "administration",
    label: "Administration",
    icon: <Settings className="h-4 w-4" />,
    content: (
      <div className="prose-doc">
        <h1>Administration</h1>
        <p>The Settings area at <code>/settings</code> provides full organization administration.</p>
        <h2>Sections</h2>
        <ul>
          <li><strong>Profile</strong> — name, avatar, timezone, language</li>
          <li><strong>Organization</strong> — name, slug, default retry policy, branding</li>
          <li><strong>Members</strong> — invite, suspend, remove, change roles</li>
          <li><strong>Roles</strong> — read-only permission capability matrix</li>
          <li><strong>API Keys</strong> — create, rotate, revoke project keys</li>
          <li><strong>Notifications</strong> — email/browser alerts per event type</li>
          <li><strong>Audit Logs</strong> — searchable, filterable, exportable event log</li>
          <li><strong>Security</strong> — active sessions, revocation</li>
          <li><strong>Preferences</strong> — theme, density, date formats</li>
          <li><strong>Feature Flags</strong> — UI-level feature toggles</li>
        </ul>
      </div>
    ),
  },
  {
    id: "api",
    label: "API Overview",
    icon: <Code2 className="h-4 w-4" />,
    content: (
      <div className="prose-doc">
        <h1>API Overview</h1>
        <p>All FlowForge capabilities are accessible via REST API. Base URL: <code>http://localhost:8080/api/v1</code></p>
        <h2>Authentication</h2>
        <p>All endpoints require a valid JWT bearer token or project API key.</p>
        <h2>Key Endpoints</h2>
        <table>
          <thead><tr><th>Method</th><th>Path</th><th>Description</th></tr></thead>
          <tbody>
            <tr><td>POST</td><td>/auth/login</td><td>Authenticate and receive JWT</td></tr>
            <tr><td>GET</td><td>/projects</td><td>List all projects</td></tr>
            <tr><td>POST</td><td>/projects</td><td>Create a project</td></tr>
            <tr><td>GET</td><td>/projects/:id/jobs</td><td>List jobs in a project</td></tr>
            <tr><td>POST</td><td>/projects/:id/jobs</td><td>Create a job</td></tr>
            <tr><td>POST</td><td>/projects/:id/jobs/:jid/trigger</td><td>Trigger a job</td></tr>
            <tr><td>GET</td><td>/executions</td><td>List executions</td></tr>
            <tr><td>POST</td><td>/executions/:id/retry</td><td>Retry a failed execution</td></tr>
            <tr><td>GET</td><td>/monitoring/stream</td><td>SSE live event stream</td></tr>
          </tbody>
        </table>
      </div>
    ),
  },
  {
    id: "shortcuts",
    label: "Keyboard Shortcuts",
    icon: <Keyboard className="h-4 w-4" />,
    content: (
      <div className="prose-doc">
        <h1>Keyboard Shortcuts</h1>
        <table>
          <thead><tr><th>Shortcut</th><th>Action</th></tr></thead>
          <tbody>
            {[
              ["⌘K / Ctrl+K", "Open command palette"],
              ["G then D", "Go to Dashboard"],
              ["G then P", "Go to Projects"],
              ["G then W", "Go to Workflows"],
              ["G then E", "Go to Executions"],
              ["G then A", "Go to Analytics"],
              ["G then S", "Go to Settings"],
              ["Esc", "Close modal / palette"],
            ].map(([k, v]) => (
              <tr key={k}><td><code>{k}</code></td><td>{v}</td></tr>
            ))}
          </tbody>
        </table>
      </div>
    ),
  },
  {
    id: "changelog",
    label: "Release Notes",
    icon: <FileText className="h-4 w-4" />,
    content: (
      <div className="prose-doc">
        <h1>Release Notes</h1>
        <h2>v1.0.0 — July 2026</h2>
        <p>Initial production release.</p>
        <ul>
          <li>Full multi-tenant platform with RBAC and API key management</li>
          <li>DAG workflow builder with ReactFlow visual editor</li>
          <li>Distributed worker pool with heartbeat and lease recovery</li>
          <li>Kafka-backed execution event streaming via transactional outbox</li>
          <li>Real-time SSE operations center with queue and worker monitoring</li>
          <li>Analytics dashboard with Recharts — execution trends, retry analytics, distributed traces</li>
          <li>Full audit log viewer with CSV export</li>
          <li>Settings administration: profile, org, members, notifications, security, preferences, feature flags</li>
          <li>Public landing page with responsive nav and FAQ</li>
          <li>Documentation portal with searchable sidebar</li>
          <li>Guided onboarding flow for first-time users</li>
          <li>SEO metadata, Open Graph, Twitter Cards, Web App Manifest</li>
          <li>Production build verified: TypeScript ✓ · ESLint ✓ · 16 routes compiled</li>
        </ul>
      </div>
    ),
  },
];

// ─── Shared doc components ────────────────────────────────────────────────────

function CodeBlock({ code }: { code: string }) {
  const [copied, setCopied] = React.useState(false);
  function copy() {
    navigator.clipboard.writeText(code);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  }
  return (
    <div className="relative my-4 rounded-lg border border-slate-800 bg-slate-950 overflow-hidden group">
      <button onClick={copy} className="absolute top-2 right-2 text-[10px] text-slate-500 hover:text-slate-300 px-2 py-0.5 rounded border border-slate-800 bg-slate-900 transition-colors opacity-0 group-hover:opacity-100">
        {copied ? "Copied!" : "Copy"}
      </button>
      <pre className="p-4 text-xs font-mono text-slate-300 overflow-x-auto leading-relaxed whitespace-pre">{code}</pre>
    </div>
  );
}

function CallOut({ type, children }: { type: "tip" | "important" | "warning" | "info"; children: React.ReactNode }) {
  const styles = {
    tip:       { icon: <CheckCircle2 className="h-4 w-4" />, cls: "border-emerald-800/50 bg-emerald-950/30 text-emerald-300" },
    important: { icon: <Zap className="h-4 w-4" />,          cls: "border-violet-800/50 bg-violet-950/30 text-violet-300" },
    warning:   { icon: <AlertTriangle className="h-4 w-4" />, cls: "border-amber-800/50 bg-amber-950/30 text-amber-300" },
    info:      { icon: <Info className="h-4 w-4" />,          cls: "border-blue-800/50 bg-blue-950/30 text-blue-300" },
  };
  const { icon, cls } = styles[type];
  return (
    <div className={`my-4 flex items-start gap-3 rounded-lg border px-4 py-3 text-sm ${cls}`}>
      <span className="flex-shrink-0 mt-0.5">{icon}</span>
      <div>{children}</div>
    </div>
  );
}

// ─── Docs Page ────────────────────────────────────────────────────────────────

export default function DocsPage() {
  const [activeId, setActiveId] = React.useState("getting-started");
  const [search, setSearch] = React.useState("");
  const [sidebarOpen, setSidebarOpen] = React.useState(false);

  const filtered = DOC_SECTIONS.filter(
    (s) => !search || s.label.toLowerCase().includes(search.toLowerCase())
  );

  const active = DOC_SECTIONS.find((s) => s.id === activeId) ?? DOC_SECTIONS[0];

  return (
    <div className="flex min-h-screen bg-slate-950 text-slate-100">
      {/* Sidebar */}
      <aside className={cn(
        "fixed inset-y-0 left-0 z-40 w-64 flex-shrink-0 border-r border-slate-800 bg-slate-950 flex flex-col transform transition-transform duration-200",
        sidebarOpen ? "translate-x-0" : "-translate-x-full",
        "md:relative md:translate-x-0"
      )}>
        {/* Sidebar header */}
        <div className="flex items-center justify-between px-4 py-4 border-b border-slate-800">
          <Link href="/" className="flex items-center gap-2 text-sm font-semibold text-slate-200 hover:text-white">
            <div className="h-6 w-6 rounded-md bg-gradient-to-br from-violet-600 to-indigo-600 flex items-center justify-center font-bold text-white text-xs">F</div>
            FlowForge Docs
          </Link>
          <button className="md:hidden text-slate-400" onClick={() => setSidebarOpen(false)} aria-label="Close sidebar">
            <X className="h-4 w-4" />
          </button>
        </div>

        {/* Search */}
        <div className="px-3 py-3 border-b border-slate-800">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-3.5 w-3.5 text-slate-500 pointer-events-none" />
            <input
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Search docs..."
              className="w-full h-8 pl-8 pr-3 rounded-md border border-slate-800 bg-slate-900 text-xs text-slate-200 placeholder:text-slate-500 focus:outline-none focus:ring-2 focus:ring-violet-500"
            />
          </div>
        </div>

        {/* Nav links */}
        <nav className="flex-1 overflow-y-auto py-3 px-2 space-y-0.5">
          {filtered.map((s) => (
            <button
              key={s.id}
              onClick={() => { setActiveId(s.id); setSidebarOpen(false); }}
              className={cn(
                "w-full flex items-center gap-2.5 px-3 py-2 rounded-md text-sm font-medium transition-all text-left",
                activeId === s.id
                  ? "bg-violet-950/50 text-violet-300 border border-violet-800/40"
                  : "text-slate-400 hover:text-slate-200 hover:bg-slate-800/50 border border-transparent"
              )}
              aria-current={activeId === s.id ? "page" : undefined}
            >
              {s.icon}
              {s.label}
            </button>
          ))}
          {filtered.length === 0 && (
            <p className="text-xs text-slate-500 px-3 py-2">No results for &quot;{search}&quot;</p>
          )}
        </nav>

        {/* Sidebar footer */}
        <div className="px-4 py-3 border-t border-slate-800">
          <Link href="/" className="flex items-center gap-1.5 text-xs text-slate-500 hover:text-slate-300 transition-colors">
            <ArrowLeft className="h-3 w-3" /> Back to home
          </Link>
        </div>
      </aside>

      {/* Mobile overlay */}
      {sidebarOpen && (
        <div className="md:hidden fixed inset-0 z-30 bg-slate-950/70" onClick={() => setSidebarOpen(false)} />
      )}

      {/* Content */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* Top bar */}
        <header className="sticky top-0 z-20 border-b border-slate-800 bg-slate-950/95 backdrop-blur-sm px-4 md:px-8 h-14 flex items-center gap-3">
          <button className="md:hidden p-1.5 text-slate-400 hover:text-slate-200" onClick={() => setSidebarOpen(true)} aria-label="Open sidebar">
            <Menu className="h-5 w-5" />
          </button>
          <div className="flex items-center gap-1.5 text-xs text-slate-500">
            <BookOpen className="h-3.5 w-3.5" />
            <span>Documentation</span>
            <ChevronRight className="h-3 w-3" />
            <span className="text-slate-300">{active.label}</span>
          </div>
        </header>

        {/* Doc content */}
        <main className="flex-1 px-6 md:px-12 py-10 max-w-4xl mx-auto w-full">
          <article>
            {active.content}
          </article>

          {/* Prev / Next */}
          <div className="mt-16 pt-8 border-t border-slate-800 flex justify-between">
            {DOC_SECTIONS.indexOf(active) > 0 && (
              <button
                onClick={() => setActiveId(DOC_SECTIONS[DOC_SECTIONS.indexOf(active) - 1].id)}
                className="flex items-center gap-2 text-sm text-slate-400 hover:text-violet-400 transition-colors"
              >
                <ArrowLeft className="h-4 w-4" />
                {DOC_SECTIONS[DOC_SECTIONS.indexOf(active) - 1].label}
              </button>
            )}
            {DOC_SECTIONS.indexOf(active) < DOC_SECTIONS.length - 1 && (
              <button
                onClick={() => setActiveId(DOC_SECTIONS[DOC_SECTIONS.indexOf(active) + 1].id)}
                className="ml-auto flex items-center gap-2 text-sm text-slate-400 hover:text-violet-400 transition-colors"
              >
                {DOC_SECTIONS[DOC_SECTIONS.indexOf(active) + 1].label}
                <ChevronRight className="h-4 w-4" />
              </button>
            )}
          </div>
        </main>
      </div>
    </div>
  );
}
