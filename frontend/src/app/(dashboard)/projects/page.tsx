"use client";

import * as React from "react";
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Plus, Folder } from "lucide-react";

const mockProjects = [
  { id: "p1", name: "Infrastructure Backup", desc: "Periodic database, filesystem, and asset backup pipelines.", status: "ACTIVE" },
  { id: "p2", name: "Webhooks Dispatcher", desc: "Event-driven webhook dispatching service for external endpoints.", status: "ACTIVE" },
  { id: "p3", name: "Data Analytics Cron", desc: "Hourly aggregation and data warehouse ingestion DAGs.", status: "INACTIVE" },
];

export default function ProjectsPage() {
  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-100">Projects</h1>
          <p className="text-sm text-slate-400">Manage tenant-isolated namespaces and resources.</p>
        </div>
        <Button size="sm" className="gap-1.5">
          <Plus className="h-4 w-4" />
          New Project
        </Button>
      </div>

      <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
        {mockProjects.map((proj) => (
          <Card key={proj.id} className="flex flex-col justify-between">
            <CardHeader>
              <div className="flex items-center space-x-3">
                <div className="p-2 rounded-md bg-slate-800 text-purple-400 shrink-0">
                  <Folder className="h-5 w-5" />
                </div>
                <div>
                  <CardTitle className="text-md">{proj.name}</CardTitle>
                  <CardDescription className="text-xs uppercase mt-0.5 tracking-wider font-semibold text-slate-500">
                    ID: {proj.id}
                  </CardDescription>
                </div>
              </div>
            </CardHeader>
            <CardContent>
              <p className="text-sm text-slate-400 leading-relaxed">{proj.desc}</p>
            </CardContent>
            <CardFooter className="flex justify-between items-center mt-auto">
              <span className="text-xs font-semibold text-slate-500">Status: {proj.status}</span>
              <Button size="sm" variant="outline">
                Open Project
              </Button>
            </CardFooter>
          </Card>
        ))}
      </div>
    </div>
  );
}
