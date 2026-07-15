// Settings services: Profile, Organization, Members, Audit, Notifications, Feature Flags

function delay(ms: number) {
  return new Promise<void>((r) => setTimeout(r, ms));
}

// ─── Profile Service ──────────────────────────────────────────────────────────

export interface UserProfile {
  id: string;
  name: string;
  email: string;
  avatarUrl?: string;
  timeZone: string;
  language: string;
  lastLogin: string;
  createdAt: string;
}

export const ProfileService = {
  async getProfile(): Promise<UserProfile> {
    await delay(200);
    return {
      id: "user-001",
      name: "Admin User",
      email: "login@flowforge.com",
      avatarUrl: undefined,
      timeZone: "Asia/Kolkata",
      language: "en",
      lastLogin: new Date(Date.now() - 3600000).toISOString(),
      createdAt: "2026-07-01T00:00:00Z",
    };
  },
  async updateProfile(patch: Partial<UserProfile>): Promise<UserProfile> {
    await delay(400);
    return { ...(await ProfileService.getProfile()), ...patch };
  },
};

// ─── Organization Service ─────────────────────────────────────────────────────

export interface Organization {
  id: string;
  name: string;
  description: string;
  slug: string;
  defaultTimeZone: string;
  defaultRetryPolicy: string;
  logoUrl?: string;
  plan: string;
  createdAt: string;
}

export const OrganizationService = {
  async getOrganization(): Promise<Organization> {
    await delay(200);
    return {
      id: "org-001",
      name: "FlowForge Dev Tenant",
      description: "Primary development tenant for FlowForge platform.",
      slug: "flowforge-dev",
      defaultTimeZone: "Asia/Kolkata",
      defaultRetryPolicy: "EXPONENTIAL_BACKOFF",
      plan: "Premium",
      createdAt: "2026-07-01T00:00:00Z",
    };
  },
  async updateOrganization(patch: Partial<Organization>): Promise<Organization> {
    await delay(500);
    return { ...(await OrganizationService.getOrganization()), ...patch };
  },
};

// ─── Member Service ───────────────────────────────────────────────────────────

export type MemberRole = "OWNER" | "ADMIN" | "DEVELOPER" | "VIEWER";
export type MemberStatus = "ACTIVE" | "SUSPENDED" | "INVITED";

export interface Member {
  id: string;
  name: string;
  email: string;
  role: MemberRole;
  status: MemberStatus;
  lastActive: string;
  avatarUrl?: string;
}

const MOCK_MEMBERS: Member[] = [
  { id: "m-1", name: "Admin User", email: "login@flowforge.com", role: "OWNER", status: "ACTIVE", lastActive: "Just now" },
  { id: "m-2", name: "Dev Alice", email: "alice@example.com", role: "DEVELOPER", status: "ACTIVE", lastActive: "2 hr ago" },
  { id: "m-3", name: "Bob Viewer", email: "bob@example.com", role: "VIEWER", status: "ACTIVE", lastActive: "1 day ago" },
  { id: "m-4", name: "Carol Admin", email: "carol@example.com", role: "ADMIN", status: "INVITED", lastActive: "Never" },
  { id: "m-5", name: "Dave Dev", email: "dave@example.com", role: "DEVELOPER", status: "SUSPENDED", lastActive: "5 day ago" },
];

export const MemberService = {
  async listMembers(): Promise<Member[]> {
    await delay(250);
    return [...MOCK_MEMBERS];
  },
  async inviteMember(email: string, role: MemberRole): Promise<Member> {
    await delay(400);
    return { id: `m-${Date.now()}`, name: email.split("@")[0], email, role, status: "INVITED", lastActive: "Never" };
  },
  async updateRole(memberId: string, role: MemberRole): Promise<Member> {
    await delay(300);
    const m = MOCK_MEMBERS.find((x) => x.id === memberId)!;
    return { ...m, role };
  },
  async suspendMember(memberId: string): Promise<void> { await delay(300); },
  async reactivateMember(memberId: string): Promise<void> { await delay(300); },
  async removeMember(memberId: string): Promise<void> { await delay(300); },
};

