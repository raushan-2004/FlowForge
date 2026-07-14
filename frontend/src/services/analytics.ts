// Analytics, Trace, Metrics, and Report services

// ─── Time Range ──────────────────────────────────────────────────────────────

export type TimeRange = "1h" | "24h" | "7d" | "30d" | "custom";

export interface TimeRangeConfig {
  label: string;
  value: TimeRange;
}

export const TIME_RANGES: TimeRangeConfig[] = [
  { label: "Last Hour", value: "1h" },
  { label: "Last 24 Hours", value: "24h" },
  { label: "Last 7 Days", value: "7d" },
  { label: "Last 30 Days", value: "30d" },
];

// ─── Analytics Cards ──────────────────────────────────────────────────────────

export interface AnalyticsCard {
  id: string;
  title: string;
  value: string | number;
  trend: number;
  trendLabel: string;
  isPositiveTrend: boolean;
  tooltip: string;
}

// ─── Chart Types ──────────────────────────────────────────────────────────────

export interface ExecutionTrendPoint {
  label: string;
  success: number;
  failure: number;
  total: number;
  retries: number;
  avgDurationMs: number;
}

export interface WorkerUtilizationPoint {
  label: string;
  utilization: number;
  executions: number;
  failures: number;
}

export interface QueueGrowthPoint {
  label: string;
  pending: number;
  processing: number;
  dead: number;
}

export interface HttpStatusPoint {
  name: string;
  value: number;
  color: string;
}

export interface HeatmapCell {
  hour: number;
  day: string;
  value: number;
}

export interface DurationBucketPoint {
  range: string;
  count: number;
}

// ─── Workflow Analytics ───────────────────────────────────────────────────────

export interface WorkflowStat {
  id: string;
  name: string;
  executions: number;
  successRate: number;
  avgDurationMs: number;
  retryRate: number;
  failureRate: number;
  lastRun: string;
}

// ─── Worker Analytics ─────────────────────────────────────────────────────────

export interface WorkerStat {
  id: string;
  name: string;
  utilization: number;
  executionCount: number;
  leaseCount: number;
  avgDurationMs: number;
  failureRate: number;
  status: "ONLINE" | "OFFLINE" | "DRAINING";
}

// ─── Retry Analytics ──────────────────────────────────────────────────────────

export interface RetryReasonPoint {
  reason: string;
  count: number;
  pct: number;
  color: string;
}

export interface RetryTrendPoint {
  label: string;
  retries: number;
  exhausted: number;
  avgDelay: number;
}

// ─── Trace ───────────────────────────────────────────────────────────────────

export interface TraceSpan {
  spanId: string;
  parentSpanId?: string;
  service: string;
  operation: string;
  startOffsetMs: number;
  durationMs: number;
  status: "OK" | "ERROR" | "UNSET";
  attributes: Record<string, string>;
}

export interface Trace {
  traceId: string;
  executionId?: string;
  workflowRunId?: string;
  workerId?: string;
  startedAt: string;
  totalDurationMs: number;
  spans: TraceSpan[];
}

// ─── Metrics Explorer ─────────────────────────────────────────────────────────

export interface MetricItem {
  name: string;
  description: string;
  value: number;
  unit: string;
  labels: Record<string, string>;
  type: "GAUGE" | "COUNTER" | "HISTOGRAM";
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

function delay(ms: number) {
  return new Promise<void>((r) => setTimeout(r, ms));
}

function getLabels(range: TimeRange): string[] {
  switch (range) {
    case "1h":  return ["10m", "20m", "30m", "40m", "50m", "60m"];
    case "24h": return ["00h", "03h", "06h", "09h", "12h", "15h", "18h", "21h", "24h"];
    case "7d":  return ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];
    case "30d": return ["Wk 1", "Wk 2", "Wk 3", "Wk 4"];
    default:    return ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];
  }
}

function baseVolume(range: TimeRange): number {
  switch (range) {
    case "1h":  return 15;
    case "24h": return 90;
    case "7d":  return 380;
    case "30d": return 1800;
    default:    return 380;
  }
}

// ─── Service ──────────────────────────────────────────────────────────────────

