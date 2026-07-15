import type { Metadata, Viewport } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
  display: "swap",
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
  display: "swap",
});

export const viewport: Viewport = {
  themeColor: [
    { media: "(prefers-color-scheme: dark)", color: "#020617" },
    { media: "(prefers-color-scheme: light)", color: "#f8fafc" },
  ],
  width: "device-width",
  initialScale: 1,
};

export const metadata: Metadata = {
  metadataBase: new URL("https://flowforge.dev"),
  title: {
    default: "FlowForge — Distributed Job Orchestration & Workflow Engine",
    template: "%s | FlowForge",
  },
  description:
    "FlowForge is a resilient, multi-tenant execution manager designed for high-throughput distributed job scheduling, conditional DAG workflow routing, heartbeated worker pools, and real-time observability.",
  keywords: [
    "workflow orchestration",
    "job scheduler",
    "distributed computing",
    "DAG workflows",
    "Kafka",
    "multi-tenant",
    "task queue",
    "execution engine",
    "FlowForge",
  ],
  authors: [{ name: "FlowForge Team", url: "https://flowforge.dev" }],
  creator: "FlowForge Inc.",
  publisher: "FlowForge Inc.",
  robots: {
    index: true,
    follow: true,
    googleBot: { index: true, follow: true, "max-snippet": -1, "max-image-preview": "large" },
  },
  openGraph: {
    type: "website",
    locale: "en_US",
    url: "https://flowforge.dev",
    siteName: "FlowForge",
    title: "FlowForge — Distributed Job Orchestration & Workflow Engine",
    description:
      "A resilient, multi-tenant execution manager for high-throughput job scheduling, DAG workflows, and real-time observability.",
    images: [
      {
        url: "/og-image.png",
        width: 1200,
        height: 630,
        alt: "FlowForge — Distributed Workflow Orchestration",
      },
    ],
  },
  twitter: {
    card: "summary_large_image",
    title: "FlowForge — Distributed Job Orchestration",
    description: "High-throughput job scheduling, DAG workflows, and real-time observability.",
    images: ["/og-image.png"],
    creator: "@flowforge",
  },
  icons: {
    icon: [{ url: "/favicon.ico" }, { url: "/icon-192.png", sizes: "192x192", type: "image/png" }],
    apple: [{ url: "/apple-icon.png", sizes: "180x180", type: "image/png" }],
  },
  manifest: "/manifest.json",
  alternates: { canonical: "https://flowforge.dev" },
};

export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html
      lang="en"
      className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}
      suppressHydrationWarning
    >
      <body className="min-h-full flex flex-col bg-slate-950 text-slate-100">
        {children}
      </body>
    </html>
  );
}
