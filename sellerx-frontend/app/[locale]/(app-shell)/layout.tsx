"use client";

import { AuthProvider } from "@/hooks/useAuth";
import { AppLayout } from "@/components/layout";
import { ImpersonationBanner } from "@/components/admin/impersonation-banner";

export default function AppShellLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <AuthProvider>
      <ImpersonationBanner />
      <AppLayout>{children}</AppLayout>
    </AuthProvider>
  );
}
