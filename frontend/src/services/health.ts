import { api } from "@/lib/api";

export interface ServiceHealth {
  status: "Healthy" | "Warning" | "Critical";
  responseTimeMs: number;
  uptime: string;
  lastHeartbeat: string;
}

export interface SystemHealthOverview {
  apiService: ServiceHealth;
  scheduler: ServiceHealth;
  workerCluster: ServiceHealth;
  kafka: ServiceHealth;
  postgres: ServiceHealth;
  resultProcessor: ServiceHealth;
  eventProcessor: ServiceHealth;
}

export const HealthService = {
  async getSystemHealth(): Promise<SystemHealthOverview> {
    try {
      // Future endpoint integration: return api.get<SystemHealthOverview>("/health/overview");
      // For now, we return highly realistic mock metrics matching active cluster states
      await new Promise((resolve) => setTimeout(resolve, 300));
      return {
        apiService: { status: "Healthy", responseTimeMs: 12, uptime: "14d 6h", lastHeartbeat: "Just now" },
        scheduler: { status: "Healthy", responseTimeMs: 45, uptime: "14d 6h", lastHeartbeat: "Just now" },
        workerCluster: { status: "Healthy", responseTimeMs: 180, uptime: "4d 2h", lastHeartbeat: "12s ago" },
        kafka: { status: "Healthy", responseTimeMs: 5, uptime: "30d", lastHeartbeat: "Just now" },
        postgres: { status: "Healthy", responseTimeMs: 3, uptime: "30d", lastHeartbeat: "Just now" },
        resultProcessor: { status: "Healthy", responseTimeMs: 34, uptime: "14d 6h", lastHeartbeat: "Just now" },
        eventProcessor: { status: "Healthy", responseTimeMs: 28, uptime: "14d 6h", lastHeartbeat: "Just now" },
      };
    } catch (e) {
      return {
        apiService: { status: "Critical", responseTimeMs: 0, uptime: "--", lastHeartbeat: "--" },
        scheduler: { status: "Critical", responseTimeMs: 0, uptime: "--", lastHeartbeat: "--" },
        workerCluster: { status: "Critical", responseTimeMs: 0, uptime: "--", lastHeartbeat: "--" },
        kafka: { status: "Critical", responseTimeMs: 0, uptime: "--", lastHeartbeat: "--" },
        postgres: { status: "Critical", responseTimeMs: 0, uptime: "--", lastHeartbeat: "--" },
        resultProcessor: { status: "Critical", responseTimeMs: 0, uptime: "--", lastHeartbeat: "--" },
        eventProcessor: { status: "Critical", responseTimeMs: 0, uptime: "--", lastHeartbeat: "--" },
      };
    }
  },
};
