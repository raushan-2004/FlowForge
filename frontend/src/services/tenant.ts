import { api } from "@/lib/api";

export interface TenantResponse {
  publicId: string;
  name: string;
  status: string;
  creatorPublicId: string;
}

export const TenantService = {
  async listTenants(): Promise<TenantResponse[]> {
    return api.get<TenantResponse[]>("/tenants");
  },

  async getTenant(tenantId: string): Promise<TenantResponse> {
    return api.get<TenantResponse>(`/tenants/${tenantId}`);
  },

  async createTenant(name: string): Promise<TenantResponse> {
    return api.post<TenantResponse>("/tenants", { name });
  },
};
