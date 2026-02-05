"use client";

import { useEffect, useRef } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { useImpersonation } from "@/hooks/use-impersonation";
import type { ImpersonationMeta } from "@/hooks/use-impersonation";

export default function ImpersonatePage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { startImpersonation } = useImpersonation();
  const processed = useRef(false);

  useEffect(() => {
    if (processed.current) return;
    processed.current = true;

    const token = searchParams.get("token");
    const metaParam = searchParams.get("meta");

    if (!token || !metaParam) {
      router.replace("/dashboard");
      return;
    }

    try {
      const meta: ImpersonationMeta = JSON.parse(decodeURIComponent(metaParam));
      startImpersonation(token, meta);

      // Clean URL for security — remove token from address bar
      window.history.replaceState({}, "", window.location.pathname);

      // Redirect to dashboard
      router.replace("/dashboard");
    } catch {
      router.replace("/dashboard");
    }
  }, [searchParams, startImpersonation, router]);

  return (
    <div className="flex items-center justify-center min-h-[50vh]">
      <div className="text-center space-y-2">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto" />
        <p className="text-sm text-slate-500">Hesaba yönlendiriliyorsunuz...</p>
      </div>
    </div>
  );
}
