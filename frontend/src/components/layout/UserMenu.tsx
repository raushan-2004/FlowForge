"use client";

import * as React from "react";
import { useAuth } from "@/providers/AuthProvider";
import { User, LogOut, Settings, Shield } from "lucide-react";
import { cn } from "@/lib/utils";

export function UserMenu() {
  const { user, logout } = useAuth();
  const [isOpen, setIsOpen] = React.useState(false);
  const ref = React.useRef<HTMLDivElement>(null);

  // Close on outside clicks
  React.useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (ref.current && !ref.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const initials = user?.name
    ? user.name.split(" ").map((n) => n[0]).join("").toUpperCase()
    : "U";

  return (
    <div ref={ref} className="relative">
      <button
        onClick={() => setIsOpen((prev) => !prev)}
        className="flex items-center justify-center h-8 w-8 rounded-full bg-purple-600/20 text-purple-400 font-bold border border-purple-500/30 hover:bg-purple-600/30 transition-all cursor-pointer focus:outline-hidden focus:ring-2 focus:ring-purple-500"
      >
        {initials}
      </button>

      {isOpen && (
        <div className="absolute right-0 mt-2 w-56 rounded-md border border-slate-800 bg-slate-950 p-1 shadow-lg text-slate-400 z-50">
          <div className="px-3 py-2 border-b border-slate-900">
            <p className="text-sm font-semibold text-slate-100">{user?.name || "Developer Account"}</p>
            <p className="text-xs text-slate-500 truncate">{user?.email || "developer@flowforge.dev"}</p>
          </div>
          <div className="py-1">
            <button
              onClick={() => setIsOpen(false)}
              className="flex w-full items-center px-3 py-2 rounded-md text-sm hover:bg-slate-900 hover:text-slate-100 transition-colors text-left cursor-pointer"
            >
              <User className="mr-2.5 h-4 w-4" />
              Profile Settings
            </button>
            <button
              onClick={() => setIsOpen(false)}
              className="flex w-full items-center px-3 py-2 rounded-md text-sm hover:bg-slate-900 hover:text-slate-100 transition-colors text-left cursor-pointer"
            >
              <Shield className="mr-2.5 h-4 w-4" />
              Access Tokens
            </button>
          </div>
          <div className="border-t border-slate-900 p-1">
            <button
              onClick={() => {
                setIsOpen(false);
                logout();
              }}
              className="flex w-full items-center px-3 py-2 rounded-md text-sm text-red-400 hover:bg-red-950/20 hover:text-red-300 transition-colors text-left cursor-pointer"
            >
              <LogOut className="mr-2.5 h-4 w-4" />
              Sign Out
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
