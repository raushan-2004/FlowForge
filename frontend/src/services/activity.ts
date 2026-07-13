import { api } from "@/lib/api";

export interface ActivityItem {
  id: string;
  type:
    | "WORKFLOW_STARTED"
    | "WORKFLOW_COMPLETED"
    | "EXECUTION_STARTED"
    | "EXECUTION_COMPLETED"
    | "RETRY_SCHEDULED"
    | "WORKER_JOINED"
    | "WORKER_OFFLINE";
  timestamp: string;
  entityName: string;
  tenantName: string;
  severity: "info" | "warning" | "success" | "error";
}

export const ActivityService = {
  async getLiveFeed(): Promise<ActivityItem[]> {
    await new Promise((resolve) => setTimeout(resolve, 200));
    return [
      { id: "act-1", type: "WORKFLOW_STARTED", timestamp: "Just now", entityName: "Database Backup DAG", tenantName: "Tenant A", severity: "info" },
      { id: "act-2", type: "EXECUTION_STARTED", timestamp: "12s ago", entityName: "Postgres Dump Job", tenantName: "Tenant A", severity: "info" },
      { id: "act-3", type: "WORKER_JOINED", timestamp: "45s ago", entityName: "k8s-worker-node-4f", tenantName: "System", severity: "success" },
      { id: "act-4", type: "EXECUTION_COMPLETED", timestamp: "1 min ago", entityName: "Clean Log Storage", tenantName: "Tenant B", severity: "success" },
      { id: "act-5", type: "RETRY_SCHEDULED", timestamp: "3 mins ago", entityName: "Webhook Deliverer", tenantName: "Tenant A", severity: "warning" },
      { id: "act-6", type: "WORKFLOW_COMPLETED", timestamp: "5 mins ago", entityName: "Hourly Ingestion Rollup", tenantName: "Tenant C", severity: "success" },
      { id: "act-7", type: "WORKER_OFFLINE", timestamp: "15 mins ago", entityName: "k8s-worker-node-12", tenantName: "System", severity: "error" },
    ];
  },
};
