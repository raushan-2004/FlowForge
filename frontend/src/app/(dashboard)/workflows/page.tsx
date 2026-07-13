"use client";

import * as React from "react";
import { DataTable } from "@/components/ui/DataTable";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { Button } from "@/components/ui/Button";
import { Plus, Play } from "lucide-react";

interface Workflow {
  id: string;
  name: string;
  version: number;
  status: string;
  nodesCount: number;
  lastRun: string;
}

const mockWorkflows: Workflow[] = [
  { id: "wf-1", name: "Hourly Ingestion & Rollup", version: 3, status: "ACTIVE", nodesCount: 5, lastRun: "12 mins ago" },
  { id: "wf-2", name: "User Signup Finalization", version: 1, status: "ACTIVE", nodesCount: 3, lastRun: "1 hour ago" },
  { id: "wf-3", name: "System Cleanup Cron", version: 2, status: "ACTIVE", nodesCount: 2, lastRun: "3 hours ago" },
  { id: "wf-4", name: "Daily Billing Report", version: 5, status: "ACTIVE", nodesCount: 8, lastRun: "1 day ago" },
];

const columns = [
  { header: "Workflow ID", accessorKey: "id" as const },
  { header: "Workflow Name", accessorKey: "name" as const },
  { header: "Version", cell: (item: Workflow) => <span className="font-mono bg-slate-900 px-1.5 py-0.5 rounded-sm border border-slate-800 text-xs">v{item.version}</span> },
  { header: "Status", cell: (item: Workflow) => <StatusBadge status={item.status} /> },
  { header: "Nodes Count", accessorKey: "nodesCount" as const },
  { header: "Last Run", accessorKey: "lastRun" as const },
  {
    header: "Action",
    cell: (item: Workflow) => (
      <Button variant="ghost" size="sm" className="gap-1.5 hover:bg-purple-950/20 hover:text-purple-400">
        <Play className="h-3.5 w-3.5" />
        Trigger
      </Button>
    ),
  },
];

export default function WorkflowsPage() {
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-100">Workflows</h1>
          <p className="text-sm text-slate-400">Define and run directed acyclic graphs (DAGs) of executions.</p>
        </div>
        <Button size="sm" className="gap-1.5">
          <Plus className="h-4 w-4" />
          Create Workflow
        </Button>
      </div>

      <div className="bg-slate-900/10 backdrop-blur-xs border border-slate-900 rounded-lg p-1">
        <DataTable
          columns={columns}
          data={mockWorkflows}
          emptyTitle="No workflows found"
          emptyDescription="You haven't defined any workflow DAGs for this project yet."
        />
      </div>
    </div>
  );
}
