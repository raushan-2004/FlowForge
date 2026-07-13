import { cn } from "@/lib/utils";

export interface SpinnerProps extends React.HTMLAttributes<HTMLDivElement> {
  size?: "sm" | "default" | "lg";
}

export function Spinner({ size = "default", className, ...props }: SpinnerProps) {
  return (
    <div
      role="status"
      className={cn(
        "animate-spin rounded-full border-2 border-slate-700 border-t-purple-500",
        {
          "h-4 w-4 border-1.5": size === "sm",
          "h-8 w-8": size === "default",
          "h-12 w-12 border-3": size === "lg",
        },
        className
      )}
      {...props}
    >
      <span className="sr-only">Loading...</span>
    </div>
  );
}
