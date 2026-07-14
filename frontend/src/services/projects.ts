import { api } from "@/lib/api";

export interface ProjectRequest {
  tenantPublicId: string;
  name: string;
  description: string;
  status?: string;
}

export interface ProjectResponse {
  publicId: string;
  tenantPublicId: string;
  name: string;
  description: string;
  status: string;
  jobsCount?: number;
  workflowsCount?: number;
  keysCount?: number;
}

export const ProjectService = {
  async listProjects(): Promise<ProjectResponse[]> {
    return api.get<ProjectResponse[]>("/projects");
  },

  async getProject(projectId: string): Promise<ProjectResponse> {
    return api.get<ProjectResponse>(`/projects/${projectId}`);
  },

  async createProject(request: ProjectRequest): Promise<ProjectResponse> {
    return api.post<ProjectResponse>("/projects", request);
  },

  async updateProject(projectId: string, request: Partial<ProjectRequest>): Promise<ProjectResponse> {
    return api.put<ProjectResponse>(`/projects/${projectId}`, request);
  },

  async deleteProject(projectId: string): Promise<void> {
    return api.delete<void>(`/projects/${projectId}`);
  },
};
