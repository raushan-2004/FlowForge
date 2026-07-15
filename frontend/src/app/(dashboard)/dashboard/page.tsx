"use client";

import * as React from "react";
import { OnboardingBanner } from "@/components/onboarding/OnboardingBanner";
import { useQuery } from "@tanstack/react-query";
import { MetricsService, ChartDataPoint } from "@/services/metrics";
import { HealthService } from "@/services/health";
import { ActivityService, ActivityItem } from "@/services/activity";
import { MetricCard } from "@/components/ui/MetricCard";
import { Spinner } from "@/components/ui/Spinner";
import { ErrorState } from "@/components/ui/ErrorState";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { Button } from "@/components/ui/Button";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/Card";
import {
  Users,
  GitBranch,
  Cpu,
  RefreshCw,
  Clock,
  CheckCircle,
  AlertTriangle,
  Play,
  CheckCircle2,
  XCircle,
  HelpCircle,
  Database,
  Share2,
} from "lucide-react";
import {
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
  Tooltip,
  Legend,
} from "recharts";

type RefreshOption = "Off" | "5s" | "15s" | "30s" | "60s";

const refreshMsMap: Record<RefreshOption, number | false> = {
  Off: false,
  "5s": 5000,
  "15s": 15000,
  "30s": 30000,
  "60s": 60000,
};

