"use client";

import * as React from "react";
import { useAuth } from "./AuthProvider";
import { Dialog, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from "@/components/ui/Dialog";
import { Button } from "@/components/ui/Button";

interface SessionContextProps {
  sessionExpired: boolean;
  setSessionExpired: (expired: boolean) => void;
}

const SessionContext = React.createContext<SessionContextProps | undefined>(undefined);

export function SessionProvider({ children }: { children: React.ReactNode }) {
  const { token, logout } = useAuth();
  const [sessionExpired, setSessionExpired] = React.useState(false);
  const [mounted, setMounted] = React.useState(false);

  React.useEffect(() => {
    setMounted(true);
  }, []);

  // Monitor token state transitions
  React.useEffect(() => {
    if (!token && mounted && localStorage.getItem("flowforge-token-expired") === "true") {
      setSessionExpired(true);
    }
  }, [token, mounted]);

  const handleCloseDialog = () => {
    setSessionExpired(false);
    localStorage.removeItem("flowforge-token-expired");
    logout();
    if (typeof window !== "undefined") {
      window.location.href = "/login";
    }
  };

  if (!mounted) {
    return <>{children}</>;
  }

  return (
    <SessionContext.Provider value={{ sessionExpired, setSessionExpired }}>
      {children}
      <Dialog isOpen={sessionExpired} onClose={handleCloseDialog}>
        <DialogHeader>
          <DialogTitle>Session Expired</DialogTitle>
          <DialogDescription>
            Your login session has expired or is no longer valid. Please sign in again to restore access.
          </DialogDescription>
        </DialogHeader>
        <DialogFooter>
          <Button onClick={handleCloseDialog} className="w-full">
            Proceed to Sign In
          </Button>
        </DialogFooter>
      </Dialog>
    </SessionContext.Provider>
  );
}

export function useSession() {
  const context = React.useContext(SessionContext);
  if (!context) {
    return {
      sessionExpired: false,
      setSessionExpired: () => {},
    };
  }
  return context;
}
