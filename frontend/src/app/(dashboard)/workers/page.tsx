"use client";

import * as React from "react";
import { useQuery } from "@tanstack/react-query";
import { useLiveEvent } from "@/services/monitoring/live-events";
import { WorkerMonitoringService, WorkerStatus } from "@/services/monitoring/workers";
import { SchedulerMonitoringService, SchedulerMetrics } from "@/services/monitoring/scheduler";
import { QueueMonitoringService, QueueMetrics } from "@/services/monitoring/queues";
import { KafkaMonitoringService, KafkaMetrics } from "@/services/monitoring/kafka";
import { DatabaseMonitoringService, DatabaseMetrics } from "@/services/monitoring/database";
import { AlertService, SystemAlert } from "@/services/monitoring/alerts";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { Button } from "@/components/ui/Button";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/Card";
import {
  Activity,
  Cpu,
  Layers,
  HardDrive,
  Users,
  ShieldCheck,
  AlertTriangle,
  Play,
  Terminal,
  Bell,
  RefreshCw,
  Search,
  CheckCircle,
  Radio,
  Clock,
  Database,
  ArrowRight,
  Sparkles,
} from "lucide-react";
import { cn } from "@/lib/utils";

export default function WorkersPage() {
  const { status, subscribe } = useLiveEvent();

  // 1. Initial queries
  const { data: initialWorkers = [] } = useQuery({
    queryKey: ["monitoring-workers"],
    queryFn: WorkerMonitoringService.getWorkers,
  });

  const { data: initialScheduler } = useQuery({
    queryKey: ["monitoring-scheduler"],
    queryFn: SchedulerMonitoringService.getMetrics,
  });

  const { data: initialQueues } = useQuery({
    queryKey: ["monitoring-queues"],
    queryFn: QueueMonitoringService.getMetrics,
  });

  const { data: initialKafka } = useQuery({
    queryKey: ["monitoring-kafka"],
    queryFn: KafkaMonitoringService.getMetrics,
  });

  const { data: initialDatabase } = useQuery({
    queryKey: ["monitoring-database"],
    queryFn: DatabaseMonitoringService.getMetrics,
  });

  const { data: initialAlerts = [] } = useQuery({
    queryKey: ["monitoring-alerts"],
    queryFn: AlertService.getAlerts,
  });

  // 2. Real-time local state synced via SSE listeners
  const [workers, setWorkers] = React.useState<WorkerStatus[]>([]);
  const [scheduler, setScheduler] = React.useState<SchedulerMetrics | null>(null);
  const [queues, setQueues] = React.useState<QueueMetrics | null>(null);
  const [kafka, setKafka] = React.useState<KafkaMetrics | null>(null);
  const [database, setDatabase] = React.useState<DatabaseMetrics | null>(null);
  const [alerts, setAlerts] = React.useState<SystemAlert[]>([]);
  const [isAlertDrawerOpen, setIsAlertDrawerOpen] = React.useState(false);
  const [alertsFilter, setAlertsFilter] = React.useState<"all" | "critical" | "warning">("all");

  // Live feed states
  const [feed, setFeed] = React.useState<Array<{ id: string; event: string; timestamp: string; severity: string; project: string; workflow: string }>>([
    { id: "feed-1", event: "Worker k8s-worker-node-1 joined cluster", timestamp: new Date(Date.now() - 30000).toLocaleTimeString(), severity: "INFO", project: "System", workflow: "Infrastructure" },
    { id: "feed-2", event: "Lease recovered for workflow Run publicId", timestamp: new Date(Date.now() - 15000).toLocaleTimeString(), severity: "WARN", project: "Main Workspace", workflow: "Ingestion Payload" }
  ]);
  const [feedSearch, setFeedSearch] = React.useState("");
  const [feedSeverity, setFeedSeverity] = React.useState("");

  // Sync initial query values to local states
  React.useEffect(() => {
    if (initialWorkers.length > 0) setWorkers(initialWorkers);
  }, [initialWorkers]);

  React.useEffect(() => {
    if (initialScheduler) setScheduler(initialScheduler);
  }, [initialScheduler]);

  React.useEffect(() => {
    if (initialQueues) setQueues(initialQueues);
  }, [initialQueues]);

  React.useEffect(() => {
    if (initialKafka) setKafka(initialKafka);
  }, [initialKafka]);

  React.useEffect(() => {
    if (initialDatabase) setDatabase(initialDatabase);
  }, [initialDatabase]);

  React.useEffect(() => {
    if (initialAlerts.length > 0) setAlerts(initialAlerts);
  }, [initialAlerts]);

  // 3. Register SSE subscriptions
  React.useEffect(() => {
    const unsubWorkers = subscribe("WORKER_UPDATE", (e) => {
      setWorkers((prev) => {
        const index = prev.findIndex((w) => w.publicId === e.data.publicId);
        if (index !== -1) {
          const next = [...prev];
          next[index] = { ...next[index], ...e.data };
          return next;
        }
        return [...prev, e.data];
      });
    });

    const unsubQueues = subscribe("QUEUE_UPDATE", (e) => {
      setQueues(e.data);
    });

    const unsubDatabase = subscribe("DATABASE_UPDATE", (e) => {
      setDatabase(e.data);
    });

    const unsubKafka = subscribe("KAFKA_UPDATE", (e) => {
      setKafka(e.data);
    });

    const unsubFeed = subscribe("EXECUTION_EVENT", (e) => {
      setFeed((prev) => [e.data, ...prev].slice(0, 100)); // Cap live logs window size
    });

    return () => {
      unsubWorkers();
      unsubQueues();
      unsubDatabase();
      unsubKafka();
      unsubFeed();
    };
  }, [subscribe]);

  const activeAlertsCount = alerts.filter((a) => !a.acknowledged).length;

  const handleAcknowledgeAlert = (id: string) => {
    setAlerts((prev) =>
      prev.map((a) => (a.id === id ? { ...a, acknowledged: true } : a))
    );
  };

  const handleDismissAlert = (id: string) => {
    setAlerts((prev) => prev.filter((a) => a.id !== id));
  };

  const filteredFeed = React.useMemo(() => {
    let list = feed;
    if (feedSearch.trim()) {
      const q = feedSearch.toLowerCase();
      list = list.filter((f) => f.event.toLowerCase().includes(q) || f.workflow.toLowerCase().includes(q));
    }
    if (feedSeverity) {
      list = list.filter((f) => f.severity === feedSeverity);
    }
    return list;
  }, [feed, feedSearch, feedSeverity]);

  const filteredAlerts = React.useMemo(() => {
    let list = alerts;
    if (alertsFilter !== "all") {
      list = list.filter((a) => a.severity === alertsFilter);
    }
    return list;
  }, [alerts, alertsFilter]);

  return (
    <div className="space-y-6 flex flex-col h-[calc(100vh-7rem)] overflow-hidden">
      {/* 1. Header Row */}
      <div className="flex items-center justify-between shrink-0">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-100 flex items-center gap-2">
            Operations Center
            <span className="flex h-2.5 w-2.5 items-center justify-center rounded-full bg-slate-900 border border-slate-800">
              <span className={cn("animate-ping absolute inline-flex h-2 w-2 rounded-full opacity-75", {
                "bg-green-400": status === "connected",
                "bg-amber-400": status === "connecting",
                "bg-red-400": status === "disconnected",
              })} />
              <span className={cn("relative inline-flex rounded-full h-1.5 w-1.5", {
                "bg-green-500": status === "connected",
                "bg-amber-500": status === "connecting",
                "bg-red-500": status === "disconnected",
              })} />
            </span>
          </h1>
          <p className="text-sm text-slate-400">
            Real-time cluster telemetry, database transaction states, and task queue analytics.
          </p>
        </div>

        <div className="flex items-center space-x-3">
          <div className="flex items-center space-x-1.5 bg-slate-900 border border-slate-800 p-2 rounded-lg text-xs">
            <Radio className={cn("h-3.5 w-3.5", {
              "text-green-500": status === "connected",
              "text-amber-500 animate-pulse": status === "connecting",
              "text-red-500": status === "disconnected",
            })} />
            <span className="font-semibold text-slate-350 capitalize">SSE: {status}</span>
          </div>

          <Button
            onClick={() => setIsAlertDrawerOpen(true)}
            variant="outline"
            className="h-9 relative border-slate-800 hover:bg-slate-900 text-slate-200 cursor-pointer"
          >
            <Bell className="h-4 w-4" />
            {activeAlertsCount > 0 && (
              <span className="absolute -top-1 -right-1 flex h-4 w-4 items-center justify-center rounded-full bg-red-600 text-[9px] font-bold text-white">
                {activeAlertsCount}
              </span>
            )}
          </Button>
        </div>
      </div>

      {/* 2. Operations Telemetry Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 shrink-0">
        {/* Workers Panel */}
        <Card className="bg-slate-950 border-slate-900 shadow-md">
          <CardHeader className="py-3 flex flex-row items-center justify-between space-y-0">
            <CardTitle className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Worker Fleet</CardTitle>
            <Users className="h-4 w-4 text-purple-500" />
          </CardHeader>
          <CardContent className="py-2">
            <div className="flex justify-between items-baseline">
              <span className="text-2xl font-bold text-slate-100">
                {workers.filter((w) => w.status === "ACTIVE").length} / {workers.length}
              </span>
              <span className="text-[10px] text-slate-500 font-medium">Nodes Active</span>
            </div>
            <div className="mt-2 space-y-1">
              {workers.slice(0, 2).map((w) => (
                <div key={w.publicId} className="flex justify-between items-center text-[10px]">
                  <span className="font-mono text-slate-400 truncate max-w-[120px]">{w.workerName}</span>
                  <StatusBadge status={w.status} className="text-[8px] px-1 py-0" />
                </div>
              ))}
            </div>
          </CardContent>
        </Card>

        {/* Scheduler Panel */}
        <Card className="bg-slate-950 border-slate-900 shadow-md">
          <CardHeader className="py-3 flex flex-row items-center justify-between space-y-0">
            <CardTitle className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Scheduler Engine</CardTitle>
            <Clock className="h-4 w-4 text-blue-500" />
          </CardHeader>
          <CardContent className="py-2">
            <div className="flex justify-between items-baseline">
              <span className="text-2xl font-bold text-slate-100">
                {scheduler ? `${scheduler.schedulingLatencyMs}ms` : "--"}
              </span>
              <span className="text-[10px] text-slate-500 font-medium">Cycle Delay</span>
            </div>
            <div className="mt-2 flex justify-between items-center text-[10px] text-slate-500">
              <span>Next Run: <strong className="text-slate-400">{scheduler ? scheduler.nextSchedulingCycle.slice(11, 19) : "--"}</strong></span>
              <span>Jobs: <strong className="text-slate-400">{scheduler ? scheduler.dueJobsCount : 0}</strong></span>
            </div>
          </CardContent>
        </Card>

        {/* Queue Panel */}
        <Card className="bg-slate-950 border-slate-900 shadow-md">
          <CardHeader className="py-3 flex flex-row items-center justify-between space-y-0">
            <CardTitle className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Queue Depths</CardTitle>
            <Layers className="h-4 w-4 text-emerald-500" />
          </CardHeader>
          <CardContent className="py-2">
            <div className="flex justify-between items-baseline">
              <span className="text-2xl font-bold text-slate-100">
                {queues ? queues.pendingCount + queues.runningCount : 0}
              </span>
              <span className="text-[10px] text-slate-500 font-medium">Total Runs</span>
            </div>
            <div className="mt-2 grid grid-cols-3 gap-1 text-[9px] text-center font-mono">
              <div className="bg-slate-900/40 p-1 rounded-sm border border-slate-900">
                <span className="text-slate-500 block">PENDING</span>
                <span className="text-slate-200 font-bold">{queues ? queues.pendingCount : 0}</span>
              </div>
              <div className="bg-slate-900/40 p-1 rounded-sm border border-slate-900">
                <span className="text-slate-500 block">RETRY</span>
                <span className="text-slate-200 font-bold">{queues ? queues.retryCount : 0}</span>
              </div>
              <div className="bg-slate-900/40 p-1 rounded-sm border border-slate-900">
                <span className="text-red-500 block font-bold">DLQ</span>
                <span className="text-red-400 font-bold">{queues ? queues.dlqCount : 0}</span>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Database & Kafka Panel */}
        <Card className="bg-slate-950 border-slate-900 shadow-md">
          <CardHeader className="py-3 flex flex-row items-center justify-between space-y-0">
            <CardTitle className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Message Hub & DB</CardTitle>
            <Database className="h-4 w-4 text-amber-500" />
          </CardHeader>
          <CardContent className="py-2">
            <div className="space-y-2 text-xs">
              <div className="flex justify-between items-center">
                <span className="text-slate-500 font-medium">PostgreSQL:</span>
                <span className="font-mono text-green-400 font-semibold">{database ? `${database.poolUsage}% Pool` : "HEALTHY"}</span>
              </div>
              <div className="flex justify-between items-center border-t border-slate-900 pt-1.5">
                <span className="text-slate-500 font-medium">Kafka Broker:</span>
                <span className="font-mono text-green-400 font-semibold">{kafka ? `${kafka.consumerLag} Lag` : "HEALTHY"}</span>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* 3. Main Workspace Grid - Workers Table & Live execution feed */}
      <div className="flex-1 flex min-h-0 gap-6">
        {/* Left pane: Worker Fleet Details */}
        <div className="flex-1 flex flex-col min-h-0 bg-slate-950 border border-slate-900 rounded-lg p-4">
          <h2 className="text-sm font-bold text-slate-200 mb-3 uppercase tracking-wider">Active Worker Fleet Node Allocations</h2>
          <div className="flex-1 overflow-y-auto space-y-3 pr-1">
            {workers.map((w) => (
              <div key={w.publicId} className="p-4 bg-slate-900/10 border border-slate-900 rounded-lg flex flex-col space-y-3">
                <div className="flex justify-between items-center">
                  <div>
                    <h3 className="text-xs font-bold text-slate-200 font-mono">{w.workerName}</h3>
                    <span className="text-[9px] text-slate-500 block">ID: {w.publicId}</span>
                  </div>
                  <div className="flex items-center space-x-2">
                    <StatusBadge status={w.status} className="text-[10px]" />
                    <span className="text-[9px] text-slate-500">HB: {w.lastHeartbeat.slice(11, 19)}</span>
                  </div>
                </div>

                {w.status !== "OFFLINE" && (
                  <div className="grid grid-cols-3 gap-4 text-xs">
                    <div>
                      <span className="text-slate-500 block text-[9px] uppercase font-semibold">Active Leases</span>
                      <strong className="text-slate-350">{w.activeLeases} runs</strong>
                    </div>
                    <div>
                      <span className="text-slate-500 block text-[9px] uppercase font-semibold">CPU Allocation</span>
                      <div className="flex items-center space-x-1.5 mt-0.5">
                        <div className="flex-1 bg-slate-900 h-1.5 rounded-full overflow-hidden border border-slate-800">
                          <div className="bg-purple-600 h-full" style={{ width: `${w.cpuUsage || 0}%` }} />
                        </div>
                        <span className="font-mono text-[9px] text-slate-400">{w.cpuUsage || 0}%</span>
                      </div>
                    </div>
                    <div>
                      <span className="text-slate-500 block text-[9px] uppercase font-semibold">Memory Load</span>
                      <div className="flex items-center space-x-1.5 mt-0.5">
                        <div className="flex-1 bg-slate-900 h-1.5 rounded-full overflow-hidden border border-slate-800">
                          <div className="bg-blue-600 h-full" style={{ width: `${w.memoryUsage || 0}%` }} />
                        </div>
                        <span className="font-mono text-[9px] text-slate-400">{w.memoryUsage || 0}%</span>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            ))}
          </div>
        </div>

        {/* Right pane: Live execution events feed */}
        <div className="w-[420px] shrink-0 flex flex-col min-h-0 bg-slate-950 border border-slate-900 rounded-lg p-4">
          {/* Header row: title + severity */}
          <div className="flex items-center justify-between mb-2">
            <h2 className="text-sm font-bold text-slate-200 uppercase tracking-wider flex items-center gap-1.5">
              <Terminal className="h-4 w-4 text-purple-400" />
              Live Activity Stream
            </h2>
            <select
              value={feedSeverity}
              onChange={(e) => setFeedSeverity(e.target.value)}
              className="bg-slate-900 border border-slate-800 rounded text-[10px] text-slate-300 font-semibold px-2 py-1 cursor-pointer focus:outline-none focus:ring-1 focus:ring-purple-500"
            >
              <option value="">All severities</option>
              <option value="INFO">INFO</option>
              <option value="WARN">WARN</option>
              <option value="ERROR">ERROR</option>
            </select>
          </div>

          {/* Search row */}
          <div className="relative mb-3">
            <Search className="absolute left-2.5 top-2.5 h-3.5 w-3.5 text-slate-500 pointer-events-none" />
            <input
              type="text"
              placeholder="Filter live event entries..."
              value={feedSearch}
              onChange={(e) => setFeedSearch(e.target.value)}
              className="h-8 w-full rounded-md border border-slate-800 bg-slate-900/60 pl-8 pr-3 text-xs text-slate-300 placeholder:text-slate-600 focus:outline-none focus:ring-1 focus:ring-purple-500"
            />
          </div>

          <div className="flex-1 overflow-y-auto font-mono text-[10px] bg-slate-950 border border-slate-900 rounded-md p-3 space-y-2.5 leading-relaxed">
            {filteredFeed.length === 0 ? (
              <p className="text-slate-500 text-center py-16">No events matching search filter criteria.</p>
            ) : (
              filteredFeed.map((item) => (
                <div key={item.id} className="flex items-start space-x-2">
                  <span className="text-slate-650 shrink-0 select-none">[{item.timestamp}]</span>
                  <span className={cn("font-bold shrink-0 select-none", {
                    "text-blue-400": item.severity === "INFO",
                    "text-amber-400": item.severity === "WARN",
                    "text-red-400": item.severity === "ERROR",
                  })}>[{item.severity}]</span>
                  <span className="text-slate-350">{item.event}</span>
                </div>
              ))
            )}
          </div>
        </div>
      </div>

      {/* 4. Sliding Operations Alerts Drawer */}
      {isAlertDrawerOpen && (
        <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex justify-end">
          {/* Overlay click to close */}
          <div className="flex-1" onClick={() => setIsAlertDrawerOpen(false)} />
          
          <div className="w-[360px] bg-slate-950 border-l border-slate-900 h-full flex flex-col p-4 shadow-xl">
            <div className="flex justify-between items-center pb-3 border-b border-slate-900 mb-4 shrink-0">
              <h2 className="text-sm font-bold text-slate-100 flex items-center gap-1.5">
                <AlertTriangle className="h-4 w-4 text-red-500" />
                Active Alerts Panel
              </h2>
              <Button
                onClick={() => setIsAlertDrawerOpen(false)}
                variant="ghost"
                className="h-8 text-xs text-slate-400 hover:text-slate-200 cursor-pointer"
              >
                Close
              </Button>
            </div>

            {/* Filter buttons */}
            <div className="flex space-x-1 p-1 bg-slate-900 border border-slate-950 rounded-md text-xs mb-4 shrink-0">
              {(["all", "critical", "warning"] as const).map((mode) => (
                <button
                  key={mode}
                  onClick={() => setAlertsFilter(mode)}
                  className={cn(
                    "flex-1 py-1 text-center font-semibold rounded-sm capitalize transition-colors cursor-pointer",
                    alertsFilter === mode
                      ? "bg-slate-800 text-slate-100"
                      : "text-slate-500 hover:text-slate-305"
                  )}
                >
                  {mode}
                </button>
              ))}
            </div>

            <div className="flex-1 overflow-y-auto space-y-3 min-h-0">
              {filteredAlerts.length === 0 ? (
                <p className="text-slate-500 text-center py-16">No active system alerts recorded.</p>
              ) : (
                filteredAlerts.map((alt) => (
                  <div
                    key={alt.id}
                    className={cn(
                      "p-3 rounded-lg border flex flex-col space-y-2",
                      alt.acknowledged
                        ? "bg-slate-900/10 border-slate-900 text-slate-450"
                        : alt.severity === "critical"
                        ? "bg-red-950/10 border-red-900/30 text-red-300"
                        : "bg-amber-950/10 border-amber-900/30 text-amber-300"
                    )}
                  >
                    <div className="flex justify-between items-start">
                      <span className="text-xs font-bold">{alt.title}</span>
                      <span className="text-[8px] text-slate-500">{new Date(alt.timestamp).toLocaleTimeString()}</span>
                    </div>
                    <p className="text-[10px] text-slate-400">{alt.description}</p>
                    
                    <div className="flex justify-end space-x-2 pt-1 border-t border-slate-900/40">
                      {!alt.acknowledged && (
                        <Button
                          onClick={() => handleAcknowledgeAlert(alt.id)}
                          size="sm"
                          className="h-6 text-[9px] bg-slate-900 border border-slate-850 text-slate-300 hover:bg-slate-800"
                        >
                          Ack
                        </Button>
                      )}
                      <Button
                        onClick={() => handleDismissAlert(alt.id)}
                        size="sm"
                        className="h-6 text-[9px] bg-slate-900/40 border border-slate-900 text-slate-450 hover:bg-red-950/20 hover:text-red-400"
                      >
                        Dismiss
                      </Button>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
