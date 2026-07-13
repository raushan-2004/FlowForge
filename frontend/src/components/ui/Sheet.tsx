import * as React from "react";
import { AnimatePresence, motion } from "framer-motion";
import { cn } from "@/lib/utils";
import { X } from "lucide-react";

interface SheetProps {
  isOpen: boolean;
  onClose: () => void;
  side?: "left" | "right";
  children: React.ReactNode;
}

export function Sheet({ isOpen, onClose, side = "right", children }: SheetProps) {
  React.useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = "hidden";
    } else {
      document.body.style.overflow = "unset";
    }
    return () => {
      document.body.style.overflow = "unset";
    };
  }, [isOpen]);

  const slideVariants = {
    left: {
      initial: { x: "-100%" },
      animate: { x: 0 },
      exit: { x: "-100%" },
    },
    right: {
      initial: { x: "100%" },
      animate: { x: 0 },
      exit: { x: "100%" },
    },
  };

  return (
    <AnimatePresence>
      {isOpen && (
        <div className="fixed inset-0 z-50 flex">
          {/* Backdrop */}
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onClose}
            className="absolute inset-0 bg-black/60 backdrop-blur-xs"
          />

          {/* Sheet Drawer Content */}
          <motion.div
            variants={slideVariants[side]}
            initial="initial"
            animate="animate"
            exit="exit"
            transition={{ type: "spring", damping: 25, stiffness: 200 }}
            className={cn(
              "relative z-10 h-full w-full max-w-md bg-slate-900 border-slate-800 p-6 shadow-xl text-slate-100 flex flex-col justify-between",
              {
                "mr-auto border-r": side === "left",
                "ml-auto border-l": side === "right",
              }
            )}
          >
            {/* Close Trigger */}
            <button
              onClick={onClose}
              className="absolute right-4 top-4 rounded-sm opacity-70 hover:opacity-100 transition-opacity focus:outline-hidden focus:ring-2 focus:ring-purple-500 text-slate-400 hover:text-slate-100 cursor-pointer"
              aria-label="Close sheet"
            >
              <X className="h-4 w-4" />
            </button>

            <div className="flex-1 overflow-y-auto pr-2 mt-4">{children}</div>
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  );
}

export function SheetHeader({ className, ...props }: React.HTMLAttributes<HTMLDivElement>) {
  return <div className={cn("flex flex-col space-y-2 text-left mb-6", className)} {...props} />;
}

export function SheetTitle({ className, ...props }: React.HTMLAttributes<HTMLHeadingElement>) {
  return <h2 className={cn("text-lg font-semibold text-slate-100", className)} {...props} />;
}

export function SheetDescription({ className, ...props }: React.HTMLAttributes<HTMLParagraphElement>) {
  return <p className={cn("text-sm text-slate-400", className)} {...props} />;
}
