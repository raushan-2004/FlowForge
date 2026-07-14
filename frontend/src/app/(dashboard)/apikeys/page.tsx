"use client";

import * as React from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { ApiKeyService, ApiKeyResponse } from "@/services/apikeys";
import { ProjectService } from "@/services/projects";
import { EntityTable } from "@/components/crud/EntityTable";
import { EntityToolbar } from "@/components/crud/EntityToolbar";
import { Button } from "@/components/ui/Button";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { Dialog, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from "@/components/ui/Dialog";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { Plus, Trash2, RefreshCw, Copy, Check, Eye, ShieldAlert } from "lucide-react";
import { useAuth } from "@/providers/AuthProvider";

const keyFormSchema = z.object({
  name: z.string().min(2, "Name must be at least 2 characters"),
});

type KeyFormValues = z.infer<typeof keyFormSchema>;

export default function ApiKeysPage() {
  const queryClient = useQueryClient();
  const { currentTenant } = useAuth();

  // Active Project Selection (keys belong to a project)
  const [selectedProjectId, setSelectedProjectId] = React.useState("");
  const [search, setSearch] = React.useState("");

  // Create modal states
  const [isCreateOpen, setIsCreateOpen] = React.useState(false);
  const [createdSecret, setCreatedSecret] = React.useState<string | null>(null);
  const [copied, setCopied] = React.useState(false);

  // Revoke Dialog state
  const [isRevokeOpen, setIsRevokeOpen] = React.useState(false);
  const [keyToRevoke, setKeyToRevoke] = React.useState<ApiKeyResponse | null>(null);

  // Rotate Dialog state
  const [isRotateOpen, setIsRotateOpen] = React.useState(false);
  const [keyToRotate, setKeyToRotate] = React.useState<ApiKeyResponse | null>(null);

  // Fetch Projects list
  const { data: projects = [] } = useQuery({
    queryKey: ["projects", currentTenant?.id],
    queryFn: ProjectService.listProjects,
  });

  // Auto-select first project when projects are loaded
  React.useEffect(() => {
    if (projects.length > 0 && !selectedProjectId) {
      setSelectedProjectId(projects[0].publicId);
    }
  }, [projects, selectedProjectId]);

  // Fetch Keys for selected project
  const { data: apiKeys = [], isLoading, isError, refetch } = useQuery({
    queryKey: ["api-keys", selectedProjectId],
    queryFn: () => ApiKeyService.listKeys(selectedProjectId),
    enabled: !!selectedProjectId,
  });

  const { register, handleSubmit, reset, formState: { errors } } = useForm<KeyFormValues>({
    defaultValues: { name: "" },
  });

  // Mutations
  const createMutation = useMutation({
    mutationFn: (name: string) => ApiKeyService.createKey(selectedProjectId, { name }),
    onSuccess: (response) => {
      queryClient.invalidateQueries({ queryKey: ["api-keys", selectedProjectId] });
      setCreatedSecret(response.cleartextSecret);
      setIsCreateOpen(false);
      reset({ name: "" });
    },
  });

  const rotateMutation = useMutation({
    mutationFn: (keyId: string) =>
      ApiKeyService.rotateKey(selectedProjectId, keyId, { name: "Rotated Key" }),
    onSuccess: (response) => {
      queryClient.invalidateQueries({ queryKey: ["api-keys", selectedProjectId] });
      setCreatedSecret(response.cleartextSecret);
      setIsRotateOpen(false);
    },
  });

  const revokeMutation = useMutation({
    mutationFn: (keyId: string) => ApiKeyService.revokeKey(selectedProjectId, keyId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["api-keys", selectedProjectId] });
      setIsRevokeOpen(false);
    },
  });

  const handleCreateSubmit = (values: KeyFormValues) => {
    createMutation.mutate(values.name);
  };

  const handleCopySecret = () => {
    if (createdSecret) {
      navigator.clipboard.writeText(createdSecret);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  // Filter Keys locally
  const filteredKeys = React.useMemo(() => {
    if (!search.trim()) return apiKeys;
    const q = search.toLowerCase();
    return apiKeys.filter((k) => k.name.toLowerCase().includes(q));
  }, [apiKeys, search]);

  const columns = [
    { key: "name", header: "Key Identifier Name", render: (item: ApiKeyResponse) => <span className="font-bold text-slate-100">{item.name}</span> },
    {
      key: "prefix",
      header: "Key Mask Prefix",
      render: (item: ApiKeyResponse) => (
        <code className="text-xs bg-slate-950 border border-slate-900 px-2 py-0.5 rounded-sm font-mono text-purple-400">
          {item.prefix}********************
        </code>
      ),
    },
    { key: "createdAt", header: "Created At", render: (item: ApiKeyResponse) => <span className="text-xs text-slate-400">{item.createdAt || "--"}</span> },
    { key: "lastUsedAt", header: "Last Used", render: (item: ApiKeyResponse) => <span className="text-xs text-slate-400">{item.lastUsedAt || "Never"}</span> },
    { key: "status", header: "Status", render: (item: ApiKeyResponse) => <StatusBadge status={item.status} /> },
    {
      key: "actions",
      header: "Actions",
      className: "text-right w-24",
      render: (item: ApiKeyResponse) => (
        <div className="flex items-center justify-end space-x-1.5">
          <Button
            variant="ghost"
            size="icon"
            onClick={() => {
              setKeyToRotate(item);
              setIsRotateOpen(true);
            }}
            title="Rotate Key"
            className="h-8 w-8 hover:bg-slate-800 text-slate-400 hover:text-slate-200"
          >
            <RefreshCw className="h-4 w-4" />
          </Button>
          <Button
            variant="ghost"
            size="icon"
            onClick={() => {
              setKeyToRevoke(item);
              setIsRevokeOpen(true);
            }}
            title="Revoke Key"
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
          <h1 className="text-2xl font-bold tracking-tight text-slate-100">API Keys</h1>
          <p className="text-sm text-slate-400">Secure tokens for external job dispatches and agent synchronization.</p>
        </div>
        <Button size="sm" onClick={() => setIsCreateOpen(true)} disabled={!selectedProjectId} className="gap-1.5">
          <Plus className="h-4 w-4" />
          Generate API Key
        </Button>
      </div>

      {/* Select Active Project context */}
      <div className="flex items-center space-x-2 text-xs bg-slate-900 border border-slate-800 p-3 rounded-lg text-slate-400 max-w-md">
        <span className="font-semibold text-slate-300">Target Namespace Context:</span>
        <select
          value={selectedProjectId}
          onChange={(e) => setSelectedProjectId(e.target.value)}
          className="bg-transparent border-0 text-slate-100 font-bold focus:ring-0 outline-hidden cursor-pointer"
        >
          {projects.map((p) => (
            <option key={p.publicId} value={p.publicId} className="bg-slate-950">
              {p.name}
            </option>
          ))}
        </select>
      </div>

      <EntityToolbar
        searchPlaceholder="Search tokens by name..."
        searchValue={search}
        onSearchChange={setSearch}
      />

      <div className="bg-slate-900/10 border border-slate-900 rounded-lg p-1">
        <EntityTable
          columns={columns}
          data={filteredKeys}
          idKey="publicId"
          isLoading={isLoading}
          emptyTitle="No API Keys found"
          emptyDescription="Ensure you have selected the correct Project context above, then click 'Generate API Key'."
          mobileCardRender={(item) => (
            <div className="space-y-2 text-xs">
              <div className="flex justify-between items-center">
                <span className="font-bold text-slate-100">{item.name}</span>
                <StatusBadge status={item.status} />
              </div>
              <code className="block text-[10px] bg-slate-950 border border-slate-900 px-2 py-1 rounded-sm font-mono text-purple-400 my-1.5">
                {item.prefix}********************
              </code>
              <div className="flex justify-between items-center text-[10px] text-slate-500">
                <span>Created: {item.createdAt}</span>
                <div className="flex space-x-1">
                  <Button variant="ghost" size="icon" onClick={() => { setKeyToRotate(item); setIsRotateOpen(true); }} className="h-8 w-8">
                    <RefreshCw className="h-4 w-4" />
                  </Button>
                  <Button variant="ghost" size="icon" onClick={() => { setKeyToRevoke(item); setIsRevokeOpen(true); }} className="h-8 w-8 text-red-450">
                    <Trash2 className="h-4 w-4" />
                  </Button>
                </div>
              </div>
            </div>
          )}
        />
      </div>

      {/* API Key Creation Form Overlay Dialog */}
      <Dialog isOpen={isCreateOpen} onClose={() => setIsCreateOpen(false)}>
        <DialogHeader>
          <DialogTitle>Generate Access Token</DialogTitle>
          <DialogDescription>
            Input an identifier. The generated secret key will only be displayed once.
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={handleSubmit(handleCreateSubmit)} className="space-y-4 py-4">
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-400">Token Identifier</label>
            <input
              type="text"
              placeholder="e.g. CI-CD Deployer Key"
              {...register("name")}
              className="h-10 w-full rounded-md border border-slate-800 bg-slate-950 px-3 text-sm text-slate-200 focus:ring-2 focus:ring-purple-500"
            />
            {errors.name && <p className="text-xs text-red-400">{errors.name.message}</p>}
          </div>
          <DialogFooter>
            <Button type="button" variant="ghost" onClick={() => setIsCreateOpen(false)}>
              Cancel
            </Button>
            <Button type="submit">Generate Token</Button>
          </DialogFooter>
        </form>
      </Dialog>

      {/* Copy Secret Display Dialog */}
      <Dialog isOpen={!!createdSecret} onClose={() => setCreatedSecret(null)}>
        <DialogHeader>
          <DialogTitle>Copy Access Secret</DialogTitle>
          <DialogDescription>
            For security, this secret key cannot be recovered or displayed again. Copy and save it now.
          </DialogDescription>
        </DialogHeader>
        <div className="space-y-4 py-4">
          <div className="p-3 bg-red-950/20 border border-red-900/40 rounded-md flex items-start space-x-2 text-xs text-red-300">
            <ShieldAlert className="h-4 w-4 shrink-0 mt-0.5" />
            <span>WARNING: If you navigate away or close this dialog, you will never be able to view this token again.</span>
          </div>

          <div className="flex items-center space-x-2">
            <input
              type="text"
              readOnly
              value={createdSecret || ""}
              className="h-10 flex-1 rounded-md border border-slate-850 bg-slate-950 px-3 text-xs text-slate-100 font-mono select-all"
            />
            <Button onClick={handleCopySecret} className="h-10 gap-1" variant="secondary">
              {copied ? <Check className="h-4 w-4 text-green-500" /> : <Copy className="h-4 w-4" />}
              {copied ? "Copied!" : "Copy"}
            </Button>
          </div>
        </div>
        <DialogFooter>
          <Button onClick={() => setCreatedSecret(null)} className="w-full">
            I Have Saved the Secret Key
          </Button>
        </DialogFooter>
      </Dialog>

      {/* Revoke confirmation */}
      <ConfirmDialog
        isOpen={isRevokeOpen}
        onClose={() => setIsRevokeOpen(false)}
        onConfirm={() => {
          if (keyToRevoke) {
            revokeMutation.mutate(keyToRevoke.publicId);
          }
        }}
        title="Revoke Token Access?"
        description={`This action will permanently invalidate "${keyToRevoke?.name}". Any clients using this token will immediately fail to authenticate.`}
        confirmText="Revoke Token Access"
        isDestructive
      />

      {/* Rotate confirmation */}
      <ConfirmDialog
        isOpen={isRotateOpen}
        onClose={() => setIsRotateOpen(false)}
        onConfirm={() => {
          if (keyToRotate) {
            rotateMutation.mutate(keyToRotate.publicId);
          }
        }}
        title="Rotate Security Token?"
        description={`Rotating "${keyToRotate?.name}" will generate a new secret and deprecate the current prefix token. Clients will need to update their configurations.`}
        confirmText="Proceed Rotation"
        isDestructive
      />
    </div>
  );
}
