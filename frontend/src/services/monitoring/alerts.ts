export interface SystemAlert {
  id: string;
  title: string;
  description: string;
  severity: "critical" | "warning" | "info";
  timestamp: string;
  acknowledged: boolean;
}

export const AlertService = {
  async getAlerts(): Promise<SystemAlert[]> {
    return [
      {
        id: "alert-1",
        title: "Worker Offline",
        description: "Worker k8s-worker-node-3 has missed heartbeats for over 10 minutes.",
        severity: "warning",
        timestamp: new Date(Date.now() - 600000).toISOString(),
        acknowledged: false,
      },
      {
        id: "alert-2",
        title: "PostgreSQL Pool Limit High",
        description: "Active connections reached 92% of configured limits during pipeline triggers batch runs.",
        severity: "critical",
        timestamp: new Date(Date.now() - 30000).toISOString(),
        acknowledged: false,
      },
      {
        id: "alert-3",
        title: "Kafka Consumer Lag Rising",
        description: "Consumer lag on topic 'flowforge.executions' is currently above threshold.",
        severity: "warning",
        timestamp: new Date(Date.now() - 150000).toISOString(),
        acknowledged: false,
      }
    ];
  }
};
