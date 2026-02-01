"use client";

import React, { createContext, useContext, useEffect, ReactNode } from "react";
import { usePathname, useRouter } from "@/i18n/navigation";
import { useMe, useRefreshAuth, useLogout } from "@/hooks/queries/use-auth";

interface User {
  id: string;
  email: string;
  name: string;
  role?: "USER" | "ADMIN";
}

interface AuthContextType {
  user: User | null;
  loading: boolean;
  refresh: () => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();

  // Use React Query hooks to fetch user data and handle auth operations
  const { data: user, isLoading, error, refetch } = useMe();
  const refreshMutation = useRefreshAuth();
  const logoutMutation = useLogout();

  // Auth Operations - use React Query mutations
  const refresh = async () => {
    try {
      await refreshMutation.mutateAsync();
      await refetch();
    } catch (error) {
      const isSignInPage = pathname === "/sign-in";
      if (!isSignInPage) {
        router.replace("/sign-in");
      }
    }
  };

  const logout = async () => {
    try {
      await logoutMutation.mutateAsync();
    } catch {
      // Logout hatası olsa bile kullanıcıyı çıkar
    }
    router.replace("/sign-in");
  };

  // If there is a 401 error and not on the login page, try to refresh once
  useEffect(() => {
    if (error && !user && pathname !== "/sign-in") {
      const errorMessage = error?.message?.toLowerCase() || "";
      if (
        errorMessage.includes("unauthorized") ||
        errorMessage.includes("401")
      ) {
        refresh();
      }
    }
  }, [error?.message, user, pathname, refresh]);

  return React.createElement(
    AuthContext.Provider,
    { value: { user: user || null, loading: isLoading, refresh, logout } },
    children,
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within an AuthProvider");
  return ctx;
}
