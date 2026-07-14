"use client";

import * as React from "react";
import { Handle, Position } from "@xyflow/react";
import { Play } from "lucide-react";

export function StartNode() {
  return (
    <div className="px-4 py-3 rounded-lg border border-purple-500/50 bg-purple-950/20 shadow-lg text-slate-100 flex items-center space-x-3 w-44">
      <div className="h-8 w-8 rounded-full bg-purple-500/20 flex items-center justify-center border border-purple-500">
        <Play className="h-4 w-4 text-purple-400 fill-purple-400" />
      </div>
      <div>
        <p className="text-xs uppercase font-bold text-slate-500 tracking-wider">Trigger</p>
        <p className="text-sm font-bold text-slate-200">START</p>
      </div>
      <Handle
        type="source"
        position={Position.Right}
        className="w-2.5 h-2.5 bg-purple-500 border border-slate-950 rounded-full"
      />
    </div>
  );
}