export const AnalyticsService = {
  async getSummaryCards(range: TimeRange): Promise<AnalyticsCard[]> {
    await delay(250);
    const mult = range === "1h" ? 0.04 : range === "24h" ? 1 : range === "7d" ? 7 : 30;
    return [
      {
        id: "total-executions",
        title: "Total Executions",
        value: Math.round(2847 * mult).toLocaleString(),
        trend: 12.3,
        trendLabel: "vs previous period",
        isPositiveTrend: true,
        tooltip: "Total number of job executions dispatched in the selected period.",
      },
      {
        id: "success-rate",
        title: "Success Rate",
        value: "98.7%",
        trend: 0.4,
        trendLabel: "vs previous period",
        isPositiveTrend: true,
        tooltip: "Percentage of executions that completed successfully.",
      },
      {
        id: "failure-rate",
        title: "Failure Rate",
        value: "1.3%",
        trend: 0.4,
        trendLabel: "vs previous period",
        isPositiveTrend: false,
        tooltip: "Percentage of executions that failed after all retries.",
      },
      {
        id: "avg-duration",
        title: "Avg Duration",
        value: "452ms",
        trend: 5.2,
        trendLabel: "faster vs previous period",
        isPositiveTrend: true,
        tooltip: "Average end-to-end execution duration including HTTP dispatch.",
      },
      {
        id: "retry-rate",
        title: "Retry Rate",
        value: "3.1%",
        trend: 0.8,
        trendLabel: "vs previous period",
        isPositiveTrend: false,
        tooltip: "Percentage of executions that were retried at least once.",
      },
      {
        id: "active-workers",
        title: "Active Workers",
        value: 12,
        trend: 0,
        trendLabel: "no change",
        isPositiveTrend: true,
        tooltip: "Number of worker nodes currently connected and polling.",
      },
      {
        id: "queue-growth",
        title: "Queue Growth",
        value: "+0.8%",
        trend: 0.8,
        trendLabel: "pending increase",
        isPositiveTrend: false,
        tooltip: "Rate at which the pending queue is growing relative to processing.",
      },
      {
        id: "throughput",
        title: "Throughput",
        value: `${Math.round(118 * mult)}/hr`,
        trend: 8.5,
        trendLabel: "vs previous period",
        isPositiveTrend: true,
        tooltip: "Average number of executions completed per hour.",
      },
    ];
  },

  async getExecutionTrend(range: TimeRange): Promise<ExecutionTrendPoint[]> {
    await delay(300);
    const labels = getLabels(range);
    const base = baseVolume(range);
    return labels.map((label) => {
      const noise = Math.random() * 0.4 - 0.2;
      const total = Math.round(base * (1 + noise));
      const failure = Math.round(total * (0.008 + Math.random() * 0.015));
      return {
        label,
        total,
        success: total - failure,
        failure,
        retries: Math.round(total * (0.02 + Math.random() * 0.03)),
        avgDurationMs: Math.round(390 + Math.random() * 200),
      };
    });
  },

  async getWorkerUtilization(range: TimeRange): Promise<WorkerUtilizationPoint[]> {
    await delay(280);
    return getLabels(range).map((label) => ({
      label,
      utilization: Math.round(30 + Math.random() * 55),
      executions: Math.round(50 + Math.random() * 150),
      failures: Math.round(Math.random() * 5),
    }));
  },

  async getQueueGrowth(range: TimeRange): Promise<QueueGrowthPoint[]> {
    await delay(260);
    return getLabels(range).map((label) => ({
      label,
      pending: Math.round(2 + Math.random() * 12),
      processing: Math.round(1 + Math.random() * 8),
      dead: Math.round(Math.random() * 2),
    }));
  },

  async getHttpStatusDistribution(): Promise<HttpStatusPoint[]> {
    await delay(220);
    return [
      { name: "200 OK", value: 1250, color: "#10b981" },
      { name: "201 Created", value: 450, color: "#34d399" },
      { name: "302 Found", value: 120, color: "#3b82f6" },
      { name: "400 Bad Req", value: 25, color: "#f59e0b" },
      { name: "404 Not Found", value: 18, color: "#f97316" },
      { name: "500 Error", value: 9, color: "#ef4444" },
    ];
  },

  async getDurationDistribution(): Promise<DurationBucketPoint[]> {
    await delay(230);
    return [
      { range: "<100ms", count: 312 },
      { range: "100–250ms", count: 680 },
      { range: "250–500ms", count: 945 },
      { range: "500ms–1s", count: 512 },
      { range: "1–2s", count: 210 },
      { range: "2–5s", count: 88 },
      { range: ">5s", count: 24 },
    ];
  },

  async getHeatmap(): Promise<HeatmapCell[]> {
    await delay(200);
    const days = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"];
    const cells: HeatmapCell[] = [];
    for (const day of days) {
      for (let hour = 0; hour < 24; hour++) {
        const isWorkHour = hour >= 8 && hour <= 18;
        const isWeekend = day === "Sat" || day === "Sun";
        cells.push({
          hour,
          day,
          value:
            isWorkHour && !isWeekend
              ? Math.round(20 + Math.random() * 80)
              : Math.round(Math.random() * 20),
        });
      }
    }
    return cells;
  },

  async getWorkflowStats(): Promise<WorkflowStat[]> {
    await delay(300);
    return [
      { id: "wf-1", name: "Daily ETL Pipeline", executions: 1420, successRate: 99.2, avgDurationMs: 342, retryRate: 1.1, failureRate: 0.8, lastRun: "2 min ago" },
      { id: "wf-2", name: "Webhook Dispatcher", executions: 892, successRate: 97.8, avgDurationMs: 210, retryRate: 3.4, failureRate: 2.2, lastRun: "30s ago" },
      { id: "wf-3", name: "Invoice Processor", executions: 655, successRate: 98.9, avgDurationMs: 880, retryRate: 2.1, failureRate: 1.1, lastRun: "5 min ago" },
      { id: "wf-4", name: "Notification Sender", executions: 2140, successRate: 99.6, avgDurationMs: 145, retryRate: 0.5, failureRate: 0.4, lastRun: "12s ago" },
      { id: "wf-5", name: "Data Sync Job", executions: 408, successRate: 94.3, avgDurationMs: 1820, retryRate: 8.2, failureRate: 5.7, lastRun: "1 hr ago" },
      { id: "wf-6", name: "Report Generator", executions: 186, successRate: 96.8, avgDurationMs: 3240, retryRate: 4.8, failureRate: 3.2, lastRun: "3 hr ago" },
    ];
  },

  async getWorkerStats(): Promise<WorkerStat[]> {
    await delay(280);
    return [
      { id: "w-1", name: "worker-node-01", utilization: 78, executionCount: 412, leaseCount: 445, avgDurationMs: 380, failureRate: 0.7, status: "ONLINE" },
      { id: "w-2", name: "worker-node-02", utilization: 62, executionCount: 321, leaseCount: 335, avgDurationMs: 420, failureRate: 1.2, status: "ONLINE" },
      { id: "w-3", name: "worker-node-03", utilization: 45, executionCount: 198, leaseCount: 210, avgDurationMs: 390, failureRate: 0.5, status: "ONLINE" },
      { id: "w-4", name: "worker-node-04", utilization: 91, executionCount: 510, leaseCount: 525, avgDurationMs: 445, failureRate: 2.1, status: "ONLINE" },
      { id: "w-5", name: "worker-node-05", utilization: 0, executionCount: 0, leaseCount: 0, avgDurationMs: 0, failureRate: 0, status: "OFFLINE" },
      { id: "w-6", name: "worker-node-06", utilization: 15, executionCount: 89, leaseCount: 92, avgDurationMs: 355, failureRate: 0.3, status: "DRAINING" },
    ];
  },

  async getRetryReasons(): Promise<RetryReasonPoint[]> {
    await delay(240);
    return [
      { reason: "HTTP 500 Server Error", count: 142, pct: 38.5, color: "#ef4444" },
      { reason: "Connection Timeout", count: 98, pct: 26.6, color: "#f97316" },
      { reason: "HTTP 429 Rate Limit", count: 61, pct: 16.5, color: "#f59e0b" },
      { reason: "DNS Resolution Failed", count: 34, pct: 9.2, color: "#8b5cf6" },
      { reason: "SSL Certificate Error", count: 22, pct: 6.0, color: "#3b82f6" },
      { reason: "Other", count: 12, pct: 3.2, color: "#64748b" },
    ];
  },

  async getRetryTrend(range: TimeRange): Promise<RetryTrendPoint[]> {
    await delay(260);
    return getLabels(range).map((label) => ({
      label,
      retries: Math.round(8 + Math.random() * 20),
      exhausted: Math.round(Math.random() * 3),
      avgDelay: Math.round(30 + Math.random() * 90),
    }));
  },
};

