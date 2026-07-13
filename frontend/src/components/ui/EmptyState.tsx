import * as React from "react";
import { cn } from "@/lib/utils";
import { Button } from "./Button";

export interface EmptyStateProps {
  title: string;
  description: string;
  icon?: React.ReactNode;
  actionText?: string;
  onAction?: () => void;
  className?: string;
}

export function EmptyState({
  title,
  description,
  icon,
  actionText,
  onAction,
  className,
}: EmptyStateProps) {
  return (
    <div
      className={cn(
        "flex flex-col items-center justify-center rounded-lg border border-dashed border-slate-800 p-8 text-center bg-slate-900/10 backdrop-blur-xs",
        className
      )}
    >
      {icon && (
        <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-slate-800/80 text-slate-300 mb-4">
          {icon}
        </div>
      )}
      <h3 className="text-lg font-semibold text-slate-200">{title}</h3>
      <p className="mt-1.5 text-sm text-slate-400 max-w-sm">{description}</p>
      {actionText && onAction && (
        <div className="mt-6">
          <Button onClick={onAction} size="sm">
            {actionText}
          </Button>
        </div>
      )}
    </div>
  );
}
