"use client";

import * as React from "react";
import Link from "next/link";
import {
  ArrowRight, Cpu, GitBranch, ShieldCheck, BarChart3, Activity,
  Zap, Database, Network, Server, CheckCircle2, ChevronDown,
  Star, Menu, X, ExternalLink, Play, Eye, Settings2, Users,
  Clock, RefreshCw, Globe, Lock, Layers, Terminal, BookOpen,
} from "lucide-react";

// ─── Sticky Nav ───────────────────────────────────────────────────────────────

function PublicNav() {
  const [scrolled, setScrolled] = React.useState(false);
  const [mobileOpen, setMobileOpen] = React.useState(false);

  React.useEffect(() => {
    const fn = () => setScrolled(window.scrollY > 20);
    window.addEventListener("scroll", fn, { passive: true });
    return () => window.removeEventListener("scroll", fn);
  }, []);

  const links = [
    { label: "Features", href: "#features" },
    { label: "Architecture", href: "#architecture" },
    { label: "Docs", href: "/docs" },
    { label: "Demo", href: "/login" },
  ];

  return (
    <header
      className={`fixed top-0 left-0 right-0 z-50 transition-all duration-300 ${
        scrolled ? "bg-slate-950/95 backdrop-blur-md border-b border-slate-800/60 shadow-lg" : "bg-transparent"
      }`}
    >
      <div className="max-w-7xl mx-auto px-6 h-16 flex items-center justify-between">
        {/* Brand */}
        <Link href="/" className="flex items-center gap-2.5 group" aria-label="FlowForge home">
          <div className="h-8 w-8 rounded-lg bg-gradient-to-br from-violet-600 to-indigo-600 flex items-center justify-center font-bold text-white text-sm shadow-lg shadow-violet-900/40 group-hover:shadow-violet-700/50 transition-shadow">
            F
          </div>
          <span className="font-bold text-slate-100 text-lg tracking-tight">FlowForge</span>
        </Link>

        {/* Desktop Nav */}
        <nav className="hidden md:flex items-center gap-6" aria-label="Main navigation">
          {links.map((l) => (
            <a key={l.href} href={l.href}
              className="text-sm text-slate-400 hover:text-slate-100 transition-colors font-medium">
              {l.label}
            </a>
          ))}
        </nav>

        {/* CTAs */}
        <div className="hidden md:flex items-center gap-3">
          <Link href="/login" className="text-sm text-slate-400 hover:text-slate-200 font-medium transition-colors">
            Sign In
          </Link>
          <Link href="/login"
            className="inline-flex items-center gap-1.5 h-9 px-4 rounded-lg bg-violet-600 hover:bg-violet-500 text-white text-sm font-semibold transition-all shadow-md shadow-violet-900/40">
            Launch Console <ArrowRight className="h-3.5 w-3.5" />
          </Link>
        </div>

        {/* Mobile menu button */}
        <button className="md:hidden p-2 text-slate-400" onClick={() => setMobileOpen(o => !o)} aria-label="Toggle menu">
          {mobileOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
        </button>
      </div>

      {/* Mobile Menu */}
      {mobileOpen && (
        <div className="md:hidden bg-slate-950 border-t border-slate-800 px-6 py-4 space-y-3">
          {links.map((l) => (
            <a key={l.href} href={l.href} onClick={() => setMobileOpen(false)}
              className="block text-sm text-slate-300 hover:text-white py-2 font-medium">
              {l.label}
            </a>
          ))}
          <Link href="/login" onClick={() => setMobileOpen(false)}
            className="block w-full text-center h-10 leading-10 rounded-lg bg-violet-600 hover:bg-violet-500 text-white text-sm font-semibold transition-colors">
            Launch Console
          </Link>
        </div>
      )}
    </header>
  );
}

// ─── Animated grid background ─────────────────────────────────────────────────

function GridBackground() {
  return (
    <div className="absolute inset-0 overflow-hidden pointer-events-none" aria-hidden="true">
      <div className="absolute inset-0 bg-[linear-gradient(to_right,#1e293b_1px,transparent_1px),linear-gradient(to_bottom,#1e293b_1px,transparent_1px)] bg-[size:64px_64px] opacity-30" />
      <div className="absolute top-0 left-1/2 -translate-x-1/2 w-[800px] h-[400px] bg-violet-600/10 rounded-full blur-3xl" />
      <div className="absolute top-32 right-0 w-[500px] h-[500px] bg-indigo-600/8 rounded-full blur-3xl" />
    </div>
  );
}

// ─── Badge ────────────────────────────────────────────────────────────────────

function Pill({ children }: { children: React.ReactNode }) {
  return (
    <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-violet-950/60 border border-violet-800/40 text-violet-300">
      {children}
    </span>
  );
}

// ─── Hero Section ─────────────────────────────────────────────────────────────

function HeroSection() {
  return (
    <section className="relative pt-32 pb-24 flex flex-col items-center text-center px-6" aria-labelledby="hero-heading">
      <GridBackground />
      <div className="relative z-10 max-w-4xl mx-auto space-y-8">
        <Pill>
          <Zap className="h-3 w-3" /> v1.0 · Production Hardened
        </Pill>

        <h1 id="hero-heading" className="text-5xl md:text-7xl font-extrabold tracking-tight leading-tight text-transparent bg-clip-text bg-gradient-to-br from-slate-100 via-violet-200 to-indigo-300">
          Orchestrate Any<br className="hidden md:block" /> Distributed Workload
        </h1>

        <p className="text-lg md:text-xl text-slate-400 max-w-2xl mx-auto leading-relaxed">
          FlowForge is a resilient, multi-tenant execution engine with conditional DAG routing,
          heartbeated worker pools, real-time monitoring, and sub-millisecond job dispatch — built on
          Spring Boot, Kafka, and PostgreSQL.
        </p>

        <div className="flex flex-col sm:flex-row gap-4 justify-center pt-2">
          <Link href="/login"
            className="inline-flex items-center justify-center gap-2 h-12 px-8 rounded-xl bg-violet-600 hover:bg-violet-500 text-white font-semibold text-sm transition-all shadow-xl shadow-violet-900/40 hover:shadow-violet-700/40 hover:scale-[1.02]">
            <Play className="h-4 w-4" /> Launch Console
          </Link>
          <a href="#features"
            className="inline-flex items-center justify-center gap-2 h-12 px-8 rounded-xl border border-slate-700 bg-slate-900/60 hover:bg-slate-800 text-slate-200 font-semibold text-sm transition-all">
            Explore Features <ChevronDown className="h-4 w-4" />
          </a>
        </div>

        {/* Stats row */}
        <div className="flex flex-wrap justify-center gap-8 pt-8 text-center">
          {[
            { label: "Max Throughput", value: "50K jobs/min" },
            { label: "Failover Time", value: "< 30s" },
            { label: "API Latency", value: "< 5ms p99" },
          ].map(({ label, value }) => (
            <div key={label}>
              <p className="text-2xl font-bold text-violet-300">{value}</p>
              <p className="text-xs text-slate-500 mt-0.5">{label}</p>
            </div>
          ))}
        </div>
      </div>

      {/* Mock terminal preview */}
      <div className="relative z-10 mt-16 w-full max-w-3xl mx-auto">
        <div className="rounded-2xl border border-slate-800 bg-slate-900/80 backdrop-blur-sm shadow-2xl overflow-hidden">
          <div className="flex items-center gap-2 px-4 py-3 bg-slate-900 border-b border-slate-800">
            <div className="h-3 w-3 rounded-full bg-red-500/70" />
            <div className="h-3 w-3 rounded-full bg-yellow-500/70" />
            <div className="h-3 w-3 rounded-full bg-emerald-500/70" />
            <span className="ml-2 text-xs font-mono text-slate-500">flowforge — execution console</span>
          </div>
          <div className="p-5 font-mono text-xs space-y-2 text-left">
            <div className="text-slate-500"># Trigger a distributed workflow execution</div>
            <div><span className="text-violet-400">POST</span> <span className="text-slate-300">/api/v1/projects/</span><span className="text-emerald-400">proj_abc123</span><span className="text-slate-300">/jobs/</span><span className="text-emerald-400">job_xyz789</span><span className="text-slate-300">/trigger</span></div>
            <div className="text-slate-500">{"{"}</div>
            <div className="pl-4 text-slate-300">&quot;input&quot;: {"{"} &quot;dataset&quot;: &quot;q4-reports&quot;, &quot;parallel&quot;: 16 {"}"}</div>
            <div className="text-slate-500">{"}"}</div>
            <div className="mt-2 text-emerald-400">✓ Execution queued · ID: exec_pr8xk2 · ETA: 340ms</div>
            <div className="text-slate-500">→ Worker <span className="text-blue-400">worker-node-03</span> claimed lease · started at 11:42:07.331</div>
            <div className="text-slate-500">→ DAG stage <span className="text-violet-400">validate → transform → persist</span> routing active</div>
            <div className="text-emerald-400">✓ Execution completed in 412ms · status: SUCCESS</div>
          </div>
        </div>
      </div>
    </section>
  );
}

// ─── Features Grid ────────────────────────────────────────────────────────────

const FEATURES = [
  {
    icon: <GitBranch className="h-6 w-6" />,
    title: "DAG Workflow Engine",
    description: "Build complex fan-out/fan-in pipelines with cycle detection, reachability validation, and conditional routing.",
    color: "text-violet-400",
    bg: "bg-violet-950/40 border-violet-800/40",
  },
  {
    icon: <Cpu className="h-6 w-6" />,
    title: "Distributed Workers",
    description: "Heartbeated worker pool with automatic lease recovery, liveness detection, and dead-letter handling.",
    color: "text-blue-400",
    bg: "bg-blue-950/40 border-blue-800/40",
  },
  {
    icon: <Activity className="h-6 w-6" />,
    title: "Live Monitoring",
    description: "Real-time SSE-powered execution feed, queue depth, Kafka lag, worker health, and system alerts.",
    color: "text-emerald-400",
    bg: "bg-emerald-950/40 border-emerald-800/40",
  },
  {
    icon: <BarChart3 className="h-6 w-6" />,
    title: "Analytics & Observability",
    description: "Distributed tracing, metrics explorer, execution heatmaps, retry analytics, and CSV/JSON reports.",
    color: "text-amber-400",
    bg: "bg-amber-950/40 border-amber-800/40",
  },
  {
    icon: <ShieldCheck className="h-6 w-6" />,
    title: "Multi-Tenant Isolation",
    description: "Project-scoped API keys, RBAC permissions, tenant membership, and full audit log trails.",
    color: "text-indigo-400",
    bg: "bg-indigo-950/40 border-indigo-800/40",
  },
  {
    icon: <Zap className="h-6 w-6" />,
    title: "API-First Architecture",
    description: "Full REST API with idempotent job dispatch, versioned endpoints, and OpenAPI documentation.",
    color: "text-rose-400",
    bg: "bg-rose-950/40 border-rose-800/40",
  },
  {
    icon: <RefreshCw className="h-6 w-6" />,
    title: "Smart Retry Engine",
    description: "Configurable exponential backoff, dead-letter queues, retry reason tracking, and manual requeue.",
    color: "text-cyan-400",
    bg: "bg-cyan-950/40 border-cyan-800/40",
  },
  {
    icon: <Globe className="h-6 w-6" />,
    title: "HTTP Dispatching",
    description: "Trigger any HTTP endpoint with configurable headers, auth, timeouts, and response validation.",
    color: "text-teal-400",
    bg: "bg-teal-950/40 border-teal-800/40",
  },
];

function FeaturesSection() {
  return (
    <section id="features" className="py-24 px-6" aria-labelledby="features-heading">
      <div className="max-w-7xl mx-auto">
        <div className="text-center mb-16 space-y-4">
          <Pill><Star className="h-3 w-3" /> Core Capabilities</Pill>
          <h2 id="features-heading" className="text-4xl md:text-5xl font-bold tracking-tight text-slate-100">
            Everything you need to<br className="hidden md:block" /> run distributed workflows
          </h2>
          <p className="text-slate-400 max-w-2xl mx-auto">
            From simple cron jobs to complex multi-stage DAG pipelines — FlowForge provides the
            reliability and observability to run them at scale.
          </p>
        </div>

        <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
          {FEATURES.map((f) => (
            <div key={f.title}
              className="group rounded-xl border border-slate-800 bg-slate-900/50 p-5 hover:border-slate-700 hover:-translate-y-1 transition-all duration-200 cursor-default">
              <div className={`inline-flex h-10 w-10 items-center justify-center rounded-lg border ${f.bg} ${f.color} mb-4`}>
                {f.icon}
              </div>
              <h3 className="font-semibold text-slate-100 mb-2">{f.title}</h3>
              <p className="text-sm text-slate-400 leading-relaxed">{f.description}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

// ─── Architecture Overview ────────────────────────────────────────────────────

function ArchitectureSection() {
  const nodes = [
    { icon: <Globe className="h-5 w-5" />, label: "Browser / Client", desc: "Next.js dashboard, REST API", color: "border-violet-700 bg-violet-950/50" },
    { icon: <Server className="h-5 w-5" />, label: "API Service", desc: "Spring Boot · JWT Auth · Rate limiting", color: "border-blue-700 bg-blue-950/50" },
    { icon: <Network className="h-5 w-5" />, label: "Kafka / Redpanda", desc: "Event streaming · Outbox pattern", color: "border-amber-700 bg-amber-950/50" },
    { icon: <Cpu className="h-5 w-5" />, label: "Worker Pool", desc: "Distributed execution · Heartbeat leases", color: "border-emerald-700 bg-emerald-950/50" },
    { icon: <Database className="h-5 w-5" />, label: "PostgreSQL", desc: "Durable state · Flyway migrations", color: "border-cyan-700 bg-cyan-950/50" },
    { icon: <Layers className="h-5 w-5" />, label: "Event Processor", desc: "Result ingestion · Audit events", color: "border-rose-700 bg-rose-950/50" },
  ];

  return (
    <section id="architecture" className="py-24 px-6 bg-slate-900/30 border-y border-slate-800/50" aria-labelledby="arch-heading">
      <div className="max-w-5xl mx-auto">
        <div className="text-center mb-16 space-y-4">
          <Pill><Layers className="h-3 w-3" /> Architecture</Pill>
          <h2 id="arch-heading" className="text-4xl font-bold text-slate-100">Built for resilience at every layer</h2>
          <p className="text-slate-400 max-w-xl mx-auto">
            Every component is designed for fault tolerance, horizontal scalability, and operational clarity.
          </p>
        </div>

        <div className="flex flex-col items-center gap-0">
          {nodes.map((node, i) => (
            <React.Fragment key={node.label}>
              <div className={`w-full max-w-md rounded-xl border p-4 flex items-center gap-4 ${node.color}`}>
                <div className="text-slate-300 flex-shrink-0">{node.icon}</div>
                <div>
                  <p className="font-semibold text-slate-100 text-sm">{node.label}</p>
                  <p className="text-xs text-slate-400 mt-0.5">{node.desc}</p>
                </div>
              </div>
              {i < nodes.length - 1 && (
                <div className="flex flex-col items-center gap-0.5 py-2">
                  <div className="w-px h-3 bg-slate-700" />
                  <ChevronDown className="h-4 w-4 text-slate-600" />
                </div>
              )}
            </React.Fragment>
          ))}
        </div>
      </div>
    </section>
  );
}

// ─── Product Preview Cards ────────────────────────────────────────────────────

function ProductPreviewSection() {
  const panels = [
    {
      icon: <Eye className="h-5 w-5 text-violet-400" />,
      title: "Execution Explorer",
      desc: "Search, filter, and inspect every execution with full timeline, attempt history, retry context, and log streaming.",
      stat: "Full execution lineage",
    },
    {
      icon: <Activity className="h-5 w-5 text-emerald-400" />,
      title: "Operations Center",
      desc: "Live SSE-powered feed of running executions, queue depths, Kafka consumer lag, and worker health.",
      stat: "< 1s update latency",
    },
    {
      icon: <BarChart3 className="h-5 w-5 text-amber-400" />,
      title: "Analytics Dashboard",
      desc: "Recharts-powered dashboards with time-range selection, execution heatmaps, retry trends, and report export.",
      stat: "30+ metric dimensions",
    },
    {
      icon: <Settings2 className="h-5 w-5 text-blue-400" />,
      title: "Workflow Builder",
      desc: "ReactFlow-based visual DAG editor with real-time validation, cycle detection, and one-click execution.",
      stat: "Drag & drop DAG design",
    },
  ];

  return (
    <section className="py-24 px-6" aria-labelledby="preview-heading">
      <div className="max-w-7xl mx-auto">
        <div className="text-center mb-16 space-y-4">
          <Pill><Eye className="h-3 w-3" /> Product Tour</Pill>
          <h2 id="preview-heading" className="text-4xl font-bold text-slate-100">The full operational picture</h2>
        </div>

        <div className="grid gap-6 sm:grid-cols-2">
          {panels.map((p) => (
            <div key={p.title} className="rounded-2xl border border-slate-800 bg-slate-900/50 p-6 hover:border-slate-700 transition-all group">
              <div className="flex items-center gap-3 mb-4">
                <div className="h-9 w-9 rounded-lg border border-slate-700 bg-slate-800 flex items-center justify-center">
                  {p.icon}
                </div>
                <h3 className="font-semibold text-slate-100">{p.title}</h3>
              </div>
              <p className="text-sm text-slate-400 leading-relaxed mb-4">{p.desc}</p>
              <div className="flex items-center justify-between">
                <span className="text-xs font-mono text-violet-400 bg-violet-950/50 border border-violet-800/40 px-2 py-0.5 rounded-full">{p.stat}</span>
                <Link href="/login" className="text-xs text-slate-500 hover:text-violet-400 flex items-center gap-1 transition-colors group-hover:text-violet-400">
                  Try it <ArrowRight className="h-3 w-3" />
                </Link>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

// ─── Tech Stack ───────────────────────────────────────────────────────────────

function TechStackSection() {
  const stack = [
    { label: "Spring Boot 3.3", category: "Backend" },
    { label: "PostgreSQL 16", category: "Database" },
    { label: "Apache Kafka", category: "Messaging" },
    { label: "Flyway 10", category: "Migrations" },
    { label: "Next.js 16", category: "Frontend" },
    { label: "React 19", category: "UI" },
    { label: "Recharts", category: "Analytics" },
    { label: "ReactFlow", category: "Workflow" },
    { label: "React Query", category: "Data" },
    { label: "Valkey", category: "Cache" },
    { label: "Docker", category: "Infra" },
    { label: "OpenTelemetry", category: "Observability" },
  ];

  return (
    <section className="py-20 px-6 bg-slate-900/20 border-y border-slate-800/50" aria-label="Technology stack">
      <div className="max-w-5xl mx-auto">
        <h2 className="text-center text-sm font-semibold text-slate-500 uppercase tracking-wider mb-10">
          Built on a modern, battle-tested stack
        </h2>
        <div className="flex flex-wrap justify-center gap-3">
          {stack.map(({ label, category }) => (
            <div key={label} className="flex items-center gap-2 px-4 py-2 rounded-full border border-slate-800 bg-slate-900/60 text-sm text-slate-300 hover:border-slate-700 transition-colors">
              <span className="text-[10px] text-slate-600 font-mono">{category}</span>
              <span className="w-px h-3 bg-slate-700" />
              <span className="font-medium">{label}</span>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

// ─── FAQ ──────────────────────────────────────────────────────────────────────

const FAQS = [
  {
    q: "Is FlowForge production-ready?",
    a: "Yes. FlowForge is built with production-grade Spring Boot 3, Flyway-managed PostgreSQL, Kafka event streaming, and heartbeated worker pools with automatic lease recovery.",
  },
  {
    q: "How does multi-tenancy work?",
    a: "Each organization (tenant) is fully isolated. Projects, jobs, workflows, API keys, and executions are scoped to the tenant. Role-based access controls (OWNER, ADMIN, DEVELOPER, VIEWER) govern what each member can do.",
  },
  {
    q: "What happens if a worker crashes mid-execution?",
    a: "Workers send heartbeats every few seconds. If a heartbeat is missed, the scheduler detects the stale lease and reassigns the execution to a healthy worker — typically within 30 seconds.",
  },
  {
    q: "Can I use FlowForge for cron-style scheduling?",
    a: "Absolutely. Jobs support cron expressions, one-shot triggers, API-initiated dispatch, and workflow-chained triggers. The scheduler is powered by a dedicated microservice.",
  },
  {
    q: "Does FlowForge support retries?",
    a: "Yes. Each job can be configured with exponential backoff, linear backoff, fixed delay, or no-retry policies. Exhausted executions move to a dead-letter queue with full retry context.",
  },
];

function FAQSection() {
  const [open, setOpen] = React.useState<number | null>(null);

  return (
    <section className="py-24 px-6" aria-labelledby="faq-heading">
      <div className="max-w-3xl mx-auto">
        <div className="text-center mb-12 space-y-3">
          <h2 id="faq-heading" className="text-3xl font-bold text-slate-100">Frequently Asked Questions</h2>
          <p className="text-slate-400">Everything you need to know before you start.</p>
        </div>

        <div className="space-y-3">
          {FAQS.map((faq, i) => (
            <div key={i} className="rounded-xl border border-slate-800 overflow-hidden">
              <button
                onClick={() => setOpen(open === i ? null : i)}
                className="w-full flex items-center justify-between px-5 py-4 text-left text-sm font-medium text-slate-200 hover:bg-slate-800/40 transition-colors"
                aria-expanded={open === i}
              >
                {faq.q}
                <ChevronDown className={`h-4 w-4 text-slate-500 transition-transform flex-shrink-0 ml-4 ${open === i ? "rotate-180" : ""}`} />
              </button>
              {open === i && (
                <div className="px-5 pb-4 text-sm text-slate-400 leading-relaxed border-t border-slate-800">
                  <div className="pt-4">{faq.a}</div>
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

// ─── CTA Section ──────────────────────────────────────────────────────────────

function CTASection() {
  return (
    <section className="py-24 px-6" aria-label="Call to action">
      <div className="max-w-3xl mx-auto text-center">
        <div className="rounded-2xl border border-violet-800/40 bg-gradient-to-br from-violet-950/60 to-indigo-950/60 p-12 space-y-6">
          <Pill><CheckCircle2 className="h-3 w-3" /> Ready to orchestrate?</Pill>
          <h2 className="text-4xl font-bold text-slate-100">Start running workflows today</h2>
          <p className="text-slate-400">
            Log in to the console and create your first project, job, and workflow in under 5 minutes.
          </p>
          <div className="flex flex-col sm:flex-row gap-4 justify-center pt-2">
            <Link href="/login"
              className="inline-flex items-center justify-center gap-2 h-12 px-8 rounded-xl bg-violet-600 hover:bg-violet-500 text-white font-semibold text-sm transition-all shadow-xl shadow-violet-900/40">
              <Play className="h-4 w-4" /> Open Console
            </Link>
            <Link href="/docs"
              className="inline-flex items-center justify-center gap-2 h-12 px-8 rounded-xl border border-slate-700 bg-slate-900/60 hover:bg-slate-800 text-slate-200 font-semibold text-sm transition-all">
              <BookOpen className="h-4 w-4" /> Read the Docs
            </Link>
          </div>
        </div>
      </div>
    </section>
  );
}

// ─── Footer ───────────────────────────────────────────────────────────────────

function Footer() {
  const cols = [
    {
      title: "Product",
      links: [
        { label: "Features", href: "#features" },
        { label: "Architecture", href: "#architecture" },
        { label: "Docs", href: "/docs" },
        { label: "Changelog", href: "/docs#changelog" },
      ],
    },
    {
      title: "Console",
      links: [
        { label: "Dashboard", href: "/dashboard" },
        { label: "Workflows", href: "/workflows" },
        { label: "Analytics", href: "/analytics" },
        { label: "Settings", href: "/settings" },
      ],
    },
    {
      title: "Resources",
      links: [
        { label: "GitHub", href: "https://github.com/raushan-2004/FlowForge" },
        { label: "API Reference", href: "/docs#api" },
        { label: "Getting Started", href: "/docs#getting-started" },
      ],
    },
  ];

  return (
    <footer className="border-t border-slate-800/60 py-16 px-6" aria-label="Site footer">
      <div className="max-w-7xl mx-auto">
        <div className="grid gap-10 md:grid-cols-4">
          <div className="space-y-4">
            <div className="flex items-center gap-2">
              <div className="h-7 w-7 rounded-lg bg-gradient-to-br from-violet-600 to-indigo-600 flex items-center justify-center font-bold text-white text-sm">F</div>
              <span className="font-bold text-slate-100">FlowForge</span>
            </div>
            <p className="text-xs text-slate-500 leading-relaxed">
              Distributed job orchestration & workflow engine for production engineering teams.
            </p>
            <a href="https://github.com/raushan-2004/FlowForge" target="_blank" rel="noopener noreferrer"
              className="inline-flex items-center gap-1.5 text-xs text-slate-500 hover:text-slate-300 transition-colors">
              <ExternalLink className="h-3 w-3" /> View on GitHub
            </a>
          </div>

          {cols.map((col) => (
            <div key={col.title}>
              <h3 className="text-xs font-semibold text-slate-400 uppercase tracking-wider mb-4">{col.title}</h3>
              <ul className="space-y-2.5">
                {col.links.map((l) => (
                  <li key={l.label}>
                    <a href={l.href} className="text-sm text-slate-500 hover:text-slate-200 transition-colors">
                      {l.label}
                    </a>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>

        <div className="mt-12 pt-8 border-t border-slate-800/60 flex flex-col sm:flex-row items-center justify-between gap-4 text-xs text-slate-600">
          <span>© 2026 FlowForge Inc. All rights reserved.</span>
          <div className="flex gap-6">
            <a href="#" className="hover:text-slate-400 transition-colors">Privacy Policy</a>
            <a href="#" className="hover:text-slate-400 transition-colors">Terms of Service</a>
            <a href="#" className="hover:text-slate-400 transition-colors">Security</a>
          </div>
        </div>
      </div>
    </footer>
  );
}

// ─── Main Page ────────────────────────────────────────────────────────────────

export default function LandingPage() {
  return (
    <div className="flex flex-col min-h-screen bg-slate-950 text-slate-100 antialiased">
      <PublicNav />
      <main>
        <HeroSection />
        <FeaturesSection />
        <ArchitectureSection />
        <ProductPreviewSection />
        <TechStackSection />
        <FAQSection />
        <CTASection />
      </main>
      <Footer />
    </div>
  );
}
