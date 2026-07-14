export interface WorkerStatus {
  publicId: string;
  workerName: string;
  status: "ACTIVE" | "DRAINING" | "OFFLINE";
  lastHeartbeat: string;
  activeLeases: number;
  runningExecutions: number;
  cpuUsage?: number;
  memoryUsage?: number;
  capabilities: string[];
  version: string;
}

export const WorkerMonitoringService = {
  async getWorkers(): Promise<WorkerStatus[]> {
    return [
      {
        publicId: "worker-node-1",
        workerName: "k8s-worker-node-1",
        status: "ACTIVE",
        lastHeartbeat: new Date(Date.now() - 2000).toISOString(),
        activeLeases: 3,
        runningExecutions: 2,
        cpuUsage: 24,
        memoryUsage: 48,
        capabilities: ["HTTP_DISPATCH", "JSON_VALIDATION", "KAFKA_PRODUCE"],
        version: "v1.2.0"
      },
      {
        publicId: "worker-node-2",
        workerName: "k8s-worker-node-2",
        status: "DRAINING",
        lastHeartbeat: new Date(Date.now() - 5000).toISOString(),
        activeLeases: 1,
        runningExecutions: 1,
        cpuUsage: 12,
        memoryUsage: 82,
        capabilities: ["HTTP_DISPATCH", "DB_QUERY"],
        version: "v1.2.0"
      },
      {
        publicId: "worker-node-3",
        workerName: "k8s-worker-node-3",
        status: "OFFLINE",
        lastHeartbeat: new Date(Date.now() - 600000).toISOString(),
        activeLeases: 0,
        runningExecutions: 0,
        cpuUsage: 0,
        memoryUsage: 0,
        capabilities: ["HTTP_DISPATCH"],
        version: "v1.1.8"
      }
    ];
  }
};
