"use client";

import * as React from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useForm } from "react-hook-form";
import { z } from "zod";
import { useAuth } from "@/providers/AuthProvider";
import { Button } from "@/components/ui/Button";
import { Card, CardHeader, CardTitle, CardDescription, CardContent, CardFooter } from "@/components/ui/Card";
import { KeyRound, Mail, Eye, EyeOff, AlertTriangle } from "lucide-react";

// 1. Zod Validation Schema
const loginSchema = z.object({
  email: z.string().email("Please enter a valid email address"),
  password: z.string().min(6, "Password must be at least 6 characters"),
  rememberMe: z.boolean().optional(),
});

type LoginFormValues = z.infer<typeof loginSchema>;

function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { login } = useAuth();
  
  const [showPassword, setShowPassword] = React.useState(false);
  const [isSubmitting, setIsSubmitting] = React.useState(false);
  const [apiError, setApiError] = React.useState<string | null>(null);

  // 2. React Hook Form Integration
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFormValues>({
    defaultValues: {
      email: "",
      password: "",
      rememberMe: false,
    },
  });

  const onSubmit = async (values: LoginFormValues) => {
    setIsSubmitting(false);
    setApiError(null);
    setIsSubmitting(true);

    try {
      await login(values.email, values.password);
      
      // Determine redirection path
      const redirectUrl = searchParams.get("redirect") || "/dashboard";
      router.push(redirectUrl);
    } catch (error: any) {
      console.error("Authentication failed:", error);
      setApiError(error.message || "Invalid credentials. Please verify your email and password.");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Card className="w-full max-w-md bg-slate-900 border-slate-800 shadow-xl">
      <CardHeader className="text-center space-y-2">
        <div className="mx-auto h-10 w-10 rounded-md bg-purple-600 flex items-center justify-center font-bold text-white text-lg">
          F
        </div>
        <CardTitle className="text-xl">FlowForge Portal</CardTitle>
        <CardDescription>Sign in to manage workflows, triggers, and cluster workers.</CardDescription>
      </CardHeader>

      <form onSubmit={handleSubmit(onSubmit)}>
        <CardContent className="space-y-4">
          {/* Centralized API errors */}
          {apiError && (
            <div className="p-3 rounded-md bg-red-950/40 border border-red-900/50 flex items-start space-x-2 text-xs text-red-300">
              <AlertTriangle className="h-4 w-4 shrink-0 mt-0.5" />
              <span>{apiError}</span>
            </div>
          )}

          {/* Email input field */}
          <div className="space-y-1.5">
            <label className="text-xs font-semibold text-slate-400">Email Address</label>
            <div className="relative flex items-center">
              <Mail className="absolute left-3 h-4 w-4 text-slate-500 pointer-events-none" />
              <input
                type="email"
                placeholder="you@example.com"
                {...register("email")}
                className="h-10 w-full rounded-md border border-slate-800 bg-slate-950 pl-9 pr-3 text-sm text-slate-200 placeholder:text-slate-500 focus-visible:outline-hidden focus-visible:ring-2 focus-visible:ring-purple-500"
              />
            </div>
            {errors.email && (
              <p className="text-xs font-medium text-red-400">{errors.email.message}</p>
            )}
          </div>

          {/* Password input field */}
          <div className="space-y-1.5">
            <div className="flex justify-between items-center">
              <label className="text-xs font-semibold text-slate-400">Password</label>
              <button
                type="button"
                onClick={() => alert("Forgot password recovery is a placeholder on development sandboxes.")}
                className="text-[10px] text-purple-400 hover:underline focus:outline-hidden cursor-pointer"
              >
                Forgot Password?
              </button>
            </div>
            <div className="relative flex items-center">
              <KeyRound className="absolute left-3 h-4 w-4 text-slate-500 pointer-events-none" />
              <input
                type={showPassword ? "text" : "password"}
                placeholder="••••••••"
                {...register("password")}
                className="h-10 w-full rounded-md border border-slate-800 bg-slate-950 pl-9 pr-10 text-sm text-slate-200 placeholder:text-slate-500 focus-visible:outline-hidden focus-visible:ring-2 focus-visible:ring-purple-500"
              />
              <button
                type="button"
                onClick={() => setShowPassword((prev) => !prev)}
                className="absolute right-3 p-1 rounded-sm text-slate-500 hover:text-slate-300 focus:outline-hidden cursor-pointer"
              >
                {showPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
              </button>
            </div>
            {errors.password && (
              <p className="text-xs font-medium text-red-400">{errors.password.message}</p>
            )}
          </div>

          {/* Remember Me checkbox */}
          <div className="flex items-center space-x-2">
            <input
              type="checkbox"
              id="rememberMe"
              {...register("rememberMe")}
              className="rounded-sm border-slate-800 bg-slate-950 text-purple-600 focus:ring-purple-500 h-4 w-4"
            />
            <label htmlFor="rememberMe" className="text-xs font-semibold text-slate-400 select-none cursor-pointer">
              Remember Me
            </label>
          </div>
        </CardContent>

        <CardFooter className="flex flex-col space-y-4">
          <Button type="submit" className="w-full" disabled={isSubmitting}>
            {isSubmitting ? "Authenticating..." : "Sign In"}
          </Button>
          <p className="text-xs text-center text-slate-500">
            For local developer sandboxes, use any valid credentials.
          </p>
        </CardFooter>
      </form>
    </Card>
  );
}

export default function LoginPage() {
  return (
    <React.Suspense
      fallback={
        <div className="flex items-center justify-center p-8 bg-slate-900 border border-slate-800 rounded-lg w-full max-w-md h-96">
          <span className="text-sm text-slate-400 font-semibold animate-pulse">Loading auth form...</span>
        </div>
      }
    >
      <LoginForm />
    </React.Suspense>
  );
}
