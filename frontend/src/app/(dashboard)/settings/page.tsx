"use client";

import * as React from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import {
  User, Building2, Users, FolderKanban, Key, Bell, ScrollText,
  Shield, SlidersHorizontal, Info, ChevronRight, Save, Plus,
  Trash2, RefreshCw, Download, Search, Check, X, ToggleLeft,
  ToggleRight, Mail, Monitor, AlertTriangle, UserPlus, Ban,
  RotateCcw, Copy, Eye, EyeOff, Zap, Clock, Globe, Palette,
  ShieldCheck, ShieldAlert, LogOut,
} from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Badge } from "@/components/ui/Badge";
import { Skeleton } from "@/components/ui/Skeleton";
import { SearchInput } from "@/components/ui/SearchInput";
import { Dialog, DialogHeader, DialogTitle, DialogDescription, DialogFooter } from "@/components/ui/Dialog";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { cn } from "@/lib/utils";
import { useTheme } from "@/providers/ThemeProvider";
import { usePreferences } from "@/providers/UserPreferencesProvider";
import {
  ProfileService, OrganizationService, MemberService, AuditService,
  NotificationService, FeatureFlagService, PERMISSION_MATRIX, ALL_PERMISSIONS,
  MOCK_AUDIT_ENTRIES,
  type UserProfile, type Organization, type Member, type MemberRole,
  type NotificationPrefs, type AuditEntry, type FeatureFlag,
} from "@/services/settings";

// ─── Nav sections ─────────────────────────────────────────────────────────────

type SettingsSection =
  | "profile" | "organization" | "members" | "roles"
  | "apikeys" | "notifications" | "audit" | "security"
  | "preferences" | "flags" | "about";

const NAV_ITEMS: { id: SettingsSection; label: string; icon: React.ReactNode; badge?: string }[] = [
  { id: "profile",       label: "Profile",            icon: <User className="h-4 w-4" /> },
  { id: "organization",  label: "Organization",       icon: <Building2 className="h-4 w-4" /> },
  { id: "members",       label: "Members",            icon: <Users className="h-4 w-4" /> },
  { id: "roles",         label: "Roles & Permissions", icon: <ShieldCheck className="h-4 w-4" /> },
  { id: "apikeys",       label: "API Keys",           icon: <Key className="h-4 w-4" /> },
  { id: "notifications", label: "Notifications",      icon: <Bell className="h-4 w-4" /> },
  { id: "audit",         label: "Audit Logs",         icon: <ScrollText className="h-4 w-4" /> },
  { id: "security",      label: "Security",           icon: <Shield className="h-4 w-4" /> },
  { id: "preferences",   label: "Preferences",        icon: <SlidersHorizontal className="h-4 w-4" /> },
  { id: "flags",         label: "Feature Flags",      icon: <Zap className="h-4 w-4" />, badge: "Beta" },
  { id: "about",         label: "About",              icon: <Info className="h-4 w-4" /> },
];

// ─── Shared helpers ───────────────────────────────────────────────────────────

function SectionHeader({ title, description, action }: { title: string; description?: string; action?: React.ReactNode }) {
  return (
    <div className="flex items-start justify-between mb-6">
      <div>
        <h2 className="text-lg font-semibold text-slate-100">{title}</h2>
        {description && <p className="text-sm text-slate-400 mt-0.5">{description}</p>}
      </div>
      {action && <div className="flex-shrink-0">{action}</div>}
    </div>
  );
}

function FieldRow({ label, description, children }: { label: string; description?: string; children: React.ReactNode }) {
  return (
    <div className="flex items-center justify-between py-4 border-b border-slate-800/60 last:border-0">
      <div className="pr-8">
        <p className="text-sm font-medium text-slate-200">{label}</p>
        {description && <p className="text-xs text-slate-500 mt-0.5">{description}</p>}
      </div>
      <div className="flex-shrink-0">{children}</div>
    </div>
  );
}

function InputField({ value, onChange, placeholder, type = "text", disabled }: {
  value: string; onChange: (v: string) => void; placeholder?: string; type?: string; disabled?: boolean;
}) {
  return (
    <input
      type={type}
      value={value}
      onChange={(e) => onChange(e.target.value)}
      placeholder={placeholder}
      disabled={disabled}
      className="h-9 w-56 rounded-md border border-slate-700 bg-slate-950 px-3 text-sm text-slate-200 placeholder:text-slate-500 focus:outline-none focus:ring-2 focus:ring-violet-500 disabled:opacity-50 disabled:cursor-not-allowed"
    />
  );
}

function SelectField({ value, onChange, options, disabled }: {
  value: string; onChange: (v: string) => void; options: { label: string; value: string }[]; disabled?: boolean;
}) {
  return (
    <select
      value={value}
      onChange={(e) => onChange(e.target.value)}
      disabled={disabled}
      className="h-9 w-56 rounded-md border border-slate-700 bg-slate-950 px-3 text-sm text-slate-200 focus:outline-none focus:ring-2 focus:ring-violet-500 disabled:opacity-50"
    >
      {options.map((o) => (
        <option key={o.value} value={o.value} className="bg-slate-950">{o.label}</option>
      ))}
    </select>
  );
}

function Toggle({ value, onChange, id }: { value: boolean; onChange: (v: boolean) => void; id: string }) {
  return (
    <button
      id={id}
      role="switch"
      aria-checked={value}
      onClick={() => onChange(!value)}
      className={cn(
        "relative inline-flex h-6 w-11 items-center rounded-full transition-colors focus:outline-none focus:ring-2 focus:ring-violet-500",
        value ? "bg-violet-600" : "bg-slate-700"
      )}
    >
      <span className={cn("inline-block h-4 w-4 transform rounded-full bg-white shadow transition-transform", value ? "translate-x-6" : "translate-x-1")} />
    </button>
  );
}

function Toast({ message, type, onClose }: { message: string; type: "success" | "error"; onClose: () => void }) {
  React.useEffect(() => { const t = setTimeout(onClose, 3000); return () => clearTimeout(t); }, [onClose]);
  return (
    <div className={cn("fixed bottom-6 right-6 z-50 flex items-center gap-3 rounded-lg border px-4 py-3 text-sm font-medium shadow-lg animate-in slide-in-from-bottom-2",
      type === "success" ? "border-emerald-800/50 bg-emerald-950 text-emerald-300" : "border-red-800/50 bg-red-950 text-red-300"
    )}>
      {type === "success" ? <Check className="h-4 w-4" /> : <X className="h-4 w-4" />}
      {message}
      <button onClick={onClose} className="ml-2 text-slate-400 hover:text-white"><X className="h-3 w-3" /></button>
    </div>
  );
}

