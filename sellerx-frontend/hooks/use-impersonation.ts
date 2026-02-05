"use client";

import { useState, useEffect, useCallback } from "react";

const STORAGE_KEY = "impersonation_token";
const META_KEY = "impersonation_meta";

export interface ImpersonationMeta {
  targetUserId: number;
  targetUserName: string;
  targetUserEmail: string;
  adminUserId: number;
  startedAt: string;
}

export function useImpersonation() {
  const [isImpersonating, setIsImpersonating] = useState(false);
  const [meta, setMeta] = useState<ImpersonationMeta | null>(null);

  useEffect(() => {
    const token = sessionStorage.getItem(STORAGE_KEY);
    const metaStr = sessionStorage.getItem(META_KEY);
    if (token && metaStr) {
      try {
        setMeta(JSON.parse(metaStr));
        setIsImpersonating(true);
      } catch {
        // Corrupted data, clean up
        sessionStorage.removeItem(STORAGE_KEY);
        sessionStorage.removeItem(META_KEY);
      }
    }
  }, []);

  const startImpersonation = useCallback(
    (token: string, metadata: ImpersonationMeta) => {
      sessionStorage.setItem(STORAGE_KEY, token);
      sessionStorage.setItem(META_KEY, JSON.stringify(metadata));
      setMeta(metadata);
      setIsImpersonating(true);
    },
    [],
  );

  const stopImpersonation = useCallback(() => {
    sessionStorage.removeItem(STORAGE_KEY);
    sessionStorage.removeItem(META_KEY);
    setMeta(null);
    setIsImpersonating(false);
    // Close the impersonation tab
    window.close();
  }, []);

  const getToken = useCallback((): string | null => {
    return sessionStorage.getItem(STORAGE_KEY);
  }, []);

  return {
    isImpersonating,
    meta,
    targetUserName: meta?.targetUserName ?? null,
    targetUserEmail: meta?.targetUserEmail ?? null,
    startImpersonation,
    stopImpersonation,
    getToken,
  };
}

/**
 * Static helper to check impersonation state outside React components.
 * Used by API client.
 */
export function getImpersonationToken(): string | null {
  if (typeof window === "undefined") return null;
  return sessionStorage.getItem(STORAGE_KEY);
}
