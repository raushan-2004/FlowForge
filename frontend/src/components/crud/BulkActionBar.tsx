"use client";

import * as React from "react";
import { motion, AnimatePresence } from "framer-motion";
import { Button } from "../ui/Button";
import { X } from "lucide-react";

interface BulkAction {
  label: string;
  onClick: () => void;
  variant?: "default" | "destructive" | "outline" | "secondary";
  icon?: React.ReactNode;
}

interface BulkActionBarProps {
  selectedCount: number;
  onClearSelection: () => void;
  actions: BulkAction[];
}

export function BulkActionBar({
  selectedCount,
  onClearSelection,
  actions,
}: BulkActionBarProps) {
  return (
    <AnimatePresence>
      {selectedCount > 0 && (
        <motion.div
          initial={{ y: 80, opacity: 0 }}
          animate={{ y: 0, opacity: 1 }}
          exit={{ y: 80, opacity: 0 }}
          transition={{ type: "spring", stiffness: 300, damping: 25 }}
          className="fixed bottom-6 left-1/2 -translate-x-1/2 z-50 flex items-center justify-between gap-6 px-6 py-4 bg-slate-900 border border-slate-800 rounded-full shadow-2xl shadow-purple-950/20 text-slate-100 min-w-[320px] max-w-lg w-full sm:w-auto"
        >
          <div className="flex items-center space-x-3 text-sm">
            <button
              onClick={onClearSelection}
              className="p-1 rounded-full hover:bg-slate-850 text-slate-400 hover:text-slate-200 cursor-pointer"
              aria-label="Clear selection"
            >
              <X className="h-4 w-4" />
            </button>
            <span className="font-semibold">
              {selectedCount} selected
            </span>
          </div>

          <div className="flex items-center space-x-2">
            {actions.map((act, idx) => (
              <Button
                key={idx}
                variant={act.variant || "secondary"}
                size="sm"
                onClick={act.onClick}
                className="rounded-full gap-1.5 h-8 px-3 text-xs"
              >
                {act.icon}
                {act.label}
              </Button>
            ))}
          </div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}
