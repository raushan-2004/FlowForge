"use client";

import * as React from "react";
import { useRouter } from "next/navigation";
import { Dialog, DialogHeader, DialogTitle } from "../ui/Dialog";
import { SearchInput } from "../ui/SearchInput";
import { FileCode, Play, Cpu, GitFork, User, Settings } from "lucide-react";

const searchTargets = [
  { label: "Go to Dashboard", path: "/dashboard", icon: Play },
  { label: "Manage Projects", path: "/projects", icon: FileCode },
  { label: "Traverse Workflows", path: "/workflows", icon: GitFork },
  { label: "Audit Executions", path: "/executions", icon: Cpu },
  { label: "Configure Workers", path: "/workers", icon: Settings },
];

export function CommandPalette() {
  const router = useRouter();
  const [isOpen, setIsOpen] = React.useState(false);
  const [search, setSearch] = React.useState("");

  React.useEffect(() => {
    function handleKeyDown(e: KeyboardEvent) {
      if ((e.ctrlKey || e.metaKey) && e.key === "k") {
        e.preventDefault();
        setIsOpen((prev) => !prev);
      }
    }
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, []);

  const filtered = searchTargets.filter((item) =>
    item.label.toLowerCase().includes(search.toLowerCase())
  );

  const handleSelect = (path: string) => {
    setIsOpen(false);
    setSearch("");
    router.push(path);
  };

  return (
    <Dialog isOpen={isOpen} onClose={() => setIsOpen(false)}>
      <DialogHeader>
        <DialogTitle className="text-sm font-semibold text-slate-400">Search Navigation Shortcut</DialogTitle>
      </DialogHeader>

      <div className="mt-2">
        <SearchInput
          placeholder="Type command or destination... (Ctrl+K)"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          onClear={() => setSearch("")}
          className="max-w-full bg-slate-950 border-slate-800"
        />
      </div>

      <div className="mt-4 max-h-60 overflow-y-auto divide-y divide-slate-850">
        {filtered.map((item, idx) => {
          const Icon = item.icon;
          return (
            <button
              key={idx}
              onClick={() => handleSelect(item.path)}
              className="flex w-full items-center px-3 py-2.5 rounded-md text-sm text-slate-300 hover:bg-slate-800/50 hover:text-slate-100 transition-colors text-left cursor-pointer"
            >
              <Icon className="mr-3 h-4 w-4 text-purple-400 shrink-0" />
              <span>{item.label}</span>
            </button>
          );
        })}
        {filtered.length === 0 && (
          <p className="text-center text-xs text-slate-500 py-4">No matching commands found.</p>
        )}
      </div>
    </Dialog>
  );
}
