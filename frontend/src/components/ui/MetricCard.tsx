import * as React from "react";
import { Card, CardContent } from "./Card";
import { cn } from "@/lib/utils";

export interface MetricCardProps {
  title: string;
  value: string | number;
  description?: string;
  trend?: {
    value: number;
    isPositive: boolean;
  };
  icon?: React.ReactNode;
  className?: string;
}

export function MetricCard({
  title,
  value,
  description,
  trend,
  icon,
  className,
}: MetricCardProps) {
  return (
    <Card className={cn("overflow-hidden border border-slate-800 bg-slate-900/40", className)}>
      <CardContent className="p-6">
        <div className="flex items-center justify-between space-y-0 pb-2">
          <p className="text-sm font-medium text-slate-400">{title}</p>
          {icon && <div className="text-slate-400">{icon}</div>}
        </div>
        <div className="flex items-baseline space-x-2">
          <h2 className="text-3xl font-bold tracking-tight text-slate-100">{value}</h2>
          {trend && (
            <span
              className={cn(
                "inline-flex items-center text-xs font-semibold rounded-full px-1.5 py-0.5",
                {
                  "bg-green-950/50 text-green-400": trend.isPositive,
                  "bg-red-950/50 text-red-400": !trend.isPositive,
                }
              )}
            >
              {trend.isPositive ? "+" : "-"}
              {Math.abs(trend.value)}%
            </span>
          )}
        </div>
        {description && <p className="text-xs text-slate-500 mt-1">{description}</p>}
      </CardContent>
    </Card>
  );
}
