"use client";

import * as React from "react";
import { MetricCard } from "@/components/ui/MetricCard";
import { DataTable } from "@/components/ui/DataTable";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { Button } from "@/components/ui/Button";
import { Activity, GitBranch, Cpu, Users } from "lucide-react";

interface RecentExecution {
  id: string;
  workflow: string;
  status: string;
  duration: string;
  triggeredAt: string;
}

const mockExecutions: RecentExecution[] = [
  { id: "exec-1", workflow: "Production Backup", status: "SUCCEEDED", duration: "1.2s", triggeredAt: "2 mins ago" },
  { id: "exec-2", workflow: "Staging Auto-Deploy", status: "RUNNING", duration: "--", triggeredAt: "Just now" },
  { id: "exec-3", workflow: "DB Schema Migration", status: "QUEUED", duration: "--", triggeredAt: "1 min ago" },
  { id: "exec-4", workflow: "Log Archiver", status: "FAILED", duration: "4.5s", triggeredAt: "10 mins ago" },
];

const columns = [
  { header: "Execution ID", accessorKey: "id" as const },
  { header: "Workflow", accessorKey: "workflow" as const },
  {
    header: "Status",
    cell: (item: RecentExecution) => <StatusBadge status={item.status} />,
  },
  { header: "Duration", accessorKey: "duration" as const },
  { header: "Triggered At", accessorKey: "triggeredAt" as const },
];

export default function DashboardPage() {
  const [loading, setLoading] = React.useState(false);

  const handleRefresh = () => {
    setLoading(true);
    setTimeout(() => setLoading(false), 800);
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-100">FlowForge Console</h1>
          <p className="text-sm text-slate-400">Real-time status overview of active jobs, workers, and workflows.</p>
        </div>
        <Button onClick={handleRefresh} disabled={loading} size="sm">
          {loading ? "Syncing..." : "Refresh Console"}
        </Button>
      </div>

      {/* Metric Cards Grid */}
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
        <MetricCard
          title="Active Workers"
          value="12 / 15"
          description="Workers currently claiming executions"
          icon={<Users className="h-4 w-4" />}
          trend={{ value: 4.2, isPositive: true }}
        />
        <MetricCard
          title="Total Workflows"
          value="24"
          description="Active DAG definitions"
          icon={<GitBranch className="h-4 w-4" />}
        />
        <MetricCard
          title="Active Executions"
          value="3"
          description="Executions running or queued"
          icon={<Cpu className="h-4 w-4" />}
          trend={{ value: 12.5, isPositive: true }}
        />
        <MetricCard
          title="Avg Success Rate"
          value="98.4%"
          description="Out of 1.2k executions today"
          icon={<Activity className="h-4 w-4" />}
          trend={{ value: 0.8, isPositive: true }}
        />
      </div>

      {/* Recent Executions DataTable */}
      <div className="space-y-4">
        <h2 className="text-lg font-bold text-slate-200">Recent Executions Activity</h2>
        <DataTable
          columns={columns}
          data={mockExecutions}
          isLoading={loading}
          emptyTitle="No recent executions"
          emptyDescription="There has been no execution activity recorded within the last 24 hours."
        />
      </div>
    </div>
  );
}
