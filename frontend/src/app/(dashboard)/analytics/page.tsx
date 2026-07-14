"use client";

import * as React from "react";
import { useQuery } from "@tanstack/react-query";
import dynamic from "next/dynamic";
import {
  BarChart3,
  TrendingUp,
  TrendingDown,
  Activity,
  Users,
  Clock,
  RefreshCw,
  Download,
  Search,
  AlertTriangle,
  CheckCircle2,
  XCircle,
  Zap,
  Server,
  Database,
  Network,
  Eye,
  Filter,
  FileText,
  ChevronRight,
  Info,
  GitBranch,
} from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/Card";
import { Badge } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { Tabs, TabsList, TabsTrigger, TabsContent } from "@/components/ui/Tabs";
import { Skeleton } from "@/components/ui/Skeleton";
import { SearchInput } from "@/components/ui/SearchInput";
import { cn } from "@/lib/utils";
import {
  AnalyticsService,
  TraceService,
  MetricsService,
  ReportService,
  TIME_RANGES,
  type TimeRange,
  type WorkflowStat,
  type WorkerStat,
  type MetricItem,
  type Trace,
  type TraceSpan,
} from "@/services/analytics";

// ─── Lazy Chart Imports ───────────────────────────────────────────────────────

const {
  ResponsiveContainer,
  AreaChart,
  Area,
  BarChart,
  Bar,
  LineChart,
  Line,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
} = require("recharts");

// ─── Colour Tokens ────────────────────────────────────────────────────────────

const C = {
  success: "#10b981",
  failure: "#ef4444",
  retry: "#f59e0b",
  pending: "#6366f1",
  processing: "#3b82f6",
  dead: "#64748b",
  util: "#8b5cf6",
  duration: "#06b6d4",
  total: "#a78bfa",
  avgDelay: "#f97316",
};

// ─── Tooltip style ────────────────────────────────────────────────────────────

const TOOLTIP_STYLE = {
  contentStyle: {
    backgroundColor: "#0f172a",
    border: "1px solid #1e293b",
    borderRadius: 8,
    color: "#e2e8f0",
    fontSize: 12,
  },
  labelStyle: { color: "#94a3b8" },
};

// ─── Time Range Selector ──────────────────────────────────────────────────────

function TimeRangeSelector({
  value,
  onChange,
}: {
  value: TimeRange;
  onChange: (r: TimeRange) => void;
}) {
  return (
    <div className="flex items-center gap-1 bg-slate-950 border border-slate-800 rounded-lg p-1">
      {TIME_RANGES.map((r) => (
        <button
          key={r.value}
          onClick={() => onChange(r.value)}
          className={cn(
            "px-3 py-1.5 rounded-md text-xs font-medium transition-all duration-200",
            value === r.value
              ? "bg-violet-600 text-white shadow-sm shadow-violet-900/40"
              : "text-slate-400 hover:text-slate-200 hover:bg-slate-800"
          )}
        >
          {r.label}
        </button>
      ))}
    </div>
  );
}

// ─── Analytics Card ───────────────────────────────────────────────────────────

function AnalyticsMetricCard({
  title,
  value,
  trend,
  trendLabel,
  isPositiveTrend,
  tooltip,
  icon,
  loading,
}: {
  title: string;
  value: string | number;
  trend: number;
  trendLabel: string;
  isPositiveTrend: boolean;
  tooltip: string;
  icon: React.ReactNode;
  loading?: boolean;
}) {
  const [showTip, setShowTip] = React.useState(false);

  if (loading) {
    return (
      <Card className="border-slate-800 bg-slate-900/40">
        <CardContent className="p-5">
          <Skeleton className="h-4 w-28 mb-3" />
          <Skeleton className="h-8 w-20 mb-2" />
          <Skeleton className="h-3 w-24" />
        </CardContent>
      </Card>
    );
  }

  return (
    <Card className="border-slate-800 bg-slate-900/40 hover:border-slate-700 transition-all duration-200 group">
      <CardContent className="p-5">
        <div className="flex items-start justify-between mb-3">
          <p className="text-xs font-medium text-slate-400 uppercase tracking-wider">{title}</p>
          <div className="flex items-center gap-1">
            <div className="text-slate-500 group-hover:text-violet-400 transition-colors">{icon}</div>
            <div className="relative">
              <button
                onMouseEnter={() => setShowTip(true)}
                onMouseLeave={() => setShowTip(false)}
                className="text-slate-600 hover:text-slate-400 transition-colors"
                aria-label={`Info: ${tooltip}`}
              >
                <Info className="h-3 w-3" />
              </button>
              {showTip && (
                <div className="absolute right-0 top-5 z-50 w-52 rounded-lg border border-slate-700 bg-slate-900 p-2.5 text-xs text-slate-300 shadow-xl">
                  {tooltip}
                </div>
              )}
            </div>
          </div>
        </div>
        <div className="text-2xl font-bold text-slate-100 tracking-tight mb-2">{value}</div>
        <div className="flex items-center gap-1.5">
          {isPositiveTrend ? (
            <TrendingUp className="h-3 w-3 text-emerald-400" />
          ) : (
            <TrendingDown className="h-3 w-3 text-red-400" />
          )}
          <span
            className={cn(
              "text-xs font-semibold",
              isPositiveTrend ? "text-emerald-400" : "text-red-400"
            )}
          >
            {trend > 0 ? "+" : ""}
            {trend}%
          </span>
          <span className="text-xs text-slate-500">{trendLabel}</span>
        </div>
      </CardContent>
    </Card>
  );
}

// ─── Section Heading ──────────────────────────────────────────────────────────

function SectionHeading({
  icon,
  title,
  description,
}: {
  icon: React.ReactNode;
  title: string;
  description?: string;
}) {
  return (
    <div className="flex items-center gap-3 mb-4">
      <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-violet-950/60 border border-violet-800/40 text-violet-400">
        {icon}
      </div>
      <div>
        <h2 className="text-base font-semibold text-slate-100">{title}</h2>
        {description && (
          <p className="text-xs text-slate-500">{description}</p>
        )}
      </div>
    </div>
  );
}

// ─── Execution Heatmap ────────────────────────────────────────────────────────

