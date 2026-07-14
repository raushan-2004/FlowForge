import { api } from "@/lib/api";

export interface ApiKeyRequest {
  name: string;
}

export interface ApiKeyResponse {
  publicId: string;
  name: string;
  prefix: string;
  status: string;
  createdAt: string;
  lastUsedAt?: string;
}

export interface ApiKeyCreateResponse {
  metadata: ApiKeyResponse;
  cleartextSecret: string;
}

export const ApiKeyService = {
  async listKeys(projectId: string): Promise<ApiKeyResponse[]> {
    return api.get<ApiKeyResponse[]>(`/projects/${projectId}/keys`);
  },

  async getKey(projectId: string, keyId: string): Promise<ApiKeyResponse> {
    return api.get<ApiKeyResponse>(`/projects/${projectId}/keys/${keyId}`);
  },

  async createKey(projectId: string, request: ApiKeyRequest): Promise<ApiKeyCreateResponse> {
    return api.post<ApiKeyCreateResponse>(`/projects/${projectId}/keys`, request);
  },

  async rotateKey(projectId: string, keyId: string, request: ApiKeyRequest): Promise<ApiKeyCreateResponse> {
    return api.post<ApiKeyCreateResponse>(`/projects/${projectId}/keys/${keyId}/rotate`, request);
  },

  async revokeKey(projectId: string, keyId: string): Promise<void> {
    return api.delete<void>(`/projects/${projectId}/keys/${keyId}`);
  },
};
