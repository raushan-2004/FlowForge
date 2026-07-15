"use client";

import * as React from "react";
import Link from "next/link";
import {
  Rocket, FolderKanban, Cpu, GitBranch, Play, LayoutDashboard,
  X, ChevronRight, Check, Sparkles,
} from "lucide-react";
import { cn } from "@/lib/utils";

// ─── Onboarding steps ─────────────────────────────────────────────────────────

interface OnboardingStep {
  id: string;
  icon: React.ReactNode;
  title: string;
  description: string;
  action: string;
  href: string;
}

const STEPS: OnboardingStep[] = [
  {
    id: "welcome",
    icon: <Sparkles className="h-5 w-5" />,
    title: "Welcome to FlowForge",
    description: "You're in! FlowForge is your distributed job orchestration console. Let's get you set up in a few steps.",
    action: "Start Setup",
    href: "#",
  },
  {
    id: "project",
    icon: <FolderKanban className="h-5 w-5" />,
    title: "Create your first project",
    description: "Projects are the top-level namespace for organizing your jobs, workflows, and API keys.",
    action: "Go to Projects",
    href: "/projects",
  },
  {
    id: "job",
    icon: <Cpu className="h-5 w-5" />,
    title: "Create your first job",
    description: "Define an HTTP endpoint to call, a schedule, and a retry policy. Your first job is the unit of work.",
    action: "Go to Jobs",
    href: "/jobs",
  },
  {
    id: "workflow",
    icon: <GitBranch className="h-5 w-5" />,
    title: "Build your first workflow",
    description: "Chain jobs into a DAG pipeline using the visual Workflow Builder with drag-and-drop simplicity.",
    action: "Open Builder",
    href: "/workflows",
  },
  {
    id: "execution",
    icon: <Play className="h-5 w-5" />,
    title: "Trigger your first execution",
    description: "Hit the trigger button to dispatch your workflow. Watch it run in real-time in the Executions view.",
    action: "View Executions",
    href: "/executions",
  },
  {
    id: "dashboard",
    icon: <LayoutDashboard className="h-5 w-5" />,
    title: "Explore the dashboard",
    description: "Your main command center — live execution metrics, system health, and quick access to everything.",
    action: "View Dashboard",
    href: "/dashboard",
  },
];

const STORAGE_KEY = "flowforge-onboarding";

interface OnboardingState {
  dismissed: boolean;
  completedSteps: string[];
  currentStep: number;
}

// ─── Hook ─────────────────────────────────────────────────────────────────────

export function useOnboarding() {
  const [state, setState] = React.useState<OnboardingState>({
    dismissed: false,
    completedSteps: [],
    currentStep: 0,
  });
  const [mounted, setMounted] = React.useState(false);

  React.useEffect(() => {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (saved) {
      try { setState(JSON.parse(saved)); } catch {}
    }
    setMounted(true);
  }, []);

  function save(next: OnboardingState) {
    setState(next);
    localStorage.setItem(STORAGE_KEY, JSON.stringify(next));
  }

  function completeStep(id: string) {
    const next = {
      ...state,
      completedSteps: [...new Set([...state.completedSteps, id])],
      currentStep: Math.min(state.currentStep + 1, STEPS.length - 1),
    };
    save(next);
  }

  function dismiss() { save({ ...state, dismissed: true }); }
  function reset() { save({ dismissed: false, completedSteps: [], currentStep: 0 }); }

  const isComplete = state.completedSteps.length >= STEPS.length;
  const show = mounted && !state.dismissed && !isComplete;

  return { state, show, completeStep, dismiss, reset, isComplete, STEPS };
}

// ─── Onboarding Banner ────────────────────────────────────────────────────────