function ExecutionHeatmap({ range }: { range: TimeRange }) {
  const { data: cells = [], isLoading } = useQuery({
    queryKey: ["analytics-heatmap", range],
    queryFn: () => AnalyticsService.getHeatmap(),
    staleTime: 60000,
  });

  const days = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];
  const hours = Array.from({ length: 24 }, (_, i) => i);
  const maxVal = Math.max(...cells.map((c) => c.value), 1);

  if (isLoading) {
    return <Skeleton className="h-36 w-full" />;
  }

  function getCell(day: string, hour: number) {
    return cells.find((c) => c.day === day && c.hour === hour);
  }

  function getCellColor(val: number) {
    if (val === 0) return "bg-slate-900";
    const pct = val / maxVal;
    if (pct < 0.2) return "bg-violet-950";
    if (pct < 0.4) return "bg-violet-900/80";
    if (pct < 0.6) return "bg-violet-700/80";
    if (pct < 0.8) return "bg-violet-600";
    return "bg-violet-500";
  }

  return (
    <div className="overflow-x-auto">
      <div className="flex flex-col gap-0.5 min-w-[560px]">
        <div className="flex gap-0.5 ml-9">
          {hours.map((h) => (
            <div key={h} className="w-5 text-[9px] text-slate-600 text-center">
              {h % 4 === 0 ? `${h}h` : ""}
            </div>
          ))}
        </div>
        {days.map((day) => (
          <div key={day} className="flex items-center gap-0.5">
            <span className="w-8 text-[10px] text-slate-500 text-right pr-1">{day}</span>
            {hours.map((hour) => {
              const cell = getCell(day, hour);
              return (
                <div
                  key={hour}
                  title={`${day} ${hour}:00 — ${cell?.value ?? 0} executions`}
                  className={cn(
                    "w-5 h-5 rounded-sm transition-all cursor-default",
                    getCellColor(cell?.value ?? 0)
                  )}
                />
              );
            })}
          </div>
        ))}
      </div>
      <div className="flex items-center gap-2 mt-3 ml-9">
        <span className="text-[10px] text-slate-500">Low</span>
        {["bg-slate-900", "bg-violet-950", "bg-violet-900/80", "bg-violet-700/80", "bg-violet-600", "bg-violet-500"].map(
          (c) => (
            <div key={c} className={cn("w-4 h-4 rounded-sm", c)} />
          )
        )}
        <span className="text-[10px] text-slate-500">High</span>
      </div>
    </div>
  );
}

// ─── Workflow Analytics Table ─────────────────────────────────────────────────

