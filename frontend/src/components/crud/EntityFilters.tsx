"use client";

import * as React from "react";
import { Filter } from "lucide-react";

export interface FilterOption {
  value: string;
  label: string;
}

export interface FilterDefinition {
  key: string;
  label: string;
  options: FilterOption[];
}

interface EntityFiltersProps {
  filters: FilterDefinition[];
  selectedValues: Record<string, string>;
  onFilterChange: (key: string, value: string) => void;
}

export function EntityFilters({ filters, selectedValues, onFilterChange }: EntityFiltersProps) {
  return (
    <div className="flex flex-wrap items-center gap-2">
      <div className="flex items-center space-x-1.5 text-xs text-slate-500 mr-1 select-none">
        <Filter className="h-3.5 w-3.5" />
        <span>Filters:</span>
      </div>
      {filters.map((f) => (
        <div
          key={f.key}
          className="flex items-center space-x-1 text-xs bg-slate-900 border border-slate-800 rounded-md px-2.5 py-1.5 text-slate-300"
        >
          <span className="font-semibold text-slate-500">{f.label}:</span>
          <select
            value={selectedValues[f.key] || ""}
            onChange={(e) => onFilterChange(f.key, e.target.value)}
            className="bg-transparent border-0 text-slate-200 focus:ring-0 outline-hidden font-medium cursor-pointer py-0 pl-1 pr-6"
          >
            <option value="" className="bg-slate-950">All</option>
            {f.options.map((opt) => (
              <option key={opt.value} value={opt.value} className="bg-slate-950">
                {opt.label}
              </option>
            ))}
          </select>
        </div>
      ))}
    </div>
  );
}