export function OnboardingBanner() {
  const { state, show, completeStep, dismiss, STEPS } = useOnboarding();

  if (!show) return null;

  const step = STEPS[state.currentStep];
  const progress = (state.completedSteps.length / STEPS.length) * 100;

  return (
    <div
      role="region"
      aria-label="Setup guide"
      className="mb-6 rounded-xl border border-violet-800/40 bg-gradient-to-r from-violet-950/50 to-indigo-950/40 p-5 relative overflow-hidden"
    >
      {/* Background decoration */}
      <div className="absolute top-0 right-0 w-48 h-48 bg-violet-600/5 rounded-full -translate-y-1/2 translate-x-1/4 pointer-events-none" />

      <button
        onClick={dismiss}
        className="absolute top-3 right-3 text-slate-500 hover:text-slate-300 transition-colors p-1"
        aria-label="Dismiss setup guide"
      >
        <X className="h-4 w-4" />
      </button>

      <div className="flex items-start gap-4">
        {/* Icon */}
        <div className="flex-shrink-0 h-10 w-10 rounded-lg bg-violet-600/30 border border-violet-600/40 flex items-center justify-center text-violet-300">
          {step.icon}
        </div>

        {/* Content */}
        <div className="flex-1 min-w-0 pr-8">
          <div className="flex items-center gap-2 mb-1">
            <p className="text-xs font-semibold text-violet-400 uppercase tracking-wider">
              Setup Guide · Step {state.currentStep + 1} of {STEPS.length}
            </p>
          </div>
          <h3 className="font-semibold text-slate-100 text-sm mb-1">{step.title}</h3>
          <p className="text-xs text-slate-400 leading-relaxed mb-3">{step.description}</p>

          {/* Progress bar */}
          <div className="mb-3">
            <div className="flex items-center justify-between mb-1">
              <span className="text-[10px] text-slate-500">{state.completedSteps.length} of {STEPS.length} completed</span>
              <span className="text-[10px] font-mono text-violet-400">{Math.round(progress)}%</span>
            </div>
            <div className="h-1.5 bg-slate-800 rounded-full overflow-hidden">
              <div
                className="h-full bg-violet-500 rounded-full transition-all duration-500"
                style={{ width: `${progress}%` }}
                role="progressbar"
                aria-valuenow={Math.round(progress)}
                aria-valuemin={0}
                aria-valuemax={100}
              />
            </div>
          </div>

          {/* Step dots */}
          <div className="flex items-center gap-1.5 mb-3">
            {STEPS.map((s, i) => (
              <div
                key={s.id}
                className={cn(
                  "h-2 rounded-full transition-all duration-300",
                  state.completedSteps.includes(s.id)
                    ? "w-4 bg-violet-500"
                    : i === state.currentStep
                    ? "w-4 bg-violet-600/60 border border-violet-500"
                    : "w-2 bg-slate-700"
                )}
              />
            ))}
          </div>

          {/* Actions */}
          <div className="flex items-center gap-3">
            {step.href !== "#" ? (
              <Link
                href={step.href}
                onClick={() => completeStep(step.id)}
                className="inline-flex items-center gap-1.5 h-8 px-4 rounded-lg bg-violet-600 hover:bg-violet-500 text-white text-xs font-semibold transition-colors"
              >
                {step.action} <ChevronRight className="h-3.5 w-3.5" />
              </Link>
            ) : (
              <button
                onClick={() => completeStep(step.id)}
                className="inline-flex items-center gap-1.5 h-8 px-4 rounded-lg bg-violet-600 hover:bg-violet-500 text-white text-xs font-semibold transition-colors"
              >
                {step.action} <ChevronRight className="h-3.5 w-3.5" />
              </button>
            )}
            <button
              onClick={dismiss}
              className="text-xs text-slate-500 hover:text-slate-300 transition-colors"
            >
              Skip setup
            </button>
          </div>
        </div>
      </div>

      {/* Step checklist */}
      <div className="mt-4 pt-4 border-t border-slate-800/60 grid grid-cols-2 sm:grid-cols-3 gap-2">
        {STEPS.map((s, i) => {
          const done = state.completedSteps.includes(s.id);
          const current = i === state.currentStep && !done;
          return (
            <div key={s.id} className={cn(
              "flex items-center gap-2 text-xs px-2 py-1 rounded-md",
              done ? "text-emerald-400" : current ? "text-violet-300" : "text-slate-600"
            )}>
              {done
                ? <Check className="h-3 w-3 flex-shrink-0" />
                : current
                ? <div className="h-3 w-3 rounded-full border-2 border-violet-400 flex-shrink-0" />
                : <div className="h-3 w-3 rounded-full border border-slate-700 flex-shrink-0" />
              }
              <span className="truncate">{s.title.replace("your first ", "")}</span>
            </div>
          );
        })}
      </div>
    </div>
  );
}
