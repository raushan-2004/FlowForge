import { api } from "@/lib/api";

export interface WorkflowDefinitionRequest {
  name: string;
  definitionJson: string;
}

export interface WorkflowDefinitionResponse {
  publicId: string;
  name: string;
  version: number;
  active: boolean;
  definitionJson: string;
}

export const WorkflowService = {
  async listWorkflows(projectId: string): Promise<WorkflowDefinitionResponse[]> {
    // In our backend controller: we don't have a GET workflows list endpoint yet, 
    // so we can list via a mock adapter or save/load directly. Let's make it fetch mock workflow list 
    // or handle empty state gracefully.
    await new Promise((resolve) => setTimeout(resolve, 200));
    return [];
  },

  async getWorkflow(definitionPublicId: string): Promise<WorkflowDefinitionResponse> {
    // Return mock workflow details if requested
    await new Promise((resolve) => setTimeout(resolve, 200));
    return {
      publicId: definitionPublicId,
      name: "Default Ingestion Pipeline",
      version: 1,
      active: true,
      definitionJson: JSON.stringify({
        nodes: [
          { id: "node-start", type: "START" },
          { id: "node-end", type: "END" },
        ],
        edges: [
          { from: "node-start", to: "node-end" }
        ]
      })
    };
  },

  async saveWorkflow(projectId: string, request: WorkflowDefinitionRequest): Promise<WorkflowDefinitionResponse> {
    return api.post<WorkflowDefinitionResponse>(`/projects/${projectId}/workflows`, request);
  },

  async runWorkflow(definitionPublicId: string): Promise<any> {
    return api.post(`/workflows/${definitionPublicId}/runs`);
  },
};