// ─── Audit Service ────────────────────────────────────────────────────────────

export interface AuditEntry {
  id: string;
  timestamp: string;
  user: string;
  userEmail: string;
  action: string;
  resource: string;
  resourceId?: string;
  ipAddress?: string;
  result: "SUCCESS" | "FAILURE" | "DENIED";
  details?: string;
}

const ACTIONS = [
  "USER_LOGIN", "USER_LOGOUT", "API_KEY_CREATED", "API_KEY_REVOKED", "API_KEY_ROTATED",
  "JOB_CREATED", "JOB_UPDATED", "JOB_DELETED", "PROJECT_CREATED", "PROJECT_ARCHIVED",
  "MEMBER_INVITED", "MEMBER_REMOVED", "ROLE_CHANGED", "SETTINGS_UPDATED", "EXECUTION_TRIGGERED",
];

function randomAuditEntry(i: number): AuditEntry {
  const action = ACTIONS[i % ACTIONS.length];
  const ts = new Date(Date.now() - i * 420000).toISOString();
  const users = ["Admin User", "Dev Alice", "Carol Admin", "Bob Viewer"];
  const emails = ["login@flowforge.com", "alice@example.com", "carol@example.com", "bob@example.com"];
  const idx = i % users.length;
  const ips = ["203.0.113.1", "198.51.100.42", "192.0.2.100", "203.0.113.55"];
  return {
    id: `audit-${i}`,
    timestamp: ts,
    user: users[idx],
    userEmail: emails[idx],
    action,
    resource: action.split("_")[0].toLowerCase(),
    resourceId: `res-${Math.floor(Math.random() * 1000)}`,
    ipAddress: ips[i % ips.length],
    result: i % 15 === 0 ? "FAILURE" : i % 20 === 0 ? "DENIED" : "SUCCESS",
    details: action === "ROLE_CHANGED" ? "Role changed from VIEWER to DEVELOPER" : undefined,
  };
}

export const MOCK_AUDIT_ENTRIES: AuditEntry[] = Array.from({ length: 120 }, (_, i) => randomAuditEntry(i));