// ─── Trace Service ────────────────────────────────────────────────────────────

const MOCK_TRACES: Trace[] = [
  {
    traceId: "trace-abc123def456",
    executionId: "exec-001",
    workerId: "worker-node-01",
    startedAt: new Date(Date.now() - 120000).toISOString(),
    totalDurationMs: 452,
    spans: [
      { spanId: "span-1", service: "api-service", operation: "POST /api/v1/executions", startOffsetMs: 0, durationMs: 12, status: "OK", attributes: { "http.method": "POST", "http.status_code": "202" } },
      { spanId: "span-2", parentSpanId: "span-1", service: "scheduler-service", operation: "scheduleJob", startOffsetMs: 15, durationMs: 8, status: "OK", attributes: { "job.id": "job-001" } },
      { spanId: "span-3", parentSpanId: "span-2", service: "worker-service", operation: "leaseExecution", startOffsetMs: 28, durationMs: 6, status: "OK", attributes: { "worker.id": "worker-node-01" } },
      { spanId: "span-4", parentSpanId: "span-3", service: "worker-service", operation: "httpDispatch", startOffsetMs: 38, durationMs: 380, status: "OK", attributes: { "http.url": "https://api.example.com/webhook", "http.status_code": "200" } },
      { spanId: "span-5", parentSpanId: "span-4", service: "result-processor", operation: "processResult", startOffsetMs: 425, durationMs: 22, status: "OK", attributes: {} },
    ],
  },
  {
    traceId: "trace-xyz789ghi012",
    executionId: "exec-002",
    workerId: "worker-node-02",
    startedAt: new Date(Date.now() - 300000).toISOString(),
    totalDurationMs: 1840,
    spans: [
      { spanId: "span-a", service: "api-service", operation: "POST /api/v1/executions", startOffsetMs: 0, durationMs: 14, status: "OK", attributes: { "http.method": "POST" } },
      { spanId: "span-b", parentSpanId: "span-a", service: "scheduler-service", operation: "scheduleJob", startOffsetMs: 18, durationMs: 9, status: "OK", attributes: {} },
      { spanId: "span-c", parentSpanId: "span-b", service: "worker-service", operation: "httpDispatch", startOffsetMs: 35, durationMs: 1750, status: "ERROR", attributes: { "http.url": "https://slow-api.example.com/process", "error": "timeout" } },
      { spanId: "span-d", parentSpanId: "span-c", service: "result-processor", operation: "processResult", startOffsetMs: 1800, durationMs: 38, status: "ERROR", attributes: { "error.type": "TIMEOUT" } },
    ],
  },
  {
    traceId: "trace-mnp345qrs678",
    executionId: "exec-003",
    workerId: "worker-node-04",
    startedAt: new Date(Date.now() - 600000).toISOString(),
    totalDurationMs: 210,
    spans: [
      { spanId: "span-x", service: "api-service", operation: "POST /api/v1/executions", startOffsetMs: 0, durationMs: 10, status: "OK", attributes: {} },
      { spanId: "span-y", parentSpanId: "span-x", service: "worker-service", operation: "httpDispatch", startOffsetMs: 22, durationMs: 168, status: "OK", attributes: { "http.status_code": "201" } },
      { spanId: "span-z", parentSpanId: "span-y", service: "result-processor", operation: "processResult", startOffsetMs: 198, durationMs: 12, status: "OK", attributes: {} },
    ],
  },
];

