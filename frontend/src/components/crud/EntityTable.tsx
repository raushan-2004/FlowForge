"use client";

import * as React from "react";
import { Table, TableHeader, TableBody, TableRow, TableCell, TableHead } from "../ui/Table";
import { Skeleton } from "../ui/Skeleton";
import { EmptyState } from "../ui/EmptyState";
import { cn } from "@/lib/utils";

export interface ColumnDef<T> {
  key: string;
  header: string;
  className?: string;
  render?: (item: T) => React.ReactNode;
}

interface EntityTableProps<T> {
  columns: ColumnDef<T>[];
  data: T[];
  idKey: keyof T;
  selectedIds?: string[];
  onSelectRow?: (id: string, selected: boolean) => void;
  onSelectAll?: (selected: boolean) => void;
  isLoading?: boolean;
  emptyTitle?: string;
  emptyDescription?: string;
  mobileCardRender: (item: T) => React.ReactNode;
}

export function EntityTable<T>({
  columns,
  data,
  idKey,
  selectedIds = [],
  onSelectRow,
  onSelectAll,
  isLoading,
  emptyTitle,
  emptyDescription,
  mobileCardRender,
}: EntityTableProps<T>) {
  const allSelected = data.length > 0 && data.every((row) => selectedIds.includes(String(row[idKey])));

  if (isLoading) {
    return (
      <div className="space-y-3">
        {Array.from({ length: 5 }).map((_, idx) => (
          <Skeleton key={idx} className="h-12 w-full" />
        ))}
      </div>
    );
  }

  if (data.length === 0) {
    return (
      <EmptyState
        title={emptyTitle || "No records found"}
        description={emptyDescription || "No items are currently available for this view."}
      />
    );
  }

  return (
    <div className="w-full">
      {/* 1. Mobile & Tablet view (Cards) */}
      <div className="block md:hidden space-y-4">
        {data.map((row, idx) => {
          const id = String(row[idKey]);
          const isSelected = selectedIds.includes(id);

          return (
            <div
              key={idx}
              className={cn(
                "p-4 rounded-lg border bg-slate-900/30 transition-colors",
                isSelected ? "border-purple-600/50 bg-purple-950/5" : "border-slate-900"
              )}
            >
              <div className="flex items-center space-x-2.5 mb-3 border-b border-slate-900 pb-2">
                {onSelectRow && (
                  <input
                    type="checkbox"
                    checked={isSelected}
                    onChange={(e) => onSelectRow(id, e.target.checked)}
                    className="rounded-sm border-slate-800 bg-slate-950 text-purple-600 focus:ring-purple-500 h-4 w-4"
                  />
                )}
                <span className="text-[10px] text-slate-500 font-mono">Row #{idx + 1}</span>
              </div>
              {mobileCardRender(row)}
            </div>
          );
        })}
      </div>

      {/* 2. Desktop view (Full Grid Table) */}
      <div className="hidden md:block">
        <Table>
          <TableHeader>
            <TableRow>
              {onSelectAll && (
                <TableHead className="w-12 text-center">
                  <input
                    type="checkbox"
                    checked={allSelected}
                    onChange={(e) => onSelectAll(e.target.checked)}
                    className="rounded-sm border-slate-800 bg-slate-950 text-purple-600 focus:ring-purple-500 h-4 w-4"
                  />
                </TableHead>
              )}
              {columns.map((col) => (
                <TableHead key={col.key} className={col.className}>
                  {col.header}
                </TableHead>
              ))}
            </TableRow>
          </TableHeader>
          <TableBody>
            {data.map((row, rowIdx) => {
              const id = String(row[idKey]);
              const isSelected = selectedIds.includes(id);

              return (
                <TableRow
                  key={rowIdx}
                  className={cn(
                    "transition-colors",
                    isSelected ? "bg-purple-950/10 border-l border-l-purple-500" : ""
                  )}
                >
                  {onSelectRow && (
                    <TableCell className="w-12 text-center">
                      <input
                        type="checkbox"
                        checked={isSelected}
                        onChange={(e) => onSelectRow(id, e.target.checked)}
                        className="rounded-sm border-slate-800 bg-slate-950 text-purple-600 focus:ring-purple-500 h-4 w-4"
                      />
                    </TableCell>
                  )}
                  {columns.map((col) => (
                    <TableCell key={col.key} className={col.className}>
                      {col.render ? col.render(row) : String(row[col.key as keyof T] ?? "")}
                    </TableCell>
                  ))}
                </TableRow>
              );
            })}
          </TableBody>
        </Table>
      </div>
    </div>
  );
}
