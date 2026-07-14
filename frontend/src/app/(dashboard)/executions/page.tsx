"use client";

import * as React from "react";
import { useQuery } from "@tanstack/react-query";
import { ExecutionService, ExecutionResponse, ExecutionAttemptResponse } from "@/services/executions";
import { TimelineService, TimelineEvent } from "@/services/timeline";
import { LogService, LogLine } from "@/services/logs";
import { WorkflowVisualizationService } from "@/services/visualization";
import { ProjectService } from "@/services/projects";
import { EntityTable } from "@/components/crud/EntityTable";
import { EntityToolbar } from "@/components/crud/EntityToolbar";
import { Pagination } from "@/components/crud/Pagination";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { Button } from "@/components/ui/Button";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/Card";
import { MonacoEditor } from "@/components/ui/MonacoEditor";
import {
  ReactFlow,
  Background,
  Node,
  Edge as FlowEdge,
  Handle,
  Position,
} from "@xyflow/react";
import "@xyflow/react/dist/style.css";
import {
  Activity,
  ArrowRight,
  Clock,
  Cpu,
  Globe,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  Play,
  Copy,
  Download,
  Terminal,
  Columns,
  RefreshCw,
  Search,
  BookOpen,
} from "lucide-react";
import { useAuth } from "@/providers/AuthProvider";
import { cn } from "@/lib/utils";

// Minimal Read-only Custom Nodes for Path Highlighting
function ReadOnlyStartNode() {
  return (
    <div className="px-3 py-2 rounded-md border border-slate-800 bg-slate-900 text-xs font-semibold text-slate-300 flex items-center space-x-2">
      <span className="h-2 w-2 rounded-full bg-purple-500" />
      <span>START</span>
      <Handle type="source" position={Position.Right} style={{ opacity: 0 }} />
    </div>
  );
}

function ReadOnlyJobNode({ data }: { data: any }) {
  const statusColors: Record<string, string> = {
    succeeded: "border-green-500/50 bg-green-950/10 text-green-300",
    failed: "border-red-500/50 bg-red-950/10 text-red-300",
    retried: "border-orange-500/50 bg-orange-950/10 text-orange-300",
    unexecuted: "border-slate-800 bg-slate-900/60 text-slate-500",
  };

  const status = data.status || "unexecuted";

  return (
    <div className={cn("px-3 py-2 rounded-md border text-xs font-semibold flex flex-col w-40 min-h-[50px] justify-center shadow-md", statusColors[status])}>
      <Handle type="target" position={Position.Left} style={{ opacity: 0 }} />
      <span className="truncate">{data.name || "HTTP Dispatch"}</span>
      <span className="text-[9px] text-slate-500 uppercase tracking-wide mt-0.5">{status}</span>
      <Handle type="source" position={Position.Right} style={{ opacity: 0 }} />
    </div>
  );
}

function ReadOnlyEndNode() {
  return (
    <div className="px-3 py-2 rounded-md border border-slate-800 bg-slate-900 text-xs font-semibold text-slate-300 flex items-center space-x-2">
      <span className="h-2 w-2 rounded-full bg-slate-500" />
      <span>END</span>
      <Handle type="target" position={Position.Left} style={{ opacity: 0 }} />
    </div>
  );
}

const nodeTypes = {
  START: ReadOnlyStartNode,
  JOB: ReadOnlyJobNode,
  END: ReadOnlyEndNode,
};