export const TraceService = {
  async searchTraces(query: string): Promise<Trace[]> {
    await delay(300);
    if (!query) return MOCK_TRACES;
    const q = query.toLowerCase();
    return MOCK_TRACES.filter(
      (t) =>
        t.traceId.includes(q) ||
        t.executionId?.includes(q) ||
        t.workerId?.includes(q) ||
        t.workflowRunId?.includes(q)
    );
  },

  async getTrace(traceId: string): Promise<Trace | undefined> {
    await delay(200);
    return MOCK_TRACES.find((t) => t.traceId === traceId);
  },
};

// ─── Metrics Service ──────────────────────────────────────────────────────────

const MOCK_METRICS: MetricItem[] = [
  { name: "flowforge_executions_total", description: "Total number of executions processed", value: 142502, unit: "count", labels: { service: "api-service" }, type: "COUNTER" },
  { name: "flowforge_executions_success_total", description: "Successfully completed executions", value: 140653, unit: "count", labels: { service: "api-service" }, type: "COUNTER" },
  { name: "flowforge_executions_failed_total", description: "Failed executions", value: 1849, unit: "count", labels: { service: "api-service" }, type: "COUNTER" },
  { name: "flowforge_execution_duration_ms", description: "Execution duration in milliseconds", value: 452, unit: "ms", labels: { quantile: "p50" }, type: "HISTOGRAM" },
  { name: "flowforge_execution_duration_p95_ms", description: "95th percentile execution duration", value: 1240, unit: "ms", labels: { quantile: "p95" }, type: "HISTOGRAM" },
  { name: "flowforge_workers_active", description: "Number of active worker nodes", value: 12, unit: "count", labels: { region: "us-east-1" }, type: "GAUGE" },
  { name: "flowforge_workers_online", description: "Workers reporting as online", value: 10, unit: "count", labels: {}, type: "GAUGE" },
  { name: "flowforge_scheduler_queue_depth", description: "Current depth of the scheduler queue", value: 8, unit: "count", labels: { queue: "default" }, type: "GAUGE" },
  { name: "flowforge_retry_queue_depth", description: "Current depth of the retry queue", value: 2, unit: "count", labels: { queue: "retry" }, type: "GAUGE" },
  { name: "flowforge_http_requests_total", description: "Total HTTP requests dispatched", value: 1872, unit: "count", labels: { method: "POST" }, type: "COUNTER" },
  { name: "flowforge_http_2xx_total", description: "Successful HTTP 2xx responses", value: 1720, unit: "count", labels: { status_class: "2xx" }, type: "COUNTER" },
  { name: "flowforge_http_5xx_total", description: "Server error HTTP 5xx responses", value: 9, unit: "count", labels: { status_class: "5xx" }, type: "COUNTER" },
  { name: "flowforge_scheduler_cycle_duration_ms", description: "Scheduler database scan cycle time", value: 1180, unit: "ms", labels: { service: "scheduler-service" }, type: "GAUGE" },
  { name: "flowforge_retries_total", description: "Total retry attempts", value: 4410, unit: "count", labels: {}, type: "COUNTER" },
  { name: "flowforge_retries_exhausted_total", description: "Executions where retries were exhausted", value: 142, unit: "count", labels: {}, type: "COUNTER" },
  { name: "jvm_memory_used_bytes", description: "JVM heap memory used", value: 512000000, unit: "bytes", labels: { area: "heap" }, type: "GAUGE" },
  { name: "jvm_threads_live_threads", description: "Current live thread count", value: 42, unit: "count", labels: { service: "api-service" }, type: "GAUGE" },
  { name: "db_pool_active_connections", description: "Active HikariCP database connections", value: 5, unit: "count", labels: { pool: "HikariPool-1" }, type: "GAUGE" },
];

