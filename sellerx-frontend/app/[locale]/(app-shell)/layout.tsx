"use client";

import { AuthProvider } from "@/hooks/useAuth";
import { AppLayout } from "@/components/layout";

export default function AppShellLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <AuthProvider>
      <AppLayout>{children}</AppLayout>
    </AuthProvider>
  );
}
