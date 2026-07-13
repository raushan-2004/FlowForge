import Link from "next/link";
import { ArrowRight, Cpu, GitBranch, ShieldCheck } from "lucide-react";

export default function LandingPage() {
  return (
    <div className="flex flex-col min-h-screen bg-slate-950 text-slate-100 antialiased font-sans">
      {/* Header */}
      <header className="flex h-16 w-full items-center justify-between px-6 border-b border-slate-900">
        <div className="flex items-center space-x-2">
          <div className="h-6 w-6 rounded-md bg-purple-600 flex items-center justify-center font-bold text-white text-sm">F</div>
          <span className="font-bold text-slate-100 tracking-wide">FlowForge</span>
        </div>
        <Link
          href="/login"
          className="inline-flex items-center justify-center rounded-md text-xs font-semibold h-9 px-4 bg-slate-900 border border-slate-800 hover:bg-slate-800 text-slate-200 transition-colors"
        >
          Sign In
        </Link>
      </header>

      {/* Hero Section */}
      <main className="flex-1 flex flex-col justify-center items-center text-center p-6 space-y-12 max-w-5xl mx-auto py-24">
        <div className="space-y-4">
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-semibold bg-purple-950/60 border border-purple-900/30 text-purple-400">
            FlowForge v1.0 Production Hardened
          </span>
          <h1 className="text-4xl md:text-6xl font-bold tracking-tight text-transparent bg-clip-text bg-gradient-to-r from-slate-100 via-purple-300 to-indigo-300">
            Distributed Job Orchestration & Workflow Engine
          </h1>
          <p className="text-md md:text-lg text-slate-400 max-w-2xl mx-auto leading-relaxed">
            A resilient, multi-tenant execution manager designed for high throughput, conditional DAG routing, heartbeated worker pools, and sub-millisecond dispatching.
          </p>
        </div>

        <div className="flex flex-col sm:flex-row gap-4 justify-center">
          <Link
            href="/login"
            className="inline-flex items-center justify-center rounded-md text-sm font-semibold h-11 px-8 bg-purple-600 hover:bg-purple-700 text-white shadow-md shadow-purple-600/20 transition-all duration-150 gap-1.5"
          >
            Launch Console
            <ArrowRight className="h-4 w-4" />
          </Link>
          <a
            href="https://github.com/raushan-2004/FlowForge"
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center justify-center rounded-md text-sm font-semibold h-11 px-8 bg-slate-900 border border-slate-800 hover:bg-slate-800 text-slate-200 transition-colors"
          >
            Read API Docs
          </a>
        </div>

        {/* Feature Highlights Grid */}
        <div className="grid gap-6 sm:grid-cols-3 max-w-4xl w-full pt-12">
          <div className="p-6 rounded-lg border border-slate-900 bg-slate-900/10 backdrop-blur-xs text-left space-y-3">
            <Cpu className="h-6 w-6 text-purple-400" />
            <h3 className="font-semibold text-slate-200">Worker Registry</h3>
            <p className="text-xs text-slate-400 leading-relaxed">Distributed worker registry and automatic lease renewals with safe lease recovery.</p>
          </div>
          <div className="p-6 rounded-lg border border-slate-900 bg-slate-900/10 backdrop-blur-xs text-left space-y-3">
            <GitBranch className="h-6 w-6 text-purple-400" />
            <h3 className="font-semibold text-slate-200">DAG Workflows</h3>
            <p className="text-xs text-slate-400 leading-relaxed">Complex fan-out and fan-in workflow pipelines with cycles and reachability checks.</p>
          </div>
          <div className="p-6 rounded-lg border border-slate-900 bg-slate-900/10 backdrop-blur-xs text-left space-y-3">
            <ShieldCheck className="h-6 w-6 text-purple-400" />
            <h3 className="font-semibold text-slate-200">Tenant Isolation</h3>
            <p className="text-xs text-slate-400 leading-relaxed">Granular tenant and project membership access controls with secure API key auth.</p>
          </div>
        </div>
      </main>

      {/* Footer */}
      <footer className="h-16 w-full flex items-center justify-between border-t border-slate-900 px-6 text-xs text-slate-500">
        <span>© 2026 FlowForge Inc. All rights reserved.</span>
        <div className="flex space-x-4">
          <a href="#" className="hover:underline">Privacy Policy</a>
          <a href="#" className="hover:underline">Terms of Service</a>
        </div>
      </footer>
    </div>
  );
}
