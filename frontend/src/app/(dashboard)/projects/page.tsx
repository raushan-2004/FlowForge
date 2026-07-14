"use client";

import * as React from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { ProjectService, ProjectResponse } from "@/services/projects";
import { EntityTable } from "@/components/crud/EntityTable";
import { EntityToolbar } from "@/components/crud/EntityToolbar";
import { BulkActionBar } from "@/components/crud/BulkActionBar";
import { Pagination } from "@/components/crud/Pagination";
import { Button } from "@/components/ui/Button";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { Sheet, SheetHeader, SheetTitle, SheetDescription } from "@/components/ui/Sheet";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { Plus, Trash2, Edit, ExternalLink, AlertTriangle } from "lucide-react";
import Link from "next/link";
import { useAuth } from "@/providers/AuthProvider";

// Zod schema for Create/Edit project form
const projectFormSchema = z.object({
  name: z.string().min(2, "Name must be at least 2 characters"),
  description: z.string().min(5, "Description must be at least 5 characters"),
  status: z.string().optional(),
});

type ProjectFormValues = z.infer<typeof projectFormSchema>;

export default function ProjectsPage() {
  const queryClient = useQueryClient();
  const { currentTenant } = useAuth();

  // Selected state for bulk actions
  const [selectedIds, setSelectedIds] = React.useState<string[]>([]);
  const [search, setSearch] = React.useState("");
  const [statusFilter, setStatusFilter] = React.useState("");
  const [currentPage, setCurrentPage] = React.useState(1);

  // Edit/Create Sheet Dialog state
  const [isSheetOpen, setIsSheetOpen] = React.useState(false);
  const [editingProject, setEditingProject] = React.useState<ProjectResponse | null>(null);

  // Delete dialog state
  const [isDeleteOpen, setIsDeleteOpen] = React.useState(false);
  const [projectToDelete, setProjectToDelete] = React.useState<ProjectResponse | null>(null);

  // Fetch Projects query
  const { data: projects = [], isLoading, isError, refetch } = useQuery({
    queryKey: ["projects", currentTenant?.id],
    queryFn: ProjectService.listProjects,
    // Add realistic fallback data if backend is empty
    initialData: () => [],
  });

  // Mutators
  const createMutation = useMutation({
    mutationFn: ProjectService.createProject,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["projects"] });
      setIsSheetOpen(false);
    },
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, data }: { id: string; data: any }) => ProjectService.updateProject(id, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["projects"] });
      setIsSheetOpen(false);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: ProjectService.deleteProject,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["projects"] });
      setIsDeleteOpen(false);
      setSelectedIds([]);
    },
  });

  // Form setup
  const {
    register,
    handleSubmit,
    setValue,
    reset,
    formState: { errors, isDirty },
  } = useForm<ProjectFormValues>({
    defaultValues: {
      name: "",
      description: "",
      status: "ACTIVE",
    },
  });

  // Handle create click
  const handleCreateClick = () => {
    setEditingProject(null);
    reset({ name: "", description: "", status: "ACTIVE" });
    setIsSheetOpen(true);
  };

  // Handle edit click
  const handleEditClick = (proj: ProjectResponse) => {
    setEditingProject(proj);
    setValue("name", proj.name);
    setValue("description", proj.description);
    setValue("status", proj.status);
    setIsSheetOpen(true);
  };

  // Form submit handler
  const onSubmit = (values: ProjectFormValues) => {
    if (editingProject) {
      updateMutation.mutate({
        id: editingProject.publicId,
        data: {
          name: values.name,
          description: values.description,
          status: values.status,
        },
      });
    } else {
      createMutation.mutate({
        tenantPublicId: currentTenant?.id || "default-tenant",
        name: values.name,
        description: values.description,
        status: values.status || "ACTIVE",
      });
    }
  };

  // Checkbox handlers
  const handleSelectRow = (id: string, selected: boolean) => {
    setSelectedIds((prev) =>
      selected ? [...prev, id] : prev.filter((x) => x !== id)
    );
  };

  const handleSelectAll = (selected: boolean) => {
    setSelectedIds(selected ? filteredProjects.map((p) => p.publicId) : []);
  };

  // Filter & Search Project lists locally
  const filteredProjects = React.useMemo(() => {
    let list = projects;
    if (search.trim()) {
      const q = search.toLowerCase();
      list = list.filter((p) => p.name.toLowerCase().includes(q) || p.description.toLowerCase().includes(q));
    }
    if (statusFilter) {
      list = list.filter((p) => p.status === statusFilter);
    }
    return list;
  }, [projects, search, statusFilter]);

  const totalPages = Math.ceil(filteredProjects.length / 10) || 1;
  const paginatedProjects = React.useMemo(() => {
    const start = (currentPage - 1) * 10;
    return filteredProjects.slice(start, start + 10);
  }, [filteredProjects, currentPage]);

  const columns = [
    {
      key: "name",
      header: "Project Name",
      render: (item: ProjectResponse) => (
        <Link href={`/projects/${item.publicId}`} className="font-bold text-slate-100 hover:text-purple-400 hover:underline">
          {item.name}
        </Link>
      ),
    },
    { key: "description", header: "Description" },
    {
      key: "status",
      header: "Status",
      render: (item: ProjectResponse) => <StatusBadge status={item.status} />,
    },
    {
      key: "actions",
      header: "Actions",
      className: "text-right w-24",
      render: (item: ProjectResponse) => (
        <div className="flex items-center justify-end space-x-1.5">
          <Button variant="ghost" size="icon" onClick={() => handleEditClick(item)} className="h-8 w-8 hover:bg-slate-800 text-slate-400 hover:text-slate-200">
            <Edit className="h-4 w-4" />
          </Button>
          <Button
            variant="ghost"
            size="icon"
            onClick={() => {
              setProjectToDelete(item);
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
          <h1 className="text-2xl font-bold tracking-tight text-slate-100">Projects</h1>
          <p className="text-sm text-slate-400">Manage, isolate, and configure environments for jobs and workflows.</p>
        </div>
        <Button size="sm" onClick={handleCreateClick} className="gap-1.5">
          <Plus className="h-4 w-4" />
          Create Project
        </Button>
      </div>

      <EntityToolbar
        searchPlaceholder="Search projects by name..."
        searchValue={search}
        onSearchChange={setSearch}
        filters={[
          {
            key: "status",
            label: "Status",
            options: [
              { value: "ACTIVE", label: "Active" },
              { value: "INACTIVE", label: "Inactive" },
            ],
          },
        ]}
        selectedFilters={{ status: statusFilter }}
        onFilterChange={(_, val) => setStatusFilter(val)}
      />

      <div className="bg-slate-900/10 border border-slate-900 rounded-lg p-1">
        <EntityTable
          columns={columns}
          data={paginatedProjects}
          idKey="publicId"
          selectedIds={selectedIds}
          onSelectRow={handleSelectRow}
          onSelectAll={handleSelectAll}
          isLoading={isLoading}
          emptyTitle="No projects available"
          emptyDescription="Click 'Create Project' above to define a new environment namespace."
          mobileCardRender={(item) => (
            <div className="space-y-2">
              <Link href={`/projects/${item.publicId}`} className="font-bold text-slate-100 hover:underline">
                {item.name}
              </Link>
              <p className="text-xs text-slate-400">{item.description}</p>
              <div className="flex items-center justify-between pt-2">
                <StatusBadge status={item.status} />
                <div className="flex space-x-1">
                  <Button variant="ghost" size="icon" onClick={() => handleEditClick(item)} className="h-8 w-8">
                    <Edit className="h-4 w-4" />
                  </Button>
                  <Button variant="ghost" size="icon" onClick={() => { setProjectToDelete(item); setIsDeleteOpen(true); }} className="h-8 w-8 text-red-400">
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
              if (confirm(`Are you sure you want to delete the ${selectedIds.length} selected projects?`)) {
                selectedIds.forEach((id) => deleteMutation.mutate(id));
              }
            },
          },
        ]}
      />

      {/* Create / Edit Drawer Sheet */}
      <Sheet isOpen={isSheetOpen} onClose={() => setIsSheetOpen(false)}>
        <SheetHeader>
          <SheetTitle>{editingProject ? "Edit Project Properties" : "Create New Project"}</SheetTitle>
          <SheetDescription>
            Specify namespace options. Jobs and API Keys will inherit these settings.
          </SheetDescription>
        </SheetHeader>
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-4 mt-6">
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-400">Project Name</label>
            <input
              type="text"
              placeholder="e.g. Production Ingestion"
              {...register("name")}
              className="h-10 w-full rounded-md border border-slate-800 bg-slate-950 px-3 text-sm text-slate-200 placeholder:text-slate-500 focus:ring-2 focus:ring-purple-500"
            />
            {errors.name && <p className="text-xs text-red-400">{errors.name.message}</p>}
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-400">Description</label>
            <textarea
              placeholder="Provide context about jobs running inside this sandbox..."
              rows={4}
              {...register("description")}
              className="w-full rounded-md border border-slate-800 bg-slate-950 p-3 text-sm text-slate-200 placeholder:text-slate-500 focus:ring-2 focus:ring-purple-500"
            />
            {errors.description && <p className="text-xs text-red-400">{errors.description.message}</p>}
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-400">Status</label>
            <select
              {...register("status")}
              className="h-10 w-full rounded-md border border-slate-800 bg-slate-950 px-3 text-sm text-slate-200 focus:ring-2 focus:ring-purple-500 cursor-pointer"
            >
              <option value="ACTIVE">Active</option>
              <option value="INACTIVE">Inactive</option>
            </select>
          </div>

          <div className="flex space-x-2 pt-4 justify-end">
            <Button type="button" variant="ghost" onClick={() => setIsSheetOpen(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={createMutation.isPending || updateMutation.isPending}>
              {editingProject ? "Save Changes" : "Create Project"}
            </Button>
          </div>
        </form>
      </Sheet>

      {/* Delete Confirmation Dialog */}
      <ConfirmDialog
        isOpen={isDeleteOpen}
        onClose={() => setIsDeleteOpen(false)}
        onConfirm={() => {
          if (projectToDelete) {
            deleteMutation.mutate(projectToDelete.publicId);
          }
        }}
        title="Delete Project Namespace?"
        description={`This action will permanently delete the project "${projectToDelete?.name}". All associated jobs, workflows, and API keys will be removed.`}
        confirmText="Permanently Delete"
        isDestructive
      />
    </div>
  );
}