export default function DashboardPage() {
  const [range, setRange] = React.useState<"hourly" | "daily" | "weekly">("daily");
  const [refreshInterval, setRefreshInterval] = React.useState<RefreshOption>("30s");
  const [mounted, setMounted] = React.useState(false);

  React.useEffect(() => {
    setMounted(true);
  }, []);

  const refetchInterval = refreshMsMap[refreshInterval];

  // 1. Metric Cards Query
  const {
    data: metrics,
    isLoading: metricsLoading,
    isError: metricsError,
    refetch: refetchMetrics,
  } = useQuery({
    queryKey: ["dashboard-metrics"],
    queryFn: MetricsService.getSummary,
    refetchInterval,
  });

  // 2. Chart Data Query
  const {
    data: chartData,
    isLoading: chartLoading,
    isError: chartError,
    refetch: refetchCharts,
  } = useQuery({
    queryKey: ["dashboard-charts", range],
    queryFn: () => MetricsService.getChartData(range),
    refetchInterval,
  });

  // 3. HTTP Status distribution query
  const {
    data: httpDist,
    isLoading: httpDistLoading,
    isError: httpDistError,
    refetch: refetchHttpDist,
  } = useQuery({
    queryKey: ["dashboard-http-dist"],
    queryFn: MetricsService.getHttpStatusDistribution,
    refetchInterval,
  });

  // 4. System Health Query
  const {
    data: health,
    isLoading: healthLoading,
    isError: healthError,
    refetch: refetchHealth,
  } = useQuery({
    queryKey: ["dashboard-health"],
    queryFn: HealthService.getSystemHealth,
    refetchInterval,
  });

  // 5. Activity Feed Query
  const {
    data: activities,
    isLoading: activitiesLoading,
    isError: activitiesError,
    refetch: refetchActivities,
  } = useQuery({
    queryKey: ["dashboard-activities"],
    queryFn: ActivityService.getLiveFeed,
    refetchInterval,
  });

  const handleManualRefresh = () => {
    refetchMetrics();
    refetchCharts();
    refetchHttpDist();
    refetchHealth();
    refetchActivities();
  };

  const getStatusColor = (status: "Healthy" | "Warning" | "Critical") => {
    if (status === "Healthy") return "text-green-400";
    if (status === "Warning") return "text-orange-400";
    return "text-red-400";
  };

  const getActivityIcon = (type: ActivityItem["type"]) => {
    switch (type) {
      case "WORKFLOW_STARTED":
      case "EXECUTION_STARTED":
        return <Play className="h-4 w-4 text-blue-400" />;
      case "WORKFLOW_COMPLETED":
      case "EXECUTION_COMPLETED":
        return <CheckCircle2 className="h-4 w-4 text-green-400" />;
      case "RETRY_SCHEDULED":
        return <RefreshCw className="h-4 w-4 text-orange-400 animate-spin" />;
      case "WORKER_JOINED":
        return <Users className="h-4 w-4 text-purple-400" />;
      case "WORKER_OFFLINE":
        return <XCircle className="h-4 w-4 text-red-400 animate-pulse" />;
      default:
        return <HelpCircle className="h-4 w-4 text-slate-400" />;
    }
  };

  return (
    <div className="space-y-6">
      <OnboardingBanner />
      {/* Console Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-100">Operations Control Center</h1>
          <p className="text-sm text-slate-400">Live operational throughput, task retry pipelines, and microservices diagnostics.</p>
        </div>
        <div className="flex items-center space-x-2.5">
          {/* Auto Refresh Configuration Selector */}
          <div className="flex items-center space-x-1.5 text-xs bg-slate-900 border border-slate-800 rounded-md px-2 py-1 text-slate-400">
            <span className="font-semibold select-none">Sync:</span>
            <select
              value={refreshInterval}
              onChange={(e) => setRefreshInterval(e.target.value as RefreshOption)}
              className="bg-transparent border-0 text-slate-200 focus:ring-0 outline-hidden font-bold cursor-pointer"
            >
              <option value="Off" className="bg-slate-950">Off</option>
              <option value="5s" className="bg-slate-950">5s</option>
              <option value="15s" className="bg-slate-950">15s</option>
              <option value="30s" className="bg-slate-950">30s</option>
              <option value="60s" className="bg-slate-950">60s</option>
            </select>
          </div>
          <Button onClick={handleManualRefresh} size="sm" variant="outline" className="gap-1.5 border-slate-800">
            <RefreshCw className="h-3.5 w-3.5" />
            Sync Now
          </Button>
        </div>
      </div>

      {/* 1. Metric Cards Grid */}
      {metricsLoading ? (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
          {Array.from({ length: 8 }).map((_, idx) => (
            <Card key={idx} className="h-28 animate-pulse bg-slate-900/30 border-slate-900" />
          ))}
        </div>
      ) : metricsError || !metrics ? (
        <ErrorState message="Failed to load dashboard metrics summary stats." onRetry={refetchMetrics} />
      ) : (
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
          <MetricCard
            title="Active Executions"
            value={metrics.activeExecutions.value}
            description={metrics.activeExecutions.description}
            icon={<Cpu className="h-4 w-4" />}
            trend={{ value: metrics.activeExecutions.trend, isPositive: metrics.activeExecutions.isPositive }}
          />
          <MetricCard
            title="Running Workflows"
            value={metrics.runningWorkflows.value}
            description={metrics.runningWorkflows.description}
            icon={<GitBranch className="h-4 w-4" />}
            trend={{ value: metrics.runningWorkflows.trend, isPositive: metrics.runningWorkflows.isPositive }}
          />
          <MetricCard
            title="Active Workers"
            value={metrics.activeWorkers.value}
            description={metrics.activeWorkers.description}
            icon={<Users className="h-4 w-4" />}
          />
          <MetricCard
            title="Scheduler Queue"
            value={metrics.schedulerQueue.value}
            description={metrics.schedulerQueue.description}
            icon={<Clock className="h-4 w-4" />}
            trend={{ value: metrics.schedulerQueue.trend, isPositive: metrics.schedulerQueue.isPositive }}
          />
          <MetricCard
            title="Retry Queue"
            value={metrics.retryQueue.value}
            description={metrics.retryQueue.description}
            icon={<RefreshCw className="h-4 w-4" />}
            trend={{ value: metrics.retryQueue.trend, isPositive: metrics.retryQueue.isPositive }}
          />
          <MetricCard
            title="Avg Success Rate"
            value={metrics.successRate.value}
            description={metrics.successRate.description}
            icon={<CheckCircle className="h-4 w-4 text-green-500" />}
          />
          <MetricCard
            title="Failed Executions (24h)"
            value={metrics.failedExecutions24h.value}
            description={metrics.failedExecutions24h.description}
            icon={<AlertTriangle className="h-4 w-4 text-red-500" />}
            trend={{ value: metrics.failedExecutions24h.trend, isPositive: metrics.failedExecutions24h.isPositive }}
          />
          <MetricCard
            title="Avg Duration"
            value={metrics.avgDuration.value}
            description={metrics.avgDuration.description}
            icon={<Clock className="h-4 w-4" />}
          />
        </div>
      )}

      {/* 2. Dashboard Charts & Distribution */}
      <div className="grid gap-6 md:grid-cols-3">
        {/* Main Chart Panel */}
        <Card className="md:col-span-2">
          <CardHeader className="flex flex-row items-center justify-between">
            <div>
              <CardTitle className="text-md">Executions & Latency Trends</CardTitle>
            </div>
            <div className="flex space-x-1 bg-slate-950 p-0.5 rounded-md border border-slate-900">
              {(["hourly", "daily", "weekly"] as const).map((r) => (
                <button
                  key={r}
                  onClick={() => setRange(r)}
                  className={`text-[10px] font-bold px-2.5 py-1 rounded-sm uppercase transition-colors cursor-pointer ${
                    range === r ? "bg-slate-800 text-slate-100" : "text-slate-500 hover:text-slate-300"
                  }`}
                >
                  {r}
                </button>
              ))}
            </div>
          </CardHeader>
          <CardContent className="h-72">
            {chartLoading ? (
              <div className="flex h-full items-center justify-center">
                <Spinner />
              </div>
            ) : chartError || !chartData ? (
              <ErrorState message="Failed to load chart metrics." onRetry={refetchCharts} />
            ) : mounted ? (
              <ResponsiveContainer width="100%" height="100%">
                <AreaChart data={chartData}>
                  <defs>
                    <linearGradient id="successGrad" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="5%" stopColor="#8b5cf6" stopOpacity={0.2} />
                      <stop offset="95%" stopColor="#8b5cf6" stopOpacity={0} />
                    </linearGradient>
                  </defs>
                  <XAxis dataKey="label" stroke="#475569" fontSize={11} />
                  <YAxis stroke="#475569" fontSize={11} />
                  <Tooltip contentStyle={{ backgroundColor: "#020617", borderColor: "#1e293b" }} />
                  <Legend wrapperStyle={{ fontSize: 11 }} />
                  <Area
                    type="monotone"
                    name="Successes"
                    dataKey="success"
                    stroke="#8b5cf6"
                    fillOpacity={1}
                    fill="url(#successGrad)"
                  />
                  <Line type="monotone" name="Avg Latency (ms)" dataKey="durationMs" stroke="#3b82f6" strokeWidth={2} dot={false} />
                  <Line type="monotone" name="Retries" dataKey="retries" stroke="#f59e0b" strokeWidth={2} />
                </AreaChart>
              </ResponsiveContainer>
            ) : null}
          </CardContent>
        </Card>

        {/* Pie Chart Distribution Panel */}
        <Card>
          <CardHeader>
            <CardTitle className="text-md">HTTP Status Codes</CardTitle>
          </CardHeader>
          <CardContent className="h-72 flex flex-col justify-between">
            {httpDistLoading ? (
              <div className="flex h-full items-center justify-center">
                <Spinner />
              </div>
            ) : httpDistError || !httpDist ? (
              <ErrorState message="Failed to load HTTP status code distribution." onRetry={refetchHttpDist} />
            ) : mounted ? (
              <>
                <div className="h-44 w-full relative">
                  <ResponsiveContainer width="100%" height="100%">
                    <PieChart>
                      <Pie
                        data={httpDist}
                        cx="50%"
                        cy="50%"
                        innerRadius={50}
                        outerRadius={70}
                        paddingAngle={4}
                        dataKey="value"
                      >
                        {httpDist.map((entry, index) => (
                          <Cell key={`cell-${index}`} fill={entry.color} />
                        ))}
                      </Pie>
                      <Tooltip contentStyle={{ backgroundColor: "#020617", borderColor: "#1e293b", fontSize: 11 }} />
                    </PieChart>
                  </ResponsiveContainer>
                </div>
                <div className="grid grid-cols-2 gap-2 text-[11px] text-slate-400">
                  {httpDist.map((d, idx) => (
                    <div key={idx} className="flex items-center space-x-1.5">
                      <span className="h-2 w-2 rounded-full shrink-0" style={{ backgroundColor: d.color }} />
                      <span className="truncate">{d.name} ({d.value})</span>
                    </div>
                  ))}
                </div>
              </>
            ) : null}
          </CardContent>
        </Card>
      </div>

      {/* 3. System Diagnostics & scrolling Live Activities */}
      <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
        {/* System Health Panel */}
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle className="text-md">Cluster Health Overview</CardTitle>
          </CardHeader>
          <CardContent>
            {healthLoading ? (
              <div className="flex h-48 items-center justify-center">
                <Spinner />
              </div>
            ) : healthError || !health ? (
              <ErrorState message="Failed to load cluster health metrics." onRetry={refetchHealth} />
            ) : (
              <div className="divide-y divide-slate-850">
                <div className="grid grid-cols-4 gap-2 text-xs font-semibold text-slate-500 pb-2">
                  <span>Microservice</span>
                  <span>Health</span>
                  <span>Latency</span>
                  <span>Heartbeat / Uptime</span>
                </div>
                {Object.entries(health).map(([key, value]) => {
                  const label = key.replace(/([A-Z])/g, " $1").trim();
                  return (
                    <div key={key} className="grid grid-cols-4 gap-2 py-3 text-xs text-slate-300 items-center">
                      <span className="font-semibold text-slate-200 capitalize">{label}</span>
                      <span className="flex items-center space-x-1.5">
                        <span className={`h-2 w-2 rounded-full bg-current ${getStatusColor(value.status)}`} />
                        <span className="font-medium">{value.status}</span>
                      </span>
                      <span>{value.responseTimeMs > 0 ? `${value.responseTimeMs}ms` : "--"}</span>
                      <span className="text-slate-500 font-mono">
                        {value.lastHeartbeat !== "--" ? value.lastHeartbeat : value.uptime}
                      </span>
                    </div>
                  );
                })}
              </div>
            )}
          </CardContent>
        </Card>

        {/* Live Activity Feed */}
        <Card>
          <CardHeader>
            <CardTitle className="text-md">Live Activity Feed</CardTitle>
          </CardHeader>
          <CardContent className="h-[290px] overflow-y-auto pr-1">
            {activitiesLoading ? (
              <div className="flex h-full items-center justify-center">
                <Spinner />
              </div>
            ) : activitiesError || !activities ? (
              <ErrorState message="Failed to load live activity events feed." onRetry={refetchActivities} />
            ) : (
              <div className="relative border-l border-slate-800 pl-4 ml-2 space-y-4">
                {activities.map((act) => (
                  <div key={act.id} className="relative">
                    {/* Floating Icon */}
                    <span className="absolute -left-[25px] top-0.5 flex h-4 w-4 items-center justify-center rounded-full bg-slate-950 border border-slate-800 shrink-0">
                      {getActivityIcon(act.type)}
                    </span>
                    <div className="space-y-0.5">
                      <div className="flex justify-between items-center text-[10px] text-slate-500">
                        <span className="font-semibold text-purple-400 font-mono text-[9px] uppercase px-1 py-0.5 bg-slate-900 rounded-sm">
                          {act.tenantName}
                        </span>
                        <span>{act.timestamp}</span>
                      </div>
                      <p className="text-xs font-semibold text-slate-200 leading-tight">
                        {act.entityName}
                      </p>
                      <p className="text-[10px] text-slate-500 uppercase font-bold tracking-wide">
                        {act.type.replace("_", " ")}
                      </p>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
