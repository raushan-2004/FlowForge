"use client";

import * as React from "react";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "../ui/Button";

interface PaginationProps {
  currentPage: number;
  totalPages: number;
  onPageChange: (page: number) => void;
  siblingCount?: number;
}

export function Pagination({
  currentPage,
  totalPages,
  onPageChange,
  siblingCount = 1,
}: PaginationProps) {
  if (totalPages <= 1) return null;

  const getPageNumbers = () => {
    const pages: (number | string)[] = [];
    const totalNumbers = siblingCount * 2 + 3;
    const totalBlocks = totalNumbers + 2;

    if (totalPages > totalBlocks) {
      const startPage = Math.max(2, currentPage - siblingCount);
      const endPage = Math.min(totalPages - 1, currentPage + siblingCount);

      let pagesToShow: (number | string)[] = [1];

      if (startPage > 2) {
        pagesToShow.push("...");
      }

      for (let i = startPage; i <= endPage; i++) {
        pagesToShow.push(i);
      }

      if (endPage < totalPages - 1) {
        pagesToShow.push("...");
      }

      pagesToShow.push(totalPages);
      return pagesToShow;
    }

    return Array.from({ length: totalPages }, (_, i) => i + 1);
  };

  const pages = getPageNumbers();

  return (
    <div className="flex items-center justify-between px-2 py-4 border-t border-slate-900 mt-4 select-none">
      <div className="text-xs text-slate-500">
        Page <span className="font-semibold text-slate-300">{currentPage}</span> of{" "}
        <span className="font-semibold text-slate-300">{totalPages}</span>
      </div>
      <div className="flex items-center space-x-1.5">
        <Button
          variant="outline"
          size="sm"
          onClick={() => onPageChange(currentPage - 1)}
          disabled={currentPage === 1}
          className="border-slate-800"
        >
          <ChevronLeft className="h-4 w-4" />
        </Button>
        {pages.map((p, idx) => {
          if (p === "...") {
            return (
              <span key={idx} className="px-2 text-slate-600 text-sm">
                ...
              </span>
            );
          }
          const pageNum = p as number;
          return (
            <Button
              key={idx}
              variant={currentPage === pageNum ? "default" : "outline"}
              size="sm"
              onClick={() => onPageChange(pageNum)}
              className={currentPage === pageNum ? "" : "border-slate-800 text-slate-300"}
            >
              {pageNum}
            </Button>
          );
        })}
        <Button
          variant="outline"
          size="sm"
          onClick={() => onPageChange(currentPage + 1)}
          disabled={currentPage === totalPages}
          className="border-slate-800"
        >
          <ChevronRight className="h-4 w-4" />
        </Button>
      </div>
    </div>
  );
}