function useToast() {
  const [toast, setToast] = React.useState<{ message: string; type: "success" | "error" } | null>(null);
  const show = React.useCallback((message: string, type: "success" | "error" = "success") => setToast({ message, type }), []);
  const hide = React.useCallback(() => setToast(null), []);
  return { toast, show, hide };
}

// ─── Profile Section ──────────────────────────────────────────────────────────

function ProfileSection() {
  const { show, hide, toast } = useToast();
  const { data: profile, isLoading } = useQuery({ queryKey: ["settings-profile"], queryFn: ProfileService.getProfile });
  const [name, setName] = React.useState("");
  const [tz, setTz] = React.useState("");
  const [lang, setLang] = React.useState("en");
  const [dirty, setDirty] = React.useState(false);
  const [saving, setSaving] = React.useState(false);

  React.useEffect(() => {
    if (profile) { setName(profile.name); setTz(profile.timeZone); setLang(profile.language); }
  }, [profile]);

  async function handleSave() {
    setSaving(true);
    try { await ProfileService.updateProfile({ name, timeZone: tz, language: lang }); show("Profile updated successfully"); setDirty(false); }
    catch { show("Failed to update profile", "error"); }
    finally { setSaving(false); }
  }

  const TZ_OPTIONS = [
    { label: "Asia/Kolkata (IST)", value: "Asia/Kolkata" },
    { label: "America/New_York (EST)", value: "America/New_York" },
    { label: "America/Los_Angeles (PST)", value: "America/Los_Angeles" },
    { label: "Europe/London (GMT)", value: "Europe/London" },
    { label: "UTC", value: "UTC" },
  ];
  const LANG_OPTIONS = [{ label: "English", value: "en" }, { label: "Spanish", value: "es" }, { label: "French", value: "fr" }];

  if (isLoading) return <div className="space-y-4">{[1,2,3,4].map(i => <Skeleton key={i} className="h-14" />)}</div>;

  return (
    <div>
      {toast && <Toast {...toast} onClose={hide} />}
      <SectionHeader title="Profile" description="Manage your personal information and preferences." action={dirty ? <Button size="sm" onClick={handleSave} disabled={saving} className="gap-2"><Save className="h-4 w-4" />{saving ? "Saving..." : "Save Changes"}</Button> : undefined} />

      <Card className="border-slate-800 bg-slate-900/40">
        <CardContent className="p-0 divide-y divide-slate-800/60">
          <div className="flex items-center justify-between px-6 py-5">
            <div>
              <p className="text-sm font-medium text-slate-200">Avatar</p>
              <p className="text-xs text-slate-500 mt-0.5">Your profile picture across the platform</p>
            </div>
            <div className="flex items-center gap-3">
              <div className="h-12 w-12 rounded-full bg-violet-700 flex items-center justify-center text-white font-bold text-lg">
                {profile?.name?.charAt(0).toUpperCase() ?? "U"}
              </div>
              <Button variant="outline" size="sm">Upload</Button>
            </div>
          </div>
          <div className="px-6">
            <FieldRow label="Full Name" description="Your display name visible to team members.">
              <InputField value={name} onChange={(v) => { setName(v); setDirty(true); }} placeholder="Your name" />
            </FieldRow>
            <FieldRow label="Email Address" description="Email cannot be changed here.">
              <InputField value={profile?.email ?? ""} onChange={() => {}} disabled />
            </FieldRow>
            <FieldRow label="Time Zone" description="Used for scheduling and timestamps.">
              <SelectField value={tz} onChange={(v) => { setTz(v); setDirty(true); }} options={TZ_OPTIONS} />
            </FieldRow>
            <FieldRow label="Language" description="Interface language preference.">
              <SelectField value={lang} onChange={(v) => { setLang(v); setDirty(true); }} options={LANG_OPTIONS} />
            </FieldRow>
            <FieldRow label="Last Login" description="Most recent authenticated session.">
              <span className="text-xs text-slate-400">{profile?.lastLogin ? new Date(profile.lastLogin).toLocaleString() : "--"}</span>
            </FieldRow>
            <FieldRow label="Account Created" description="Date this account was provisioned.">
              <span className="text-xs text-slate-400">{profile?.createdAt ? new Date(profile.createdAt).toLocaleDateString() : "--"}</span>
            </FieldRow>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

// ─── Organization Section ─────────────────────────────────────────────────────

function OrganizationSection() {
  const { show, hide, toast } = useToast();
  const { data: org, isLoading } = useQuery({ queryKey: ["settings-org"], queryFn: OrganizationService.getOrganization });
  const [orgName, setOrgName] = React.useState("");
  const [desc, setDesc] = React.useState("");
  const [tz, setTz] = React.useState("");
  const [retryPolicy, setRetryPolicy] = React.useState("");
  const [dirty, setDirty] = React.useState(false);
  const [saving, setSaving] = React.useState(false);

  React.useEffect(() => {
    if (org) { setOrgName(org.name); setDesc(org.description); setTz(org.defaultTimeZone); setRetryPolicy(org.defaultRetryPolicy); }
  }, [org]);

  async function handleSave() {
    setSaving(true);
    try { await OrganizationService.updateOrganization({ name: orgName, description: desc, defaultTimeZone: tz, defaultRetryPolicy: retryPolicy }); show("Organization settings saved"); setDirty(false); }
    catch { show("Failed to save organization settings", "error"); }
    finally { setSaving(false); }
  }

  if (isLoading) return <div className="space-y-4">{[1,2,3].map(i => <Skeleton key={i} className="h-14" />)}</div>;

  return (
    <div>
      {toast && <Toast {...toast} onClose={hide} />}
      <SectionHeader title="Organization" description="Configure your tenant identity and default policies."
        action={dirty ? <Button size="sm" onClick={handleSave} disabled={saving} className="gap-2"><Save className="h-4 w-4" />{saving ? "Saving..." : "Save Changes"}</Button> : undefined} />

      <Card className="border-slate-800 bg-slate-900/40">
        <CardContent className="p-0">
          <div className="px-6 divide-y divide-slate-800/60">
            <FieldRow label="Organization Name" description="Displayed in the portal header.">
              <InputField value={orgName} onChange={(v) => { setOrgName(v); setDirty(true); }} placeholder="My Organization" />
            </FieldRow>
            <FieldRow label="Identifier (Slug)" description="Read-only unique organization slug.">
              <code className="text-xs font-mono bg-slate-950 border border-slate-800 px-3 py-1.5 rounded text-slate-400">{org?.slug}</code>
            </FieldRow>
            <FieldRow label="Description" description="Optional description of your organization.">
              <InputField value={desc} onChange={(v) => { setDesc(v); setDirty(true); }} placeholder="Description" />
            </FieldRow>
            <FieldRow label="Default Time Zone" description="Used for job scheduling and audit timestamps.">
              <SelectField value={tz} onChange={(v) => { setTz(v); setDirty(true); }} options={[
                { label: "Asia/Kolkata", value: "Asia/Kolkata" },
                { label: "UTC", value: "UTC" },
                { label: "America/New_York", value: "America/New_York" },
              ]} />
            </FieldRow>
            <FieldRow label="Default Retry Policy" description="Applied to all jobs unless overridden.">
              <SelectField value={retryPolicy} onChange={(v) => { setRetryPolicy(v); setDirty(true); }} options={[
                { label: "Exponential Backoff", value: "EXPONENTIAL_BACKOFF" },
                { label: "Linear Backoff", value: "LINEAR_BACKOFF" },
                { label: "Fixed Delay", value: "FIXED_DELAY" },
                { label: "No Retry", value: "NONE" },
              ]} />
            </FieldRow>
            <FieldRow label="Plan" description="Your current subscription tier.">
              <Badge variant="default" className="text-xs">{org?.plan}</Badge>
            </FieldRow>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

// ─── Members Section ──────────────────────────────────────────────────────────

const ROLE_COLORS: Record<MemberRole, string> = {
  OWNER:     "border-violet-800/50 bg-violet-950/60 text-violet-300",
  ADMIN:     "border-blue-800/50 bg-blue-950/60 text-blue-300",
  DEVELOPER: "border-emerald-800/50 bg-emerald-950/60 text-emerald-300",
  VIEWER:    "border-slate-700 bg-slate-800/60 text-slate-400",
};

const STATUS_COLORS: Record<string, string> = {
  ACTIVE:    "border-emerald-800/50 bg-emerald-950/60 text-emerald-400",
  INVITED:   "border-amber-800/50 bg-amber-950/60 text-amber-400",
  SUSPENDED: "border-red-800/50 bg-red-950/60 text-red-400",
};

function MembersSection() {
  const qc = useQueryClient();
  const { show, hide, toast } = useToast();
  const [inviteOpen, setInviteOpen] = React.useState(false);
  const [inviteEmail, setInviteEmail] = React.useState("");
  const [inviteRole, setInviteRole] = React.useState<MemberRole>("DEVELOPER");
  const [removeTarget, setRemoveTarget] = React.useState<Member | null>(null);
  const [search, setSearch] = React.useState("");

  const { data: members = [], isLoading } = useQuery({ queryKey: ["settings-members"], queryFn: MemberService.listMembers });

  const inviteMutation = useMutation({
    mutationFn: () => MemberService.inviteMember(inviteEmail, inviteRole),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["settings-members"] }); setInviteOpen(false); setInviteEmail(""); show("Invitation sent"); },
    onError: () => show("Failed to send invitation", "error"),
  });

  const removeMutation = useMutation({
    mutationFn: (id: string) => MemberService.removeMember(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["settings-members"] }); setRemoveTarget(null); show("Member removed"); },
  });

  const suspendMutation = useMutation({
    mutationFn: (id: string) => MemberService.suspendMember(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["settings-members"] }); show("Member suspended"); },
  });

  const reactivateMutation = useMutation({
    mutationFn: (id: string) => MemberService.reactivateMember(id),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["settings-members"] }); show("Member reactivated"); },
  });

  const roleMutation = useMutation({
    mutationFn: ({ id, role }: { id: string; role: MemberRole }) => MemberService.updateRole(id, role),
    onSuccess: () => { qc.invalidateQueries({ queryKey: ["settings-members"] }); show("Role updated"); },
  });

  const filtered = members.filter(m =>
    !search || m.name.toLowerCase().includes(search.toLowerCase()) || m.email.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div>
      {toast && <Toast {...toast} onClose={hide} />}
      <SectionHeader title="Members" description="Manage who has access to your organization."
        action={<Button size="sm" onClick={() => setInviteOpen(true)} className="gap-2"><UserPlus className="h-4 w-4" />Invite Member</Button>} />

      <div className="mb-4">
        <SearchInput value={search} onChange={e => setSearch(e.target.value)} placeholder="Search members..." className="max-w-sm" />
      </div>

      <Card className="border-slate-800 bg-slate-900/40 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm" role="table">
            <thead>
              <tr className="border-b border-slate-800 bg-slate-900/60">
                {["Member", "Email", "Role", "Status", "Last Active", "Actions"].map(h => (
                  <th key={h} className="px-4 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {isLoading ? [1,2,3].map(i => (
                <tr key={i} className="border-b border-slate-800/50">
                  {[1,2,3,4,5,6].map(j => <td key={j} className="px-4 py-3"><Skeleton className="h-4 w-full" /></td>)}
                </tr>
              )) : filtered.map((m, i) => (
                <tr key={m.id} className={cn("border-b border-slate-800/50 hover:bg-slate-800/30 transition-colors", i % 2 !== 0 && "bg-slate-900/20")}>
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-2">
                      <div className="h-7 w-7 rounded-full bg-violet-800 flex items-center justify-center text-xs font-bold text-white">{m.name.charAt(0)}</div>
                      <span className="font-medium text-slate-200 text-xs">{m.name}</span>
                    </div>
                  </td>
                  <td className="px-4 py-3 text-xs text-slate-400">{m.email}</td>
                  <td className="px-4 py-3">
                    <select
                      value={m.role}
                      onChange={e => roleMutation.mutate({ id: m.id, role: e.target.value as MemberRole })}
                      disabled={m.role === "OWNER"}
                      className={cn("text-[10px] font-semibold px-2 py-0.5 rounded-full border bg-transparent cursor-pointer disabled:cursor-not-allowed", ROLE_COLORS[m.role])}
                    >
                      {(["OWNER","ADMIN","DEVELOPER","VIEWER"] as MemberRole[]).map(r => (
                        <option key={r} value={r} className="bg-slate-950">{r}</option>
                      ))}
                    </select>
                  </td>
                  <td className="px-4 py-3">
                    <span className={cn("text-[10px] font-semibold px-2 py-0.5 rounded-full border", STATUS_COLORS[m.status] ?? "text-slate-400")}>{m.status}</span>
                  </td>
                  <td className="px-4 py-3 text-xs text-slate-500">{m.lastActive}</td>
                  <td className="px-4 py-3">
                    <div className="flex items-center gap-1">
                      {m.status === "ACTIVE" && m.role !== "OWNER" && (
                        <button title="Suspend" onClick={() => suspendMutation.mutate(m.id)} className="p-1.5 rounded text-slate-500 hover:text-amber-400 hover:bg-amber-950/30 transition-colors">
                          <Ban className="h-3.5 w-3.5" />
                        </button>
                      )}
                      {m.status === "SUSPENDED" && (
                        <button title="Reactivate" onClick={() => reactivateMutation.mutate(m.id)} className="p-1.5 rounded text-slate-500 hover:text-emerald-400 hover:bg-emerald-950/30 transition-colors">
                          <RotateCcw className="h-3.5 w-3.5" />
                        </button>
                      )}
                      {m.role !== "OWNER" && (
                        <button title="Remove" onClick={() => setRemoveTarget(m)} className="p-1.5 rounded text-slate-500 hover:text-red-400 hover:bg-red-950/30 transition-colors">
                          <Trash2 className="h-3.5 w-3.5" />
                        </button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>

      <Dialog isOpen={inviteOpen} onClose={() => setInviteOpen(false)}>
        <DialogHeader>
          <DialogTitle>Invite Member</DialogTitle>
          <DialogDescription>Send an email invitation to join your organization.</DialogDescription>
        </DialogHeader>
        <div className="space-y-4 py-4">
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-400">Email Address</label>
            <input type="email" value={inviteEmail} onChange={e => setInviteEmail(e.target.value)} placeholder="user@example.com"
              className="h-10 w-full rounded-md border border-slate-800 bg-slate-950 px-3 text-sm text-slate-200 focus:outline-none focus:ring-2 focus:ring-violet-500" />
          </div>
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-400">Role</label>
            <select value={inviteRole} onChange={e => setInviteRole(e.target.value as MemberRole)}
              className="h-10 w-full rounded-md border border-slate-800 bg-slate-950 px-3 text-sm text-slate-200 focus:outline-none focus:ring-2 focus:ring-violet-500">
              {(["ADMIN","DEVELOPER","VIEWER"] as MemberRole[]).map(r => <option key={r} value={r} className="bg-slate-950">{r}</option>)}
            </select>
          </div>
        </div>
        <DialogFooter>
          <Button variant="ghost" onClick={() => setInviteOpen(false)}>Cancel</Button>
          <Button onClick={() => inviteMutation.mutate()} disabled={!inviteEmail || inviteMutation.isPending}>
            {inviteMutation.isPending ? "Sending..." : "Send Invite"}
          </Button>
        </DialogFooter>
      </Dialog>

      <ConfirmDialog isOpen={!!removeTarget} onClose={() => setRemoveTarget(null)}
        onConfirm={() => removeTarget && removeMutation.mutate(removeTarget.id)}
        title="Remove Member?" description={`Remove "${removeTarget?.name}" from the organization? They will immediately lose all access.`}
        confirmText="Remove Member" isDestructive />
    </div>
  );
}

// ─── Roles & Permissions Section ──────────────────────────────────────────────

function RolesSection() {
  const roles: MemberRole[] = ["OWNER", "ADMIN", "DEVELOPER", "VIEWER"];

  return (
    <div>
      <SectionHeader title="Roles & Permissions" description="Read-only capability matrix for each role in your organization." />
      <Card className="border-slate-800 bg-slate-900/40 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm" role="table">
            <thead>
              <tr className="border-b border-slate-800 bg-slate-900/60">
                <th className="px-4 py-3 text-left text-xs font-medium text-slate-400 uppercase tracking-wider w-64">Permission</th>
                {roles.map(r => (
                  <th key={r} className="px-4 py-3 text-center text-xs font-medium text-slate-400 uppercase tracking-wider">
                    <span className={cn("px-2 py-0.5 rounded-full border text-[10px] font-semibold", ROLE_COLORS[r])}>{r}</span>
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {ALL_PERMISSIONS.map((perm, i) => (
                <tr key={perm.id} className={cn("border-b border-slate-800/50 hover:bg-slate-800/20 transition-colors", i % 2 !== 0 && "bg-slate-900/20")}>
                  <td className="px-4 py-3">
                    <p className="text-xs font-mono text-violet-300">{perm.id}</p>
                    <p className="text-[10px] text-slate-500 mt-0.5">{perm.description}</p>
                  </td>
                  {roles.map(role => {
                    const has = PERMISSION_MATRIX[role].includes(perm.id);
                    return (
                      <td key={role} className="px-4 py-3 text-center">
                        {has
                          ? <Check className="h-4 w-4 text-emerald-400 mx-auto" aria-label="Granted" />
                          : <X className="h-4 w-4 text-slate-700 mx-auto" aria-label="Not granted" />}
                      </td>
                    );
                  })}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>

      <p className="text-xs text-slate-500 mt-4">Permission changes require role reassignment by an OWNER or ADMIN.</p>
    </div>
  );
}

// ─── API Keys Section (extended) ──────────────────────────────────────────────

function ApiKeysSection() {
  return (
    <div>
      <SectionHeader title="API Key Administration" description="Manage API keys with rotation, enable/disable, and usage metadata." />
      <Card className="border-slate-800 bg-slate-900/40">
        <CardContent className="py-12 flex flex-col items-center gap-3">
          <Key className="h-10 w-10 text-violet-500" />
          <p className="text-sm font-medium text-slate-300">Full API Key management is available in the dedicated section.</p>
          <p className="text-xs text-slate-500">Navigate to <span className="font-mono text-violet-400">/apikeys</span> for create, rotate, revoke, and usage metadata.</p>
          <a href="/apikeys">
            <Button variant="outline" size="sm" className="mt-2 gap-2">
              Open API Keys <ChevronRight className="h-3.5 w-3.5" />
            </Button>
          </a>
        </CardContent>
      </Card>

      <div className="mt-6 grid gap-3 sm:grid-cols-3">
        {[
          { icon: <RefreshCw className="h-5 w-5 text-blue-400" />, title: "Rotate", desc: "Generate new secret, deprecate old prefix" },
          { icon: <ShieldAlert className="h-5 w-5 text-red-400" />, title: "Revoke", desc: "Permanently invalidate key for all clients" },
          { icon: <Eye className="h-5 w-5 text-violet-400" />, title: "Usage Metadata", desc: "Last used timestamp, prefix, creation date" },
        ].map(item => (
          <Card key={item.title} className="border-slate-800 bg-slate-900/40">
            <CardContent className="p-4 flex gap-3 items-start">
              {item.icon}
              <div>
                <p className="text-xs font-semibold text-slate-200">{item.title}</p>
                <p className="text-[10px] text-slate-500 mt-0.5">{item.desc}</p>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}

// ─── Notifications Section ────────────────────────────────────────────────────

function NotificationsSection() {
  const { show, hide, toast } = useToast();
  const { data: prefs, isLoading } = useQuery({ queryKey: ["settings-notif"], queryFn: NotificationService.getPrefs });
  const [local, setLocal] = React.useState<NotificationPrefs | null>(null);
  const [saving, setSaving] = React.useState(false);

  React.useEffect(() => { if (prefs) setLocal(prefs); }, [prefs]);

  function update(patch: Partial<NotificationPrefs>) { setLocal(p => p ? { ...p, ...patch } : p); }

  async function handleSave() {
    if (!local) return;
    setSaving(true);
    try { await NotificationService.savePrefs(local); show("Notification preferences saved"); }
    catch { show("Failed to save preferences", "error"); }
    finally { setSaving(false); }
  }

  if (isLoading || !local) return <div className="space-y-4">{[1,2,3].map(i => <Skeleton key={i} className="h-14" />)}</div>;

  return (
    <div>
      {toast && <Toast {...toast} onClose={hide} />}
      <SectionHeader title="Notification Preferences" description="Configure alert channels and per-event notification rules."
        action={<Button size="sm" onClick={handleSave} disabled={saving} className="gap-2"><Save className="h-4 w-4" />{saving ? "Saving..." : "Save"}</Button>} />

      <div className="space-y-4">
        <Card className="border-slate-800 bg-slate-900/40">
          <CardHeader className="pb-2"><CardTitle className="text-sm">Channels</CardTitle></CardHeader>
          <CardContent className="p-0">
            <div className="px-6 divide-y divide-slate-800/60">
              <FieldRow label="Email Notifications" description="Receive alerts via email.">
                <Toggle id="notif-email" value={local.emailEnabled} onChange={v => update({ emailEnabled: v })} />
              </FieldRow>
              <FieldRow label="Browser Notifications" description="Desktop push notifications via browser.">
                <Toggle id="notif-browser" value={local.browserEnabled} onChange={v => update({ browserEnabled: v })} />
              </FieldRow>
            </div>
          </CardContent>
        </Card>

        <Card className="border-slate-800 bg-slate-900/40">
          <CardHeader className="pb-2"><CardTitle className="text-sm">Alert Events</CardTitle></CardHeader>
          <CardContent className="p-0">
            <div className="px-6 divide-y divide-slate-800/60">
              {[
                { key: "retryAlerts", label: "Retry Alerts", desc: "Notify when executions exceed retry threshold." },
                { key: "workerOfflineAlerts", label: "Worker Offline Alerts", desc: "Notify when a worker node goes offline." },
                { key: "queueAlerts", label: "Queue Depth Alerts", desc: "Notify when queue exceeds configured depth." },
                { key: "workflowFailureAlerts", label: "Workflow Failure Alerts", desc: "Notify on workflow execution failures." },
              ].map(({ key, label, desc }) => (
                <FieldRow key={key} label={label} description={desc}>
                  <Toggle id={`notif-${key}`} value={local[key as keyof NotificationPrefs] as boolean} onChange={v => update({ [key]: v })} />
                </FieldRow>
              ))}
            </div>
          </CardContent>
        </Card>

        <Card className="border-slate-800 bg-slate-900/40">
          <CardHeader className="pb-2"><CardTitle className="text-sm">Thresholds</CardTitle></CardHeader>
          <CardContent className="p-0">
            <div className="px-6 divide-y divide-slate-800/60">
              <FieldRow label="Retry Alert Threshold" description="Alert when retries per execution exceed this count.">
                <input type="number" value={local.alertThresholdRetries} min={1} max={100}
                  onChange={e => update({ alertThresholdRetries: parseInt(e.target.value) })}
                  className="h-9 w-24 rounded-md border border-slate-700 bg-slate-950 px-3 text-sm text-slate-200 focus:outline-none focus:ring-2 focus:ring-violet-500 text-right" />
              </FieldRow>
              <FieldRow label="Queue Depth Threshold" description="Alert when pending queue depth exceeds this value.">
                <input type="number" value={local.alertThresholdQueueDepth} min={1} max={10000}
                  onChange={e => update({ alertThresholdQueueDepth: parseInt(e.target.value) })}
                  className="h-9 w-24 rounded-md border border-slate-700 bg-slate-950 px-3 text-sm text-slate-200 focus:outline-none focus:ring-2 focus:ring-violet-500 text-right" />
              </FieldRow>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

// ─── Audit Log Section ────────────────────────────────────────────────────────

const RESULT_COLOR: Record<string, string> = {
  SUCCESS: "text-emerald-400",
  FAILURE: "text-red-400",
  DENIED:  "text-amber-400",
};

function AuditSection() {
  const [search, setSearch] = React.useState("");
  const [resultFilter, setResultFilter] = React.useState("ALL");
  const [actionFilter, setActionFilter] = React.useState("ALL");
  const [page, setPage] = React.useState(0);
  const PAGE_SIZE = 20;

  const { data: entries = [], isLoading } = useQuery({
    queryKey: ["settings-audit", search, resultFilter, actionFilter],
    queryFn: () => AuditService.listAudit({ search, result: resultFilter, action: actionFilter }),
    staleTime: 15000,
  });

  const uniqueActions = React.useMemo(() => Array.from(new Set(MOCK_AUDIT_ENTRIES.map(e => e.action))).sort(), []);
  const pageEntries = entries.slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE);
  const totalPages = Math.ceil(entries.length / PAGE_SIZE);

  return (
    <div>
      <SectionHeader title="Audit Logs" description="Track all user actions, resource changes, and access events."
        action={<Button variant="outline" size="sm" onClick={() => AuditService.exportCsv(entries)} className="gap-2"><Download className="h-4 w-4" />Export CSV</Button>} />

      <div className="flex flex-wrap gap-3 mb-4">
        <SearchInput value={search} onChange={e => { setSearch(e.target.value); setPage(0); }} placeholder="Search user, action, resource..." className="flex-1 min-w-48" />
        <select value={resultFilter} onChange={e => { setResultFilter(e.target.value); setPage(0); }}
          className="h-10 rounded-md border border-slate-800 bg-slate-950 px-3 text-sm text-slate-200 focus:outline-none focus:ring-2 focus:ring-violet-500">
          {["ALL","SUCCESS","FAILURE","DENIED"].map(r => <option key={r} value={r} className="bg-slate-950">{r}</option>)}
        </select>
        <select value={actionFilter} onChange={e => { setActionFilter(e.target.value); setPage(0); }}
          className="h-10 rounded-md border border-slate-800 bg-slate-950 px-3 text-sm text-slate-200 focus:outline-none focus:ring-2 focus:ring-violet-500">
          <option value="ALL" className="bg-slate-950">All Actions</option>
          {uniqueActions.map(a => <option key={a} value={a} className="bg-slate-950">{a}</option>)}
        </select>
      </div>

      <Card className="border-slate-800 bg-slate-900/40 overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-xs" role="table">
            <thead>
              <tr className="border-b border-slate-800 bg-slate-900/60">
                {["Timestamp","User","Action","Resource","IP Address","Result"].map(h => (
                  <th key={h} className="px-3 py-3 text-left font-medium text-slate-400 uppercase tracking-wider text-[10px]">{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {isLoading ? [1,2,3,4,5].map(i => (
                <tr key={i} className="border-b border-slate-800/50">
                  {[1,2,3,4,5,6].map(j => <td key={j} className="px-3 py-2.5"><Skeleton className="h-3" /></td>)}
                </tr>
              )) : pageEntries.length === 0 ? (
                <tr><td colSpan={6} className="px-3 py-12 text-center text-slate-500">No audit entries match the current filters</td></tr>
              ) : pageEntries.map((e, i) => (
                <tr key={e.id} className={cn("border-b border-slate-800/50 hover:bg-slate-800/30 transition-colors", i % 2 !== 0 && "bg-slate-900/20")}>
                  <td className="px-3 py-2.5 font-mono text-slate-500 whitespace-nowrap">{new Date(e.timestamp).toLocaleString()}</td>
                  <td className="px-3 py-2.5">
                    <p className="text-slate-200 font-medium">{e.user}</p>
                    <p className="text-[10px] text-slate-500">{e.userEmail}</p>
                  </td>
                  <td className="px-3 py-2.5"><code className="text-violet-300 font-mono">{e.action}</code></td>
                  <td className="px-3 py-2.5 text-slate-400">{e.resource}{e.resourceId && <span className="text-slate-600 ml-1">#{e.resourceId}</span>}</td>
                  <td className="px-3 py-2.5 font-mono text-slate-500">{e.ipAddress ?? "—"}</td>
                  <td className="px-3 py-2.5">
                    <span className={cn("font-semibold", RESULT_COLOR[e.result])}>{e.result}</span>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        {totalPages > 1 && (
          <div className="flex items-center justify-between px-4 py-3 border-t border-slate-800">
            <p className="text-xs text-slate-500">{entries.length} entries · Page {page + 1} of {totalPages}</p>
            <div className="flex gap-2">
              <Button variant="outline" size="sm" onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}>Previous</Button>
              <Button variant="outline" size="sm" onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))} disabled={page >= totalPages - 1}>Next</Button>
            </div>
          </div>
        )}
      </Card>
    </div>
  );
}

// ─── Security Section ─────────────────────────────────────────────────────────

function SecuritySection() {
  const SESSIONS = [
    { id: "sess-1", device: "Chrome on Windows", location: "Kolkata, IN", lastSeen: "Current session", current: true },
    { id: "sess-2", device: "Safari on macOS", location: "Mumbai, IN", lastSeen: "2 hr ago", current: false },
    { id: "sess-3", device: "Firefox on Linux", location: "Delhi, IN", lastSeen: "2 days ago", current: false },
  ];

  const [sessions, setSessions] = React.useState(SESSIONS);
  const [revoked, setRevoked] = React.useState<string[]>([]);
  const { show, hide, toast } = useToast();

  function revokeSession(id: string) {
    setRevoked(r => [...r, id]);
    setSessions(s => s.filter(x => x.id !== id));
    show("Session revoked");
  }

  return (
    <div className="space-y-6">
      {toast && <Toast {...toast} onClose={hide} />}
      <SectionHeader title="Security" description="Manage active sessions, tokens, and account security." />

      <Card className="border-slate-800 bg-slate-900/40">
        <CardHeader className="pb-3">
          <CardTitle className="text-sm flex items-center gap-2"><Monitor className="h-4 w-4 text-violet-400" />Active Sessions</CardTitle>
          <CardDescription className="text-xs">Authenticated sessions across all your devices.</CardDescription>
        </CardHeader>
        <CardContent className="space-y-3">
          {sessions.map(sess => (
            <div key={sess.id} className={cn("flex items-center justify-between rounded-lg border p-3", sess.current ? "border-violet-800/50 bg-violet-950/20" : "border-slate-800 bg-slate-900/40")}>
              <div>
                <div className="flex items-center gap-2">
                  <p className="text-sm font-medium text-slate-200">{sess.device}</p>
                  {sess.current && <Badge variant="default" className="text-[10px] py-0">Current</Badge>}
                </div>
                <p className="text-xs text-slate-500 mt-0.5">{sess.location} · {sess.lastSeen}</p>
              </div>
              {!sess.current && (
                <Button variant="outline" size="sm" onClick={() => revokeSession(sess.id)} className="text-red-400 border-red-800/50 hover:bg-red-950/30 gap-1.5">
                  <LogOut className="h-3.5 w-3.5" />Revoke
                </Button>
              )}
            </div>
          ))}
        </CardContent>
      </Card>

      <Card className="border-slate-800 bg-slate-900/40">
        <CardHeader className="pb-3">
          <CardTitle className="text-sm flex items-center gap-2"><Shield className="h-4 w-4 text-violet-400" />Account Security</CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          <div className="px-6 divide-y divide-slate-800/60">
            <FieldRow label="Password" description="Last changed: 30 days ago.">
              <Button variant="outline" size="sm">Change Password</Button>
            </FieldRow>
            <FieldRow label="Two-Factor Authentication" description="Adds an extra layer of security.">
              <Badge variant="warning" className="text-xs">Not Enabled</Badge>
            </FieldRow>
            <FieldRow label="Login History" description="Recent authentication attempts.">
              <Button variant="ghost" size="sm" className="gap-1 text-xs">View History <ChevronRight className="h-3 w-3" /></Button>
            </FieldRow>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

// ─── Preferences Section ──────────────────────────────────────────────────────

function PreferencesSection() {
  const { theme, toggleTheme } = useTheme();
  const { preferences, setRefreshInterval } = usePreferences();
  const { show, hide, toast } = useToast();
  const [density, setDensity] = React.useState<"comfortable" | "compact">("comfortable");
  const [pageSize, setPageSize] = React.useState(25);
  const [dateFormat, setDateFormat] = React.useState("DD/MM/YYYY");
  const [timeFormat, setTimeFormat] = React.useState("24h");

  function handleSave() {
    // preferences are already auto-saved via provider; local ones saved here
    localStorage.setItem("flowforge-ui-prefs", JSON.stringify({ density, pageSize, dateFormat, timeFormat }));
    show("Preferences saved");
  }

  React.useEffect(() => {
    const saved = localStorage.getItem("flowforge-ui-prefs");
    if (saved) { try { const p = JSON.parse(saved); setDensity(p.density ?? "comfortable"); setPageSize(p.pageSize ?? 25); setDateFormat(p.dateFormat ?? "DD/MM/YYYY"); setTimeFormat(p.timeFormat ?? "24h"); } catch {} }
  }, []);

  return (
    <div>
      {toast && <Toast {...toast} onClose={hide} />}
      <SectionHeader title="Preferences" description="Customize the interface to match your workflow."
        action={<Button size="sm" onClick={handleSave} className="gap-2"><Save className="h-4 w-4" />Save</Button>} />

      <Card className="border-slate-800 bg-slate-900/40">
        <CardContent className="p-0">
          <div className="px-6 divide-y divide-slate-800/60">
            <FieldRow label="Theme" description="Toggle between dark and light interface.">
              <Button variant="outline" size="sm" onClick={toggleTheme}>{theme === "dark" ? "Dark Mode" : "Light Mode"}</Button>
            </FieldRow>
            <FieldRow label="Table Density" description="Controls row height in data tables.">
              <div className="flex gap-2">
                {(["comfortable", "compact"] as const).map(d => (
                  <button key={d} onClick={() => setDensity(d)} className={cn("px-3 py-1.5 rounded-md border text-xs font-medium transition-all",
                    density === d ? "border-violet-600 bg-violet-950/40 text-violet-300" : "border-slate-700 bg-slate-900 text-slate-400 hover:border-slate-600"
                  )}>
                    {d.charAt(0).toUpperCase() + d.slice(1)}
                  </button>
                ))}
              </div>
            </FieldRow>
            <FieldRow label="Table Page Size" description="Default number of rows per page.">
              <SelectField value={String(pageSize)} onChange={v => setPageSize(Number(v))} options={[
                { label: "10 rows", value: "10" }, { label: "25 rows", value: "25" },
                { label: "50 rows", value: "50" }, { label: "100 rows", value: "100" },
              ]} />
            </FieldRow>
            <FieldRow label="Date Format" description="How dates are displayed across the UI.">
              <SelectField value={dateFormat} onChange={setDateFormat} options={[
                { label: "DD/MM/YYYY", value: "DD/MM/YYYY" },
                { label: "MM/DD/YYYY", value: "MM/DD/YYYY" },
                { label: "YYYY-MM-DD", value: "YYYY-MM-DD" },
              ]} />
            </FieldRow>
            <FieldRow label="Time Format" description="12-hour or 24-hour clock display.">
              <div className="flex gap-2">
                {(["12h", "24h"] as const).map(f => (
                  <button key={f} onClick={() => setTimeFormat(f)} className={cn("px-3 py-1.5 rounded-md border text-xs font-medium transition-all",
                    timeFormat === f ? "border-violet-600 bg-violet-950/40 text-violet-300" : "border-slate-700 bg-slate-900 text-slate-400 hover:border-slate-600"
                  )}>
                    {f}
                  </button>
                ))}
              </div>
            </FieldRow>
            <FieldRow label="Dashboard Refresh Interval" description="How often live widgets auto-refresh.">
              <SelectField value={String(preferences.refreshInterval)} onChange={v => setRefreshInterval(Number(v))} options={[
                { label: "10 seconds", value: "10" }, { label: "30 seconds", value: "30" },
                { label: "1 minute", value: "60" }, { label: "5 minutes", value: "300" },
                { label: "Off", value: "0" },
              ]} />
            </FieldRow>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

// ─── Feature Flags Section ────────────────────────────────────────────────────

function FeatureFlagsSection() {
  const { data: flags = [], isLoading } = useQuery({ queryKey: ["settings-flags"], queryFn: FeatureFlagService.listFlags });
  const { show, hide, toast } = useToast();
  const [localFlags, setLocalFlags] = React.useState<FeatureFlag[]>([]);

  React.useEffect(() => { if (flags.length) setLocalFlags(flags); }, [flags]);

  function toggleFlag(id: string) {
    setLocalFlags(prev => prev.map(f => f.id === id && !f.readOnly ? { ...f, enabled: !f.enabled } : f));
    show("Feature flag updated (UI only — backend sync pending)");
  }

  const categories = Array.from(new Set(localFlags.map(f => f.category)));

  return (
    <div>
      {toast && <Toast {...toast} onClose={hide} />}
      <SectionHeader title="Feature Flags" description="UI-only feature toggles. Read-only flags are controlled by the backend." />
      <div className="mb-4 rounded-lg border border-amber-800/40 bg-amber-950/20 px-4 py-3 text-xs text-amber-300 flex items-center gap-2">
        <AlertTriangle className="h-4 w-4 flex-shrink-0" />
        Feature flags marked read-only are managed by the platform infrastructure and cannot be changed from the UI.
      </div>

      {isLoading ? (
        <div className="space-y-3">{[1,2,3].map(i => <Skeleton key={i} className="h-14" />)}</div>
      ) : (
        <div className="space-y-4">
          {categories.map(cat => (
            <Card key={cat} className="border-slate-800 bg-slate-900/40">
              <CardHeader className="pb-2">
                <CardTitle className="text-xs font-semibold text-slate-400 uppercase tracking-wider">{cat}</CardTitle>
              </CardHeader>
              <CardContent className="p-0">
                <div className="px-6 divide-y divide-slate-800/60">
                  {localFlags.filter(f => f.category === cat).map(flag => (
                    <FieldRow key={flag.id} label={flag.name.replace(/_/g, " ")} description={flag.description}>
                      <div className="flex items-center gap-2">
                        {flag.readOnly && <span className="text-[10px] text-slate-600 border border-slate-700 px-1.5 py-0.5 rounded">Read-only</span>}
                        <Toggle id={`flag-${flag.id}`} value={flag.enabled} onChange={() => !flag.readOnly && toggleFlag(flag.id)} />
                      </div>
                    </FieldRow>
                  ))}
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}

// ─── About Section ────────────────────────────────────────────────────────────

function AboutSection() {
  return (
    <div>
      <SectionHeader title="About FlowForge" description="Platform version, build information, and legal." />
      <Card className="border-slate-800 bg-slate-900/40">
        <CardContent className="p-0">
          <div className="px-6 divide-y divide-slate-800/60">
            {[
              { label: "Platform Version", value: "1.0.0-SNAPSHOT" },
              { label: "API Service", value: "1.0.0-SNAPSHOT · Spring Boot 3.3.4" },
              { label: "Frontend", value: "1.0.0 · Next.js 16.2.10 · React 19" },
              { label: "Database", value: "PostgreSQL 16 · Flyway 10" },
              { label: "Message Bus", value: "Apache Kafka (Redpanda v24.1.2)" },
              { label: "Build", value: new Date().toLocaleDateString("en-IN", { year: "numeric", month: "long", day: "numeric" }) },
            ].map(({ label, value }) => (
              <FieldRow key={label} label={label} description={undefined}>
                <span className="text-xs font-mono text-slate-400">{value}</span>
              </FieldRow>
            ))}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

// ─── Main Settings Page ───────────────────────────────────────────────────────

export default function SettingsPage() {
  const [section, setSection] = React.useState<SettingsSection>("profile");
  const [mobileNavOpen, setMobileNavOpen] = React.useState(false);

  const SECTION_MAP: Record<SettingsSection, React.ReactNode> = {
    profile:       <ProfileSection />,
    organization:  <OrganizationSection />,
    members:       <MembersSection />,
    roles:         <RolesSection />,
    apikeys:       <ApiKeysSection />,
    notifications: <NotificationsSection />,
    audit:         <AuditSection />,
    security:      <SecuritySection />,
    preferences:   <PreferencesSection />,
    flags:         <FeatureFlagsSection />,
    about:         <AboutSection />,
  };

  return (
    <div className="flex gap-0 h-full -m-6" aria-label="Settings">
      {/* Left Nav */}
      <aside className="hidden md:flex flex-col w-56 flex-shrink-0 border-r border-slate-800/60 bg-slate-950/40 h-full overflow-y-auto">
        <div className="px-4 pt-5 pb-3">
          <h1 className="text-xs font-semibold text-slate-400 uppercase tracking-wider">Settings</h1>
        </div>
        <nav className="flex-1 px-2 pb-4 space-y-0.5">
          {NAV_ITEMS.map(item => (
            <button
              key={item.id}
              onClick={() => setSection(item.id)}
              className={cn(
                "w-full flex items-center gap-2.5 px-3 py-2 rounded-md text-sm font-medium transition-all text-left",
                section === item.id
                  ? "bg-violet-950/50 text-violet-300 border border-violet-800/40"
                  : "text-slate-400 hover:text-slate-200 hover:bg-slate-800/50 border border-transparent"
              )}
              aria-current={section === item.id ? "page" : undefined}
            >
              {item.icon}
              <span className="flex-1">{item.label}</span>
              {item.badge && <span className="text-[9px] px-1.5 py-0.5 rounded-full bg-amber-950/60 border border-amber-800/40 text-amber-400 font-semibold">{item.badge}</span>}
            </button>
          ))}
        </nav>
      </aside>

      {/* Mobile nav trigger */}
      <div className="md:hidden fixed bottom-4 left-4 z-40">
        <Button size="sm" onClick={() => setMobileNavOpen(o => !o)} className="shadow-lg gap-2">
          <SlidersHorizontal className="h-4 w-4" />
          {NAV_ITEMS.find(n => n.id === section)?.label}
        </Button>
      </div>

      {/* Mobile nav panel */}
      {mobileNavOpen && (
        <div className="md:hidden fixed inset-0 z-50 bg-slate-950/80 backdrop-blur-sm" onClick={() => setMobileNavOpen(false)}>
          <div className="absolute bottom-0 left-0 right-0 bg-slate-900 border-t border-slate-800 rounded-t-2xl p-4" onClick={e => e.stopPropagation()}>
            <div className="grid grid-cols-2 gap-2">
              {NAV_ITEMS.map(item => (
                <button key={item.id} onClick={() => { setSection(item.id); setMobileNavOpen(false); }}
                  className={cn("flex items-center gap-2 px-3 py-2.5 rounded-lg text-sm font-medium transition-all",
                    section === item.id ? "bg-violet-950/60 text-violet-300 border border-violet-800/40" : "text-slate-400 hover:bg-slate-800 border border-transparent"
                  )}>
                  {item.icon}{item.label}
                </button>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Main Content */}
      <main className="flex-1 overflow-y-auto p-6 min-w-0">
        <div className="max-w-3xl mx-auto">
          {SECTION_MAP[section]}
        </div>
      </main>
    </div>
  );
}
