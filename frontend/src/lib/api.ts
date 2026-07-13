export interface ApiError {
  message: string;
  code?: string;
  status: number;
}

export class ApiClient {
  private baseUrl: string;

  constructor(baseUrl: string = "/api/v1") {
    this.baseUrl = baseUrl;
  }

  private async request<T>(
    path: string,
    options: RequestInit = {}
  ): Promise<T> {
    // 1. Request Interceptors (Header injects)
    const token = typeof window !== "undefined" ? localStorage.getItem("flowforge-token") : null;
    const tenant = typeof window !== "undefined" ? localStorage.getItem("flowforge-tenant") : null;

    const headers = new Headers(options.headers);
    headers.set("Content-Type", "application/json");

    if (token) {
      headers.set("Authorization", `Bearer ${token}`);
    }
    if (tenant) {
      headers.set("X-FlowForge-Tenant", tenant);
    }

    const config: RequestInit = {
      ...options,
      headers,
    };

    try {
      const response = await fetch(`${this.baseUrl}${path}`, config);

      // 2. Response Interceptors (Error Mapping & JWT refresh placeholder)
      if (!response.ok) {
        if (response.status === 401) {
          // Token expired or invalid -> trigger silent refresh placeholder
          console.warn("Silent token refresh placeholder triggered.");
        }
        
        let errorData: any = {};
        try {
          errorData = await response.json();
        } catch (e) {
          // Response is not JSON
        }

        const apiError: ApiError = {
          message: errorData.message || response.statusText || "Something went wrong",
          code: errorData.code,
          status: response.status,
        };
        throw apiError;
      }

      // Successful JSON response
      if (response.status === 204) {
        return {} as T;
      }
      return (await response.json()) as T;
    } catch (error: any) {
      if (error.status) {
        throw error;
      }
      // Network failure
      const networkError: ApiError = {
        message: error.message || "Network request failed",
        status: 503,
      };
      throw networkError;
    }
  }

  public get<T>(path: string, options?: RequestInit): Promise<T> {
    return this.request<T>(path, { ...options, method: "GET" });
  }

  public post<T>(path: string, body?: any, options?: RequestInit): Promise<T> {
    return this.request<T>(path, {
      ...options,
      method: "POST",
      body: body ? JSON.stringify(body) : undefined,
    });
  }

  public put<T>(path: string, body?: any, options?: RequestInit): Promise<T> {
    return this.request<T>(path, {
      ...options,
      method: "PUT",
      body: body ? JSON.stringify(body) : undefined,
    });
  }

  public delete<T>(path: string, options?: RequestInit): Promise<T> {
    return this.request<T>(path, { ...options, method: "DELETE" });
  }
}

export const api = new ApiClient();
