"use client";

import * as React from "react";
import { Handle, Position } from "@xyflow/react";
import { CheckCircle2 } from "lucide-react";

export function EndNode() {
  return (
    <div className="px-4 py-3 rounded-lg border border-red-500/50 bg-red-950/20 shadow-lg text-slate-100 flex items-center space-x-3 w-44">
      <Handle
        type="target"
        position={Position.Left}
        className="w-2.5 h-2.5 bg-slate-400 border border-slate-950 rounded-full"
      />
      <div className="h-8 w-8 rounded-full bg-red-500/20 flex items-center justify-center border border-red-500">
        <CheckCircle2 className="h-4 w-4 text-red-400" />
      </div>
      <div>
        <p className="text-xs uppercase font-bold text-slate-500 tracking-wider">Finalization</p>
        <p className="text-sm font-bold text-slate-200">END</p>
      </div>
    </div>
  );
}
