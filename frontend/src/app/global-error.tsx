"use client";

import * as React from "react";
import { AlertTriangle, RefreshCw, Home } from "lucide-react";

export default function GlobalError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  React.useEffect(() => {
    // Log to error reporting service in production
    console.error("[FlowForge Error Boundary]", error);
  }, [error]);

  return (
    <html lang="en">
      <body className="min-h-screen bg-slate-950 text-slate-100 flex items-center justify-center p-6">
        <div className="text-center space-y-6 max-w-md">
          <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-red-950/60 border border-red-800/50">
            <AlertTriangle className="h-8 w-8 text-red-400" />
          </div>
          <div className="space-y-2">
            <h1 className="text-2xl font-bold text-slate-100">Something went wrong</h1>
            <p className="text-sm text-slate-400">
              An unexpected error occurred. Our team has been notified.
              {error.digest && (
                <span className="block mt-1 font-mono text-xs text-slate-600">
                  Error ID: {error.digest}
                </span>
              )}
            </p>
          </div>
          <div className="flex flex-col sm:flex-row gap-3 justify-center">
            <button
              onClick={() => reset()}
              className="inline-flex items-center justify-center gap-2 h-10 px-6 rounded-lg bg-violet-600 hover:bg-violet-500 text-white text-sm font-semibold transition-colors"
            >
              <RefreshCw className="h-4 w-4" /> Try again
            </button>
            <a
              href="/"
              className="inline-flex items-center justify-center gap-2 h-10 px-6 rounded-lg border border-slate-700 bg-slate-900 hover:bg-slate-800 text-slate-200 text-sm font-semibold transition-colors"
            >
              <Home className="h-4 w-4" /> Go home
            </a>
          </div>
        </div>
      </body>
    </html>
  );
}
