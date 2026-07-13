"use client";

import * as React from "react";

interface UserPreferences {
  sidebarCollapsed: boolean;
  refreshInterval: number; // in seconds
}

interface UserPreferencesContextProps {
  preferences: UserPreferences;
  setSidebarCollapsed: (collapsed: boolean) => void;
  setRefreshInterval: (interval: number) => void;
}

const UserPreferencesContext = React.createContext<UserPreferencesContextProps | undefined>(undefined);

export function UserPreferencesProvider({ children }: { children: React.ReactNode }) {
  const [preferences, setPreferences] = React.useState<UserPreferences>({
    sidebarCollapsed: false,
    refreshInterval: 30,
  });
  const [mounted, setMounted] = React.useState(false);

  React.useEffect(() => {
    const saved = localStorage.getItem("flowforge-preferences");
    if (saved) {
      try {
        setPreferences(JSON.parse(saved));
      } catch (e) {
        // Ignore parsing errors
      }
    }
    setMounted(true);
  }, []);

  const savePreferences = React.useCallback((updated: Partial<UserPreferences>) => {
    setPreferences((prev) => {
      const next = { ...prev, ...updated };
      localStorage.setItem("flowforge-preferences", JSON.stringify(next));
      return next;
    });
  }, []);

  const setSidebarCollapsed = React.useCallback((collapsed: boolean) => {
    savePreferences({ sidebarCollapsed: collapsed });
  }, [savePreferences]);

  const setRefreshInterval = React.useCallback((interval: number) => {
    savePreferences({ refreshInterval: interval });
  }, [savePreferences]);

  if (!mounted) {
    return <>{children}</>;
  }

  return (
    <UserPreferencesContext.Provider
      value={{
        preferences,
        setSidebarCollapsed,
        setRefreshInterval,
      }}
    >
      {children}
    </UserPreferencesContext.Provider>
  );
}

export function usePreferences() {
  const context = React.useContext(UserPreferencesContext);
  if (!context) {
    throw new Error("usePreferences must be used within a UserPreferencesProvider");
  }
  return context;
}
