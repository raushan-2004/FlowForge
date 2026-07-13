"use client";

import * as React from "react";

interface UserProfile {
  id: string;
  email: string;
  name: string;
}

interface AuthContextProps {
  user: UserProfile | null;
  token: string | null;
  isAuthenticated: boolean;
  login: (token: string, user: UserProfile) => void;
  logout: () => void;
}

const AuthContext = React.createContext<AuthContextProps | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = React.useState<UserProfile | null>(null);
  const [token, setToken] = React.useState<string | null>(null);
  const [mounted, setMounted] = React.useState(false);

  React.useEffect(() => {
    const savedUser = localStorage.getItem("flowforge-user");
    const savedToken = localStorage.getItem("flowforge-token");
    if (savedUser && savedToken) {
      setUser(JSON.parse(savedUser));
      setToken(savedToken);
    }
    setMounted(true);
  }, []);

  const login = React.useCallback((newToken: string, newUser: UserProfile) => {
    setUser(newUser);
    setToken(newToken);
    localStorage.setItem("flowforge-user", JSON.stringify(newUser));
    localStorage.setItem("flowforge-token", newToken);
  }, []);

  const logout = React.useCallback(() => {
    setUser(null);
    setToken(null);
    localStorage.removeItem("flowforge-user");
    localStorage.removeItem("flowforge-token");
  }, []);

  if (!mounted) {
    return <>{children}</>;
  }

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated: !!token,
        login,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = React.useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
