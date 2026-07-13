import Link from "next/link";
import { ArrowLeft, Ban } from "lucide-react";

export default function NotFound() {
  return (
    <div className="flex min-h-screen w-screen flex-col items-center justify-center bg-slate-950 text-slate-100 p-4">
      <div className="text-center space-y-6 max-w-md">
        <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-slate-900 border border-slate-800 text-slate-400">
          <Ban className="h-8 w-8 text-purple-500 animate-pulse" />
        </div>
        <div className="space-y-2">
          <h1 className="text-3xl font-bold tracking-tight text-slate-100">Page Not Found</h1>
          <p className="text-sm text-slate-400">
            The resource you are trying to access does not exist or has been moved.
          </p>
        </div>
        <div>
          <Link
            href="/dashboard"
            className="inline-flex items-center justify-center rounded-md text-sm font-semibold h-10 px-6 bg-purple-600 hover:bg-purple-700 text-white shadow-md shadow-purple-600/20 transition-all duration-150 gap-1.5"
          >
            <ArrowLeft className="h-4 w-4" />
            Back to Dashboard
          </Link>
        </div>
      </div>
    </div>
  );
}
