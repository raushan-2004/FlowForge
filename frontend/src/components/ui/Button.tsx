import * as React from "react";
import { cn } from "@/lib/utils";

export interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?:
    | "default"
    | "destructive"
    | "outline"
    | "secondary"
    | "ghost"
    | "link";
  size?: "default" | "sm" | "lg" | "icon";
}

const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  ({ className, variant = "default", size = "default", ...props }, ref) => {
    return (
      <button
        className={cn(
          "inline-flex items-center justify-center rounded-md text-sm font-medium transition-colors focus-visible:outline-hidden focus-visible:ring-2 focus-visible:ring-purple-500 disabled:pointer-events-none disabled:opacity-50 active:scale-95 duration-100 cursor-pointer",
          {
            "bg-purple-600 text-white hover:bg-purple-700 shadow-md shadow-purple-600/20":
              variant === "default",
            "bg-red-600 text-white hover:bg-red-700 shadow-md shadow-red-600/20":
              variant === "destructive",
            "border border-slate-700 bg-transparent hover:bg-slate-800 text-slate-200":
              variant === "outline",
            "bg-slate-800 text-slate-100 hover:bg-slate-700":
              variant === "secondary",
            "hover:bg-slate-800 hover:text-slate-200 text-slate-400":
              variant === "ghost",
            "text-purple-400 underline-offset-4 hover:underline":
              variant === "link",
          },
          {
            "h-10 px-4 py-2": size === "default",
            "h-9 rounded-md px-3": size === "sm",
            "h-11 rounded-md px-8": size === "lg",
            "h-10 w-10": size === "icon",
          },
          className
        )}
        ref={ref}
        {...props}
      />
    );
  }
);
Button.displayName = "Button";

export { Button };
