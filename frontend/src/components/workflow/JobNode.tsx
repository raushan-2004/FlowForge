"use client";

import * as React from "react";
import { Handle, Position } from "@xyflow/react";
import { Cpu } from "lucide-react";
import { cn } from "@/lib/utils";

interface JobNodeData {
  name?: string;
  httpMethod?: string;
  targetUrl?: string;
  jobPublicId?: string;
}

export function JobNode({ data }: { data: JobNodeData }) {
  const isConfigured = !!data.jobPublicId;

  return (
    <div
      className={cn(
        "px-4 py-3 rounded-lg border shadow-lg text-slate-100 flex items-center space-x-3 w-56 transition-all",
        isConfigured
          ? "border-slate-800 bg-slate-900/90"
          : "border-orange-500/50 bg-orange-950/15 animate-pulse"
      )}
    >
      <Handle
        type="target"
        position={Position.Left}
        className="w-2.5 h-2.5 bg-slate-400 border border-slate-950 rounded-full"
      />
      <div className="h-8 w-8 rounded-full bg-slate-800 flex items-center justify-center border border-slate-700 shrink-0">
        <Cpu className={cn("h-4 w-4", isConfigured ? "text-purple-400" : "text-orange-400")} />
      </div>
      <div className="min-w-0 flex-1">
        <p className="text-[9px] uppercase font-bold text-slate-500 tracking-wider">HTTP Dispatch</p>
        <p className="text-xs font-bold text-slate-200 truncate">
          {data.name || "Configure Job"}
        </p>
        {isConfigured && data.httpMethod && (
          <div className="flex items-center space-x-1.5 mt-0.5">
            <span className="text-[9px] bg-slate-950 border border-slate-800 text-purple-400 font-mono px-1 rounded-sm">
              {data.httpMethod}
            </span>
            <span className="text-[9px] text-slate-500 truncate max-w-[90px]">{data.targetUrl}</span>
          </div>
        )}
      </div>
      <Handle
        type="source"
        position={Position.Right}
        className="w-2.5 h-2.5 bg-purple-500 border border-slate-950 rounded-full"
      />
    </div>
  );
}
