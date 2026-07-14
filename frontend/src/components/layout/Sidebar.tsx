"use client";

import * as React from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { motion } from "framer-motion";
import {
  LayoutDashboard,
  FolderKanban,
  Activity,
  GitBranch,
  Settings,
  Users,
  BarChart3,
  ChevronLeft,
  Menu,
  Cpu,
  Key,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { usePreferences } from "@/providers/UserPreferencesProvider";
import { useAuth } from "@/providers/AuthProvider";

const navItems = [
  { href: "/dashboard", label: "Dashboard", icon: LayoutDashboard },
  { href: "/projects", label: "Projects", icon: FolderKanban, permission: "VIEW_PROJECTS" },
  { href: "/jobs", label: "Jobs", icon: Cpu, permission: "VIEW_PROJECTS" },
  { href: "/apikeys", label: "API Keys", icon: Key, permission: "VIEW_PROJECTS" },
  { href: "/workflows", label: "Workflows", icon: GitBranch, permission: "VIEW_PROJECTS" },
  { href: "/executions", label: "Executions", icon: Activity, permission: "VIEW_PROJECTS" },
  { href: "/workers", label: "Workers", icon: Users, permission: "MANAGE_USERS" },
  { href: "/analytics", label: "Analytics", icon: BarChart3, permission: "VIEW_ANALYTICS" },
  { href: "/settings", label: "Settings", icon: Settings },
];

export function Sidebar() {
  const pathname = usePathname();
  const { preferences, setSidebarCollapsed } = usePreferences();
  const { hasPermission } = useAuth();
  const isCollapsed = preferences.sidebarCollapsed;

  const visibleItems = navItems.filter((item) => {
    if (item.permission) {
      return hasPermission(item.permission);
    }
    return true;
  });

  return (
    <motion.aside
      animate={{ width: isCollapsed ? 70 : 260 }}
      transition={{ duration: 0.25, ease: "easeInOut" }}
      className="hidden md:flex flex-col h-screen bg-slate-950 border-r border-slate-900 text-slate-400 select-none overflow-hidden shrink-0"
    >
      {/* Brand Header */}
      <div className="flex items-center justify-between px-6 py-4 border-b border-slate-900 h-16">
        {!isCollapsed && (
          <Link href="/dashboard" className="flex items-center space-x-2">
            <span className="h-6 w-6 rounded-md bg-purple-600 flex items-center justify-center font-bold text-white text-sm">F</span>
            <span className="font-bold text-slate-100 text-md tracking-wide">FlowForge</span>
          </Link>
        )}
        {isCollapsed && (
          <div className="mx-auto h-6 w-6 rounded-md bg-purple-600 flex items-center justify-center font-bold text-white text-sm">F</div>
        )}
        {!isCollapsed && (
          <button
            onClick={() => setSidebarCollapsed(true)}
            className="p-1 rounded-md hover:bg-slate-800 text-slate-400 hover:text-slate-100 transition-colors cursor-pointer"
          >
            <ChevronLeft className="h-4 w-4" />
          </button>
        )}
        {isCollapsed && (
          <button
            onClick={() => setSidebarCollapsed(false)}
            className="hidden md:block mx-auto p-1 rounded-md hover:bg-slate-800 text-slate-400 hover:text-slate-100 transition-colors cursor-pointer"
          >
            <Menu className="h-4 w-4" />
          </button>
        )}
      </div>

      {/* Nav List */}
      <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
        {visibleItems.map((item) => {
          const isActive = pathname.startsWith(item.href);
          const Icon = item.icon;

          return (
            <Link
              key={item.href}
              href={item.href}
              className={cn(
                "flex items-center space-x-3 px-3 py-2 rounded-md text-sm font-medium transition-all duration-150",
                {
                  "bg-purple-950/40 text-purple-400 border border-purple-900/30": isActive,
                  "hover:bg-slate-900 hover:text-slate-200 border border-transparent": !isActive,
                }
              )}
            >
              <Icon className="h-5 w-5 shrink-0" />
              {!isCollapsed && <span>{item.label}</span>}
            </Link>
          );
        })}
      </nav>
    </motion.aside>
  );
}
