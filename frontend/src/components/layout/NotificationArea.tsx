"use client";

import * as React from "react";
import { Bell, Info, AlertTriangle, CheckCircle2 } from "lucide-react";
import { cn } from "@/lib/utils";

interface NotificationItem {
  id: string;
  type: "info" | "warning" | "success";
  title: string;
  message: string;
  time: string;
}

const mockNotifications: NotificationItem[] = [
  {
    id: "1",
    type: "success",
    title: "Workflow Run Succeeded",
    message: "Production Backup workflow completed with 0 failures.",
    time: "5 mins ago",
  },
  {
    id: "2",
    type: "warning",
    title: "Worker Lease Expired",
    message: "Worker worker-420fe lease recovered by failover node.",
    time: "20 mins ago",
  },
  {
    id: "3",
    type: "info",
    title: "Project Tenant Configured",
    message: "Standard quotas successfully initialized for Tenant A.",
    time: "1 hour ago",
  },
];

export function NotificationArea() {
  const [isOpen, setIsOpen] = React.useState(false);
  const ref = React.useRef<HTMLDivElement>(null);

  React.useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (ref.current && !ref.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  return (
    <div ref={ref} className="relative">
      <button
        onClick={() => setIsOpen((prev) => !prev)}
        className="relative p-1.5 rounded-md hover:bg-slate-800 text-slate-400 hover:text-slate-100 transition-colors cursor-pointer focus:outline-hidden"
      >
        <Bell className="h-5 w-5" />
        <span className="absolute top-1 right-1 h-2.5 w-2.5 rounded-full bg-purple-500 border-2 border-slate-900" />
      </button>

      {isOpen && (
        <div className="absolute right-0 mt-2 w-80 rounded-md border border-slate-800 bg-slate-950 p-1 shadow-lg text-slate-400 z-50">
          <div className="px-4 py-2.5 border-b border-slate-900 flex justify-between items-center">
            <span className="text-sm font-semibold text-slate-200">Alert Center</span>
            <span className="text-xs text-purple-400 hover:underline cursor-pointer">Clear All</span>
          </div>

          <div className="divide-y divide-slate-900 max-h-80 overflow-y-auto">
            {mockNotifications.map((notif) => (
              <div key={notif.id} className="p-3.5 hover:bg-slate-900/50 transition-colors flex items-start space-x-3">
                {notif.type === "success" && <CheckCircle2 className="h-5 w-5 text-green-500 shrink-0 mt-0.5" />}
                {notif.type === "warning" && <AlertTriangle className="h-5 w-5 text-orange-500 shrink-0 mt-0.5" />}
                {notif.type === "info" && <Info className="h-5 w-5 text-blue-500 shrink-0 mt-0.5" />}

                <div>
                  <p className="text-xs font-semibold text-slate-200">{notif.title}</p>
                  <p className="text-[11px] text-slate-400 mt-0.5 leading-relaxed">{notif.message}</p>
                  <span className="text-[10px] text-slate-500 block mt-1">{notif.time}</span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
