"use client";

import { useState, useEffect } from "react";
import { usePathname } from "next/navigation";
import { useTheme } from "next-themes";
import { useAuth } from "@/hooks/useAuth";
import {
  LayoutDashboard,
  Users,
  Store,
  LogOut,
  User,
  Shield,
  Headphones,
  Sun,
  Moon,
} from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Button } from "@/components/ui/button";
import Link from "next/link";

interface PageTitle {
  icon: React.ElementType;
  title: string;
}

const pageTitles: Record<string, PageTitle> = {
  "/admin/dashboard": { icon: LayoutDashboard, title: "Admin Dashboard" },
  "/admin/users": { icon: Users, title: "Kullanıcı Yönetimi" },
  "/admin/stores": { icon: Store, title: "Mağaza Yönetimi" },
  "/admin/support": { icon: Headphones, title: "Destek Talepleri" },
};

export function AdminHeader() {
  const pathname = usePathname();
  const { user, logout } = useAuth();
  const { setTheme, resolvedTheme } = useTheme();
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  // Get current page path (without locale prefix)
  const currentPath = pathname?.replace(/^\/(tr|en)/, "") || "/admin/dashboard";

  // Get current page title
  const currentPageTitle = pageTitles[currentPath] || pageTitles["/admin/dashboard"];
  const PageIcon = currentPageTitle.icon;

  return (
    <header className="fixed top-0 right-0 left-[220px] z-30 flex h-14 items-center justify-between border-b border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 px-4">
      {/* Left Section - Page Title */}
      <div className="flex items-center gap-2">
        <PageIcon className="h-5 w-5 text-amber-500" />
        <h1 className="text-lg font-semibold text-slate-900 dark:text-white">{currentPageTitle.title}</h1>
      </div>

      {/* Right Section */}
      <div className="flex items-center gap-3">
        {/* Theme Toggle */}
        {mounted && (
          <Button
            variant="ghost"
            size="icon"
            className="h-8 w-8"
            onClick={() => setTheme(resolvedTheme === "dark" ? "light" : "dark")}
          >
            {resolvedTheme === "dark" ? (
              <Sun className="h-4 w-4" />
            ) : (
              <Moon className="h-4 w-4" />
            )}
          </Button>
        )}

        {/* Admin Badge */}
        <div className="flex items-center gap-1.5 px-2 py-1 rounded bg-amber-100 dark:bg-amber-900/30 text-amber-700 dark:text-amber-400 text-xs font-medium">
          <Shield className="h-3.5 w-3.5" />
          Admin
        </div>

        {/* User Menu */}
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" size="sm" className="h-8 gap-2 text-sm font-normal">
              <div className="flex h-6 w-6 items-center justify-center rounded-full bg-amber-500 text-white text-xs">
                {user?.name?.charAt(0).toUpperCase() || "A"}
              </div>
              <span className="hidden md:inline">{user?.name || "Admin"}</span>
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-48">
            <DropdownMenuItem asChild>
              <Link href="/profile" className="flex items-center gap-2">
                <User className="h-4 w-4" />
                Profil
              </Link>
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem
              onClick={() => logout()}
              className="text-red-600 focus:text-red-600"
            >
              <LogOut className="h-4 w-4 mr-2" />
              Çıkış Yap
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </header>
  );
}
