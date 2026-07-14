"use client";

import * as React from "react";
import { EntitySearch } from "./EntitySearch";
import { EntityFilters, FilterDefinition } from "./EntityFilters";

interface EntityToolbarProps {
  searchPlaceholder?: string;
  searchValue: string;
  onSearchChange: (value: string) => void;
  filters?: FilterDefinition[];
  selectedFilters?: Record<string, string>;
  onFilterChange?: (key: string, value: string) => void;
  actionButton?: React.ReactNode;
}

export function EntityToolbar({
  searchPlaceholder,
  searchValue,
  onSearchChange,
  filters = [],
  selectedFilters = {},
  onFilterChange,
  actionButton,
}: EntityToolbarProps) {
  return (
    <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 py-4 border-b border-slate-900 mb-4">
      <div className="flex flex-col sm:flex-row sm:items-center gap-3 flex-1">
        <EntitySearch
          placeholder={searchPlaceholder}
          value={searchValue}
          onChange={onSearchChange}
        />
        {filters.length > 0 && onFilterChange && (
          <EntityFilters
            filters={filters}
            selectedValues={selectedFilters}
            onFilterChange={onFilterChange}
          />
        )}
      </div>
      {actionButton && <div className="shrink-0">{actionButton}</div>}
    </div>
  );
}
