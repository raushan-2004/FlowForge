"use client";

import * as React from "react";
import { ThemeProvider } from "@/providers/ThemeProvider";
import { AuthProvider } from "@/providers/AuthProvider";
import { UserPreferencesProvider } from "@/providers/UserPreferencesProvider";
import { QueryProvider } from "@/providers/QueryProvider";

export default function AuthLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <QueryProvider>
      <ThemeProvider>
        <AuthProvider>
          <UserPreferencesProvider>
            <div className="flex min-h-screen w-screen items-center justify-center bg-slate-950 text-slate-100 p-4">
              {children}
            </div>
          </UserPreferencesProvider>
        </AuthProvider>
      </ThemeProvider>
    </QueryProvider>
  );
}
