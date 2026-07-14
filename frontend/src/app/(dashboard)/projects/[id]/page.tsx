"use client";

import * as React from "react";
import { useParams, useRouter } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { ProjectService } from "@/services/projects";
import { MetricCard } from "@/components/ui/MetricCard";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { Button } from "@/components/ui/Button";
import { ArrowLeft, Play, GitBranch, Key, Cpu, Users } from "lucide-react";

export default function ProjectDetailsPage() {
  const params = useParams();
  const router = useRouter();
  const projectId = params.id as string;

  const { data: project, isLoading, isError } = useQuery({
    queryKey: ["project", projectId],
    queryFn: () => ProjectService.getProject(projectId),
  });

  if (isLoading) {
    return (
      <div className="flex h-64 items-center justify-center">
        <span className="text-sm text-slate-400 animate-pulse">Loading project details...</span>
      </div>
    );
  }

  if (isError || !project) {
    return (
      <div className="text-center p-8 bg-red-950/20 border border-red-900/50 rounded-lg max-w-md mx-auto">
        <h3 className="text-lg font-bold text-red-200">Failed to load project</h3>
        <p className="text-sm text-red-400 mt-2">The requested project does not exist or access was denied.</p>
        <Button onClick={() => router.push("/projects")} className="mt-4" size="sm">
          Back to Projects
        </Button>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center space-x-3">
        <Button variant="outline" size="icon" onClick={() => router.push("/projects")} className="border-slate-800 h-9 w-9">
          <ArrowLeft className="h-4 w-4" />
        </Button>
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-100">{project.name}</h1>
          <p className="text-sm text-slate-400">Namespace Details and environment statistics.</p>
        </div>
      </div>

      {/* Stats Cards */}
      <div className="grid gap-4 md:grid-cols-3">
        <MetricCard
          title="Jobs Registered"
          value={project.jobsCount ?? 0}
          description="Total configured execution triggers"
          icon={<Cpu className="h-4 w-4 text-purple-400" />}
        />
        <MetricCard
          title="Workflow DAGs"
          value={project.workflowsCount ?? 0}
          description="Active workflow definitions"
          icon={<GitBranch className="h-4 w-4 text-purple-400" />}
        />
        <MetricCard
          title="API Keys"
          value={project.keysCount ?? 0}
          description="Access credentials for this project"
          icon={<Key className="h-4 w-4 text-purple-400" />}
        />
      </div>

      {/* Overview Block */}
      <div className="p-6 rounded-lg border border-slate-900 bg-slate-900/10 backdrop-blur-xs space-y-4">
        <div>
          <h3 className="text-sm font-semibold text-slate-400">Overview Description</h3>
          <p className="text-sm text-slate-200 mt-1 leading-relaxed">{project.description}</p>
        </div>
        <div className="flex items-center space-x-6 text-xs text-slate-500 pt-2 border-t border-slate-900">
          <div>
            <span>Status: </span>
            <StatusBadge status={project.status} className="ml-1" />
          </div>
          <div>
            <span>Namespace ID: </span>
            <span className="font-mono text-slate-400 select-all">{project.publicId}</span>
          </div>
        </div>
      </div>
    </div>
  );
}
