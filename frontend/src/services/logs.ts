export interface LogLine {
  id: string;
  time: string;
  level: "INFO" | "WARN" | "ERROR" | "DEBUG";
  component: string;
  message: string;
}

export const LogService = {
  async getLogs(executionId: string): Promise<LogLine[]> {
    await new Promise((resolve) => setTimeout(resolve, 200));
    return [
      { id: "log-1", time: "10:00:00.002", level: "INFO", component: "SCHEDULER", message: "Evaluating workflow triggers for project namespace" },
      { id: "log-2", time: "10:00:00.045", level: "INFO", component: "SCHEDULER", message: "Execution publicId dispatch queued" },
      { id: "log-3", time: "10:00:00.052", level: "INFO", component: "WORKER", message: "Node k8s-worker-node-4f claiming execution slots" },
      { id: "log-4", time: "10:00:00.082", level: "INFO", component: "ENGINE", message: "Parsing DAG connections. Found: 3 nodes, 2 edges" },
      { id: "log-5", time: "10:00:00.095", level: "INFO", component: "ENGINE", message: "Executing step node-start (type: START). Status: SUCCESS" },
      { id: "log-6", time: "10:00:00.102", level: "INFO", component: "DISPATCHER", message: "Initiating HTTP POST request to https://api.domain.com/v1/trigger" },
      { id: "log-7", time: "10:00:00.105", level: "DEBUG", component: "DISPATCHER", message: "Headers: Content-Type=application/json, X-FlowForge-Tenant=active-id" },
      { id: "log-8", time: "10:00:00.518", level: "INFO", component: "DISPATCHER", message: "HTTP request resolved in 416ms. Status: 200 OK" },
      { id: "log-9", time: "10:00:00.522", level: "INFO", component: "ENGINE", message: "Executing step node-end (type: END). Status: SUCCESS" },
      { id: "log-10", time: "10:00:00.528", level: "INFO", component: "SCHEDULER", message: "Finalizing execution log state databases" }
    ];
  }
};
