"use client";

import * as React from "react";
import { Button } from "@/components/ui/Button";
import { useTheme } from "@/providers/ThemeProvider";

export default function SettingsPage() {
  const { theme, toggleTheme } = useTheme();

  return (
    <div className="space-y-6 max-w-4xl">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-slate-100">Settings</h1>
        <p className="text-sm text-slate-400">Configure theme preferences, tenant limits, API keys, and notification channels.</p>
      </div>

      <div className="rounded-lg border border-slate-800 bg-slate-900/30 p-6 space-y-6">
        {/* Theme Settings */}
        <div className="flex items-center justify-between">
          <div className="space-y-0.5">
            <h3 className="text-sm font-semibold text-slate-200">System Theme</h3>
            <p className="text-xs text-slate-400">Toggle between light mode and dark mode preferences.</p>
          </div>
          <Button onClick={toggleTheme} variant="outline" size="sm">
            Current: {theme.toUpperCase()}
          </Button>
        </div>

        <hr className="border-slate-800" />

        {/* Access Quotas */}
        <div className="flex items-center justify-between">
          <div className="space-y-0.5">
            <h3 className="text-sm font-semibold text-slate-200">Tenant Limits & Quotas</h3>
            <p className="text-xs text-slate-400">View current developer resource allocation boundaries.</p>
          </div>
          <div className="text-right">
            <span className="text-xs font-semibold text-purple-400">Premium Tier</span>
          </div>
        </div>

        <hr className="border-slate-800" />

        {/* Security / Tokens */}
        <div className="flex items-center justify-between">
          <div className="space-y-0.5">
            <h3 className="text-sm font-semibold text-slate-200">API Tokens</h3>
            <p className="text-xs text-slate-400">Manage active developer tokens for command-line tool integrations.</p>
          </div>
          <Button variant="secondary" size="sm">
            Manage Keys
          </Button>
        </div>
      </div>
    </div>
  );
}
