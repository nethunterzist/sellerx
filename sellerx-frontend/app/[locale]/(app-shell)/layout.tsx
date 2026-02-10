"use client";

import { AuthProvider } from "@/hooks/useAuth";
import { AppLayout } from "@/components/layout";
import { ImpersonationBanner } from "@/components/admin/impersonation-banner";
import { CurrencyProvider } from "@/lib/contexts/currency-context";

export default function AppShellLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <AuthProvider>
      <CurrencyProvider>
        <ImpersonationBanner />
        <AppLayout>{children}</AppLayout>
      </CurrencyProvider>
    </AuthProvider>
  );
}