export default function ExecutionExplorerPage() {
  const { currentTenant } = useAuth();
  const [selectedProjectId, setSelectedProjectId] = React.useState("");
  
  // Active search query
  const [search, setSearch] = React.useState("");
  const [selectedStatus, setSelectedStatus] = React.useState("");
  const [selectedTrigger, setSelectedTrigger] = React.useState("");
  const [currentPage, setCurrentPage] = React.useState(1);

  // Inspector State
  const [selectedExecutionId, setSelectedExecutionId] = React.useState<string | null>(null);
  const [activeTab, setActiveTab] = React.useState<"timeline" | "retry" | "http" | "flow" | "logs">("timeline");
  const [httpSubTab, setHttpSubTab] = React.useState<"request" | "response">("request");

  // Filter query details
  const [logsSearch, setLogsSearch] = React.useState("");
  const [logsLevel, setLogsLevel] = React.useState("");

  // Fetch Projects context
  const { data: projects = [] } = useQuery({
    queryKey: ["projects", currentTenant?.id],
    queryFn: ProjectService.listProjects,
  });

  // Auto-select project context
  React.useEffect(() => {
    if (projects.length > 0 && !selectedProjectId) {
      setSelectedProjectId(projects[0].publicId);
    }
  }, [projects, selectedProjectId]);

  // Fetch Executions
  const { data: executions = [], isLoading, isError, refetch } = useQuery({
    queryKey: ["executions", selectedProjectId],
    queryFn: () => ExecutionService.listExecutions(selectedProjectId || undefined),
    enabled: !!selectedProjectId,
  });

  const selectedExecution = executions.find((e) => e.publicId === selectedExecutionId);

  // Fetch Attempts (History)
  const { data: attempts = [] } = useQuery({
    queryKey: ["execution-attempts", selectedExecutionId],
    queryFn: () => ExecutionService.getAttempts(selectedExecutionId!),
    enabled: !!selectedExecutionId,
  });

  const activeAttempt = attempts.length > 0 ? attempts[attempts.length - 1] : null;

  // Fetch Timeline Events
  const { data: timeline = [] } = useQuery({
    queryKey: ["execution-timeline", selectedExecutionId],
    queryFn: () => TimelineService.getTimeline(selectedExecutionId!),
    enabled: !!selectedExecutionId,
  });

  // Fetch Logs
  const { data: logs = [] } = useQuery({
    queryKey: ["execution-logs", selectedExecutionId],
    queryFn: () => LogService.getLogs(selectedExecutionId!),
    enabled: !!selectedExecutionId,
  });

  // Fetch Path Highlighting Config
  const { data: highlightedPath } = useQuery({
    queryKey: ["execution-path", selectedExecutionId],
    queryFn: () => WorkflowVisualizationService.getHighlightedPath(selectedExecutionId!),
    enabled: !!selectedExecutionId,
  });

  const handleSelectExecution = (execId: string) => {
    setSelectedExecutionId(execId);
    setActiveTab("timeline");
  };

  // Local filtering & pagination of execution list
  const filteredExecutions = React.useMemo(() => {
    let list = executions;
    if (search.trim()) {
      const q = search.toLowerCase();
      list = list.filter((e) => e.publicId.toLowerCase().includes(q) || (e.workerName && e.workerName.toLowerCase().includes(q)));
    }
    if (selectedStatus) {
      list = list.filter((e) => e.status === selectedStatus);
    }
    if (selectedTrigger) {
      list = list.filter((e) => e.triggerType === selectedTrigger);
    }
    return list;
  }, [executions, search, selectedStatus, selectedTrigger]);

  const totalPages = Math.ceil(filteredExecutions.length / 10) || 1;
  const paginatedExecutions = React.useMemo(() => {
    const start = (currentPage - 1) * 10;
    return filteredExecutions.slice(start, start + 10);
  }, [filteredExecutions, currentPage]);

  // Log filter
  const filteredLogs = React.useMemo(() => {
    let list = logs;
    if (logsSearch.trim()) {
      const q = logsSearch.toLowerCase();
      list = list.filter((l) => l.message.toLowerCase().includes(q) || l.component.toLowerCase().includes(q));
    }
    if (logsLevel) {
      list = list.filter((l) => l.level === logsLevel);
    }
    return list;
  }, [logs, logsSearch, logsLevel]);

  // React Flow highlighting mapping
  const flowElements = React.useMemo(() => {
    if (!highlightedPath) return { nodes: [], edges: [] };

    const nodesStatus = highlightedPath.nodesStatus;
    const edgesStatus = highlightedPath.edgesStatus;

    const baseNodes: Node[] = [
      { id: "node-start", type: "START", position: { x: 50, y: 100 }, data: {} },
      {
        id: "node-job-1",
        type: "JOB",
        position: { x: 200, y: 100 },
        data: { name: "Ingestion Payload Request", status: nodesStatus["node-job-1"] || "unexecuted" },
      },
      { id: "node-end", type: "END", position: { x: 420, y: 100 }, data: {} },
    ];

    const baseEdges: FlowEdge[] = [
      {
        id: "reactflow__edge-node-start-node-job-1",
        source: "node-start",
        target: "node-job-1",
        animated: edgesStatus["reactflow__edge-node-start-node-job-1"] === "executed",
        style: {
          stroke: edgesStatus["reactflow__edge-node-start-node-job-1"] === "executed" ? "#10b981" : "#475569",
          strokeWidth: 2,
        },
      },
      {
        id: "reactflow__edge-node-job-1-node-end",
        source: "node-job-1",
        target: "node-end",
        animated: edgesStatus["reactflow__edge-node-job-1-node-end"] === "executed",
        style: {
          stroke: edgesStatus["reactflow__edge-node-job-1-node-end"] === "executed" ? "#10b981" : "#475569",
          strokeWidth: 2,
        },
      },
    ];

    return { nodes: baseNodes, edges: baseEdges };
  }, [highlightedPath]);

  // Logs actions
  const handleCopyLogs = () => {
    const formatted = filteredLogs.map((l) => `[${l.time}] [${l.level}] [${l.component}] - ${l.message}`).join("\n");
    navigator.clipboard.writeText(formatted);
    alert("Logs copied to clipboard.");
  };

  const handleDownloadLogs = () => {
    const formatted = filteredLogs.map((l) => `[${l.time}] [${l.level}] [${l.component}] - ${l.message}`).join("\n");
    const blob = new Blob([formatted], { type: "text/plain" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `execution-${selectedExecutionId}-logs.txt`;
    link.click();
    URL.revokeObjectURL(url);
  };

  const getTimelineIcon = (icon: string) => {
    switch (icon) {
      case "Clock":
        return <Clock className="h-4 w-4 text-purple-400" />;
      case "Users":
        return <Cpu className="h-4 w-4 text-blue-400" />;
      case "Play":
        return <Play className="h-4 w-4 text-purple-400" />;
      case "Globe":
        return <Globe className="h-4 w-4 text-blue-400" />;
      default:
        return <CheckCircle2 className="h-4 w-4 text-green-400" />;
    }
  };

  const columns = [
    {
      key: "publicId",
      header: "Execution ID",
      render: (item: ExecutionResponse) => (
        <button
          onClick={() => handleSelectExecution(item.publicId)}
          className={cn(
            "text-xs font-mono font-bold hover:underline text-left truncate max-w-[120px] block cursor-pointer",
            selectedExecutionId === item.publicId ? "text-purple-400" : "text-slate-300"
          )}
        >
          {item.publicId}
        </button>
      ),
    },
    {
      key: "status",
      header: "Status",
      render: (item: ExecutionResponse) => <StatusBadge status={item.status} />,
    },
    { key: "triggerType", header: "Trigger" },
    { key: "retryCount", header: "Retries", className: "text-center w-16" },
    { key: "durationMs", header: "Duration", render: (item: ExecutionResponse) => <span>{item.durationMs ? `${item.durationMs}ms` : "--"}</span> },
  ];

  return (
    <div className="space-y-6 flex flex-col h-[calc(100vh-7rem)] overflow-hidden">
      {/* 1. Header Toolbar details */}
      <div className="flex items-center justify-between shrink-0">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-100 font-sans">Execution Explorer</h1>
          <p className="text-sm text-slate-400">Investigate scheduler histories, node runs, and payload structures.</p>
        </div>

        {/* Project context filter */}
        <div className="flex items-center space-x-2 text-xs bg-slate-900 border border-slate-800 p-2.5 rounded-lg text-slate-400">
          <span className="font-semibold text-slate-300">Project Context:</span>
          <select
            value={selectedProjectId}
            onChange={(e) => setSelectedProjectId(e.target.value)}
            className="bg-transparent border-0 text-slate-100 font-bold focus:ring-0 outline-hidden cursor-pointer"
          >
            {projects.map((p) => (
              <option key={p.publicId} value={p.publicId} className="bg-slate-950">
                {p.name}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* 2. Split Pane Grid Panel */}
      <div className="flex-1 flex min-h-0 gap-6">
        {/* Left Side: Execution List */}
        <div className="flex-1 flex flex-col min-h-0 bg-slate-950 border border-slate-900 rounded-lg p-3">
          <EntityToolbar
            searchPlaceholder="Search by ID or Node..."
            searchValue={search}
            onSearchChange={setSearch}
            filters={[
              {
                key: "status",
                label: "Status",
                options: [
                  { value: "COMPLETED", label: "Completed" },
                  { value: "FAILED", label: "Failed" },
                  { value: "RUNNING", label: "Running" },
                ],
              },
              {
                key: "trigger",
                label: "Trigger",
                options: [
                  { value: "CRON", label: "Cron" },
                  { value: "MANUAL", label: "Manual" },
                ],
              },
            ]}
            selectedFilters={{ status: selectedStatus, trigger: selectedTrigger }}
            onFilterChange={(key, val) => {
              if (key === "status") setSelectedStatus(val);
              if (key === "trigger") setSelectedTrigger(val);
            }}
          />

          <div className="flex-1 overflow-y-auto min-h-0">
            <EntityTable
              columns={columns}
              data={paginatedExecutions}
              idKey="publicId"
              isLoading={isLoading}
              emptyTitle="No executions recorded"
              emptyDescription="No automated trigger runs have executed in this Project workspace context."
              mobileCardRender={(item) => (
                <div className="space-y-1.5 text-xs">
                  <div className="flex justify-between items-center">
                    <button onClick={() => handleSelectExecution(item.publicId)} className="font-mono font-bold text-slate-200">
                      {item.publicId.slice(0, 8)}...
                    </button>
                    <StatusBadge status={item.status} />
                  </div>
                  <div className="flex justify-between items-center text-slate-500">
                    <span>Trigger: {item.triggerType}</span>
                    <span>Dur: {item.durationMs}ms</span>
                  </div>
                </div>
              )}
            />
          </div>

          <div className="shrink-0 border-t border-slate-900">
            <Pagination
              currentPage={currentPage}
              totalPages={totalPages}
              onPageChange={setCurrentPage}
            />
          </div>
        </div>

        {/* Right Side: Detailed Properties Inspector */}
        <div className="w-[480px] shrink-0 flex flex-col min-h-0 bg-slate-950 border border-slate-900 rounded-lg overflow-hidden">
          {selectedExecution ? (
            <div className="flex flex-col h-full min-h-0">
              {/* Card Header stats */}
              <div className="p-4 border-b border-slate-900 bg-slate-900/10">
                <div className="flex justify-between items-start mb-2">
                  <span className="text-[10px] uppercase font-bold text-slate-500 tracking-wider">Inspect Run</span>
                  <StatusBadge status={selectedExecution.status} />
                </div>
                <h3 className="text-xs font-mono font-bold text-slate-200 select-all">{selectedExecution.publicId}</h3>
                <div className="flex items-center space-x-4 mt-2 text-[10px] text-slate-500">
                  <span>Trigger: <strong className="text-slate-400">{selectedExecution.triggerType}</strong></span>
                  <span>Worker: <strong className="text-slate-400">{selectedExecution.workerName || "Unassigned"}</strong></span>
                </div>
              </div>

              {/* Tabs list */}
              <div className="flex bg-slate-950 p-1 border-b border-slate-900 text-xs shrink-0 select-none">
                {(["timeline", "retry", "http", "flow", "logs"] as const).map((tab) => (
                  <button
                    key={tab}
                    onClick={() => setActiveTab(tab)}
                    className={cn(
                      "flex-1 py-2 text-center font-semibold rounded-md transition-colors capitalize cursor-pointer",
                      activeTab === tab
                        ? "bg-slate-900 text-slate-100"
                        : "text-slate-500 hover:text-slate-350"
                    )}
                  >
                    {tab}
                  </button>
                ))}
              </div>

              {/* Tab views panel */}
              <div className="flex-1 overflow-y-auto p-4 min-h-0">
                {/* 1. Timeline Tab */}
                {activeTab === "timeline" && (
                  <div className="relative border-l border-slate-900 pl-5 ml-2.5 space-y-5">
                    {timeline.map((ev) => (
                      <div key={ev.id} className="relative">
                        <span className="absolute -left-[31px] top-0.5 flex h-5 w-5 items-center justify-center rounded-full bg-slate-950 border border-slate-900 shrink-0">
                          {getTimelineIcon(ev.iconName)}
                        </span>
                        <div>
                          <div className="flex justify-between items-center text-[10px] text-slate-500">
                            <span>{ev.timestamp}</span>
                            {ev.durationMs && <span>{ev.durationMs}ms</span>}
                          </div>
                          <p className="text-xs font-bold text-slate-200">{ev.name}</p>
                          {ev.details && <p className="text-[10px] text-slate-500 mt-0.5">{ev.details}</p>}
                        </div>
                      </div>
                    ))}
                  </div>
                )}

                {/* 2. Retry Visualization Tab */}
                {activeTab === "retry" && (
                  <div className="space-y-4">
                    <h3 className="text-xs font-semibold text-slate-400 uppercase tracking-wider pb-2 border-b border-slate-900">
                      Execution Attempts Flow
                    </h3>
                    {attempts.length === 0 ? (
                      <p className="text-xs text-slate-500 text-center py-8">No retry attempts registered.</p>
                    ) : (
                      <div className="space-y-3">
                        {attempts.map((att, idx) => (
                          <div key={att.publicId} className="flex items-start space-x-3 p-3 bg-slate-900/30 border border-slate-900 rounded-lg">
                            <div className="h-6 w-6 rounded-full bg-slate-800 flex items-center justify-center text-xs font-bold text-slate-400">
                              {att.attemptNumber}
                            </div>
                            <div className="flex-1 space-y-1">
                              <div className="flex justify-between items-center text-[10px] text-slate-500">
                                <span>Start: {att.startedAt.slice(11, 19)}</span>
                                <StatusBadge status={att.status} className="text-[9px] px-1 py-0" />
                              </div>
                              <p className="text-xs font-bold text-slate-200">Attempt #{att.attemptNumber}</p>
                              {att.durationMs && (
                                <p className="text-[10px] text-slate-500">
                                  Resolved in <strong className="text-slate-400">{att.durationMs}ms</strong>
                                </p>
                              )}
                              {att.networkError && (
                                <div className="p-2 bg-red-950/20 border border-red-900/30 rounded-md text-[10px] text-red-400 mt-1 font-mono">
                                  Error: {att.networkError}
                                </div>
                              )}
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                )}

                {/* 3. HTTP Viewer Tab */}
                {activeTab === "http" && activeAttempt && (
                  <div className="space-y-4">
                    <div className="flex space-x-2 bg-slate-900/40 p-0.5 rounded-md border border-slate-900">
                      {(["request", "response"] as const).map((sub) => (
                        <button
                          key={sub}
                          onClick={() => setHttpSubTab(sub)}
                          className={cn(
                            "flex-1 py-1.5 text-center text-xs font-semibold rounded-sm transition-colors capitalize cursor-pointer",
                            httpSubTab === sub
                              ? "bg-slate-800 text-slate-100"
                              : "text-slate-500 hover:text-slate-350"
                          )}
                        >
                          {sub}
                        </button>
                      ))}
                    </div>

                    {httpSubTab === "request" ? (
                      <div className="space-y-3">
                        <div>
                          <label className="text-[10px] text-slate-500 uppercase font-bold">Headers</label>
                          <pre className="p-3 bg-slate-950 border border-slate-900 rounded-md text-xs text-slate-300 font-mono mt-1 whitespace-pre-wrap max-h-32 overflow-y-auto">
                            {JSON.stringify(activeAttempt.requestHeaders, null, 2)}
                          </pre>
                        </div>
                        <div>
                          <label className="text-[10px] text-slate-500 uppercase font-bold">JSON Payload Body</label>
                          <MonacoEditor
                            value={activeAttempt.requestBody || "{}"}
                            onChange={() => {}}
                            className="border border-slate-900 rounded-md overflow-hidden mt-1"
                            height="150px"
                          />
                        </div>
                      </div>
                    ) : (
                      <div className="space-y-3">
                        <div className="flex justify-between items-center">
                          <label className="text-[10px] text-slate-500 uppercase font-bold">Status Code</label>
                          <span className="text-xs bg-slate-900 border border-slate-800 text-green-400 font-mono px-2 py-0.5 rounded-sm">
                            {activeAttempt.httpStatus || 200}
                          </span>
                        </div>
                        <div>
                          <label className="text-[10px] text-slate-500 uppercase font-bold">Headers</label>
                          <pre className="p-3 bg-slate-950 border border-slate-900 rounded-md text-xs text-slate-300 font-mono mt-1 whitespace-pre-wrap max-h-32 overflow-y-auto">
                            {JSON.stringify(activeAttempt.responseHeaders, null, 2)}
                          </pre>
                        </div>
                        <div>
                          <label className="text-[10px] text-slate-500 uppercase font-bold">Response Body</label>
                          <MonacoEditor
                            value={activeAttempt.responseBody || "{\n  \"success\": true\n}"}
                            onChange={() => {}}
                            className="border border-slate-900 rounded-md overflow-hidden mt-1"
                            height="150px"
                          />
                        </div>
                      </div>
                    )}
                  </div>
                )}

                {/* 4. Workflow Path Highlighting Tab */}
                {activeTab === "flow" && (
                  <div className="h-64 w-full border border-slate-900 rounded-lg overflow-hidden bg-slate-950 relative">
                    <ReactFlow
                      nodes={flowElements.nodes}
                      edges={flowElements.edges}
                      nodeTypes={nodeTypes}
                      fitView
                      nodesDraggable={false}
                      nodesConnectable={false}
                      elementsSelectable={false}
                    >
                      <Background color="#1e293b" gap={12} size={1} />
                    </ReactFlow>
                  </div>
                )}

                {/* 5. Logs Tab */}
                {activeTab === "logs" && (
                  <div className="space-y-3">
                    <div className="flex items-center space-x-2 shrink-0">
                      <div className="relative flex-1">
                        <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-slate-500" />
                        <input
                          type="text"
                          placeholder="Search message components..."
                          value={logsSearch}
                          onChange={(e) => setLogsSearch(e.target.value)}
                          className="h-9 w-full rounded-md border border-slate-800 bg-slate-950 pl-8 pr-3 text-xs text-slate-300 focus:ring-1 focus:ring-purple-500"
                        />
                      </div>
                      <select
                        value={logsLevel}
                        onChange={(e) => setLogsLevel(e.target.value)}
                        className="h-9 rounded-md border border-slate-800 bg-slate-950 px-2 text-xs text-slate-400 cursor-pointer"
                      >
                        <option value="">All levels</option>
                        <option value="INFO">INFO</option>
                        <option value="WARN">WARN</option>
                        <option value="ERROR">ERROR</option>
                      </select>
                    </div>

                    <div className="flex space-x-2 py-1 justify-end">
                      <Button onClick={handleCopyLogs} size="sm" variant="ghost" className="h-8 text-[10px] gap-1 hover:bg-slate-900 text-slate-400">
                        <Copy className="h-3 w-3" />
                        Copy
                      </Button>
                      <Button onClick={handleDownloadLogs} size="sm" variant="ghost" className="h-8 text-[10px] gap-1 hover:bg-slate-900 text-slate-400">
                        <Download className="h-3 w-3" />
                        Download
                      </Button>
                    </div>

                    <div className="bg-slate-950 border border-slate-900 rounded-md p-3 font-mono text-[10px] text-slate-400 space-y-2 h-64 overflow-y-auto">
                      {filteredLogs.length === 0 ? (
                        <p className="text-slate-500 text-center py-12">No logs match search parameters.</p>
                      ) : (
                        filteredLogs.map((l) => (
                          <div key={l.id} className="flex items-start space-x-2 leading-relaxed">
                            <span className="text-slate-600 shrink-0 select-none">[{l.time}]</span>
                            <span className={cn("font-bold shrink-0 select-none", {
                              "text-blue-400": l.level === "INFO",
                              "text-orange-400": l.level === "WARN",
                              "text-red-400": l.level === "ERROR",
                            })}>[{l.level}]</span>
                            <span className="text-purple-400 shrink-0 select-none">[{l.component}]</span>
                            <span className="text-slate-300 whitespace-pre-wrap">{l.message}</span>
                          </div>
                        ))
                      )}
                    </div>
                  </div>
                )}
              </div>
            </div>
          ) : (
            <div className="flex-1 flex flex-col items-center justify-center p-8 text-slate-500 text-center space-y-3">
              <BookOpen className="h-10 w-10 text-slate-800 animate-pulse" />
              <div>
                <h3 className="font-bold text-slate-400 text-sm">No Run Selected</h3>
                <p className="text-xs text-slate-500 mt-1 max-w-[240px]">
                  Select an Execution ID from the list to view vertical timelines, retries, and http headers.
                </p>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