export const MetricsService = {
  async listMetrics(filter?: string): Promise<MetricItem[]> {
    await delay(200);
    if (!filter) return MOCK_METRICS;
    const f = filter.toLowerCase();
    return MOCK_METRICS.filter(
      (m) =>
        m.name.toLowerCase().includes(f) ||
        m.description.toLowerCase().includes(f) ||
        Object.values(m.labels).some((v) => v.toLowerCase().includes(f))
    );
  },
};

// ─── Report Service ───────────────────────────────────────────────────────────

export interface ReportConfig {
  range: TimeRange;
  format: "csv" | "json";
  projectId?: string;
  workflowId?: string;
}

export const ReportService = {
  async exportReport(config: ReportConfig): Promise<void> {
    await delay(500);

    const rows = [
      { timestamp: "2026-07-14T10:00:00Z", executions: 122, success: 120, failures: 2, avgDurationMs: 420, retries: 1 },
      { timestamp: "2026-07-14T11:00:00Z", executions: 149, success: 144, failures: 5, avgDurationMs: 435, retries: 3 },
      { timestamp: "2026-07-14T12:00:00Z", executions: 161, success: 160, failures: 1, avgDurationMs: 412, retries: 0 },
      { timestamp: "2026-07-14T13:00:00Z", executions: 133, success: 130, failures: 3, avgDurationMs: 458, retries: 2 },
      { timestamp: "2026-07-14T14:00:00Z", executions: 187, success: 185, failures: 2, avgDurationMs: 402, retries: 1 },
    ];

    const generatedAt = new Date().toISOString();
    let content = "";
    let filename = `flowforge-report-${generatedAt}.`;

    if (config.format === "csv") {
      filename += "csv";
      const headers = Object.keys(rows[0]).join(",");
      const dataRows = rows.map((r) => Object.values(r).join(",")).join("\n");
      content = `# FlowForge Execution Report\n# Generated: ${generatedAt}\n# Range: ${config.range}\n\n${headers}\n${dataRows}`;
    } else {
      filename += "json";
      content = JSON.stringify({ generatedAt, range: config.range, data: rows }, null, 2);
    }

    const blob = new Blob([content], { type: config.format === "csv" ? "text/csv" : "application/json" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
  },
};
