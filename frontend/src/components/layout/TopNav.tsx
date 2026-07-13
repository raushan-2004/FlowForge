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
    <header className="flex h-16 w-full items-center justify-between border-b border-slate-900 bg-slate-950 px-6 shrink-0">
      <div className="flex items-center space-x-4">
        {/* Toggle trigger for collapsed layout */}
        {preferences.sidebarCollapsed && (
          <button
            onClick={() => setSidebarCollapsed(false)}
            className="p-1 rounded-md hover:bg-slate-900 text-slate-400 hover:text-slate-100 transition-colors cursor-pointer"
            aria-label="Expand sidebar"
          >
            <Menu className="h-5 w-5" />
          </button>
        )}
        <Breadcrumbs />
        <span className="text-slate-800 text-sm">|</span>
        <TenantSwitcher />
      </div>

      <div className="flex items-center space-x-4">
        {/* Helper info helper text */}
        <span className="hidden sm:inline-block text-[11px] bg-slate-900 border border-slate-800 text-slate-400 rounded-md px-2 py-1 font-mono">
          Press Ctrl+K
        </span>
        <NotificationArea />
        <UserMenu />
      </div>
    </header>
  );
}
