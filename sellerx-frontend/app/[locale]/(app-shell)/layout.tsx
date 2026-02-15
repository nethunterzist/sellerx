"use client";

import { AuthProvider } from "@/hooks/useAuth";
import { AppLayout } from "@/components/layout";
import { ImpersonationBanner } from "@/components/admin/impersonation-banner";
import { CurrencyProvider } from "@/lib/contexts/currency-context";
import { WebSocketProvider } from "@/components/providers/websocket-provider";

export default function AppShellLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <AuthProvider>
      <CurrencyProvider>
        <WebSocketProvider showToasts={true}>
          <ImpersonationBanner />
          <AppLayout>{children}</AppLayout>
        </WebSocketProvider>
      </CurrencyProvider>
    </AuthProvider>
  );
}
