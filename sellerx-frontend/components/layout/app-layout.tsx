"use client";

import { Sidebar } from "./sidebar";
import { Header } from "./header";
import { KeyboardShortcutsProvider } from "@/components/providers/keyboard-shortcuts-provider";
import { SyncLogProvider } from "@/lib/contexts/sync-log-context";
import { SidebarProvider, useSidebar } from "@/lib/contexts/sidebar-context";
import { useGlobalSync } from "@/hooks/useGlobalSync";
import { cn } from "@/lib/utils";

interface AppLayoutProps {
  children: React.ReactNode;
}

function AppLayoutContent({ children }: AppLayoutProps) {
  // Enable global data sync based on user preferences
  useGlobalSync();
  const { collapsed } = useSidebar();

  return (
    <KeyboardShortcutsProvider>
      <div className="min-h-screen bg-muted">
        <Sidebar />
        <Header />
        <main
          className={cn(
            "pt-14 transition-all duration-300",
            collapsed ? "ml-[60px]" : "ml-[220px]"
          )}
        >
          <div className="p-4">
            {children}
          </div>
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
