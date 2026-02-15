"use client";

import { Sidebar } from "./sidebar";
import { Header } from "./header";
import { KeyboardShortcutsProvider } from "@/components/providers/keyboard-shortcuts-provider";
import { SyncLogProvider } from "@/lib/contexts/sync-log-context";
import { SidebarProvider, useSidebar } from "@/lib/contexts/sidebar-context";
import { useGlobalSync } from "@/hooks/useGlobalSync";
import { cn } from "@/lib/utils";
import { useImpersonation } from "@/hooks/use-impersonation";
import { motion } from "motion/react";
import { usePathname } from "next/navigation";

interface AppLayoutProps {
  children: React.ReactNode;
}

function AppLayoutContent({ children }: AppLayoutProps) {
  // Enable global data sync based on user preferences
  useGlobalSync();
  const { collapsed } = useSidebar();
  const { isImpersonating } = useImpersonation();
  const pathname = usePathname();

  return (
    <KeyboardShortcutsProvider>
      <div className="min-h-screen bg-muted">
        <Sidebar />
        <Header />
        <main
          className={cn(
            "transition-all duration-300",
            collapsed ? "ml-[60px]" : "ml-[220px]",
            isImpersonating ? "pt-24" : "pt-14"
          )}
        >
          <motion.div
            key={pathname}
            initial={{ opacity: 0, y: 6 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.15, ease: [0.25, 0.1, 0.25, 1] as const }}
            className="p-4"
          >
            {children}
          </motion.div>
        </main>
      </div>
    </KeyboardShortcutsProvider>
  );
}

function AppLayoutInner({ children }: AppLayoutProps) {
  return (
    <SidebarProvider>
      <AppLayoutContent>{children}</AppLayoutContent>
    </SidebarProvider>
  );
}

export function AppLayout({ children }: AppLayoutProps) {
  return (
    <SyncLogProvider>
      <AppLayoutInner>{children}</AppLayoutInner>
    </SyncLogProvider>
  );
}
