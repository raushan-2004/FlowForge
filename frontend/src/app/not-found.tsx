import Link from "next/link";
import { ArrowLeft, Search } from "lucide-react";
import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "404 — Page Not Found",
  description: "The page you are looking for does not exist.",
  robots: { index: false },
};

export default function NotFound() {
  return (
    <div className="flex min-h-screen w-full flex-col items-center justify-center bg-slate-950 text-slate-100 p-6">
      <div className="text-center space-y-8 max-w-md">
        {/* 404 number */}
        <div className="relative">
          <p className="text-[120px] font-black leading-none text-transparent bg-clip-text bg-gradient-to-br from-violet-600/40 to-indigo-600/20 select-none" aria-hidden="true">
            404
          </p>
          <div className="absolute inset-0 flex items-center justify-center">
            <div className="h-16 w-16 rounded-full bg-slate-900 border border-slate-800 flex items-center justify-center">
              <Search className="h-7 w-7 text-slate-500" />
            </div>
          </div>
        </div>

        <div className="space-y-3">
          <h1 className="text-2xl font-bold text-slate-100">Page not found</h1>
          <p className="text-sm text-slate-400 leading-relaxed">
            The page you&apos;re looking for doesn&apos;t exist or has been moved.
            Check the URL or navigate back to where you came from.
          </p>
        </div>

        <div className="flex flex-col sm:flex-row gap-3 justify-center">
          <Link
            href="/dashboard"
            className="inline-flex items-center justify-center gap-2 h-10 px-6 rounded-lg bg-violet-600 hover:bg-violet-500 text-white text-sm font-semibold transition-colors"
          >
            <ArrowLeft className="h-4 w-4" />
            Back to Dashboard
          </Link>
          <Link
            href="/"
            className="inline-flex items-center justify-center gap-2 h-10 px-6 rounded-lg border border-slate-700 bg-slate-900 hover:bg-slate-800 text-slate-200 text-sm font-semibold transition-colors"
          >
            Go to Home
          </Link>
        </div>
      </div>
    </div>
  );
}
