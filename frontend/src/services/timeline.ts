export interface TimelineEvent {
  id: string;
  name: string;
  status: "success" | "warning" | "error" | "info";
  timestamp: string;
  durationMs?: number;
  iconName: string;
  details?: string;
}

export const TimelineService = {
  async getTimeline(executionId: string): Promise<TimelineEvent[]> {
    await new Promise((resolve) => setTimeout(resolve, 200));
    return [
      { id: "ev-1", name: "Execution Queued", status: "success", timestamp: "10:00:00.000", iconName: "Clock", details: "Unclaimed task added to schedule queue" },
      { id: "ev-2", name: "Worker Claimed", status: "success", timestamp: "10:00:00.050", durationMs: 50, iconName: "Users", details: "Connected node k8s-worker-node-4f claimed run slot" },
      { id: "ev-3", name: "Running", status: "success", timestamp: "10:00:00.060", iconName: "Play", details: "DAG engine initialized node connections" },
      { id: "ev-4", name: "HTTP Request Dispatched", status: "info", timestamp: "10:00:00.100", iconName: "Globe", details: "POST request fired to target endpoint url" },
      { id: "ev-5", name: "HTTP Response Received", status: "success", timestamp: "10:00:00.520", durationMs: 420, iconName: "CheckCircle", details: "Status code 200 OK returned by target API" },
      { id: "ev-6", name: "Completed", status: "success", timestamp: "10:00:00.530", durationMs: 10, iconName: "CheckCircle2", details: "Workflow graph ran to conclusion successfully" }
    ];
  }
};
