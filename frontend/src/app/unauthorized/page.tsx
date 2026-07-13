import Link from "next/link";
import { ArrowLeft, ShieldAlert } from "lucide-react";

export default function UnauthorizedPage() {
  return (
    <div className="flex min-h-screen w-screen flex-col items-center justify-center bg-slate-950 text-slate-100 p-4">
      <div className="text-center space-y-6 max-w-md">
        <div className="mx-auto flex h-16 w-16 items-center justify-center rounded-full bg-red-950/40 border border-red-900/30 text-red-400">
          <ShieldAlert className="h-8 w-8 text-red-500 animate-bounce" />
        </div>
        <div className="space-y-2">
          <h1 className="text-3xl font-bold tracking-tight text-slate-100">Access Denied</h1>
          <p className="text-sm text-slate-400">
            You do not have the required permissions to access this feature. Please contact your tenant owner or administrator.
          </p>
        </div>
        <div>
          <Link
            href="/dashboard"
            className="inline-flex items-center justify-center rounded-md text-sm font-semibold h-10 px-6 bg-slate-900 border border-slate-800 hover:bg-slate-800 text-slate-200 transition-colors gap-1.5"
          >
            <ArrowLeft className="h-4 w-4" />
            Back to Dashboard
          </Link>
        </div>
      </div>
    </div>
  );
}
