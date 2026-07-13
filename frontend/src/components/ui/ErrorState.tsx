import * as React from "react";
import { AlertCircle } from "lucide-react";
import { Button } from "./Button";
import { cn } from "@/lib/utils";

export interface ErrorStateProps {
  title?: string;
  message: string;
  onRetry?: () => void;
  className?: string;
}

export function ErrorState({
  title = "An error occurred",
  message,
  onRetry,
  className,
}: ErrorStateProps) {
  return (
    <div
      className={cn(
        "flex flex-col items-center justify-center rounded-lg border border-red-900/50 bg-red-950/10 p-8 text-center text-slate-200 backdrop-blur-xs",
        className
      )}
    >
      <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-red-950/80 text-red-400 mb-4 border border-red-900/50">
        <AlertCircle className="h-6 w-6" />
      </div>
      <h3 className="text-lg font-semibold text-red-200">{title}</h3>
      <p className="mt-1.5 text-sm text-red-300 max-w-sm">{message}</p>
      {onRetry && (
        <div className="mt-6">
          <Button onClick={onRetry} variant="outline" size="sm" className="border-red-900/50 text-red-300 hover:bg-red-950/30">
            Try Again
          </Button>
        </div>
      )}
    </div>
  );
}
