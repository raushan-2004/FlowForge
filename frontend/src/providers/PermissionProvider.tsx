"use client";

import * as React from "react";
import { useAuth } from "./AuthProvider";

interface PermissionContextProps {
  canAccess: (permission: string) => boolean;
}

const PermissionContext = React.createContext<PermissionContextProps | undefined>(undefined);

export function PermissionProvider({ children }: { children: React.ReactNode }) {
  const { hasPermission } = useAuth();

  const canAccess = React.useCallback(
    (permission: string) => {
      return hasPermission(permission);
    },
    [hasPermission]
  );

  return (
    <PermissionContext.Provider value={{ canAccess }}>
      {children}
    </PermissionContext.Provider>
  );
}

export function usePermissions() {
  const context = React.useContext(PermissionContext);
  if (!context) {
    return {
      canAccess: () => false,
    };
  }
  return context;
}
