import * as React from "react";
import { Search, X } from "lucide-react";
import { cn } from "@/lib/utils";

export interface SearchInputProps
  extends React.InputHTMLAttributes<HTMLInputElement> {
  onClear?: () => void;
}

const SearchInput = React.forwardRef<HTMLInputElement, SearchInputProps>(
  ({ className, value, onChange, onClear, ...props }, ref) => {
    const hasValue = !!value;

    return (
      <div className="relative flex items-center w-full max-w-sm">
        <Search className="absolute left-3 h-4 w-4 text-slate-500 pointer-events-none" />
        <input
          type="text"
          value={value}
          onChange={onChange}
          ref={ref}
          className={cn(
            "h-10 w-full rounded-md border border-slate-800 bg-slate-950 pl-9 pr-8 text-sm text-slate-200 placeholder:text-slate-500 focus-visible:outline-hidden focus-visible:ring-2 focus-visible:ring-purple-500 disabled:cursor-not-allowed disabled:opacity-50",
            className
          )}
          {...props}
        />
        {hasValue && onClear && (
          <button
            type="button"
            onClick={onClear}
            className="absolute right-2.5 p-1 rounded-sm text-slate-400 hover:text-slate-200 focus:outline-hidden focus:ring-1 focus:ring-purple-500 cursor-pointer"
          >
            <X className="h-3 w-3" />
          </button>
        )}
      </div>
    );
  }
);
SearchInput.displayName = "SearchInput";

export { SearchInput };
