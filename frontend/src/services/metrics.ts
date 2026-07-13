import { api } from "@/lib/api";

export interface MetricDetail {
  value: string | number;
  trend: number;
  isPositive: boolean;
  description: string;
  lastUpdated: string;
}

export interface DashboardMetricsSummary {
  activeExecutions: MetricDetail;
  runningWorkflows: MetricDetail;
  activeWorkers: MetricDetail;
  schedulerQueue: MetricDetail;
  retryQueue: MetricDetail;
  successRate: MetricDetail;
  failedExecutions24h: MetricDetail;
  avgDuration: MetricDetail;
}

export interface ChartDataPoint {
  label: string;
  success: number;
  failure: number;
  executions: number;
  workerUtilization: number;
  retries: number;
  durationMs: number;
}

export const MetricsService = {
  async getSummary(): Promise<DashboardMetricsSummary> {
    await new Promise((resolve) => setTimeout(resolve, 200));
    return {
      activeExecutions: { value: 3, trend: 15, isPositive: true, description: "Active execution slots claimed", lastUpdated: "Just now" },
      runningWorkflows: { value: 6, trend: 20, isPositive: true, description: "Active DAG processes in pipeline", lastUpdated: "Just now" },
      activeWorkers: { value: 12, trend: 0, isPositive: true, description: "Active worker nodes connected", lastUpdated: "12s ago" },
      schedulerQueue: { value: 8, trend: 40, isPositive: false, description: "Unclaimed jobs in scheduler outbox", lastUpdated: "Just now" },
      retryQueue: { value: 1, trend: 50, isPositive: true, description: "Pending scheduled retry attempts", lastUpdated: "Just now" },
      successRate: { value: "99.2%", trend: 0.4, isPositive: true, description: "Total successfully completed jobs today", lastUpdated: "Just now" },
      failedExecutions24h: { value: 4, trend: 20, isPositive: false, description: "DAG runs marked as failed in last 24h", lastUpdated: "Just now" },
      avgDuration: { value: "452ms", trend: 5.2, isPositive: true, description: "Average roundtrip HTTP dispatch execution", lastUpdated: "Just now" },
    };
  },

  async getChartData(range: "hourly" | "daily" | "weekly"): Promise<ChartDataPoint[]> {
    await new Promise((resolve) => setTimeout(resolve, 300));
    if (range === "hourly") {
      return [
        { label: "10:00", success: 12, failure: 0, executions: 12, workerUtilization: 35, retries: 0, durationMs: 410 },
        { label: "11:00", success: 18, failure: 1, executions: 19, workerUtilization: 42, retries: 1, durationMs: 430 },
        { label: "12:00", success: 24, failure: 0, executions: 24, workerUtilization: 55, retries: 0, durationMs: 390 },
        { label: "13:00", success: 15, failure: 2, executions: 17, workerUtilization: 38, retries: 2, durationMs: 480 },
        { label: "14:00", success: 32, failure: 0, executions: 32, workerUtilization: 68, retries: 0, durationMs: 405 },
        { label: "15:00", success: 28, failure: 1, executions: 29, workerUtilization: 61, retries: 0, durationMs: 422 },
      ];
    }

    if (range === "weekly") {
      return [
        { label: "Week 1", success: 850, failure: 12, executions: 862, workerUtilization: 45, retries: 5, durationMs: 440 },
        { label: "Week 2", success: 920, failure: 8, executions: 928, workerUtilization: 52, retries: 3, durationMs: 430 },
        { label: "Week 3", success: 1040, failure: 15, executions: 1055, workerUtilization: 58, retries: 7, durationMs: 462 },
        { label: "Week 4", success: 1210, failure: 4, executions: 1214, workerUtilization: 64, retries: 2, durationMs: 418 },
      ];
    }

    // Default daily range
    return [
      { label: "Mon", success: 120, failure: 2, executions: 122, workerUtilization: 48, retries: 1, durationMs: 420 },
      { label: "Tue", success: 145, failure: 4, executions: 149, workerUtilization: 52, retries: 3, durationMs: 435 },
      { label: "Wed", success: 160, failure: 1, executions: 161, workerUtilization: 56, retries: 0, durationMs: 412 },
      { label: "Thu", success: 130, failure: 3, executions: 133, workerUtilization: 49, retries: 2, durationMs: 458 },
      { label: "Fri", success: 185, failure: 2, executions: 187, workerUtilization: 65, retries: 1, durationMs: 402 },
      { label: "Sat", success: 95, failure: 0, executions: 95, workerUtilization: 32, retries: 0, durationMs: 388 },
      { label: "Sun", success: 110, failure: 1, executions: 111, workerUtilization: 38, retries: 1, durationMs: 395 },
    ];
  },

  async getHttpStatusDistribution(): Promise<{ name: string; value: number; color: string }[]> {
    await new Promise((resolve) => setTimeout(resolve, 250));
    return [
      { name: "200 OK", value: 1250, color: "#10b981" },
      { name: "201 Created", value: 450, color: "#34d399" },
      { name: "302 Found", value: 120, color: "#3b82f6" },
      { name: "400 BadRequest", value: 25, color: "#f59e0b" },
      { name: "404 NotFound", value: 18, color: "#f97316" },
      { name: "500 InternalError", value: 9, color: "#ef4444" },
    ];
  },
};
