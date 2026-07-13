"use client";

import * as React from "react";
import { MetricCard } from "@/components/ui/MetricCard";
import { BarChart3, Clock, Flame, ShieldAlert } from "lucide-react";

export default function AnalyticsPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-slate-100">Analytics</h1>
        <p className="text-sm text-slate-400">Detailed insights into throughput, latency, queue metrics, and cluster capacity.</p>
      </div>

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <MetricCard
          title="Daily Throughput"
          value="142,502"
          description="Total executions processed in 24h"
          icon={<BarChart3 className="h-4 w-4" />}
          trend={{ value: 12.3, isPositive: true }}
        />
        <MetricCard
          title="Average Latency"
          value="452ms"
          description="Execution execution duration"
          icon={<Clock className="h-4 w-4" />}
          trend={{ value: 3.5, isPositive: false }}
        />
        <MetricCard
          title="Scheduler Cycle time"
          value="1.2s"
          description="Average database scan duration"
          icon={<Flame className="h-4 w-4" />}
        />
        <MetricCard
          title="Error Rate"
          value="0.12%"
          description="Redirection, network & job exceptions"
          icon={<ShieldAlert className="h-4 w-4" />}
          trend={{ value: 1.2, isPositive: false }}
        />
      </div>

      {/* Visual Analytics Placeholder */}
      <div className="flex h-96 items-center justify-center rounded-lg border border-slate-800 bg-slate-900/30">
        <div className="text-center space-y-2">
          <BarChart3 className="h-10 w-10 text-slate-600 mx-auto" />
          <h3 className="text-md font-semibold text-slate-300">Detailed Analytics Chart Placeholder</h3>
          <p className="text-xs text-slate-500 max-w-xs">Detailed throughput, worker load, and queue lag charts will populate here in the next stage.</p>
        </div>
      </div>
    </div>
  );
}
