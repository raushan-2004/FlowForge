export interface DatabaseMetrics {
  status: "HEALTHY" | "DEGRADED" | "UNHEALTHY";
  poolUsage: number; // percentage
  activeConnections: number;
  pendingConnections: number;
  queryLatencyMs: number;
  transactionRate: number; // tx/sec
}

export const DatabaseMonitoringService = {
  async getMetrics(): Promise<DatabaseMetrics> {
    return {
      status: "HEALTHY",
      poolUsage: 12,
      activeConnections: 6,
      pendingConnections: 0,
      queryLatencyMs: 8,
      transactionRate: 48,
    };
  }
};
