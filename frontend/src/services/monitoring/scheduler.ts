export interface SchedulerMetrics {
  status: "ACTIVE" | "DEGRADED" | "STANDBY";
  lastSchedulingCycle: string;
  nextSchedulingCycle: string;
  dueJobsCount: number;
  missedJobsCount: number;
  queueDepth: number;
  schedulingLatencyMs: number;
}

export const SchedulerMonitoringService = {
  async getMetrics(): Promise<SchedulerMetrics> {
    return {
      status: "ACTIVE",
      lastSchedulingCycle: new Date(Date.now() - 4000).toISOString(),
      nextSchedulingCycle: new Date(Date.now() + 6000).toISOString(),
      dueJobsCount: 12,
      missedJobsCount: 0,
      queueDepth: 4,
      schedulingLatencyMs: 42,
    };
  }
};
