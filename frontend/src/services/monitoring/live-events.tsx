"use client";

import { useEffect, useState, createContext, useContext, useRef, useCallback } from "react";

export type SystemEventType =
  | "WORKER_UPDATE"
  | "SCHEDULER_UPDATE"
  | "QUEUE_UPDATE"
  | "KAFKA_UPDATE"
  | "DATABASE_UPDATE"
  | "EXECUTION_EVENT"
  | "ALERT_EVENT";

export interface LiveEventPayload<T = any> {
  type: SystemEventType;
  timestamp: string;
  data: T;
}

export type LiveEventListener = (event: LiveEventPayload) => void;

class SSEConnectionManager {
  private listeners: Map<SystemEventType, Set<LiveEventListener>> = new Map();
  private reconnectTimeout: NodeJS.Timeout | null = null;
  private heartbeatInterval: NodeJS.Timeout | null = null;
  private backoffDelay = 1000;
  private maxBackoff = 30000;
  private lastHeartbeat = Date.now();
  private isClosed = false;

  public onStatusChange: (status: "connecting" | "connected" | "disconnected") => void = () => {};

  constructor() {
    this.connect();
  }

  private connect() {
    if (this.isClosed) return;
    this.onStatusChange("connecting");

    // Simulate SSE source connection
    setTimeout(() => {
      this.onStatusChange("connected");
      this.backoffDelay = 1000;
      this.lastHeartbeat = Date.now();
      this.startHeartbeatCheck();
      this.startSimulation();
    }, 800);
  }

  private startHeartbeatCheck() {
    if (this.heartbeatInterval) clearInterval(this.heartbeatInterval);
    this.heartbeatInterval = setInterval(() => {
      const diff = Date.now() - this.lastHeartbeat;
      if (diff > 12000) {
        // Heartbeat timeout (stale connection), reconnect
        this.handleDisconnect();
      }
    }, 5000);
  }

  private handleDisconnect() {
    this.onStatusChange("disconnected");
    if (this.heartbeatInterval) clearInterval(this.heartbeatInterval);
    
    if (this.reconnectTimeout) clearTimeout(this.reconnectTimeout);
    this.reconnectTimeout = setTimeout(() => {
      this.backoffDelay = Math.min(this.backoffDelay * 2, this.maxBackoff);
      this.connect();
    }, this.backoffDelay);
  }

  public subscribe(type: SystemEventType, listener: LiveEventListener) {
    if (!this.listeners.has(type)) {
      this.listeners.set(type, new Set());
    }
    this.listeners.get(type)!.add(listener);
    return () => {
      this.listeners.get(type)?.delete(listener);
    };
  }

  public emit(type: SystemEventType, data: any) {
    this.lastHeartbeat = Date.now();
    const event: LiveEventPayload = {
      type,
      timestamp: new Date().toISOString(),
      data,
    };
    this.listeners.get(type)?.forEach((listener) => listener(event));
  }

  private startSimulation() {
    // Generate periodic mocked SSE network payloads
    const interval = setInterval(() => {
      if (this.isClosed) {
        clearInterval(interval);
        return;
      }

      // 1. Worker updates
      this.emit("WORKER_UPDATE", {
        publicId: "worker-node-1",
        status: "ACTIVE",
        lastHeartbeat: new Date().toISOString(),
        activeLeases: Math.floor(Math.random() * 5),
        runningExecutions: Math.floor(Math.random() * 4),
        cpuUsage: Math.floor(10 + Math.random() * 40),
        memoryUsage: Math.floor(30 + Math.random() * 20),
        version: "v1.2.0"
      });

      // 2. Queue updates
      this.emit("QUEUE_UPDATE", {
        pending: Math.floor(1 + Math.random() * 10),
        running: Math.floor(2 + Math.random() * 5),
        retry: Math.floor(Math.random() * 3),
        dlq: Math.floor(Math.random() * 2),
        avgWaitTimeMs: Math.floor(150 + Math.random() * 100),
      });

      // 3. Database updates
      this.emit("DATABASE_UPDATE", {
        status: "HEALTHY",
        poolUsage: Math.floor(5 + Math.random() * 15),
        activeConnections: Math.floor(3 + Math.random() * 10),
        pendingConnections: 0,
        queryLatencyMs: Math.floor(5 + Math.random() * 15),
        transactionRate: Math.floor(20 + Math.random() * 50),
      });

      // 4. Kafka updates
      this.emit("KAFKA_UPDATE", {
        status: "HEALTHY",
        consumerLag: Math.floor(Math.random() * 12),
        publishRate: Math.floor(100 + Math.random() * 50),
        consumptionRate: Math.floor(95 + Math.random() * 50),
        failedPublishes: 0,
      });

      // 5. Random execution event
      const workflows = ["Ingest Request Pipeline", "Data Reconciliation", "Webhook Webhook-Sync"];
      const severities = ["INFO", "WARN", "ERROR"];
      if (Math.random() > 0.4) {
        this.emit("EXECUTION_EVENT", {
          id: `exec-feed-${Math.floor(Math.random() * 10000)}`,
          event: Math.random() > 0.5 ? "Execution Started" : "Execution Completed",
          timestamp: new Date().toLocaleTimeString(),
          severity: severities[Math.floor(Math.random() * 3)],
          project: "Main Workspace",
          workflow: workflows[Math.floor(Math.random() * workflows.length)],
        });
      }
    }, 3000);
  }

  public close() {
    this.isClosed = true;
    if (this.reconnectTimeout) clearTimeout(this.reconnectTimeout);
    if (this.heartbeatInterval) clearInterval(this.heartbeatInterval);
  }
}

export const sseManager = new SSEConnectionManager();

interface LiveEventContextProps {
  status: "connecting" | "connected" | "disconnected";
  subscribe: (type: SystemEventType, listener: LiveEventListener) => () => void;
}

const LiveEventContext = createContext<LiveEventContextProps | null>(null);

export function LiveEventProvider({ children }: { children: React.ReactNode }) {
  const [status, setStatus] = useState<"connecting" | "connected" | "disconnected">("connecting");

  useEffect(() => {
    sseManager.onStatusChange = (newStatus) => setStatus(newStatus);
    return () => {
      sseManager.close();
    };
  }, []);

  const subscribe = useCallback((type: SystemEventType, listener: LiveEventListener) => {
    return sseManager.subscribe(type, listener);
  }, []);

  return (
    <LiveEventContext.Provider value={{ status, subscribe }}>
      {children}
    </LiveEventContext.Provider>
  );
}

export function useLiveEvent() {
  const ctx = useContext(LiveEventContext);
  if (!ctx) {
    throw new Error("useLiveEvent must be used within a LiveEventProvider");
  }
  return ctx;
}
