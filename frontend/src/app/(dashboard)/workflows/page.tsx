"use client";

import * as React from "react";
import {
  ReactFlow,
  MiniMap,
  Controls,
  Background,
  useNodesState,
  useEdgesState,
  addEdge,
  Connection,
  Edge,
  Node,
  Panel,
} from "@xyflow/react";
import "@xyflow/react/dist/style.css";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { WorkflowService } from "@/services/workflows";
import { ProjectService } from "@/services/projects";
import { JobService, JobResponse } from "@/services/jobs";
import { StartNode } from "@/components/workflow/StartNode";
import { JobNode } from "@/components/workflow/JobNode";
import { EndNode } from "@/components/workflow/EndNode";
import { Button } from "@/components/ui/Button";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/Card";
import { Dialog, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from "@/components/ui/Dialog";
import {
  Save,
  Play,
  LayoutGrid,
  RotateCcw,
  RotateCw,
  Maximize2,
  Minimize2,
  HelpCircle,
  AlertTriangle,
  CheckCircle,
  Plus,
  Cpu,
  Trash2,
  Keyboard,
  Info,
} from "lucide-react";
import dagre from "dagre";
import { useAuth } from "@/providers/AuthProvider";

// Register custom node types in React Flow
const nodeTypes = {
  START: StartNode,
  JOB: JobNode,
  END: EndNode,
};

// Dagre Layout config helper
const dagreGraph = new dagre.graphlib.Graph();
dagreGraph.setDefaultEdgeLabel(() => ({}));

const getLayoutedElements = (nodes: Node[], edges: Edge[]) => {
  dagreGraph.setGraph({ rankdir: "LR" }); // Left to Right flow is highly premium

  nodes.forEach((node) => {
    dagreGraph.setNode(node.id, { width: 220, height: 75 });
  });

  edges.forEach((edge) => {
    dagreGraph.setEdge(edge.source, edge.target);
  });

  dagre.layout(dagreGraph);

  const layoutedNodes = nodes.map((node) => {
    const nodeWithPosition = dagreGraph.node(node.id);
    return {
      ...node,
      position: {
        x: nodeWithPosition.x - 220 / 2,
        y: nodeWithPosition.y - 75 / 2,
      },
    };
  });

  return { nodes: layoutedNodes, edges };
};

export default function WorkflowBuilderPage() {
  const queryClient = useQueryClient();
  const { currentTenant } = useAuth();

  // Selected Active Project Context
  const [selectedProjectId, setSelectedProjectId] = React.useState("");

  // React Flow state hooks
  const [nodes, setNodes, onNodesChange] = useNodesState<Node>([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState<Edge>([]);

  // Selection states
  const [selectedNodeId, setSelectedNodeId] = React.useState<string | null>(null);
  const [validationErrors, setValidationErrors] = React.useState<string[]>([]);
  const [saveStatus, setSaveStatus] = React.useState<"idle" | "saving" | "saved" | "error">("idle");

  // History state for Undo/Redo
  const [history, setHistory] = React.useState<{ nodes: Node[]; edges: Edge[] }[]>([]);
  const [historyIndex, setHistoryIndex] = React.useState(-1);

  // Dialog overlays
  const [isShortcutOpen, setIsShortcutOpen] = React.useState(false);
  const [isMiniMapOpen, setIsMiniMapOpen] = React.useState(true);
  const [isMobile, setIsMobile] = React.useState(false);

  // Fetch Projects list
  const { data: projects = [] } = useQuery({
    queryKey: ["projects", currentTenant?.id],
    queryFn: ProjectService.listProjects,
  });

  // Auto-select first project context
  React.useEffect(() => {
    if (projects.length > 0 && !selectedProjectId) {
      setSelectedProjectId(projects[0].publicId);
    }
  }, [projects, selectedProjectId]);

  // Fetch Jobs list to populate Job Picker
  const { data: jobs = [] } = useQuery({
    queryKey: ["jobs", selectedProjectId],
    queryFn: () => JobService.listJobs(selectedProjectId || undefined),
    enabled: !!selectedProjectId,
  });

  // Handle responsive mobile sizing checks
  React.useEffect(() => {
    if (typeof window !== "undefined") {
      const checkMobile = () => setIsMobile(window.innerWidth < 768);
      checkMobile();
      window.addEventListener("resize", checkMobile);
      return () => window.removeEventListener("resize", checkMobile);
    }
  }, []);

  // Initialize graph with one START and one END node by default
  React.useEffect(() => {
    if (nodes.length === 0) {
      const initialNodes: Node[] = [
        {
          id: "node-start",
          type: "START",
          position: { x: 50, y: 150 },
          data: {},
        },
        {
          id: "node-end",
          type: "END",
          position: { x: 450, y: 150 },
          data: {},
        },
      ];
      setNodes(initialNodes);
      setEdges([]);
      setHistory([{ nodes: initialNodes, edges: [] }]);
      setHistoryIndex(0);
    }
  }, [nodes.length, setNodes, setEdges]);

  // Track state changes to push history points
  const recordHistoryState = React.useCallback(
    (newNodes: Node[], newEdges: Edge[]) => {
      const nextHistory = history.slice(0, historyIndex + 1);
      nextHistory.push({ nodes: JSON.parse(JSON.stringify(newNodes)), edges: JSON.parse(JSON.stringify(newEdges)) });
      setHistory(nextHistory);
      setHistoryIndex(nextHistory.length - 1);
    },
    [history, historyIndex]
  );

  // Connection Handler with Edge validation
  const onConnect = React.useCallback(
    (params: Connection) => {
      // 1. Prevent self-loops
      if (params.source === params.target) {
        alert("Edge validation error: Cannot connect a node to itself (Self-loops forbidden).");
        return;
      }

      // 2. Prevent outgoing edges from END
      if (params.source === "node-end" || params.source?.startsWith("node-end")) {
        alert("Edge validation error: Outgoing connections from END nodes are forbidden.");
        return;
      }

      // 3. Prevent multiple incoming edges to START
      if (params.target === "node-start" || params.target?.startsWith("node-start")) {
        alert("Edge validation error: Incoming connections to START nodes are forbidden.");
        return;
      }

      const newEdges = addEdge(params, edges);
      setEdges(newEdges);
      recordHistoryState(nodes, newEdges);
    },
    [edges, nodes, setEdges, recordHistoryState]
  );

  // Auto Layout calculation handler
  const handleAutoLayout = React.useCallback(() => {
    const { nodes: layoutedNodes, edges: layoutedEdges } = getLayoutedElements(nodes, edges);
    setNodes(layoutedNodes);
    setEdges(layoutedEdges);
    recordHistoryState(layoutedNodes, layoutedEdges);
  }, [nodes, edges, setNodes, setEdges, recordHistoryState]);

  // Undo / Redo controllers
  const handleUndo = () => {
    if (historyIndex > 0) {
      const targetIndex = historyIndex - 1;
      setHistoryIndex(targetIndex);
      setNodes(history[targetIndex].nodes);
      setEdges(history[targetIndex].edges);
    }
  };

  const handleRedo = () => {
    if (historyIndex < history.length - 1) {
      const targetIndex = historyIndex + 1;
      setHistoryIndex(targetIndex);
      setNodes(history[targetIndex].nodes);
      setEdges(history[targetIndex].edges);
    }
  };

  // Keyboard shortcut listeners (Ctrl+Z, Ctrl+Shift+Z, Delete)
  React.useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.ctrlKey && e.shiftKey && e.key.toLowerCase() === "z") {
        e.preventDefault();
        handleRedo();
      } else if (e.ctrlKey && e.key.toLowerCase() === "z") {
        e.preventDefault();
        handleUndo();
      }
    };
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [historyIndex, history]);

  // Graph Validators (Exactly 1 Start, At least 1 End, No cycles, No disconnected/isolated nodes)
  const validateGraph = React.useCallback(() => {
    const errorsList: string[] = [];

    // 1. START validation
    const startNodes = nodes.filter((n) => n.type === "START");
    if (startNodes.length !== 1) {
      errorsList.push(`Graph must have exactly 1 START node. Found: ${startNodes.length}`);
    }

    // 2. END validation
    const endNodes = nodes.filter((n) => n.type === "END");
    if (endNodes.length < 1) {
      errorsList.push("Graph must have at least 1 END node.");
    }

    // 3. JOB configuration checks
    const jobNodes = nodes.filter((n) => n.type === "JOB");
    jobNodes.forEach((n) => {
      if (!n.data.jobPublicId) {
        errorsList.push(`JOB node [${n.id}] lacks a selected Job configuration.`);
      }
    });

    if (startNodes.length === 1) {
      const startNodeId = startNodes[0].id;

      // Build Adjacency matrix for cycle detection and connectivity checks
      const adjList: Record<string, string[]> = {};
      nodes.forEach((n) => {
        adjList[n.id] = [];
      });

      edges.forEach((e) => {
        if (adjList[e.source]) {
          adjList[e.source].push(e.target);
        }
      });

      // 4. Cycle detection using DFS
      const visited = new Set<string>();
      const recStack = new Set<string>();
      let hasCycle = false;

      const checkCycle = (nodeId: string): boolean => {
        if (recStack.has(nodeId)) return true;
        if (visited.has(nodeId)) return false;

        visited.add(nodeId);
        recStack.add(nodeId);

        for (const neighbor of adjList[nodeId] || []) {
          if (checkCycle(neighbor)) return true;
        }

        recStack.delete(nodeId);
        return false;
      };

      for (const node of nodes) {
        if (checkCycle(node.id)) {
          hasCycle = true;
          break;
        }
      }

      if (hasCycle) {
        errorsList.push("Cycle detected! Workflow execution graphs must be a Directed Acyclic Graph (DAG).");
      }

      // 5. Connectivity checks (unreachable or disconnected nodes)
      const reachable = new Set<string>();
      const dfsReachable = (nodeId: string) => {
        if (!reachable.has(nodeId)) {
          reachable.add(nodeId);
          for (const neighbor of adjList[nodeId] || []) {
            dfsReachable(neighbor);
          }
        }
      };

      dfsReachable(startNodeId);

      nodes.forEach((n) => {
        if (!reachable.has(n.id)) {
          errorsList.push(`Unreachable node detected: [${n.id}] cannot be accessed from START.`);
        }
      });
    }

    setValidationErrors(errorsList);
    return errorsList.length === 0;
  }, [nodes, edges]);

  // Handle Drag & Drop Node Creation
  const handleAddNode = (type: "START" | "JOB" | "END") => {
    // START limits
    if (type === "START" && nodes.some((n) => n.type === "START")) {
      alert("Validation: A workflow definition cannot contain more than 1 START trigger node.");
      return;
    }

    const newId = `node-${type.toLowerCase()}-${Date.now().toString().slice(-4)}`;
    const newNode: Node = {
      id: newId,
      type,
      position: { x: 250, y: 150 },
      data: { name: `New ${type}`, jobPublicId: "" },
    };

    const newNodes = [...nodes, newNode];
    setNodes(newNodes);
    recordHistoryState(newNodes, edges);
  };

  // Delete node handler
  const handleDeleteNode = (id: string) => {
    if (id === "node-start" || id === "node-end") {
      alert("System constraint: START and END boundary nodes cannot be deleted.");
      return;
    }
    const newNodes = nodes.filter((n) => n.id !== id);
    const newEdges = edges.filter((e) => e.source !== id && e.target !== id);
    setNodes(newNodes);
    setEdges(newEdges);
    setSelectedNodeId(null);
    recordHistoryState(newNodes, newEdges);
  };

  // Inspector property updater
  const handleAssignJob = (jobPublicId: string) => {
    if (!selectedNodeId) return;
    const targetJob = jobs.find((j) => j.publicId === jobPublicId);
    if (!targetJob) return;

    const newNodes = nodes.map((n) => {
      if (n.id === selectedNodeId) {
        return {
          ...n,
          data: {
            ...n.data,
            jobPublicId,
            name: targetJob.name,
            httpMethod: targetJob.httpMethod,
            targetUrl: targetJob.targetUrl,
          },
        };
      }
      return n;
    });

    setNodes(newNodes);
    recordHistoryState(newNodes, edges);
  };

  // Save Pipeline definition to Backend APIs
  const saveMutation = useMutation({
    mutationFn: (definitionJson: string) =>
      WorkflowService.saveWorkflow(selectedProjectId, {
        name: "DAG Pipeline Ingestion",
        definitionJson,
      }),
    onSuccess: () => {
      setSaveStatus("saved");
      setTimeout(() => setSaveStatus("idle"), 2000);
    },
    onError: () => {
      setSaveStatus("error");
      setTimeout(() => setSaveStatus("idle"), 3000);
    },
  });

  const handleSaveWorkflow = () => {
    setSaveStatus("saving");
    const isValid = validateGraph();
    if (!isValid) {
      setSaveStatus("error");
      alert("Cannot save: Graph validation failed. Please address errors listed in validation panel.");
      return;
    }

    // Map React Flow nodes and edges to standard DagValidator format
    const nodesPayload = nodes.map((n) => ({
      id: n.id,
      type: n.type || "JOB",
      jobPublicId: n.data.jobPublicId || null,
    }));

    const edgesPayload = edges.map((e) => ({
      from: e.source,
      to: e.target,
    }));

    const definitionJson = JSON.stringify({
      nodes: nodesPayload,
      edges: edgesPayload,
    });

    saveMutation.mutate(definitionJson);
  };

  // Renders simple warning on smaller screens (mobile responsive fallback)
  if (isMobile) {
    return (
      <div className="flex h-screen w-full flex-col items-center justify-center bg-slate-950 p-6 text-center text-slate-100">
        <AlertTriangle className="h-12 w-12 text-orange-400 mb-4 animate-bounce" />
        <h2 className="text-lg font-bold">Visual Canvas Suspended</h2>
        <p className="text-xs text-slate-400 max-w-xs mt-2 leading-relaxed">
          The Visual Workflow Builder requires a larger viewport for graph connections. Please access on a desktop browser.
        </p>
      </div>
    );
  }

  const selectedNode = nodes.find((n) => n.id === selectedNodeId);

  return (
    <div className="flex flex-col h-[calc(100vh-7rem)] overflow-hidden space-y-4">
      {/* 1. Header toolbar actions */}
      <div className="flex items-center justify-between bg-slate-900/10 border border-slate-900 px-4 py-3 rounded-lg">
        <div className="flex items-center space-x-3">
          <h1 className="text-lg font-bold text-slate-100">DAG Builder</h1>
          <span className="text-slate-800">|</span>
          <select
            value={selectedProjectId}
            onChange={(e) => setSelectedProjectId(e.target.value)}
            className="bg-transparent border-0 text-xs font-bold text-slate-400 focus:ring-0 outline-hidden cursor-pointer"
          >
            {projects.map((p) => (
              <option key={p.publicId} value={p.publicId} className="bg-slate-950">
                {p.name}
              </option>
            ))}
          </select>
        </div>

        <div className="flex items-center space-x-2">
          <Button onClick={handleUndo} disabled={historyIndex <= 0} variant="outline" size="sm" className="h-8 border-slate-800">
            <RotateCcw className="h-3.5 w-3.5" />
          </Button>
          <Button onClick={handleRedo} disabled={historyIndex >= history.length - 1} variant="outline" size="sm" className="h-8 border-slate-800">
            <RotateCw className="h-3.5 w-3.5" />
          </Button>
          <Button onClick={handleAutoLayout} variant="outline" size="sm" className="h-8 border-slate-800 gap-1 text-xs">
            <LayoutGrid className="h-3.5 w-3.5" />
            Auto Arrange
          </Button>
          <Button onClick={validateGraph} variant="outline" size="sm" className="h-8 border-slate-800 gap-1 text-xs">
            Validate Graph
          </Button>
          <Button onClick={() => setIsMiniMapOpen((prev) => !prev)} variant="outline" size="sm" className="h-8 border-slate-800 text-xs">
            {isMiniMapOpen ? "Hide MiniMap" : "Show MiniMap"}
          </Button>
          <Button onClick={() => setIsShortcutOpen(true)} variant="outline" size="sm" className="h-8 border-slate-800">
            <Keyboard className="h-4 w-4" />
          </Button>
          <Button onClick={handleSaveWorkflow} size="sm" className="h-8 gap-1 text-xs font-semibold shadow-purple-600/10">
            <Save className="h-3.5 w-3.5" />
            {saveStatus === "saving" ? "Saving..." : saveStatus === "saved" ? "Saved!" : "Save DAG"}
          </Button>
        </div>
      </div>

      {/* 2. Visual Builder Split Canvas Grid */}
      <div className="flex-1 flex min-h-0 gap-4">
        {/* Node Palette Sidebar */}
        <div className="w-56 shrink-0 bg-slate-950 border border-slate-900 rounded-lg p-3.5 space-y-4 flex flex-col justify-between">
          <div className="space-y-4">
            <div>
              <h3 className="text-xs font-bold text-slate-400 uppercase tracking-wider mb-2">Palette</h3>
              <p className="text-[10px] text-slate-500 leading-normal mb-4">Click to place node handles onto workspace canvas.</p>
            </div>
            <div className="space-y-2">
              <button
                onClick={() => handleAddNode("START")}
                className="flex items-center space-x-2.5 w-full p-2.5 rounded-md border border-purple-500/20 bg-purple-950/10 text-left hover:bg-purple-950/20 cursor-pointer"
              >
                <span className="h-6 w-6 rounded-full bg-purple-500/20 border border-purple-500 flex items-center justify-center text-[10px] font-bold text-purple-400">▶</span>
                <div>
                  <p className="text-xs font-bold text-slate-200">Start Trigger</p>
                  <p className="text-[9px] text-slate-500">Pipeline entrypoint</p>
                </div>
              </button>

              <button
                onClick={() => handleAddNode("JOB")}
                className="flex items-center space-x-2.5 w-full p-2.5 rounded-md border border-slate-800 bg-slate-900/30 text-left hover:bg-slate-900/50 cursor-pointer"
              >
                <Cpu className="h-5 w-5 text-purple-400 shrink-0" />
                <div>
                  <p className="text-xs font-bold text-slate-200">HTTP Dispatch</p>
                  <p className="text-[9px] text-slate-500">HTTP request payload</p>
                </div>
              </button>

              <button
                onClick={() => handleAddNode("END")}
                className="flex items-center space-x-2.5 w-full p-2.5 rounded-md border border-red-500/20 bg-red-950/10 text-left hover:bg-red-950/20 cursor-pointer"
              >
                <span className="h-6 w-6 rounded-full bg-red-500/20 border border-red-500 flex items-center justify-center text-[10px] font-bold text-red-400">■</span>
                <div>
                  <p className="text-xs font-bold text-slate-200">Finalization</p>
                  <p className="text-[9px] text-slate-500">Pipeline completion</p>
                </div>
              </button>
            </div>
          </div>

          <div className="p-3 bg-slate-900/30 border border-slate-900 rounded-md text-[10px] text-slate-500 space-y-1">
            <span className="font-bold text-slate-400 block uppercase">Draft Node Slots</span>
            <p>Condition, Wait, and Script nodes are draft placeholders.</p>
          </div>
        </div>

        {/* React Flow Workspace Canvas */}
        <div className="flex-1 bg-slate-950 border border-slate-900 rounded-lg overflow-hidden relative">
          <ReactFlow
            nodes={nodes}
            edges={edges}
            onNodesChange={onNodesChange}
            onEdgesChange={onEdgesChange}
            onConnect={onConnect}
            nodeTypes={nodeTypes}
            onNodeClick={(_, node) => setSelectedNodeId(node.id)}
            onPaneClick={() => setSelectedNodeId(null)}
            fitView
            snapToGrid
          >
            <Background color="#334155" gap={16} size={1} />
            <Controls className="bg-slate-900 border border-slate-800 text-slate-400 fill-current" />
            {isMiniMapOpen && (
              <MiniMap
                position="bottom-right"
                className="!bg-slate-900 border border-slate-800 rounded-lg shadow-lg overflow-hidden"
                maskColor="rgba(2, 6, 23, 0.75)"
                nodeColor="#334155"
                style={{ backgroundColor: "#0f172a" }}
              />
            )}

            {/* Validation Panel overlay */}
            {validationErrors.length > 0 && (
              <Panel position="top-left" className="bg-red-950/90 border border-red-900/60 p-3 rounded-lg shadow-xl max-w-sm">
                <div className="flex items-center space-x-1.5 text-red-400 font-bold text-xs mb-1.5">
                  <AlertTriangle className="h-4 w-4" />
                  <span>Validation Warning</span>
                </div>
                <ul className="list-disc pl-4 text-[10px] text-red-300 space-y-1">
                  {validationErrors.map((err, idx) => (
                    <li key={idx}>{err}</li>
                  ))}
                </ul>
              </Panel>
            )}
          </ReactFlow>
        </div>

        {/* Node Property Inspector Panel */}
        <div className="w-64 shrink-0 bg-slate-950 border border-slate-900 rounded-lg p-4 space-y-4">
          <div className="border-b border-slate-900 pb-2 flex justify-between items-center">
            <h3 className="text-xs font-bold text-slate-400 uppercase tracking-wider">Properties Inspector</h3>
            {selectedNode && selectedNode.id !== "node-start" && selectedNode.id !== "node-end" && (
              <Button
                variant="ghost"
                size="icon"
                onClick={() => handleDeleteNode(selectedNode.id)}
                className="h-8 w-8 text-slate-400 hover:text-red-400"
              >
                <Trash2 className="h-4 w-4" />
              </Button>
            )}
          </div>

          {selectedNode ? (
            <div className="space-y-4">
              <div className="space-y-1">
                <span className="text-[10px] text-slate-500 uppercase font-bold">Node ID</span>
                <p className="text-xs font-mono text-slate-300">{selectedNode.id}</p>
              </div>

              <div className="space-y-1">
                <span className="text-[10px] text-slate-500 uppercase font-bold">Handle Type</span>
                <p className="text-xs font-semibold text-slate-100">{selectedNode.type}</p>
              </div>

              {selectedNode.type === "JOB" ? (
                <div className="space-y-4 pt-2 border-t border-slate-900">
                  <div className="space-y-1.5">
                    <label className="text-[10px] text-slate-500 uppercase font-bold">Map Target Job</label>
                    <select
                      value={(selectedNode.data as any).jobPublicId || ""}
                      onChange={(e) => handleAssignJob(e.target.value)}
                      className="h-9 w-full rounded-md border border-slate-800 bg-slate-950 px-2.5 text-xs text-slate-200 cursor-pointer"
                    >
                      <option value="">Select Job Reference</option>
                      {jobs.map((j) => (
                        <option key={j.publicId} value={j.publicId}>
                          {j.name}
                        </option>
                      ))}
                    </select>
                  </div>

                  {(selectedNode.data as any).jobPublicId && (
                    <div className="p-3 bg-slate-900/30 border border-slate-900 rounded-lg space-y-2 text-xs text-slate-300">
                      <div>
                        <span className="text-slate-500 block text-[9px] uppercase">HTTP Endpoint</span>
                        <div className="flex items-center space-x-1.5 mt-0.5">
                          <span className="text-[9px] bg-slate-950 border border-slate-850 px-1 py-0.5 rounded-sm text-purple-400 font-mono">
                            {(selectedNode.data as any).httpMethod}
                          </span>
                          <span className="truncate font-mono text-slate-300">{(selectedNode.data as any).targetUrl}</span>
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              ) : selectedNode.type === "START" ? (
                <div className="p-3 bg-purple-950/10 border border-purple-900/30 rounded-lg text-xs text-purple-400">
                  <Info className="h-4 w-4 mb-1.5" />
                  <span>START Node registers the primary execution pipeline trigger point. Fields are read-only.</span>
                </div>
              ) : (
                <div className="p-3 bg-slate-900/30 border border-slate-900 rounded-lg text-xs text-slate-400">
                  <Info className="h-4 w-4 mb-1.5" />
                  <span>END Node finalizes the execution stream and stores pipeline logs. Fields are read-only.</span>
                </div>
              )}
            </div>
          ) : (
            <div className="flex flex-col items-center justify-center py-12 text-slate-500 text-center space-y-2">
              <HelpCircle className="h-8 w-8 text-slate-700 animate-pulse" />
              <p className="text-xs">Click any node on canvas to view and configure properties.</p>
            </div>
          )}
        </div>
      </div>

      {/* 3. Bottom Status Bar */}
      <div className="flex items-center justify-between text-[11px] text-slate-500 bg-slate-950 border border-slate-900 px-4 py-2.5 rounded-lg select-none">
        <div className="flex items-center space-x-4">
          <span>Nodes: <strong className="text-slate-400">{nodes.length}</strong></span>
          <span>Edges: <strong className="text-slate-400">{edges.length}</strong></span>
        </div>
        <div className="flex items-center space-x-2">
          {validationErrors.length === 0 ? (
            <span className="flex items-center text-green-500 font-semibold gap-1">
              <CheckCircle className="h-3.5 w-3.5" />
              Graph Valid
            </span>
          ) : (
            <span className="flex items-center text-red-500 font-semibold gap-1">
              <AlertTriangle className="h-3.5 w-3.5" />
              Graph Invalid
            </span>
          )}
        </div>
      </div>

      {/* Keyboard Shortcuts Dialog */}
      <Dialog isOpen={isShortcutOpen} onClose={() => setIsShortcutOpen(false)}>
        <DialogHeader>
          <DialogTitle>Keyboard Shortcut Mappings</DialogTitle>
          <DialogDescription>
            Use canvas keybindings to speed up execution pipelines development.
          </DialogDescription>
        </DialogHeader>
        <div className="py-4 divide-y divide-slate-900 text-xs">
          <div className="flex justify-between py-2.5">
            <span className="text-slate-400 font-semibold">Undo Action</span>
            <kbd className="bg-slate-900 border border-slate-800 px-1.5 py-0.5 rounded-sm font-mono">Ctrl + Z</kbd>
          </div>
          <div className="flex justify-between py-2.5">
            <span className="text-slate-400 font-semibold">Redo Action</span>
            <kbd className="bg-slate-900 border border-slate-800 px-1.5 py-0.5 rounded-sm font-mono">Ctrl + Shift + Z</kbd>
          </div>
          <div className="flex justify-between py-2.5">
            <span className="text-slate-400 font-semibold">Delete Node/Edge</span>
            <kbd className="bg-slate-900 border border-slate-800 px-1.5 py-0.5 rounded-sm font-mono">Backspace / Delete</kbd>
          </div>
          <div className="flex justify-between py-2.5">
            <span className="text-slate-400 font-semibold">Multi-Select nodes</span>
            <kbd className="bg-slate-900 border border-slate-800 px-1.5 py-0.5 rounded-sm font-mono">Shift + Drag</kbd>
          </div>
        </div>
        <DialogFooter>
          <Button onClick={() => setIsShortcutOpen(false)} className="w-full">
            Done
          </Button>
        </DialogFooter>
      </Dialog>
    </div>
  );
}
