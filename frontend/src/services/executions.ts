import { api } from "@/lib/api";

export interface ExecutionResponse {
  publicId: string;
  projectPublicId: string;
  jobPublicId: string;
  status: string;
  triggerType: string;
  startedAt: string;
  finishedAt?: string;
  durationMs?: number;
  workerName?: string;
  retryCount: number;
}

export interface ExecutionAttemptResponse {
  publicId: string;
  executionPublicId: string;
  attemptNumber: number;
  status: string;
  startedAt: string;
  finishedAt?: string;
  durationMs?: number;
  httpStatus?: number;
  networkError?: string;
  requestHeaders?: Record<string, string>;
  requestBody?: string;
  responseHeaders?: Record<string, string>;
  responseBody?: string;
}

export const ExecutionService = {
  async listExecutions(projectId?: string): Promise<ExecutionResponse[]> {
    const path = projectId ? `/executions?projectId=${projectId}` : "/executions";
    return api.get<ExecutionResponse[]>(path);
  },

  async getExecution(executionId: string): Promise<ExecutionResponse> {
    return api.get<ExecutionResponse>(`/executions/${executionId}`);
  },

  async getAttempts(executionId: string): Promise<ExecutionAttemptResponse[]> {
    return api.get<ExecutionAttemptResponse[]>(`/executions/${executionId}/attempts`);
  },
};
