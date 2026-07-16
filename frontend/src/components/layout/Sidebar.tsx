"use client";

import * as React from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { motion, AnimatePresence } from "framer-motion";
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
  X,
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
  const [mobileOpen, setMobileOpen] = React.useState(false);

  const visibleItems = navItems.filter((item) => {
    if (item.permission) {
      return hasPermission(item.permission);
    }
    return true;
  });

  // Close mobile sidebar on route change
  React.useEffect(() => {
    setMobileOpen(false);
  }, [pathname]);

  const NavContent = () => (
    <>
      {/* Nav List */}
      <nav className="flex-1 px-3 py-4 space-y-0.5 overflow-y-auto">
        {visibleItems.map((item) => {
          const isActive = pathname.startsWith(item.href);
          const Icon = item.icon;

          return (
            <Link
              key={item.href}
              href={item.href}
              className={cn(
                "flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-all duration-150",
                {
                  "bg-violet-950/50 text-violet-300 border border-violet-900/40": isActive,
                  "hover:bg-slate-800/70 hover:text-slate-200 text-slate-400 border border-transparent": !isActive,
                }
              )}
            >
              <Icon className="h-4 w-4 shrink-0" />
              {!isCollapsed && <span className="truncate">{item.label}</span>}
            </Link>
          );
        })}
      </nav>
    </>
  );

  return (
    <>
      {/* ── Mobile hamburger trigger ─────────────────────────────────────── */}
      <button
        className="md:hidden fixed top-3 left-3 z-50 p-2 rounded-lg bg-slate-900 border border-slate-800 text-slate-400 hover:text-slate-200 transition-colors shadow-lg"
        onClick={() => setMobileOpen(true)}
        aria-label="Open navigation"
      >
        <Menu className="h-5 w-5" />
      </button>

      {/* ── Mobile overlay ───────────────────────────────────────────────── */}
      <AnimatePresence>
        {mobileOpen && (
          <motion.div
            key="overlay"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            transition={{ duration: 0.15 }}
            className="md:hidden fixed inset-0 z-40 bg-slate-950/80 backdrop-blur-sm"
            onClick={() => setMobileOpen(false)}
          />
        )}
      </AnimatePresence>

      {/* ── Mobile drawer ────────────────────────────────────────────────── */}
      <AnimatePresence>
        {mobileOpen && (
          <motion.aside
            key="mobile-drawer"
            initial={{ x: -280 }}
            animate={{ x: 0 }}
            exit={{ x: -280 }}
            transition={{ duration: 0.2, ease: "easeOut" }}
            className="md:hidden fixed inset-y-0 left-0 z-50 w-[260px] flex flex-col bg-slate-950 border-r border-slate-800 shadow-2xl"
          >
            {/* Mobile Header */}
            <div className="flex items-center justify-between px-4 py-4 h-16 border-b border-slate-800">
              <Link href="/dashboard" className="flex items-center gap-2">
                <span className="h-7 w-7 rounded-lg bg-violet-600 flex items-center justify-center font-bold text-white text-sm">F</span>
                <span className="font-bold text-slate-100 tracking-wide">FlowForge</span>
              </Link>
              <button
                onClick={() => setMobileOpen(false)}
                className="p-1.5 rounded-md text-slate-400 hover:text-slate-100 hover:bg-slate-800 transition-colors"
                aria-label="Close navigation"
              >
                <X className="h-4 w-4" />
              </button>
            </div>
            <NavContent />
          </motion.aside>
        )}
      </AnimatePresence>

      {/* ── Desktop sidebar ──────────────────────────────────────────────── */}
      <motion.aside
        animate={{ width: isCollapsed ? 68 : 256 }}
        transition={{ duration: 0.22, ease: "easeInOut" }}
        className="hidden md:flex flex-col h-screen bg-slate-950 border-r border-slate-800/80 text-slate-400 select-none overflow-hidden shrink-0"
      >
        {/* Brand Header */}
        <div className="flex items-center justify-between px-4 py-4 border-b border-slate-800/80 h-16">
          {!isCollapsed && (
            <Link href="/dashboard" className="flex items-center gap-2 min-w-0">
              <span className="h-7 w-7 rounded-lg bg-violet-600 flex-shrink-0 flex items-center justify-center font-bold text-white text-sm">F</span>
              <span className="font-bold text-slate-100 tracking-wide truncate">FlowForge</span>
            </Link>
          )}
          {isCollapsed && (
            <div className="mx-auto h-7 w-7 rounded-lg bg-violet-600 flex items-center justify-center font-bold text-white text-sm">F</div>
          )}
          {!isCollapsed && (
            <button
              onClick={() => setSidebarCollapsed(true)}
              className="p-1.5 rounded-md hover:bg-slate-800 text-slate-500 hover:text-slate-100 transition-colors cursor-pointer flex-shrink-0"
              aria-label="Collapse sidebar"
            >
              <ChevronLeft className="h-4 w-4" />
            </button>
          )}
          {isCollapsed && (
            <button
              onClick={() => setSidebarCollapsed(false)}
              className="mx-auto p-1.5 rounded-md hover:bg-slate-800 text-slate-500 hover:text-slate-100 transition-colors cursor-pointer"
              aria-label="Expand sidebar"
            >
              <Menu className="h-4 w-4" />
            </button>
          )}
        </div>

        <NavContent />
      </motion.aside>
    </>
  );
}
