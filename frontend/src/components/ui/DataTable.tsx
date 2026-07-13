import * as React from "react";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "./Table";
import { Spinner } from "./Spinner";
import { EmptyState } from "./EmptyState";

export interface ColumnDef<T> {
  header: string;
  accessorKey?: keyof T;
  cell?: (item: T) => React.ReactNode;
}

export interface DataTableProps<T> {
  columns: ColumnDef<T>[];
  data: T[];
  isLoading?: boolean;
  emptyTitle?: string;
  emptyDescription?: string;
}

export function DataTable<T>({
  columns,
  data,
  isLoading,
  emptyTitle = "No data found",
  emptyDescription = "There are no records to display at this time.",
}: DataTableProps<T>) {
  if (isLoading) {
    return (
      <div className="flex h-48 w-full items-center justify-center rounded-lg border border-slate-800 bg-slate-900/10">
        <Spinner />
      </div>
    );
  }

  if (data.length === 0) {
    return (
      <EmptyState
        title={emptyTitle}
        description={emptyDescription}
        className="py-12"
      />
    );
  }

  return (
    <Table>
      <TableHeader>
        <TableRow>
          {columns.map((col, idx) => (
            <TableHead key={idx}>{col.header}</TableHead>
          ))}
        </TableRow>
      </TableHeader>
      <TableBody>
        {data.map((row, rowIdx) => (
          <TableRow key={rowIdx}>
            {columns.map((col, colIdx) => (
              <TableCell key={colIdx}>
                {col.cell
                  ? col.cell(row)
                  : col.accessorKey
                  ? String(row[col.accessorKey] ?? "")
                  : null}
              </TableCell>
            ))}
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}
