"use client";

import * as React from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { ChevronRight, Home } from "lucide-react";

export function Breadcrumbs() {
  const pathname = usePathname();
  const paths = pathname.split("/").filter(Boolean);

  if (paths.length === 0) return null;

  return (
    <nav className="flex items-center space-x-1.5 text-sm text-slate-500 font-medium">
      <Link href="/dashboard" className="hover:text-slate-200 transition-colors flex items-center">
        <Home className="h-4 w-4" />
      </Link>
      {paths.map((path, idx) => {
        const href = `/${paths.slice(0, idx + 1).join("/")}`;
        const isLast = idx === paths.length - 1;
        const label = path.charAt(0).toUpperCase() + path.slice(1);

        return (
          <React.Fragment key={href}>
            <ChevronRight className="h-4 w-4 text-slate-600" />
            {isLast ? (
              <span className="text-slate-300 select-none">{label}</span>
            ) : (
              <Link href={href} className="hover:text-slate-200 transition-colors">
                {label}
              </Link>
            )}
          </React.Fragment>
        );
      })}
    </nav>
  );
}
