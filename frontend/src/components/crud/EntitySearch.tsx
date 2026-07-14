"use client";

import * as React from "react";
import { SearchInput } from "../ui/SearchInput";

interface EntitySearchProps {
  placeholder?: string;
  value: string;
  onChange: (value: string) => void;
}

export function EntitySearch({ placeholder = "Search...", value, onChange }: EntitySearchProps) {
  return (
    <SearchInput
      placeholder={placeholder}
      value={value}
      onChange={(e) => onChange(e.target.value)}
      onClear={() => onChange("")}
      className="bg-slate-950 border-slate-800"
    />
  );
}
