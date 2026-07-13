import * as React from "react";
import { cn } from "@/lib/utils";

export interface BadgeProps extends React.HTMLAttributes<HTMLDivElement> {
  variant?: "default" | "secondary" | "outline" | "destructive" | "success" | "warning" | "info";
}

function Badge({ className, variant = "default", ...props }: BadgeProps) {
  return (
    <div
      className={cn(
        "inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold transition-colors focus:outline-hidden focus:ring-2 focus:ring-ring focus:ring-offset-2",
        {
          "border-transparent bg-purple-600 text-white hover:bg-purple-700": variant === "default",
          "border-transparent bg-slate-800 text-slate-100 hover:bg-slate-700": variant === "secondary",
          "border-slate-700 bg-transparent text-slate-300 hover:bg-slate-800": variant === "outline",
          "border-transparent bg-red-950/80 text-red-400 border-red-900/50": variant === "destructive",
          "border-transparent bg-green-950/80 text-green-400 border-green-900/50": variant === "success",
          "border-transparent bg-orange-950/80 text-orange-400 border-orange-900/50": variant === "warning",
          "border-transparent bg-blue-950/80 text-blue-400 border-blue-900/50": variant === "info",
        },
        className
      )}
      {...props}
    />
  );
}

export { Badge };
