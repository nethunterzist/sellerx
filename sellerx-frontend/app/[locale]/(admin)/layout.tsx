"use client";

import { useEffect } from "react";
import { useRouter } from "@/i18n/navigation";
import { AuthProvider, useAuth } from "@/hooks/useAuth";
import { AdminLayout } from "@/components/admin";
import { Loader2 } from "lucide-react";

function AdminProtectedContent({ children }: { children: React.ReactNode }) {
  const { user, loading } = useAuth();
  const router = useRouter();

  useEffect(() => {
    // If user is loaded and not an admin, redirect to dashboard
    if (!loading && user && user.role !== "ADMIN") {
      router.replace("/dashboard");
    }
  }, [user, loading, router]);

  // Show loading while checking auth
  if (loading) {
    return (
      <div className="flex h-screen items-center justify-center bg-slate-900">
        <Loader2 className="h-8 w-8 animate-spin text-amber-500" />
      </div>
    );
  }

  // If not admin, show nothing (redirect will happen)
  if (!user || user.role !== "ADMIN") {
    return (
      <div className="flex h-screen items-center justify-center bg-slate-900">
        <Loader2 className="h-8 w-8 animate-spin text-amber-500" />
      </div>
    );
  }

  return <AdminLayout>{children}</AdminLayout>;
}

export default function AdminGroupLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <AuthProvider>
      <AdminProtectedContent>{children}</AdminProtectedContent>
    </AuthProvider>
  );
}
