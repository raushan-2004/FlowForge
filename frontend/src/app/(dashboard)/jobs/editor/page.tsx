"use client";

import * as React from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useForm, Controller } from "react-hook-form";
import { z } from "zod";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { JobService, JobResponse } from "@/services/jobs";
import { ProjectService } from "@/services/projects";
import { MonacoEditor } from "@/components/ui/MonacoEditor";
import { Button } from "@/components/ui/Button";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/Card";
import { ArrowLeft, Save, Plus, Trash2, Check, AlertCircle } from "lucide-react";
import { cn } from "@/lib/utils";

// Zod validation schema
const jobFormSchema = z.object({
  projectPublicId: z.string().uuid("Please select a valid project"),
  name: z.string().min(2, "Name must be at least 2 characters"),
  description: z.string().min(5, "Description must be at least 5 characters"),
  enabled: z.boolean(),
  httpMethod: z.string(),
  targetUrl: z.string().url("Please enter a valid target URL"),
  requestBody: z.string().optional(),
  timeoutSeconds: z.coerce.number().min(1).max(300),
  retryMaxAttempts: z.coerce.number().min(0).max(10),
  retryStrategy: z.string(),
  retryBaseDelaySeconds: z.coerce.number().min(1).max(3600),
  scheduleType: z.string(),
  cronExpression: z.string().optional(),
});

type JobFormValues = z.infer<typeof jobFormSchema>;

type Section = "general" | "request" | "headers" | "body" | "retry" | "scheduling" | "preview";

