"use client";

import * as React from "react";
import { ThemeProvider } from "@/providers/ThemeProvider";
import { AuthProvider } from "@/providers/AuthProvider";
import { UserPreferencesProvider } from "@/providers/UserPreferencesProvider";
import { QueryProvider } from "@/providers/QueryProvider";
import { Sidebar } from "@/components/layout/Sidebar";
import { TopNav } from "@/components/layout/TopNav";
import { CommandPalette } from "@/components/layout/CommandPalette";

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <QueryProvider>
      <ThemeProvider>
        <AuthProvider>
          <UserPreferencesProvider>
            <div className="flex h-screen w-screen overflow-hidden bg-slate-950 text-slate-100 font-sans antialiased">
              <Sidebar />
              <div className="flex flex-col flex-1 h-full min-w-0">
                <TopNav />
                <main className="flex-1 overflow-y-auto bg-slate-950 p-6">
                  {children}
                </main>
              </div>
              <CommandPalette />
            </div>
          </UserPreferencesProvider>
        </AuthProvider>
      </ThemeProvider>
    </QueryProvider>
  );
}
