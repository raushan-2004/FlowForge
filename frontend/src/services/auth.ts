import { api } from "@/lib/api";

export interface LoginResponse {
  token: string;
}

export interface UserTenant {
  id: string;
  name: string;
  role: string;
}

export interface UserProfile {
  id: string;
  email: string;
  tenants: UserTenant[];
}

export const AuthService = {
  async login(email: string, password: string): Promise<LoginResponse> {
    return api.post<LoginResponse>("/auth/login", { email, password });
  },

  async register(email: string, password: string): Promise<any> {
    return api.post("/auth/register", { email, password });
  },

  async getMe(): Promise<UserProfile> {
    return api.get<UserProfile>("/auth/me");
  },
};