function WorkflowAnalyticsTable() {
  const [sortKey, setSortKey] = React.useState<keyof WorkflowStat>("executions");
  const [sortDesc, setSortDesc] = React.useState(true);

  const { data: workflows = [], isLoading } = useQuery({
    queryKey: ["analytics-workflows"],
    queryFn: () => AnalyticsService.getWorkflowStats(),
    staleTime: 30000,
  });

  function toggleSort(key: keyof WorkflowStat) {
    if (sortKey === key) setSortDesc(!sortDesc);
    else { setSortKey(key); setSortDesc(true); }
  }

  const sorted = React.useMemo(() => {
    return [...workflows].sort((a, b) => {
      const aVal = a[sortKey];
      const bVal = b[sortKey];
      if (typeof aVal === "number" && typeof bVal === "number") {
        return sortDesc ? bVal - aVal : aVal - bVal;
      }
      return sortDesc
        ? String(bVal).localeCompare(String(aVal))
        : String(aVal).localeCompare(String(bVal));
    });
  }, [workflows, sortKey, sortDesc]);

  const cols: { key: keyof WorkflowStat; label: string }[] = [
    { key: "name", label: "Workflow" },
    { key: "executions", label: "Executions" },
    { key: "successRate", label: "Success %" },
    { key: "failureRate", label: "Failure %" },
    { key: "avgDurationMs", label: "Avg Duration" },
    { key: "retryRate", label: "Retry %" },
    { key: "lastRun", label: "Last Run" },
  ];

  if (isLoading) {
    return (
      <div className="space-y-2">
        {[1, 2, 3, 4, 5].map((i) => (
          <Skeleton key={i} className="h-10 w-full" />
        ))}
      </div>
    );
  }

  return (
    <div className="overflow-x-auto">
      <table className="w-full text-sm" role="table">
        <thead>
          <tr className="border-b border-slate-800">
            {cols.map((col) => (
              <th
                key={col.key}
                onClick={() => toggleSort(col.key)}
                className="px-4 py-2.5 text-left text-xs font-medium text-slate-400 uppercase tracking-wider cursor-pointer hover:text-slate-200 select-none transition-colors"
              >
                <span className="flex items-center gap-1">
                  {col.label}
                  {sortKey === col.key && (
                    <span className="text-violet-400">{sortDesc ? "↓" : "↑"}</span>
                  )}
                </span>
              </th>
            ))}
            <th className="px-4 py-2.5 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">
              Actions
            </th>
          </tr>
        </thead>
        <tbody>
          {sorted.map((wf, i) => (
            <tr
              key={wf.id}
              className={cn(
                "border-b border-slate-800/50 hover:bg-slate-800/30 transition-colors",
                i % 2 === 0 ? "bg-transparent" : "bg-slate-900/20"
              )}
            >
              <td className="px-4 py-3 text-slate-200 font-medium">{wf.name}</td>
              <td className="px-4 py-3 text-slate-300 font-mono">{wf.executions.toLocaleString()}</td>
              <td className="px-4 py-3">
                <span className={cn("font-semibold", wf.successRate >= 98 ? "text-emerald-400" : wf.successRate >= 95 ? "text-yellow-400" : "text-red-400")}>
                  {wf.successRate}%
                </span>
              </td>
              <td className="px-4 py-3">
                <span className={cn("font-semibold", wf.failureRate <= 1 ? "text-slate-400" : wf.failureRate <= 3 ? "text-yellow-400" : "text-red-400")}>
                  {wf.failureRate}%
                </span>
              </td>
              <td className="px-4 py-3 text-slate-300 font-mono">
                {wf.avgDurationMs >= 1000 ? `${(wf.avgDurationMs / 1000).toFixed(1)}s` : `${wf.avgDurationMs}ms`}
              </td>
              <td className="px-4 py-3">
                <span className={cn("text-xs font-medium", wf.retryRate <= 2 ? "text-slate-400" : "text-amber-400")}>
                  {wf.retryRate}%
                </span>
              </td>
              <td className="px-4 py-3 text-slate-500 text-xs">{wf.lastRun}</td>
              <td className="px-4 py-3">
                <button className="text-xs text-violet-400 hover:text-violet-300 flex items-center gap-0.5">
                  View <ChevronRight className="h-3 w-3" />
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

// ─── Worker Analytics Grid ────────────────────────────────────────────────────

function WorkerAnalyticsGrid() {
  const { data: workers = [], isLoading } = useQuery({
    queryKey: ["analytics-workers"],
    queryFn: () => AnalyticsService.getWorkerStats(),
    staleTime: 30000,
  });

  if (isLoading) {
    return (
      <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
        {[1, 2, 3, 4, 5, 6].map((i) => <Skeleton key={i} className="h-36" />)}
      </div>
    );
  }

  const statusBadge = {
    ONLINE: { label: "Online", class: "border-emerald-800/50 bg-emerald-950/60 text-emerald-400" },
    OFFLINE: { label: "Offline", class: "border-red-800/50 bg-red-950/60 text-red-400" },
    DRAINING: { label: "Draining", class: "border-yellow-800/50 bg-yellow-950/60 text-yellow-400" },
  };

  return (
    <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
      {workers.map((w) => {
        const badge = statusBadge[w.status];
        return (
          <Card key={w.id} className={cn("border-slate-800 bg-slate-900/40 transition-all hover:border-slate-700", w.status === "OFFLINE" && "opacity-60")}>
            <CardContent className="p-4">
              <div className="flex items-start justify-between mb-3">
                <div>
                  <p className="text-sm font-mono font-medium text-slate-200">{w.name}</p>
                  <p className="text-xs text-slate-500">ID: {w.id}</p>
                </div>
                <span className={cn("text-[10px] font-semibold px-2 py-0.5 rounded-full border", badge.class)}>
                  {badge.label}
                </span>
              </div>
              {w.status !== "OFFLINE" && (
                <>
                  <div className="mb-2">
                    <div className="flex justify-between text-xs mb-1">
                      <span className="text-slate-400">Utilization</span>
                      <span className={cn("font-semibold", w.utilization >= 85 ? "text-red-400" : w.utilization >= 60 ? "text-yellow-400" : "text-emerald-400")}>
                        {w.utilization}%
                      </span>
                    </div>
                    <div className="h-1.5 rounded-full bg-slate-800 overflow-hidden">
                      <div
                        className={cn("h-full rounded-full transition-all duration-500", w.utilization >= 85 ? "bg-red-500" : w.utilization >= 60 ? "bg-yellow-500" : "bg-emerald-500")}
                        style={{ width: `${w.utilization}%` }}
                      />
                    </div>
                  </div>
                  <div className="grid grid-cols-3 gap-2 mt-3">
                    <div className="text-center">
                      <p className="text-xs font-bold text-slate-200">{w.executionCount}</p>
                      <p className="text-[10px] text-slate-500">Executions</p>
                    </div>
                    <div className="text-center">
                      <p className="text-xs font-bold text-slate-200">{w.avgDurationMs}ms</p>
                      <p className="text-[10px] text-slate-500">Avg Dur</p>
                    </div>
                    <div className="text-center">
                      <p className={cn("text-xs font-bold", w.failureRate <= 1 ? "text-slate-200" : "text-red-400")}>{w.failureRate}%</p>
                      <p className="text-[10px] text-slate-500">Failures</p>
                    </div>
                  </div>
                </>
              )}
            </CardContent>
          </Card>
        );
      })}
    </div>
  );
}

// ─── Retry Analytics ──────────────────────────────────────────────────────────

function RetryAnalyticsPanel({ range }: { range: TimeRange }) {
  const { data: reasons = [], isLoading: reasonsLoading } = useQuery({
    queryKey: ["analytics-retry-reasons"],
    queryFn: () => AnalyticsService.getRetryReasons(),
    staleTime: 60000,
  });
  const { data: trend = [], isLoading: trendLoading } = useQuery({
    queryKey: ["analytics-retry-trend", range],
    queryFn: () => AnalyticsService.getRetryTrend(range),
    staleTime: 30000,
  });

  return (
    <div className="grid gap-4 lg:grid-cols-2">
      <Card className="border-slate-800 bg-slate-900/40">
        <CardHeader className="pb-2">
          <CardTitle className="text-sm">Retry Trend</CardTitle>
          <CardDescription className="text-xs">Retries and exhausted attempts over time</CardDescription>
        </CardHeader>
        <CardContent>
          {trendLoading ? (
            <Skeleton className="h-48" />
          ) : (
            <ResponsiveContainer width="100%" height={200}>
              <AreaChart data={trend}>
                <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
                <XAxis dataKey="label" tick={{ fill: "#64748b", fontSize: 11 }} />
                <YAxis tick={{ fill: "#64748b", fontSize: 11 }} />
                <Tooltip {...TOOLTIP_STYLE} />
                <Legend wrapperStyle={{ fontSize: 11, color: "#94a3b8" }} />
                <Area type="monotone" dataKey="retries" name="Retries" stroke={C.retry} fill={C.retry} fillOpacity={0.15} strokeWidth={2} />
                <Area type="monotone" dataKey="exhausted" name="Exhausted" stroke={C.failure} fill={C.failure} fillOpacity={0.15} strokeWidth={2} />
              </AreaChart>
            </ResponsiveContainer>
          )}
        </CardContent>
      </Card>

      <Card className="border-slate-800 bg-slate-900/40">
        <CardHeader className="pb-2">
          <CardTitle className="text-sm">Retry Reasons</CardTitle>
          <CardDescription className="text-xs">Error categories causing retries</CardDescription>
        </CardHeader>
        <CardContent>
          {reasonsLoading ? (
            <Skeleton className="h-48" />
          ) : (
            <div className="flex gap-4">
              <ResponsiveContainer width="50%" height={180}>
                <PieChart>
                  <Pie data={reasons} dataKey="count" cx="50%" cy="50%" innerRadius={40} outerRadius={70} paddingAngle={2}>
                    {reasons.map((r: any, i: number) => (
                      <Cell key={i} fill={r.color} />
                    ))}
                  </Pie>
                  <Tooltip {...TOOLTIP_STYLE} formatter={(v: any) => [v, "Count"]} />
                </PieChart>
              </ResponsiveContainer>
              <div className="flex-1 space-y-2 self-center">
                {reasons.map((r: any) => (
                  <div key={r.reason} className="flex items-center gap-2">
                    <div className="w-2.5 h-2.5 rounded-full flex-shrink-0" style={{ backgroundColor: r.color }} />
                    <div className="flex-1 min-w-0">
                      <p className="text-[11px] text-slate-300 truncate">{r.reason}</p>
                    </div>
                    <span className="text-[11px] font-mono text-slate-400 flex-shrink-0">{r.pct}%</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}

// ─── Metrics Explorer ─────────────────────────────────────────────────────────

function MetricsExplorer() {
  const [filter, setFilter] = React.useState("");

  const { data: metrics = [], isLoading } = useQuery({
    queryKey: ["analytics-metrics", filter],
    queryFn: () => MetricsService.listMetrics(filter || undefined),
    staleTime: 15000,
  });

  const typeColors: Record<string, string> = {
    GAUGE: "border-blue-800/50 bg-blue-950/40 text-blue-400",
    COUNTER: "border-violet-800/50 bg-violet-950/40 text-violet-400",
    HISTOGRAM: "border-amber-800/50 bg-amber-950/40 text-amber-400",
  };

  function formatValue(val: number, unit: string): string {
    if (unit === "bytes") return `${(val / 1_000_000).toFixed(1)} MB`;
    if (val >= 1_000_000) return `${(val / 1_000_000).toFixed(1)}M`;
    if (val >= 1_000) return `${(val / 1_000).toFixed(1)}K`;
    return String(val);
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center gap-3">
        <SearchInput
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
          placeholder="Search metrics by name, description, or labels..."
          className="flex-1"
        />
        <Badge variant="secondary" className="text-xs">{metrics.length} metrics</Badge>
      </div>

      <div className="overflow-hidden rounded-lg border border-slate-800">
        <table className="w-full text-sm" role="table">
          <thead>
            <tr className="bg-slate-900/80 border-b border-slate-800">
              <th className="px-4 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">Metric</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider hidden md:table-cell">Type</th>
              <th className="px-4 py-3 text-right text-xs font-medium text-slate-400 uppercase tracking-wider">Value</th>
              <th className="px-4 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider hidden lg:table-cell">Labels</th>
            </tr>
          </thead>
          <tbody>
            {isLoading
              ? [1, 2, 3, 4, 5, 6].map((i) => (
                  <tr key={i} className="border-b border-slate-800/50">
                    <td className="px-4 py-3"><Skeleton className="h-4 w-48" /></td>
                    <td className="px-4 py-3 hidden md:table-cell"><Skeleton className="h-4 w-20" /></td>
                    <td className="px-4 py-3"><Skeleton className="h-4 w-16 ml-auto" /></td>
                    <td className="px-4 py-3 hidden lg:table-cell"><Skeleton className="h-4 w-32" /></td>
                  </tr>
                ))
              : metrics.map((m, i) => (
                  <tr key={m.name} className={cn("border-b border-slate-800/50 hover:bg-slate-800/30 transition-colors", i % 2 === 0 ? "" : "bg-slate-900/20")}>
                    <td className="px-4 py-3">
                      <p className="font-mono text-xs text-violet-300">{m.name}</p>
                      <p className="text-xs text-slate-500 mt-0.5 hidden sm:block">{m.description}</p>
                    </td>
                    <td className="px-4 py-3 hidden md:table-cell">
                      <span className={cn("text-[10px] font-semibold px-2 py-0.5 rounded-full border", typeColors[m.type])}>
                        {m.type}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-right">
                      <span className="font-mono font-bold text-slate-200">{formatValue(m.value, m.unit)}</span>
                      <span className="text-xs text-slate-500 ml-1">{m.unit !== "count" && m.unit !== "bytes" ? m.unit : ""}</span>
                    </td>
                    <td className="px-4 py-3 hidden lg:table-cell">
                      <div className="flex flex-wrap gap-1">
                        {Object.entries(m.labels).map(([k, v]) => (
                          <span key={k} className="text-[10px] font-mono bg-slate-800 text-slate-400 px-1.5 py-0.5 rounded">
                            {k}={v}
                          </span>
                        ))}
                      </div>
                    </td>
                  </tr>
                ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

// ─── Trace Viewer ─────────────────────────────────────────────────────────────

function SpanBar({ span, totalMs }: { span: TraceSpan; totalMs: number }) {
  const left = (span.startOffsetMs / totalMs) * 100;
  const width = Math.max((span.durationMs / totalMs) * 100, 1);

  const serviceColors: Record<string, string> = {
    "api-service": "#6366f1",
    "scheduler-service": "#3b82f6",
    "worker-service": "#10b981",
    "result-processor": "#f59e0b",
    "event-processor": "#8b5cf6",
  };
  const color = serviceColors[span.service] ?? "#64748b";

  return (
    <div className="flex items-center gap-3 text-xs group">
      <div className="w-28 flex-shrink-0 text-slate-400 truncate" title={span.service}>
        {span.service}
      </div>
      <div className="w-36 flex-shrink-0 text-slate-300 truncate" title={span.operation}>
        {span.operation}
      </div>
      <div className="flex-1 relative h-5 bg-slate-900 rounded overflow-hidden">
        <div
          className="absolute h-full rounded transition-all"
          style={{ left: `${left}%`, width: `${width}%`, backgroundColor: color, opacity: span.status === "ERROR" ? 1 : 0.8 }}
        />
        {span.status === "ERROR" && (
          <div
            className="absolute h-full rounded border-2 border-red-500"
            style={{ left: `${left}%`, width: `${width}%` }}
          />
        )}
      </div>
      <div className="w-16 flex-shrink-0 text-right font-mono text-slate-400">
        {span.durationMs}ms
      </div>
      <div className="flex-shrink-0">
        {span.status === "OK" ? (
          <CheckCircle2 className="h-3 w-3 text-emerald-400" />
        ) : span.status === "ERROR" ? (
          <XCircle className="h-3 w-3 text-red-400" />
        ) : null}
      </div>
    </div>
  );
}

function TraceCard({ trace, onClick, selected }: { trace: Trace; onClick: () => void; selected: boolean }) {
  const statusColor = trace.spans.some((s) => s.status === "ERROR")
    ? "border-red-800/50 bg-red-950/10"
    : "border-slate-800 bg-slate-900/40";
  return (
    <button
      onClick={onClick}
      className={cn(
        "w-full text-left rounded-lg border p-3 transition-all hover:border-violet-700/50",
        statusColor,
        selected && "border-violet-600 bg-violet-950/20"
      )}
    >
      <div className="flex items-start justify-between">
        <div>
          <p className="font-mono text-xs text-violet-300 truncate max-w-[200px]">{trace.traceId}</p>
          {trace.executionId && (
            <p className="text-[10px] text-slate-500 mt-0.5">exec: {trace.executionId}</p>
          )}
        </div>
        <div className="text-right">
          <p className="font-mono text-xs text-slate-300">{trace.totalDurationMs}ms</p>
          <p className="text-[10px] text-slate-500">{trace.spans.length} spans</p>
        </div>
      </div>
      <div className="flex items-center gap-2 mt-2">
        {trace.workerId && (
          <span className="text-[10px] font-mono bg-slate-800 text-slate-400 px-1.5 py-0.5 rounded">
            {trace.workerId}
          </span>
        )}
        {trace.spans.some((s) => s.status === "ERROR") && (
          <span className="text-[10px] font-semibold text-red-400 flex items-center gap-0.5">
            <XCircle className="h-2.5 w-2.5" /> Error
          </span>
        )}
      </div>
    </button>
  );
}

function TraceViewer() {
  const [query, setQuery] = React.useState("");
  const [selectedTrace, setSelectedTrace] = React.useState<Trace | null>(null);

  const { data: traces = [], isLoading } = useQuery({
    queryKey: ["analytics-traces", query],
    queryFn: () => TraceService.searchTraces(query),
    staleTime: 10000,
  });

  React.useEffect(() => {
    if (traces.length > 0 && !selectedTrace) {
      setSelectedTrace(traces[0]);
    }
  }, [traces, selectedTrace]);

  return (
    <div className="flex gap-4 h-full min-h-[480px]">
      {/* Trace List */}
      <div className="w-80 flex-shrink-0 flex flex-col gap-3">
        <SearchInput
          value={query}
          onChange={(e) => { setQuery(e.target.value); setSelectedTrace(null); }}
          placeholder="Search trace ID, exec ID, worker..."
        />
        <div className="space-y-2 flex-1 overflow-y-auto">
          {isLoading ? (
            [1, 2, 3].map((i) => <Skeleton key={i} className="h-20" />)
          ) : traces.length === 0 ? (
            <div className="text-center py-8 text-slate-500 text-sm">No traces found</div>
          ) : (
            traces.map((t) => (
              <TraceCard
                key={t.traceId}
                trace={t}
                onClick={() => setSelectedTrace(t)}
                selected={selectedTrace?.traceId === t.traceId}
              />
            ))
          )}
        </div>
      </div>

      {/* Trace Detail */}
      <div className="flex-1 min-w-0">
        {selectedTrace ? (
          <Card className="border-slate-800 bg-slate-900/40 h-full">
            <CardHeader className="pb-3">
              <div className="flex items-start justify-between">
                <div>
                  <CardTitle className="text-sm font-mono text-violet-300">{selectedTrace.traceId}</CardTitle>
                  <CardDescription className="text-xs mt-1">
                    Started {new Date(selectedTrace.startedAt).toLocaleTimeString()} · {selectedTrace.totalDurationMs}ms total · {selectedTrace.spans.length} spans
                  </CardDescription>
                </div>
                <div className="flex flex-wrap gap-1">
                  {selectedTrace.executionId && (
                    <a href={`/executions?id=${selectedTrace.executionId}`} className="text-[10px] text-violet-400 hover:text-violet-300 flex items-center gap-0.5 transition-colors">
                      View Execution <ChevronRight className="h-2.5 w-2.5" />
                    </a>
                  )}
                </div>
              </div>
            </CardHeader>
            <CardContent className="overflow-x-auto">
              <div className="space-y-1.5 min-w-[520px]">
                <div className="flex items-center gap-3 text-[10px] text-slate-600 uppercase tracking-wider font-medium border-b border-slate-800 pb-2 mb-3">
                  <div className="w-28">Service</div>
                  <div className="w-36">Operation</div>
                  <div className="flex-1">Timeline</div>
                  <div className="w-16 text-right">Duration</div>
                  <div className="w-4" />
                </div>
                {selectedTrace.spans.map((span) => (
                  <SpanBar key={span.spanId} span={span} totalMs={selectedTrace.totalDurationMs} />
                ))}
              </div>

              {/* Span Attributes */}
              <div className="mt-4 border-t border-slate-800 pt-4">
                <p className="text-xs font-medium text-slate-400 mb-2">Span Attributes</p>
                <div className="grid gap-2 sm:grid-cols-2">
                  {selectedTrace.spans
                    .filter((s) => Object.keys(s.attributes).length > 0)
                    .map((s) => (
                      <div key={s.spanId} className="bg-slate-950/60 rounded border border-slate-800 p-2.5">
                        <p className="text-[10px] font-semibold text-slate-400 mb-1.5 font-mono">{s.operation}</p>
                        {Object.entries(s.attributes).map(([k, v]) => (
                          <div key={k} className="flex gap-2 text-[10px]">
                            <span className="text-slate-500 flex-shrink-0">{k}:</span>
                            <span className="font-mono text-slate-300 truncate">{v}</span>
                          </div>
                        ))}
                      </div>
                    ))}
                </div>
              </div>
            </CardContent>
          </Card>
        ) : (
          <div className="flex items-center justify-center h-full text-slate-500 text-sm">
            Select a trace to view details
          </div>
        )}
      </div>
    </div>
  );
}

// ─── Report Export ────────────────────────────────────────────────────────────

function ReportExportPanel({ range }: { range: TimeRange }) {
  const [exporting, setExporting] = React.useState(false);
  const [format, setFormat] = React.useState<"csv" | "json">("csv");
  const [lastExport, setLastExport] = React.useState<string | null>(null);

  async function handleExport() {
    setExporting(true);
    try {
      await ReportService.exportReport({ range, format });
      setLastExport(new Date().toLocaleTimeString());
    } finally {
      setExporting(false);
    }
  }

  return (
    <Card className="border-slate-800 bg-slate-900/40">
      <CardHeader>
        <CardTitle className="text-sm flex items-center gap-2">
          <FileText className="h-4 w-4 text-violet-400" />
          Export Report
        </CardTitle>
        <CardDescription className="text-xs">
          Download execution analytics for the selected time range
        </CardDescription>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="grid gap-3 sm:grid-cols-2">
          <div>
            <label className="text-xs text-slate-400 mb-2 block">Format</label>
            <div className="flex gap-2">
              {(["csv", "json"] as const).map((f) => (
                <button
                  key={f}
                  onClick={() => setFormat(f)}
                  className={cn(
                    "flex-1 py-2 rounded-lg border text-xs font-medium transition-all",
                    format === f
                      ? "border-violet-600 bg-violet-950/40 text-violet-300"
                      : "border-slate-700 bg-slate-900 text-slate-400 hover:border-slate-600"
                  )}
                >
                  {f.toUpperCase()}
                </button>
              ))}
            </div>
          </div>
          <div>
            <label className="text-xs text-slate-400 mb-2 block">Time Range</label>
            <div className="py-2 px-3 rounded-lg border border-slate-700 bg-slate-900 text-xs text-slate-300">
              {TIME_RANGES.find((r) => r.value === range)?.label ?? "Custom"}
            </div>
          </div>
        </div>

        <div className="rounded-lg border border-slate-800 bg-slate-950/60 p-3">
          <p className="text-xs text-slate-400 mb-2">Report includes:</p>
          <ul className="space-y-1 text-xs text-slate-500">
            <li className="flex items-center gap-1.5"><CheckCircle2 className="h-3 w-3 text-emerald-500" /> Execution counts (success, failure)</li>
            <li className="flex items-center gap-1.5"><CheckCircle2 className="h-3 w-3 text-emerald-500" /> Average duration per interval</li>
            <li className="flex items-center gap-1.5"><CheckCircle2 className="h-3 w-3 text-emerald-500" /> Retry statistics</li>
            <li className="flex items-center gap-1.5"><CheckCircle2 className="h-3 w-3 text-emerald-500" /> Generated timestamp</li>
          </ul>
        </div>

        <div className="flex items-center gap-3">
          <Button
            onClick={handleExport}
            disabled={exporting}
            className="flex items-center gap-2"
          >
            {exporting ? (
              <RefreshCw className="h-4 w-4 animate-spin" />
            ) : (
              <Download className="h-4 w-4" />
            )}
            {exporting ? "Exporting..." : `Export ${format.toUpperCase()}`}
          </Button>
          {lastExport && (
            <p className="text-xs text-slate-500">Last exported at {lastExport}</p>
          )}
        </div>
      </CardContent>
    </Card>
  );
}

// ─── Main Page ────────────────────────────────────────────────────────────────

export default function AnalyticsPage() {
  const [range, setRange] = React.useState<TimeRange>("24h");
  const [activeTab, setActiveTab] = React.useState("overview");

  const CARD_ICONS: Record<string, React.ReactNode> = {
    "total-executions": <BarChart3 className="h-4 w-4" />,
    "success-rate": <CheckCircle2 className="h-4 w-4" />,
    "failure-rate": <XCircle className="h-4 w-4" />,
    "avg-duration": <Clock className="h-4 w-4" />,
    "retry-rate": <RefreshCw className="h-4 w-4" />,
    "active-workers": <Users className="h-4 w-4" />,
    "queue-growth": <TrendingUp className="h-4 w-4" />,
    throughput: <Zap className="h-4 w-4" />,
  };

  // Queries
  const { data: cards = [], isLoading: cardsLoading } = useQuery({
    queryKey: ["analytics-cards", range],
    queryFn: () => AnalyticsService.getSummaryCards(range),
    staleTime: 30000,
  });

  const { data: execTrend = [], isLoading: trendLoading } = useQuery({
    queryKey: ["analytics-exec-trend", range],
    queryFn: () => AnalyticsService.getExecutionTrend(range),
    staleTime: 30000,
  });

  const { data: workerUtil = [], isLoading: workerLoading } = useQuery({
    queryKey: ["analytics-worker-util", range],
    queryFn: () => AnalyticsService.getWorkerUtilization(range),
    staleTime: 30000,
  });

  const { data: queueGrowth = [], isLoading: queueLoading } = useQuery({
    queryKey: ["analytics-queue", range],
    queryFn: () => AnalyticsService.getQueueGrowth(range),
    staleTime: 30000,
  });

  const { data: httpDist = [], isLoading: httpLoading } = useQuery({
    queryKey: ["analytics-http"],
    queryFn: () => AnalyticsService.getHttpStatusDistribution(),
    staleTime: 60000,
  });

  const { data: durationDist = [], isLoading: durLoading } = useQuery({
    queryKey: ["analytics-duration"],
    queryFn: () => AnalyticsService.getDurationDistribution(),
    staleTime: 60000,
  });

  return (
    <div className="space-y-6 pb-8" aria-label="Analytics & Observability">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-100 flex items-center gap-2">
            <BarChart3 className="h-6 w-6 text-violet-400" />
            Analytics & Observability
          </h1>
          <p className="text-sm text-slate-400 mt-1">
            Execution trends, workflow insights, distributed traces, and system metrics
          </p>
        </div>
        <TimeRangeSelector value={range} onChange={setRange} />
      </div>

      {/* Summary Cards */}
      <div className="grid gap-3 grid-cols-2 md:grid-cols-4 lg:grid-cols-4 xl:grid-cols-4">
        {cardsLoading
          ? Array.from({ length: 8 }).map((_, i) => (
              <Card key={i} className="border-slate-800 bg-slate-900/40">
                <CardContent className="p-5">
                  <Skeleton className="h-3 w-24 mb-3" />
                  <Skeleton className="h-7 w-16 mb-2" />
                  <Skeleton className="h-3 w-20" />
                </CardContent>
              </Card>
            ))
          : cards.map((card) => (
              <AnalyticsMetricCard
                key={card.id}
                {...card}
                icon={CARD_ICONS[card.id] ?? <Activity className="h-4 w-4" />}
              />
            ))}
      </div>

      {/* Tabs */}
      <Tabs defaultValue="overview" value={activeTab} onValueChange={setActiveTab}>
        <TabsList className="overflow-x-auto flex-wrap h-auto gap-1">
          <TabsTrigger value="overview" id="tab-overview">Overview</TabsTrigger>
          <TabsTrigger value="workflows" id="tab-workflows">Workflows</TabsTrigger>
          <TabsTrigger value="workers" id="tab-workers">Workers</TabsTrigger>
          <TabsTrigger value="retries" id="tab-retries">Retries</TabsTrigger>
          <TabsTrigger value="traces" id="tab-traces">Traces</TabsTrigger>
          <TabsTrigger value="metrics" id="tab-metrics">Metrics</TabsTrigger>
          <TabsTrigger value="reports" id="tab-reports">Reports</TabsTrigger>
        </TabsList>

        {/* ── Overview ── */}
        <TabsContent value="overview" className="space-y-4 mt-4">
          {/* Execution Trend + Success vs Failure */}
          <div className="grid gap-4 lg:grid-cols-2">
            <Card className="border-slate-800 bg-slate-900/40">
              <CardHeader className="pb-2">
                <CardTitle className="text-sm">Execution Trend</CardTitle>
                <CardDescription className="text-xs">Total, success, and failure over time</CardDescription>
              </CardHeader>
              <CardContent>
                {trendLoading ? (
                  <Skeleton className="h-52" />
                ) : (
                  <ResponsiveContainer width="100%" height={220}>
                    <AreaChart data={execTrend}>
                      <defs>
                        <linearGradient id="gSuccess" x1="0" y1="0" x2="0" y2="1">
                          <stop offset="5%" stopColor={C.success} stopOpacity={0.25} />
                          <stop offset="95%" stopColor={C.success} stopOpacity={0} />
                        </linearGradient>
                        <linearGradient id="gFailure" x1="0" y1="0" x2="0" y2="1">
                          <stop offset="5%" stopColor={C.failure} stopOpacity={0.25} />
                          <stop offset="95%" stopColor={C.failure} stopOpacity={0} />
                        </linearGradient>
                      </defs>
                      <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
                      <XAxis dataKey="label" tick={{ fill: "#64748b", fontSize: 11 }} />
                      <YAxis tick={{ fill: "#64748b", fontSize: 11 }} />
                      <Tooltip {...TOOLTIP_STYLE} />
                      <Legend wrapperStyle={{ fontSize: 11, color: "#94a3b8" }} />
                      <Area type="monotone" dataKey="success" name="Success" stroke={C.success} fill="url(#gSuccess)" strokeWidth={2} />
                      <Area type="monotone" dataKey="failure" name="Failure" stroke={C.failure} fill="url(#gFailure)" strokeWidth={2} />
                    </AreaChart>
                  </ResponsiveContainer>
                )}
              </CardContent>
            </Card>

            <Card className="border-slate-800 bg-slate-900/40">
              <CardHeader className="pb-2">
                <CardTitle className="text-sm">Retry Trend</CardTitle>
                <CardDescription className="text-xs">Retry volume alongside total executions</CardDescription>
              </CardHeader>
              <CardContent>
                {trendLoading ? (
                  <Skeleton className="h-52" />
                ) : (
                  <ResponsiveContainer width="100%" height={220}>
                    <BarChart data={execTrend}>
                      <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
                      <XAxis dataKey="label" tick={{ fill: "#64748b", fontSize: 11 }} />
                      <YAxis tick={{ fill: "#64748b", fontSize: 11 }} />
                      <Tooltip {...TOOLTIP_STYLE} />
                      <Legend wrapperStyle={{ fontSize: 11, color: "#94a3b8" }} />
                      <Bar dataKey="total" name="Total" fill={C.total} radius={[2, 2, 0, 0]} opacity={0.6} />
                      <Bar dataKey="retries" name="Retries" fill={C.retry} radius={[2, 2, 0, 0]} />
                    </BarChart>
                  </ResponsiveContainer>
                )}
              </CardContent>
            </Card>
          </div>

          {/* Worker Utilization + Queue Growth */}
          <div className="grid gap-4 lg:grid-cols-2">
            <Card className="border-slate-800 bg-slate-900/40">
              <CardHeader className="pb-2">
                <CardTitle className="text-sm">Worker Utilization</CardTitle>
                <CardDescription className="text-xs">Average worker load over time</CardDescription>
              </CardHeader>
              <CardContent>
                {workerLoading ? (
                  <Skeleton className="h-48" />
                ) : (
                  <ResponsiveContainer width="100%" height={200}>
                    <AreaChart data={workerUtil}>
                      <defs>
                        <linearGradient id="gUtil" x1="0" y1="0" x2="0" y2="1">
                          <stop offset="5%" stopColor={C.util} stopOpacity={0.3} />
                          <stop offset="95%" stopColor={C.util} stopOpacity={0} />
                        </linearGradient>
                      </defs>
                      <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
                      <XAxis dataKey="label" tick={{ fill: "#64748b", fontSize: 11 }} />
                      <YAxis domain={[0, 100]} unit="%" tick={{ fill: "#64748b", fontSize: 11 }} />
                      <Tooltip {...TOOLTIP_STYLE} formatter={(v: any) => [`${v}%`, "Utilization"]} />
                      <Area type="monotone" dataKey="utilization" name="Utilization" stroke={C.util} fill="url(#gUtil)" strokeWidth={2} />
                    </AreaChart>
                  </ResponsiveContainer>
                )}
              </CardContent>
            </Card>

            <Card className="border-slate-800 bg-slate-900/40">
              <CardHeader className="pb-2">
                <CardTitle className="text-sm">Queue Growth</CardTitle>
                <CardDescription className="text-xs">Pending, processing, and dead-letter queue</CardDescription>
              </CardHeader>
              <CardContent>
                {queueLoading ? (
                  <Skeleton className="h-48" />
                ) : (
                  <ResponsiveContainer width="100%" height={200}>
                    <BarChart data={queueGrowth}>
                      <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
                      <XAxis dataKey="label" tick={{ fill: "#64748b", fontSize: 11 }} />
                      <YAxis tick={{ fill: "#64748b", fontSize: 11 }} />
                      <Tooltip {...TOOLTIP_STYLE} />
                      <Legend wrapperStyle={{ fontSize: 11, color: "#94a3b8" }} />
                      <Bar dataKey="pending" name="Pending" stackId="q" fill={C.pending} radius={[0, 0, 0, 0]} />
                      <Bar dataKey="processing" name="Processing" stackId="q" fill={C.processing} />
                      <Bar dataKey="dead" name="Dead Letter" stackId="q" fill={C.dead} radius={[2, 2, 0, 0]} />
                    </BarChart>
                  </ResponsiveContainer>
                )}
              </CardContent>
            </Card>
          </div>

          {/* HTTP Status + Duration Distribution */}
          <div className="grid gap-4 lg:grid-cols-2">
            <Card className="border-slate-800 bg-slate-900/40">
              <CardHeader className="pb-2">
                <CardTitle className="text-sm">HTTP Status Distribution</CardTitle>
                <CardDescription className="text-xs">Response codes from dispatched executions</CardDescription>
              </CardHeader>
              <CardContent className="flex gap-4 items-center">
                {httpLoading ? (
                  <Skeleton className="h-48 w-full" />
                ) : (
                  <>
                    <ResponsiveContainer width="45%" height={180}>
                      <PieChart>
                        <Pie data={httpDist} dataKey="value" cx="50%" cy="50%" innerRadius={42} outerRadius={72} paddingAngle={2}>
                          {httpDist.map((entry: any, i: number) => (
                            <Cell key={i} fill={entry.color} />
                          ))}
                        </Pie>
                        <Tooltip {...TOOLTIP_STYLE} formatter={(v: any, n: any) => [v.toLocaleString(), n]} />
                      </PieChart>
                    </ResponsiveContainer>
                    <div className="flex-1 space-y-2">
                      {httpDist.map((item: any) => (
                        <div key={item.name} className="flex items-center gap-2">
                          <div className="w-2.5 h-2.5 rounded-full flex-shrink-0" style={{ backgroundColor: item.color }} />
                          <span className="text-xs text-slate-300 flex-1">{item.name}</span>
                          <span className="text-xs font-mono text-slate-400">{item.value.toLocaleString()}</span>
                        </div>
                      ))}
                    </div>
                  </>
                )}
              </CardContent>
            </Card>

            <Card className="border-slate-800 bg-slate-900/40">
              <CardHeader className="pb-2">
                <CardTitle className="text-sm">Duration Distribution</CardTitle>
                <CardDescription className="text-xs">Execution time histogram buckets</CardDescription>
              </CardHeader>
              <CardContent>
                {durLoading ? (
                  <Skeleton className="h-48" />
                ) : (
                  <ResponsiveContainer width="100%" height={200}>
                    <BarChart data={durationDist} layout="vertical">
                      <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" horizontal={false} />
                      <XAxis type="number" tick={{ fill: "#64748b", fontSize: 11 }} />
                      <YAxis dataKey="range" type="category" tick={{ fill: "#64748b", fontSize: 11 }} width={70} />
                      <Tooltip {...TOOLTIP_STYLE} />
                      <Bar dataKey="count" name="Executions" fill={C.duration} radius={[0, 3, 3, 0]} />
                    </BarChart>
                  </ResponsiveContainer>
                )}
              </CardContent>
            </Card>
          </div>

          {/* Heatmap */}
          <Card className="border-slate-800 bg-slate-900/40">
            <CardHeader className="pb-2">
              <CardTitle className="text-sm">Execution Heatmap</CardTitle>
              <CardDescription className="text-xs">Execution density by day and hour</CardDescription>
            </CardHeader>
            <CardContent>
              <ExecutionHeatmap range={range} />
            </CardContent>
          </Card>
        </TabsContent>

        {/* ── Workflows ── */}
        <TabsContent value="workflows" className="mt-4">
          <Card className="border-slate-800 bg-slate-900/40">
            <CardHeader>
              <SectionHeading
                icon={<GitBranch className="h-4 w-4" />}
                title="Workflow Analytics"
                description="Execution count, success rates, durations, and retry patterns per workflow"
              />
            </CardHeader>
            <CardContent>
              <WorkflowAnalyticsTable />
            </CardContent>
          </Card>
        </TabsContent>

        {/* ── Workers ── */}
        <TabsContent value="workers" className="mt-4 space-y-4">
          <SectionHeading
            icon={<Server className="h-4 w-4" />}
            title="Worker Analytics"
            description="Per-node utilization, execution counts, lease stats, and failure rates"
          />
          <WorkerAnalyticsGrid />

          <Card className="border-slate-800 bg-slate-900/40">
            <CardHeader className="pb-2">
              <CardTitle className="text-sm">Worker Execution Comparison</CardTitle>
              <CardDescription className="text-xs">Side-by-side execution count per worker</CardDescription>
            </CardHeader>
            <CardContent>
              <WorkerUtilizationChart range={range} />
            </CardContent>
          </Card>
        </TabsContent>

        {/* ── Retries ── */}
        <TabsContent value="retries" className="mt-4 space-y-4">
          <SectionHeading
            icon={<RefreshCw className="h-4 w-4" />}
            title="Retry Analytics"
            description="Retry frequency, error categories, exhaustion rates, and average delays"
          />
          <RetryAnalyticsPanel range={range} />
        </TabsContent>

        {/* ── Traces ── */}
        <TabsContent value="traces" className="mt-4">
          <SectionHeading
            icon={<Eye className="h-4 w-4" />}
            title="Trace Viewer"
            description="OpenTelemetry-compatible distributed traces with span timeline"
          />
          <TraceViewer />
        </TabsContent>

        {/* ── Metrics ── */}
        <TabsContent value="metrics" className="mt-4">
          <SectionHeading
            icon={<Database className="h-4 w-4" />}
            title="Metrics Explorer"
            description="Browse all system metrics with type, value, unit, and labels"
          />
          <MetricsExplorer />
        </TabsContent>

        {/* ── Reports ── */}
        <TabsContent value="reports" className="mt-4 space-y-4">
          <SectionHeading
            icon={<FileText className="h-4 w-4" />}
            title="Export Reports"
            description="Download execution analytics as CSV or JSON with generated timestamp"
          />
          <div className="grid gap-4 lg:grid-cols-2">
            <ReportExportPanel range={range} />
            <Card className="border-slate-800 bg-slate-900/40">
              <CardHeader>
                <CardTitle className="text-sm">Report Preview</CardTitle>
                <CardDescription className="text-xs">Sample of the exported data structure</CardDescription>
              </CardHeader>
              <CardContent>
                <pre className="bg-slate-950 border border-slate-800 rounded-lg p-4 text-xs text-slate-300 overflow-auto max-h-72 font-mono leading-relaxed">
{`{
  "generatedAt": "${new Date().toISOString()}",
  "range": "${range}",
  "data": [
    {
      "timestamp": "2026-07-14T10:00:00Z",
      "executions": 122,
      "success": 120,
      "failures": 2,
      "avgDurationMs": 420,
      "retries": 1
    },
    {
      "timestamp": "2026-07-14T11:00:00Z",
      "executions": 149,
      "success": 144,
      "failures": 5,
      "avgDurationMs": 435,
      "retries": 3
    }
    ...
  ]
}`}
                </pre>
              </CardContent>
            </Card>
          </div>
        </TabsContent>
      </Tabs>
    </div>
  );
}

// ─── Worker Comparison Chart (deferred render) ────────────────────────────────

function WorkerUtilizationChart({ range }: { range: TimeRange }) {
  const { data: workers = [], isLoading } = useQuery({
    queryKey: ["analytics-workers"],
    queryFn: () => AnalyticsService.getWorkerStats(),
    staleTime: 30000,
  });

  const chartData = workers
    .filter((w) => w.status !== "OFFLINE")
    .map((w) => ({
      name: w.name.replace("worker-node-0", "W"),
      executions: w.executionCount,
      utilization: w.utilization,
      failureRate: w.failureRate,
    }));

  if (isLoading) return <Skeleton className="h-48" />;

  return (
    <ResponsiveContainer width="100%" height={200}>
      <BarChart data={chartData}>
        <CartesianGrid strokeDasharray="3 3" stroke="#1e293b" />
        <XAxis dataKey="name" tick={{ fill: "#64748b", fontSize: 11 }} />
        <YAxis tick={{ fill: "#64748b", fontSize: 11 }} />
        <Tooltip {...TOOLTIP_STYLE} />
        <Legend wrapperStyle={{ fontSize: 11, color: "#94a3b8" }} />
        <Bar dataKey="executions" name="Executions" fill={C.success} radius={[2, 2, 0, 0]} />
        <Bar dataKey="utilization" name="Utilization %" fill={C.util} radius={[2, 2, 0, 0]} />
      </BarChart>
    </ResponsiveContainer>
  );
}
