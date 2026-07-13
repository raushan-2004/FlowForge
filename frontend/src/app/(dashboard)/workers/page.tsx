"use client";

import * as React from "react";
import { DataTable } from "@/components/ui/DataTable";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { Button } from "@/components/ui/Button";
import { ShieldCheck, Cpu } from "lucide-react";

interface Worker {
  id: string;
  hostname: string;
  ipAddress: string;
  status: string;
  lastHeartbeat: string;
}

const mockWorkers: Worker[] = [
  { id: "worker-f47a", hostname: "k8s-node-1", ipAddress: "10.244.1.12", status: "ACTIVE", lastHeartbeat: "Just now" },
  { id: "worker-bc21", hostname: "k8s-node-2", ipAddress: "10.244.2.14", status: "ACTIVE", lastHeartbeat: "12s ago" },
  { id: "worker-90d2", hostname: "k8s-node-3", ipAddress: "10.244.3.8", status: "DRAINING", lastHeartbeat: "45s ago" },
];

const columns = [
  { header: "Worker ID", accessorKey: "id" as const },
  { header: "Hostname", accessorKey: "hostname" as const },
  { header: "IP Address", accessorKey: "ipAddress" as const },
  { header: "Status", cell: (item: Worker) => <StatusBadge status={item.status} /> },
  { header: "Last Heartbeat", accessorKey: "lastHeartbeat" as const },
];

export default function WorkersPage() {
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-100">Worker Registry</h1>
          <p className="text-sm text-slate-400">Monitor active execution nodes in the distributed worker cluster.</p>
        </div>
        <div className="flex items-center space-x-2 bg-slate-900 border border-slate-800 text-xs px-3 py-1.5 rounded-md text-slate-400">
          <ShieldCheck className="h-4 w-4 text-green-500 shrink-0" />
          <span>Cluster Status: Healthy</span>
        </div>
      </div>

      <div className="bg-slate-900/10 backdrop-blur-xs border border-slate-900 rounded-lg p-1">
        <DataTable
          columns={columns}
          data={mockWorkers}
          emptyTitle="No workers registered"
          emptyDescription="There are no active worker nodes registered in the cluster."
        />
      </div>
    </div>
  );
}
