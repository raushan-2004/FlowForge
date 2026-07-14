"use client";

import * as React from "react";
import dynamic from "next/dynamic";
import { Spinner } from "./Spinner";

// Lazy-load Monaco Editor wrapper to support performance constraints
const Editor = dynamic(() => import("@monaco-editor/react"), {
  ssr: false,
  loading: () => (
    <div className="flex h-48 w-full items-center justify-center rounded-md border border-slate-800 bg-slate-950">
      <Spinner size="sm" />
    </div>
  ),
});

interface MonacoEditorProps {
  value: string;
  onChange: (value: string) => void;
  language?: string;
  className?: string;
  height?: string;
}

export function MonacoEditor({
  value,
  onChange,
  language = "json",
  className,
  height = "200px",
}: MonacoEditorProps) {
  return (
    <div className={className}>
      <Editor
        height={height}
        language={language}
        theme="vs-dark"
        value={value}
        onChange={(val) => onChange(val || "")}
        options={{
          minimap: { enabled: false },
          scrollbar: { vertical: "auto", horizontal: "auto" },
          fontSize: 12,
          lineNumbers: "on",
          roundedSelection: false,
          scrollBeyondLastLine: false,
          readOnly: false,
          automaticLayout: true,
        }}
      />
    </div>
  );
}
export default MonacoEditor;
