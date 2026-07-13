"use client";

import * as React from "react";
import { DataTable } from "@/components/ui/DataTable";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { Button } from "@/components/ui/Button";
import { Eye } from "lucide-react";

interface Execution {
  id: string;
  jobName: string;
  triggerType: "MANUAL" | "SCHEDULER" | "WORKFLOW";
  status: string;
  startedAt: string;
  duration: string;
}

const mockExecutions: Execution[] = [
  { id: "exec-e4f1", jobName: "Backup DB Storage", triggerType: "SCHEDULER", status: "SUCCEEDED", startedAt: "5 mins ago", duration: "1.4s" },
  { id: "exec-fa32", jobName: "Publish Webhooks", triggerType: "WORKFLOW", status: "RUNNING", startedAt: "Just now", duration: "--" },
  { id: "exec-9c88", jobName: "Clean Log Storage", triggerType: "MANUAL", status: "FAILED", startedAt: "1 hour ago", duration: "0.8s" },
  { id: "exec-10dd", jobName: "Ingest Metrics", triggerType: "SCHEDULER", status: "SUCCEEDED", startedAt: "2 hours ago", duration: "2.1s" },
];

const columns = [
  { header: "Execution ID", accessorKey: "id" as const },
  { header: "Job Name", accessorKey: "jobName" as const },
  {
    header: "Trigger",
    cell: (item: Execution) => (
      <span className="text-xs bg-slate-900 border border-slate-800 text-slate-400 font-mono px-2 py-0.5 rounded-sm">
        {item.triggerType}
      </span>
    ),
  },
  { header: "Status", cell: (item: Execution) => <StatusBadge status={item.status} /> },
  { header: "Started At", accessorKey: "startedAt" as const },
  { header: "Duration", accessorKey: "duration" as const },
  {
    header: "Details",
    cell: (item: Execution) => (
      <Button variant="ghost" size="icon" className="hover:bg-slate-800 hover:text-slate-100">
        <Eye className="h-4 w-4" />
      </Button>
    ),
  },
];

export default function ExecutionsPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-slate-100">Execution History</h1>
        <p className="text-sm text-slate-400">View and audit all historical execution runs across the project.</p>
      </div>

      <div className="bg-slate-900/10 backdrop-blur-xs border border-slate-900 rounded-lg p-1">
        <DataTable
          columns={columns}
          data={mockExecutions}
          emptyTitle="No executions recorded"
          emptyDescription="There are no job execution events recorded in the system yet."
        />
      </div>
    </div>
  );
}
