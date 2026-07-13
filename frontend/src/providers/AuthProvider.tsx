"use client";

import * as React from "react";
import { AuthService, UserProfile, UserTenant } from "@/services/auth";
import { useQueryClient } from "@tanstack/react-query";

interface AuthContextProps {
  user: UserProfile | null;
  token: string | null;
  currentTenant: UserTenant | null;
  permissions: string[];
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
  switchTenant: (tenantId: string) => void;
  hasPermission: (permission: string) => boolean;
}

const AuthContext = React.createContext<AuthContextProps | undefined>(undefined);

// Mapping of TenantRole to permission capabilities
const rolePermissionsMap: Record<string, string[]> = {
  OWNER: ["VIEW_PROJECTS", "CREATE_PROJECTS", "MANAGE_USERS", "VIEW_ANALYTICS"],
  ADMIN: ["VIEW_PROJECTS", "CREATE_PROJECTS", "MANAGE_USERS", "VIEW_ANALYTICS"],
  DEVELOPER: ["VIEW_PROJECTS", "CREATE_PROJECTS", "VIEW_ANALYTICS"],
  VIEWER: ["VIEW_PROJECTS"],
};

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = React.useState<UserProfile | null>(null);
  const [token, setToken] = React.useState<string | null>(null);
  const [currentTenant, setCurrentTenant] = React.useState<UserTenant | null>(null);
  const [permissions, setPermissions] = React.useState<string[]>([]);
  const [isLoading, setIsLoading] = React.useState(true);
  const [mounted, setMounted] = React.useState(false);
  const queryClient = useQueryClient();

  // Load session from local storage on startup
  React.useEffect(() => {
    async function restoreSession() {
      const savedToken = localStorage.getItem("flowforge-token");
      if (savedToken) {
        try {
          setToken(savedToken);
          const profile = await AuthService.getMe();
          setUser(profile);

          if (profile.tenants && profile.tenants.length > 0) {
            const savedTenantId = localStorage.getItem("flowforge-tenant");
            const activeTenant =
              profile.tenants.find((t) => t.id === savedTenantId) || profile.tenants[0];

            setCurrentTenant(activeTenant);
            localStorage.setItem("flowforge-tenant", activeTenant.id);
            setPermissions(rolePermissionsMap[activeTenant.role.toUpperCase()] || []);
          }
        } catch (error) {
          console.error("Failed to restore session:", error);
          logout();
        }
      }
      setIsLoading(false);
      setMounted(true);
    }
    restoreSession();
  }, []);

  const login = async (email: string, password: string) => {
    setIsLoading(true);
    try {
      const response = await AuthService.login(email, password);
      localStorage.setItem("flowforge-token", response.token);
      setToken(response.token);

      const profile = await AuthService.getMe();
      setUser(profile);

      if (profile.tenants && profile.tenants.length > 0) {
        const savedTenantId = localStorage.getItem("flowforge-tenant");
        const activeTenant =
          profile.tenants.find((t) => t.id === savedTenantId) || profile.tenants[0];

        setCurrentTenant(activeTenant);
        localStorage.setItem("flowforge-tenant", activeTenant.id);
        setPermissions(rolePermissionsMap[activeTenant.role.toUpperCase()] || []);
      }
    } catch (error) {
      logout();
      throw error;
    } finally {
      setIsLoading(false);
    }
  };

  const logout = React.useCallback(() => {
    setUser(null);
    setToken(null);
    setCurrentTenant(null);
    setPermissions([]);
    localStorage.removeItem("flowforge-token");
    localStorage.removeItem("flowforge-tenant");
    queryClient.clear();
  }, [queryClient]);

  const switchTenant = React.useCallback(
    (tenantId: string) => {
      if (!user) return;
      const target = user.tenants.find((t) => t.id === tenantId);
      if (target) {
        setCurrentTenant(target);
        localStorage.setItem("flowforge-tenant", target.id);
        setPermissions(rolePermissionsMap[target.role.toUpperCase()] || []);
        
        // Invalidate cached query results to trigger updates
        queryClient.invalidateQueries();
      }
    },
    [user, queryClient]
  );

  const hasPermission = React.useCallback(
    (permission: string) => {
      return permissions.includes(permission);
    },
    [permissions]
  );

  if (!mounted) {
    return <>{children}</>;
  }

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        currentTenant,
        permissions,
        isAuthenticated: !!token,
        isLoading,
        login,
        logout,
        switchTenant,
        hasPermission,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = React.useContext(AuthContext);
  if (!context) {
    return {
      user: null,
      token: null,
      currentTenant: null,
      permissions: [],
      isAuthenticated: false,
      isLoading: false,
      login: async () => {},
      logout: () => {},
      switchTenant: () => {},
      hasPermission: () => false,
    };
  }
  return context;
}