export const AuditService = {
  async listAudit(opts?: { search?: string; result?: string; action?: string }): Promise<AuditEntry[]> {
    await delay(300);
    let entries = [...MOCK_AUDIT_ENTRIES];
    if (opts?.search) {
      const q = opts.search.toLowerCase();
      entries = entries.filter(
        (e) =>
          e.user.toLowerCase().includes(q) ||
          e.action.toLowerCase().includes(q) ||
          e.resource.toLowerCase().includes(q) ||
          e.userEmail.toLowerCase().includes(q)
      );
    }
    if (opts?.result && opts.result !== "ALL") {
      entries = entries.filter((e) => e.result === opts.result);
    }
    if (opts?.action && opts.action !== "ALL") {
      entries = entries.filter((e) => e.action === opts.action);
    }
    return entries;
  },
  exportCsv(entries: AuditEntry[]): void {
    const header = "Timestamp,User,Email,Action,Resource,IP,Result\n";
    const rows = entries
      .map((e) => `${e.timestamp},${e.user},${e.userEmail},${e.action},${e.resource},${e.ipAddress ?? ""},${e.result}`)
      .join("\n");
    const blob = new Blob([header + rows], { type: "text/csv" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `flowforge-audit-${new Date().toISOString()}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  },
};

// ─── Notification Service ─────────────────────────────────────────────────────

export interface NotificationPrefs {
  emailEnabled: boolean;
  browserEnabled: boolean;
  retryAlerts: boolean;
  workerOfflineAlerts: boolean;
  queueAlerts: boolean;
  workflowFailureAlerts: boolean;
  alertThresholdRetries: number;
  alertThresholdQueueDepth: number;
}

export const NotificationService = {
  async getPrefs(): Promise<NotificationPrefs> {
    await delay(200);
    const saved = typeof window !== "undefined" ? localStorage.getItem("flowforge-notification-prefs") : null;
    if (saved) return JSON.parse(saved);
    return {
      emailEnabled: true,
      browserEnabled: false,
      retryAlerts: true,
      workerOfflineAlerts: true,
      queueAlerts: false,
      workflowFailureAlerts: true,
      alertThresholdRetries: 5,
      alertThresholdQueueDepth: 50,
    };
  },
  async savePrefs(prefs: NotificationPrefs): Promise<void> {
    await delay(300);
    if (typeof window !== "undefined") {
      localStorage.setItem("flowforge-notification-prefs", JSON.stringify(prefs));
    }
  },
};

// ─── Feature Flags ────────────────────────────────────────────────────────────

export interface FeatureFlag {
  id: string;
  name: string;
  description: string;
  enabled: boolean;
  category: string;
  readOnly: boolean;
}

export const FeatureFlagService = {
  async listFlags(): Promise<FeatureFlag[]> {
    await delay(200);
    return [
      { id: "ff-1", name: "workflow_dag_v2", description: "Enable the new DAG v2 workflow execution engine.", enabled: true, category: "Engine", readOnly: true },
      { id: "ff-2", name: "kafka_event_streaming", description: "Route execution events through Kafka topics.", enabled: true, category: "Messaging", readOnly: true },
      { id: "ff-3", name: "advanced_retry_policies", description: "Enable custom exponential backoff retry configurations.", enabled: true, category: "Execution", readOnly: false },
      { id: "ff-4", name: "worker_auto_scaling", description: "Automatically scale workers based on queue depth.", enabled: false, category: "Infrastructure", readOnly: false },
      { id: "ff-5", name: "opentelemetry_tracing", description: "Emit OpenTelemetry spans for all executions.", enabled: true, category: "Observability", readOnly: true },
      { id: "ff-6", name: "rate_limiting_valkey", description: "Use Valkey-backed rate limiting for API endpoints.", enabled: true, category: "Security", readOnly: true },
      { id: "ff-7", name: "bulk_job_trigger", description: "Allow bulk trigger of up to 1000 jobs per API call.", enabled: false, category: "Execution", readOnly: false },
      { id: "ff-8", name: "audit_log_streaming", description: "Stream audit events to external SIEM systems.", enabled: false, category: "Security", readOnly: false },
    ];
  },
};

// ─── Permission Matrix ────────────────────────────────────────────────────────

export const PERMISSION_MATRIX: Record<MemberRole, string[]> = {
  OWNER: ["VIEW_PROJECTS", "CREATE_PROJECTS", "MANAGE_USERS", "VIEW_ANALYTICS", "MANAGE_API_KEYS", "DELETE_PROJECTS", "MANAGE_SETTINGS"],
  ADMIN: ["VIEW_PROJECTS", "CREATE_PROJECTS", "MANAGE_USERS", "VIEW_ANALYTICS", "MANAGE_API_KEYS", "DELETE_PROJECTS"],
  DEVELOPER: ["VIEW_PROJECTS", "CREATE_PROJECTS", "VIEW_ANALYTICS", "MANAGE_API_KEYS"],
  VIEWER: ["VIEW_PROJECTS"],
};

export const ALL_PERMISSIONS = [
  { id: "VIEW_PROJECTS", label: "View Projects", description: "Can view all projects and their details" },
  { id: "CREATE_PROJECTS", label: "Create Projects", description: "Can create and edit projects and jobs" },
  { id: "MANAGE_USERS", label: "Manage Users", description: "Can invite, suspend, and remove members" },
  { id: "VIEW_ANALYTICS", label: "View Analytics", description: "Can access the analytics and observability panels" },
  { id: "MANAGE_API_KEYS", label: "Manage API Keys", description: "Can create, rotate, and revoke API keys" },
  { id: "DELETE_PROJECTS", label: "Delete Projects", description: "Can archive and permanently delete projects" },
  { id: "MANAGE_SETTINGS", label: "Manage Settings", description: "Can modify organization-level settings" },
];
