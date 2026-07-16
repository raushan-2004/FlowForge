"use client";

import * as React from "react";
import { Breadcrumbs } from "./Breadcrumbs";
import { TenantSwitcher } from "./TenantSwitcher";
import { NotificationArea } from "./NotificationArea";
import { UserMenu } from "./UserMenu";
import { Menu } from "lucide-react";
import { usePreferences } from "@/providers/UserPreferencesProvider";

export function TopNav() {
  const { preferences, setSidebarCollapsed } = usePreferences();

  return (
    <header className="flex h-16 w-full items-center justify-between border-b border-slate-800/80 bg-slate-950 px-4 md:px-6 shrink-0 gap-2">
      <div className="flex items-center gap-3 min-w-0">
        {/* Mobile spacer for hamburger button */}
        <div className="w-8 md:hidden" />
        {/* Toggle trigger for collapsed desktop layout */}
        {preferences.sidebarCollapsed && (
          <button
            onClick={() => setSidebarCollapsed(false)}
            className="hidden md:flex p-1.5 rounded-md hover:bg-slate-800/80 text-slate-500 hover:text-slate-100 transition-colors cursor-pointer"
            aria-label="Expand sidebar"
          >
            <Menu className="h-5 w-5" />
          </button>
        )}
        <Breadcrumbs />
        <span className="text-slate-700 text-sm hidden sm:inline">|</span>
        <TenantSwitcher />
      </div>

      <div className="flex items-center gap-3 shrink-0">
        {/* Keyboard shortcut hint */}
        <span className="hidden lg:inline-block text-[11px] bg-slate-900 border border-slate-800 text-slate-500 rounded-md px-2 py-1 font-mono whitespace-nowrap">
          Press Ctrl+K
        </span>
        <NotificationArea />
        <UserMenu />
      </div>
    </header>
  );
}

