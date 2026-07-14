export interface HighlightedPath {
  nodesStatus: Record<string, "succeeded" | "failed" | "retried" | "unexecuted">;
  edgesStatus: Record<string, "executed" | "unexecuted">;
}

export const WorkflowVisualizationService = {
  async getHighlightedPath(executionId: string): Promise<HighlightedPath> {
    await new Promise((resolve) => setTimeout(resolve, 150));
    return {
      nodesStatus: {
        "node-start": "succeeded",
        "node-job-1": "retried",
        "node-end": "succeeded"
      },
      edgesStatus: {
        "reactflow__edge-node-start-node-job-1": "executed",
        "reactflow__edge-node-job-1-node-end": "executed"
      }
    };
  }
};
