export interface QueueMetrics {
  pendingCount: number;
  runningCount: number;
  retryCount: number;
  dlqCount: number;
  avgWaitTimeMs: number;
}

export const QueueMonitoringService = {
  async getMetrics(): Promise<QueueMetrics> {
    return {
      pendingCount: 3,
      runningCount: 5,
      retryCount: 1,
      dlqCount: 0,
      avgWaitTimeMs: 145,
    };
  }
};
