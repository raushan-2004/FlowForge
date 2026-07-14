import { api } from "@/lib/api";

export interface JobRequest {
  projectPublicId: string;
  name: string;
  description: string;
  enabled: boolean;
  httpMethod: string;
  targetUrl: string;
  requestHeaders: Record<string, string>;
  requestBody: string;
  timeoutSeconds: number;
  retryMaxAttempts: number;
  retryStrategy: string;
  retryBaseDelaySeconds: number;
  scheduleType: string;
  cronExpression: string;
}

export interface JobResponse {
  publicId: string;
  projectPublicId: string;
  name: string;
  description: string;
  enabled: boolean;
  httpMethod: string;
  targetUrl: string;
  requestHeaders: Record<string, string>;
  requestBody: string;
  timeoutSeconds: number;
  retryMaxAttempts: number;
  retryStrategy: string;
  retryBaseDelaySeconds: number;
  scheduleType: string;
  cronExpression: string;
  createdAt: string;
  updatedAt: string;
}

export const JobService = {
  async listJobs(projectId?: string): Promise<JobResponse[]> {
    const path = projectId ? `/jobs?projectId=${projectId}` : "/jobs";
    return api.get<JobResponse[]>(path);
  },

  async getJob(jobId: string): Promise<JobResponse> {
    return api.get<JobResponse>(`/jobs/${jobId}`);
  },

  async createJob(request: JobRequest): Promise<JobResponse> {
    return api.post<JobResponse>("/jobs", request);
  },

  async updateJob(jobId: string, request: Partial<JobRequest>): Promise<JobResponse> {
    return api.put<JobResponse>(`/jobs/${jobId}`, request);
  },

  async deleteJob(jobId: string): Promise<void> {
    return api.delete<void>(`/jobs/${jobId}`);
  },
};