export default function JobEditorPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const queryClient = useQueryClient();

  const jobId = searchParams.get("id");
  const isEditing = !!jobId;

  const [activeSection, setActiveSection] = React.useState<Section>("general");
  const [headers, setHeaders] = React.useState<{ key: string; value: string }[]>([
    { key: "Content-Type", value: "application/json" },
  ]);

  // Fetch projects to populate dropdown
  const { data: projects = [] } = useQuery({
    queryKey: ["projects"],
    queryFn: ProjectService.listProjects,
  });

  // Fetch job to edit if isEditing
  const { data: job, isLoading } = useQuery({
    queryKey: ["job", jobId],
    queryFn: () => JobService.getJob(jobId!),
    enabled: isEditing,
  });

  const {
    register,
    handleSubmit,
    setValue,
    control,
    watch,
    reset,
    formState: { errors, isDirty },
  } = useForm<JobFormValues>({
    defaultValues: {
      projectPublicId: "",
      name: "",
      description: "",
      enabled: true,
      httpMethod: "POST",
      targetUrl: "https://",
      requestBody: "{\n  \"example\": \"value\"\n}",
      timeoutSeconds: 30,
      retryMaxAttempts: 3,
      retryStrategy: "EXPONENTIAL_BACKOFF",
      retryBaseDelaySeconds: 5,
      scheduleType: "CRON",
      cronExpression: "*/5 * * * *",
    },
  });

  // Populate values when job is loaded
  React.useEffect(() => {
    if (job) {
      reset({
        projectPublicId: job.projectPublicId,
        name: job.name,
        description: job.description,
        enabled: job.enabled,
        httpMethod: job.httpMethod,
        targetUrl: job.targetUrl,
        requestBody: job.requestBody,
        timeoutSeconds: job.timeoutSeconds,
        retryMaxAttempts: job.retryMaxAttempts,
        retryStrategy: job.retryStrategy,
        retryBaseDelaySeconds: job.retryBaseDelaySeconds,
        scheduleType: job.scheduleType,
        cronExpression: job.cronExpression,
      });

      // Parse headers
      if (job.requestHeaders) {
        setHeaders(
          Object.entries(job.requestHeaders).map(([key, value]) => ({ key, value }))
        );
      }
    }
  }, [job, reset]);

  // Handle unsaved changes warning
  React.useEffect(() => {
    const handleBeforeUnload = (e: BeforeUnloadEvent) => {
      if (isDirty) {
        e.preventDefault();
        e.returnValue = "You have unsaved changes. Are you sure you want to discard them?";
      }
    };
    window.addEventListener("beforeunload", handleBeforeUnload);
    return () => window.removeEventListener("beforeunload", handleBeforeUnload);
  }, [isDirty]);

  // Mutations
  const createMutation = useMutation({
    mutationFn: JobService.createJob,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["jobs"] });
      router.push("/jobs");
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: any }) => JobService.updateJob(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["jobs"] });
      router.push("/jobs");
    },
  });

  const onSubmit = (values: JobFormValues) => {
    // Map headers array to Record
    const headersRecord: Record<string, string> = {};
    headers.forEach((h) => {
      if (h.key.trim()) {
        headersRecord[h.key.trim()] = h.value;
      }
    });

    const payload = {
      ...values,
      requestHeaders: headersRecord,
      requestBody: values.requestBody || "",
      cronExpression: values.cronExpression || "",
    };

    if (isEditing) {
      updateMutation.mutate({ id: jobId!, data: payload });
    } else {
      createMutation.mutate(payload);
    }
  };

  // Headers handlers
  const addHeaderRow = () => {
    setHeaders((prev) => [...prev, { key: "", value: "" }]);
  };

  const removeHeaderRow = (index: number) => {
    setHeaders((prev) => prev.filter((_, idx) => idx !== index));
  };

  const updateHeaderKey = (index: number, val: string) => {
    setHeaders((prev) =>
      prev.map((h, idx) => (idx === index ? { ...h, key: val } : h))
    );
  };

  const updateHeaderVal = (index: number, val: string) => {
    setHeaders((prev) =>
      prev.map((h, idx) => (idx === index ? { ...h, value: val } : h))
    );
  };

  const sections: { key: Section; label: string }[] = [
    { key: "general", label: "General Settings" },
    { key: "request", label: "HTTP Request" },
    { key: "headers", label: "Headers Editor" },
    { key: "body", label: "JSON payload" },
    { key: "retry", label: "Retry Policy" },
    { key: "scheduling", label: "Scheduling" },
    { key: "preview", label: "Preview Run" },
  ];

  const watchedValues = watch();

  if (isLoading && isEditing) {
    return (
      <div className="flex h-64 items-center justify-center">
        <span className="text-sm text-slate-400 animate-pulse">Loading job config...</span>
      </div>
    );
  }

  return (
    <div className="space-y-6 max-w-4xl mx-auto">
      {/* Editor Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center space-x-3">
          <Button variant="outline" size="icon" onClick={() => router.push("/jobs")} className="border-slate-800 h-9 w-9">
            <ArrowLeft className="h-4 w-4" />
          </Button>
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-slate-100">
              {isEditing ? `Edit: ${job?.name}` : "Create Integration Job"}
            </h1>
            <p className="text-sm text-slate-400">Configure parameters for automated task dispatching.</p>
          </div>
        </div>
        <Button onClick={handleSubmit(onSubmit)} className="gap-1.5 shadow-lg shadow-purple-600/10">
          <Save className="h-4 w-4" />
          Save Config
        </Button>
      </div>

      <div className="grid gap-6 md:grid-cols-4">
        {/* Editor Sidebar Sections */}
        <div className="space-y-1">
          {sections.map((s) => (
            <button
              key={s.key}
              type="button"
              onClick={() => setActiveSection(s.key)}
              className={cn(
                "flex w-full items-center px-3 py-2 text-xs font-semibold rounded-md transition-colors text-left cursor-pointer",
                activeSection === s.key
                  ? "bg-purple-950/40 text-purple-400 border-l-2 border-purple-500 font-bold"
                  : "text-slate-400 hover:bg-slate-900 hover:text-slate-200"
              )}
            >
              {s.label}
            </button>
          ))}
        </div>

        {/* Section Content Panel */}
        <Card className="md:col-span-3 bg-slate-900/30 border-slate-900">
          <CardContent className="pt-6">
            {/* 1. General Section */}
            {activeSection === "general" && (
              <div className="space-y-4">
                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-slate-400">Project Workspace</label>
                  <select
                    {...register("projectPublicId")}
                    className="h-10 w-full rounded-md border border-slate-800 bg-slate-950 px-3 text-sm text-slate-200 cursor-pointer"
                  >
                    <option value="">Select Project Workspace</option>
                    {projects.map((p) => (
                      <option key={p.publicId} value={p.publicId}>
                        {p.name}
                      </option>
                    ))}
                  </select>
                  {errors.projectPublicId && <p className="text-xs text-red-400">{errors.projectPublicId.message}</p>}
                </div>

                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-slate-400">Job Title</label>
                  <input
                    type="text"
                    placeholder="e.g. Sync Stripe Invoices"
                    {...register("name")}
                    className="h-10 w-full rounded-md border border-slate-800 bg-slate-950 px-3 text-sm text-slate-200 focus:ring-2 focus:ring-purple-500"
                  />
                  {errors.name && <p className="text-xs text-red-400">{errors.name.message}</p>}
                </div>

                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-slate-400">Description</label>
                  <textarea
                    placeholder="Provide details about endpoints triggered by this job..."
                    rows={4}
                    {...register("description")}
                    className="w-full rounded-md border border-slate-800 bg-slate-950 p-3 text-sm text-slate-200 focus:ring-2 focus:ring-purple-500"
                  />
                  {errors.description && <p className="text-xs text-red-400">{errors.description.message}</p>}
                </div>

                <div className="flex items-center space-x-2">
                  <input
                    type="checkbox"
                    id="enabled"
                    {...register("enabled")}
                    className="rounded-sm border-slate-800 bg-slate-950 text-purple-600 focus:ring-purple-500 h-4 w-4"
                  />
                  <label htmlFor="enabled" className="text-xs font-semibold text-slate-400 select-none cursor-pointer">
                    Enable active scheduling pipeline
                  </label>
                </div>
              </div>
            )}

            {/* 2. HTTP Request Section */}
            {activeSection === "request" && (
              <div className="space-y-4">
                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-slate-400">HTTP Method</label>
                  <select
                    {...register("httpMethod")}
                    className="h-10 w-full rounded-md border border-slate-800 bg-slate-950 px-3 text-sm text-slate-200 cursor-pointer"
                  >
                    <option value="GET">GET</option>
                    <option value="POST">POST</option>
                    <option value="PUT">PUT</option>
                    <option value="PATCH">PATCH</option>
                    <option value="DELETE">DELETE</option>
                  </select>
                </div>

                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-slate-400">Target URL</label>
                  <input
                    type="text"
                    placeholder="https://api.domain.com/v1/trigger"
                    {...register("targetUrl")}
                    className="h-10 w-full rounded-md border border-slate-800 bg-slate-950 px-3 text-sm text-slate-200 focus:ring-2 focus:ring-purple-500 font-mono"
                  />
                  {errors.targetUrl && <p className="text-xs text-red-400">{errors.targetUrl.message}</p>}
                </div>

                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-slate-400">Timeout (Seconds)</label>
                  <input
                    type="number"
                    {...register("timeoutSeconds")}
                    className="h-10 w-full rounded-md border border-slate-800 bg-slate-950 px-3 text-sm text-slate-200 focus:ring-2 focus:ring-purple-500"
                  />
                  {errors.timeoutSeconds && <p className="text-xs text-red-400">{errors.timeoutSeconds.message}</p>}
                </div>
              </div>
            )}

            {/* 3. Headers Editor Section */}
            {activeSection === "headers" && (
              <div className="space-y-4">
                <div className="flex justify-between items-center pb-2 border-b border-slate-900">
                  <h3 className="text-xs font-semibold text-slate-400">Request Headers</h3>
                  <Button type="button" onClick={addHeaderRow} size="sm" variant="outline" className="h-8 border-slate-800 text-xs gap-1">
                    <Plus className="h-3 w-3" />
                    Add Header
                  </Button>
                </div>
                {headers.length === 0 ? (
                  <p className="text-xs text-slate-500 py-4 text-center">No headers configured. Default application headers will be sent.</p>
                ) : (
                  <div className="space-y-2 max-h-72 overflow-y-auto">
                    {headers.map((h, idx) => (
                      <div key={idx} className="flex items-center space-x-2">
                        <input
                          type="text"
                          placeholder="Header Key"
                          value={h.key}
                          onChange={(e) => updateHeaderKey(idx, e.target.value)}
                          className="h-9 flex-1 rounded-md border border-slate-800 bg-slate-950 px-3 text-xs text-slate-200 font-mono"
                        />
                        <input
                          type="text"
                          placeholder="Header Value"
                          value={h.value}
                          onChange={(e) => updateHeaderVal(idx, e.target.value)}
                          className="h-9 flex-1 rounded-md border border-slate-800 bg-slate-950 px-3 text-xs text-slate-200 font-mono"
                        />
                        <Button
                          type="button"
                          variant="ghost"
                          onClick={() => removeHeaderRow(idx)}
                          className="h-9 w-9 p-0 hover:bg-slate-800 text-slate-500 hover:text-red-400"
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}

            {/* 4. JSON payload Section */}
            {activeSection === "body" && (
              <div className="space-y-4">
                <label className="text-xs font-semibold text-slate-400">Request Body Payload (JSON)</label>
                <Controller
                  name="requestBody"
                  control={control}
                  render={({ field }) => (
                    <MonacoEditor
                      value={field.value || ""}
                      onChange={field.onChange}
                      className="border border-slate-800 rounded-md overflow-hidden"
                    />
                  )}
                />
              </div>
            )}

            {/* 5. Retry Policy Section */}
            {activeSection === "retry" && (
              <div className="space-y-4">
                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-slate-400">Retry Strategy</label>
                  <select
                    {...register("retryStrategy")}
                    className="h-10 w-full rounded-md border border-slate-800 bg-slate-950 px-3 text-sm text-slate-200 cursor-pointer"
                  >
                    <option value="EXPONENTIAL_BACKOFF">Exponential Backoff</option>
                    <option value="FIXED_DELAY">Fixed Delay</option>
                    <option value="LINEAR_BACKOFF">Linear Backoff</option>
                  </select>
                </div>

                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-slate-400">Max Retry Attempts</label>
                  <input
                    type="number"
                    {...register("retryMaxAttempts")}
                    className="h-10 w-full rounded-md border border-slate-800 bg-slate-950 px-3 text-sm text-slate-200"
                  />
                  {errors.retryMaxAttempts && <p className="text-xs text-red-400">{errors.retryMaxAttempts.message}</p>}
                </div>

                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-slate-400">Base Delay (Seconds)</label>
                  <input
                    type="number"
                    {...register("retryBaseDelaySeconds")}
                    className="h-10 w-full rounded-md border border-slate-800 bg-slate-950 px-3 text-sm text-slate-200"
                  />
                  {errors.retryBaseDelaySeconds && <p className="text-xs text-red-400">{errors.retryBaseDelaySeconds.message}</p>}
                </div>
              </div>
            )}

            {/* 6. Scheduling Section */}
            {activeSection === "scheduling" && (
              <div className="space-y-4">
                <div className="space-y-1.5">
                  <label className="text-xs font-semibold text-slate-400">Schedule Type</label>
                  <select
                    {...register("scheduleType")}
                    className="h-10 w-full rounded-md border border-slate-800 bg-slate-950 px-3 text-sm text-slate-200 cursor-pointer"
                  >
                    <option value="CRON">Cron Expression</option>
                    <option value="IMMEDIATE">Immediate dispatch</option>
                  </select>
                </div>

                {watchedValues.scheduleType === "CRON" && (
                  <div className="space-y-3">
                    <div className="space-y-1.5">
                      <label className="text-xs font-semibold text-slate-400">Cron Expression</label>
                      <input
                        type="text"
                        placeholder="*/5 * * * *"
                        {...register("cronExpression")}
                        className="h-10 w-full rounded-md border border-slate-800 bg-slate-950 px-3 text-sm text-slate-200 font-mono"
                      />
                      {errors.cronExpression && <p className="text-xs text-red-400">{errors.cronExpression.message}</p>}
                    </div>
                    {/* Cron Preview Placeholder */}
                    <div className="p-3 bg-slate-950 border border-slate-900 rounded-md text-xs text-slate-400 flex items-center space-x-2">
                      <Check className="h-4 w-4 text-green-500" />
                      <span>Cron triggers match standard Unix interval mappings (runs every 5 minutes).</span>
                    </div>
                  </div>
                )}
              </div>
            )}

            {/* 7. Preview Run Section */}
            {activeSection === "preview" && (
              <div className="space-y-4">
                <h3 className="text-sm font-semibold text-slate-300">HTTP Configuration Summary</h3>
                <div className="p-4 rounded-lg bg-slate-950 border border-slate-900 space-y-3 font-mono text-xs text-slate-300">
                  <div>
                    <span className="text-purple-400 font-bold">{watchedValues.httpMethod}</span>{" "}
                    <span className="text-slate-100">{watchedValues.targetUrl}</span>
                  </div>
                  <div>
                    <span className="text-slate-500">// Headers:</span>
                    {headers.map((h, i) => (
                      <div key={i} className="pl-4">
                        <span className="text-blue-400">"{h.key}"</span>: <span className="text-green-400">"{h.value}"</span>
                      </div>
                    ))}
                  </div>
                  <div>
                    <span className="text-slate-500">// Body:</span>
                    <pre className="pl-4 text-slate-400 whitespace-pre-wrap max-h-40 overflow-y-auto">
                      {watchedValues.requestBody}
                    </pre>
                  </div>
                </div>
                <div className="flex items-center space-x-2 p-3 bg-blue-950/20 border border-blue-900/50 rounded-md text-xs text-blue-300">
                  <AlertCircle className="h-4 w-4 shrink-0" />
                  <span>Verify request payloads to prevent target endpoint parsing or serialization failures.</span>
                </div>
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
