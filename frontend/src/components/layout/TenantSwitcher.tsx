"use client";

import * as React from "react";
import { useAuth } from "@/providers/AuthProvider";
import { ChevronDown, Building, Check } from "lucide-react";
import { cn } from "@/lib/utils";

export function TenantSwitcher() {
  const { user, currentTenant, switchTenant } = useAuth();
  const [isOpen, setIsOpen] = React.useState(false);
  const ref = React.useRef<HTMLDivElement>(null);

  // Close on clicks outside
  React.useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (ref.current && !ref.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  if (!user || !user.tenants || user.tenants.length <= 1) {
    // Hide or display plain label if there is only 1 tenant
    return (
      <div className="flex items-center space-x-2 text-xs font-semibold bg-slate-900 border border-slate-800 text-slate-300 rounded-md px-2.5 py-1.5 select-none">
        <Building className="h-3.5 w-3.5 text-purple-400 shrink-0" />
        <span>{currentTenant?.name || "Single Tenant"}</span>
      </div>
    );
  }

  return (
    <div ref={ref} className="relative">
      <button
        onClick={() => setIsOpen((prev) => !prev)}
        className="flex items-center space-x-2 text-xs font-semibold bg-slate-900 hover:bg-slate-850 border border-slate-800 text-slate-300 hover:text-slate-100 rounded-md px-2.5 py-1.5 cursor-pointer transition-colors focus:outline-hidden focus:ring-1 focus:ring-purple-500"
      >
        <Building className="h-3.5 w-3.5 text-purple-400 shrink-0" />
        <span className="truncate max-w-[120px]">{currentTenant?.name || "Select Tenant"}</span>
        <ChevronDown className="h-3 w-3 text-slate-500 shrink-0" />
      </button>

      {isOpen && (
        <div className="absolute left-0 mt-2 w-56 rounded-md border border-slate-800 bg-slate-950 p-1 shadow-lg text-slate-400 z-50">
          <p className="text-[10px] uppercase font-bold text-slate-500 tracking-wider px-3 py-1.5 border-b border-slate-900">
            Active Tenant Switcher
          </p>
          <div className="py-1">
            {user.tenants.map((t) => {
              const isSelected = t.id === currentTenant?.id;
              return (
                <button
                  key={t.id}
                  onClick={() => {
                    switchTenant(t.id);
                    setIsOpen(false);
                  }}
                  className={cn(
                    "flex w-full items-center justify-between px-3 py-2 rounded-md text-xs hover:bg-slate-900 hover:text-slate-100 transition-colors text-left cursor-pointer",
                    {
                      "text-purple-400 font-semibold": isSelected,
                    }
                  )}
                >
                  <span className="truncate">{t.name}</span>
                  {isSelected && <Check className="h-3.5 w-3.5 shrink-0" />}
                </button>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}
