import * as React from "react";
import { Badge } from "./Badge";
import { cn } from "@/lib/utils";

export interface StatusBadgeProps {
  status: string;
  className?: string;
}

export function StatusBadge({ status, className }: StatusBadgeProps) {
  const normalizedStatus = status.toUpperCase();

  let variant: "default" | "secondary" | "outline" | "destructive" | "success" | "warning" | "info" = "secondary";
  let dotColor = "bg-slate-400";

  switch (normalizedStatus) {
    case "SUCCEEDED":
    case "SUCCESS":
    case "ACTIVE":
      variant = "success";
      dotColor = "bg-green-500 animate-pulse";
      break;
    case "FAILED":
    case "FAILURE":
    case "ERROR":
      variant = "destructive";
      dotColor = "bg-red-500";
      break;
    case "RUNNING":
    case "PROCESSING":
    case "CLAIMED":
      variant = "info";
      dotColor = "bg-blue-500 animate-ping";
      break;
    case "QUEUED":
    case "PENDING":
    case "RETRYING":
    case "DRAINING":
      variant = "warning";
      dotColor = "bg-orange-500";
      break;
    default:
      variant = "secondary";
      dotColor = "bg-slate-400";
  }

  return (
    <Badge variant={variant} className={cn("gap-1.5 font-medium px-2 py-0.5", className)}>
      <span className={cn("h-1.5 w-1.5 rounded-full", dotColor)} />
      {status}
    </Badge>
  );
}
