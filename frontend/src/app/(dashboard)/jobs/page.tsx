"use client";

import * as React from "react";
import { useRouter } from "next/navigation";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { JobService, JobResponse } from "@/services/jobs";
import { ProjectService } from "@/services/projects";
import { EntityTable } from "@/components/crud/EntityTable";
import { EntityToolbar } from "@/components/crud/EntityToolbar";
import { BulkActionBar } from "@/components/crud/BulkActionBar";
import { Pagination } from "@/components/crud/Pagination";
import { Button } from "@/components/ui/Button";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { Plus, Trash2, Edit, Copy, Eye, Archive } from "lucide-react";
import Link from "next/link";
import { useAuth } from "@/providers/AuthProvider";

export default function JobsPage() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const { currentTenant } = useAuth();

  // Toolbar & filter states
  const [selectedIds, setSelectedIds] = React.useState<string[]>([]);
  const [search, setSearch] = React.useState("");
  const [selectedProject, setSelectedProject] = React.useState("");
  const [selectedMethod, setSelectedMethod] = React.useState("");
  const [selectedSchedule, setSelectedSchedule] = React.useState("");
  const [selectedStatus, setSelectedStatus] = React.useState("");
  const [currentPage, setCurrentPage] = React.useState(1);

  // Archive & Delete Dialog states
  const [isDeleteOpen, setIsDeleteOpen] = React.useState(false);
  const [jobToDelete, setJobToDelete] = React.useState<JobResponse | null>(null);

  // Fetch Projects to populate filters dropdown
  const { data: projects = [] } = useQuery({
    queryKey: ["projects", currentTenant?.id],
    queryFn: ProjectService.listProjects,
  });

  // Fetch Jobs
  const { data: jobs = [], isLoading, isError, refetch } = useQuery({
    queryKey: ["jobs", selectedProject],
    queryFn: () => JobService.listJobs(selectedProject || undefined),
  });

  // Mutators
  const deleteMutation = useMutation({
    mutationFn: JobService.deleteJob,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["jobs"] });
      setIsDeleteOpen(false);
      setSelectedIds([]);
    },
  });

  const duplicateMutation = useMutation({
    mutationFn: (job: JobResponse) =>
      JobService.createJob({
        projectPublicId: job.projectPublicId,
        name: `${job.name} (Copy)`,
        description: job.description,
        enabled: job.enabled,
        httpMethod: job.httpMethod,
        targetUrl: job.targetUrl,
        requestHeaders: job.requestHeaders,
        requestBody: job.requestBody,
        timeoutSeconds: job.timeoutSeconds,
        retryMaxAttempts: job.retryMaxAttempts,
        retryStrategy: job.retryStrategy,
        retryBaseDelaySeconds: job.retryBaseDelaySeconds,
        scheduleType: job.scheduleType,
        cronExpression: job.cronExpression,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["jobs"] });
    },
  });

  const archiveMutation = useMutation({
    mutationFn: (job: JobResponse) =>
      JobService.updateJob(job.publicId, { enabled: false }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["jobs"] });
    },
  });

  // Handlers
  const handleSelectRow = (id: string, selected: boolean) => {
    setSelectedIds((prev) =>
      selected ? [...prev, id] : prev.filter((x) => x !== id)
    );
  };

  const handleSelectAll = (selected: boolean) => {
    setSelectedIds(selected ? filteredJobs.map((j) => j.publicId) : []);
  };

  // Local filtering logic
  const filteredJobs = React.useMemo(() => {
    let list = jobs;
    if (search.trim()) {
      const q = search.toLowerCase();
      list = list.filter(
        (j) => j.name.toLowerCase().includes(q) || j.targetUrl.toLowerCase().includes(q)
      );
    }
    if (selectedMethod) {
      list = list.filter((j) => j.httpMethod === selectedMethod);
    }
    if (selectedSchedule) {
      list = list.filter((j) => j.scheduleType === selectedSchedule);
    }
    if (selectedStatus) {
      const isEnabled = selectedStatus === "ENABLED";
      list = list.filter((j) => j.enabled === isEnabled);
    }
    return list;
  }, [jobs, search, selectedMethod, selectedSchedule, selectedStatus]);

  const totalPages = Math.ceil(filteredJobs.length / 10) || 1;
  const paginatedJobs = React.useMemo(() => {
    const start = (currentPage - 1) * 10;
    return filteredJobs.slice(start, start + 10);
  }, [filteredJobs, currentPage]);

  const columns = [
    {
      key: "name",
      header: "Job Name",
      render: (item: JobResponse) => (
        <span className="font-bold text-slate-200">{item.name}</span>
      ),
    },
    {
      key: "httpMethod",
      header: "Method",
      render: (item: JobResponse) => (
        <span className="text-xs bg-slate-900 border border-slate-800 text-purple-400 font-mono px-2 py-0.5 rounded-sm">
          {item.httpMethod}
        </span>
      ),
    },
    { key: "targetUrl", header: "URL", className: "truncate max-w-[200px]" },
    {
      key: "enabled",
      header: "Status",
      render: (item: JobResponse) => (
        <StatusBadge status={item.enabled ? "ACTIVE" : "INACTIVE"} />
      ),
    },
    { key: "retryStrategy", header: "Retry" },
    { key: "scheduleType", header: "Schedule" },
    {
      key: "actions",
      header: "Actions",
      className: "text-right w-36",
      render: (item: JobResponse) => (
        <div className="flex items-center justify-end space-x-1">
          <Button
            variant="ghost"
            size="icon"
            onClick={() => router.push(`/jobs/editor?id=${item.publicId}`)}
            className="h-8 w-8 hover:bg-slate-800 text-slate-400"
          >
            <Edit className="h-4 w-4" />
          </Button>
          <Button
            variant="ghost"
            size="icon"
            onClick={() => duplicateMutation.mutate(item)}
            className="h-8 w-8 hover:bg-slate-800 text-slate-400"
          >
            <Copy className="h-4 w-4" />
          </Button>
          <Button
            variant="ghost"
            size="icon"
            onClick={() => archiveMutation.mutate(item)}
            className="h-8 w-8 hover:bg-slate-800 text-slate-400"
          >
            <Archive className="h-4 w-4" />
          </Button>
          <Button
            variant="ghost"
            size="icon"
            onClick={() => {
              setJobToDelete(item);
              setIsDeleteOpen(true);
            }}
            className="h-8 w-8 hover:bg-red-950/20 text-slate-400 hover:text-red-400"
          >
            <Trash2 className="h-4 w-4" />
          </Button>
        </div>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-100">Jobs</h1>
          <p className="text-sm text-slate-400">Configure HTTP requests, headers, and periodic triggers.</p>
        </div>
        <Button size="sm" onClick={() => router.push("/jobs/editor")} className="gap-1.5">
          <Plus className="h-4 w-4" />
          Create Job
        </Button>
      </div>

      <EntityToolbar
        searchPlaceholder="Search jobs by name or target URL..."
        searchValue={search}
        onSearchChange={setSearch}
        filters={[
          {
            key: "project",
            label: "Project",
            options: projects.map((p) => ({ value: p.publicId, label: p.name })),
          },
          {
            key: "method",
            label: "Method",
            options: [
              { value: "GET", label: "GET" },
              { value: "POST", label: "POST" },
              { value: "PUT", label: "PUT" },
              { value: "DELETE", label: "DELETE" },
            ],
          },
          {
            key: "schedule",
            label: "Schedule",
            options: [
              { value: "CRON", label: "Cron" },
              { value: "IMMEDIATE", label: "Immediate" },
            ],
          },
          {
            key: "status",
            label: "Status",
            options: [
              { value: "ENABLED", label: "Active" },
              { value: "DISABLED", label: "Inactive" },
            ],
          },
        ]}
        selectedFilters={{
          project: selectedProject,
          method: selectedMethod,
          schedule: selectedSchedule,
          status: selectedStatus,
        }}
        onFilterChange={(key, val) => {
          if (key === "project") setSelectedProject(val);
          if (key === "method") setSelectedMethod(val);
          if (key === "schedule") setSelectedSchedule(val);
          if (key === "status") setSelectedStatus(val);
        }}
      />

      <div className="bg-slate-900/10 border border-slate-900 rounded-lg p-1">
        <EntityTable
          columns={columns}
          data={paginatedJobs}
          idKey="publicId"
          selectedIds={selectedIds}
          onSelectRow={handleSelectRow}
          onSelectAll={handleSelectAll}
          isLoading={isLoading}
          emptyTitle="No jobs configured"
          emptyDescription="Configure a new HTTP integration endpoint by clicking 'Create Job'."
          mobileCardRender={(item) => (
            <div className="space-y-2 text-xs">
              <div className="flex justify-between items-center">
                <span className="font-bold text-slate-100">{item.name}</span>
                <span className="bg-slate-900 border border-slate-800 text-purple-400 font-mono px-2 py-0.5 rounded-sm">
                  {item.httpMethod}
                </span>
              </div>
              <p className="text-slate-400 truncate">{item.targetUrl}</p>
              <div className="flex justify-between items-center pt-2">
                <StatusBadge status={item.enabled ? "ACTIVE" : "INACTIVE"} />
                <div className="flex space-x-1">
                  <Button variant="ghost" size="icon" onClick={() => router.push(`/jobs/editor?id=${item.publicId}`)} className="h-8 w-8">
                    <Edit className="h-4 w-4" />
                  </Button>
                  <Button variant="ghost" size="icon" onClick={() => duplicateMutation.mutate(item)} className="h-8 w-8">
                    <Copy className="h-4 w-4" />
                  </Button>
                  <Button variant="ghost" size="icon" onClick={() => { setJobToDelete(item); setIsDeleteOpen(true); }} className="h-8 w-8 text-red-400">
                    <Trash2 className="h-4 w-4" />
                  </Button>
                </div>
              </div>
            </div>
          )}
        />
        <Pagination
          currentPage={currentPage}
          totalPages={totalPages}
          onPageChange={setCurrentPage}
        />
      </div>

      {/* Bulk Action Bar */}
      <BulkActionBar
        selectedCount={selectedIds.length}
        onClearSelection={() => setSelectedIds([])}
        actions={[
          {
            label: "Delete",
            variant: "destructive",
            icon: <Trash2 className="h-3.5 w-3.5" />,
            onClick: () => {
              if (confirm(`Are you sure you want to delete the ${selectedIds.length} selected jobs?`)) {
                selectedIds.forEach((id) => deleteMutation.mutate(id));
              }
            },
          },
        ]}
      />

      {/* Delete Confirmation Dialog */}
      <ConfirmDialog
        isOpen={isDeleteOpen}
        onClose={() => setIsDeleteOpen(false)}
        onConfirm={() => {
          if (jobToDelete) {
            deleteMutation.mutate(jobToDelete.publicId);
          }
        }}
        title="Delete Job Definition?"
        description={`This action will permanently delete the job "${jobToDelete?.name}". Active schedulers will cease execution.`}
        confirmText="Permanently Delete"
        isDestructive
      />
    </div>
  );
}
