import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Documentation",
  description: "FlowForge documentation: getting started, architecture, API reference, workflows, monitoring, analytics, and administration guides.",
};

export default function DocsLayout({ children }: { children: React.ReactNode }) {
  return children;
}
