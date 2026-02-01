"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/utils";
import {
  LayoutDashboard,
  Users,
  Store,
  ChevronLeft,
  ChevronRight,
  ChevronDown,
  Shield,
  ArrowLeft,
  Headphones,
  GraduationCap,
  CreditCard,
  BarChart3,
  ShoppingCart,
  Package,
  ScrollText,
  ShieldAlert,
  Share2,
  Bell,
} from "lucide-react";
import { useState } from "react";

interface SidebarLink {
  icon: React.ElementType;
  label: string;
  href: string;
}

interface SidebarGroup {
  label: string;
  icon: React.ElementType;
  children: SidebarLink[];
}

type SidebarEntry = SidebarLink | SidebarGroup;

function isGroup(entry: SidebarEntry): entry is SidebarGroup {
  return "children" in entry;
}

const menuEntries: SidebarEntry[] = [
  { icon: LayoutDashboard, label: "Dashboard", href: "/admin/dashboard" },
  { icon: Users, label: "Kullanıcılar", href: "/admin/users" },
  { icon: Store, label: "Mağazalar", href: "/admin/stores" },
  {
    label: "Finans",
    icon: CreditCard,
    children: [
      { icon: CreditCard, label: "Abonelikler", href: "/admin/subscriptions" },
      { icon: BarChart3, label: "Gelir Raporu", href: "/admin/revenue" },
    ],
  },
  {
    label: "Platform",
    icon: ShoppingCart,
    children: [
      { icon: ShoppingCart, label: "Siparişler", href: "/admin/orders" },
      { icon: Package, label: "Ürünler", href: "/admin/products" },
    ],
  },
  {
    label: "Güvenlik",
    icon: ShieldAlert,
    children: [
      { icon: ScrollText, label: "Aktivite Logları", href: "/admin/activity-logs" },
      { icon: ShieldAlert, label: "Güvenlik Özeti", href: "/admin/security" },
    ],
  },
  { icon: GraduationCap, label: "Eğitim", href: "/admin/education" },
  { icon: Headphones, label: "Destek", href: "/admin/support" },
  { icon: Share2, label: "Referanslar", href: "/admin/referrals" },
  { icon: Bell, label: "Bildirimler", href: "/admin/notifications" },
];

export function AdminSidebar() {
  const pathname = usePathname();
  const [collapsed, setCollapsed] = useState(false);
  const [openGroups, setOpenGroups] = useState<Record<string, boolean>>({});

  const currentPath = pathname.replace(/^\/(tr|en)/, "") || "/admin/dashboard";

  const isChildActive = (group: SidebarGroup) =>
    group.children.some(
      (c) => currentPath === c.href || currentPath.startsWith(c.href + "/")
    );

  const toggleGroup = (label: string) => {
    setOpenGroups((prev) => ({ ...prev, [label]: !prev[label] }));
  };

  const isGroupOpen = (group: SidebarGroup) =>
    openGroups[group.label] ?? isChildActive(group);

  return (
    <aside
      className={cn(
        "fixed left-0 top-0 z-40 h-screen bg-slate-900 transition-all duration-300 flex flex-col",
        collapsed ? "w-[60px]" : "w-[220px]"
      )}
    >
      {/* Logo Area */}
      <div className="flex h-14 items-center justify-between border-b border-slate-700 px-4 shrink-0">
        {!collapsed && (
          <div className="flex items-center gap-2">
            <Shield className="h-5 w-5 text-amber-500" />
            <span className="text-lg font-semibold text-white">Admin</span>
          </div>
        )}
        <button
          onClick={() => setCollapsed(!collapsed)}
          className="flex h-8 w-8 items-center justify-center rounded bg-slate-800 text-slate-300 hover:bg-slate-700 transition-colors"
          aria-label={collapsed ? "Genişlet" : "Daralt"}
        >
          {collapsed ? (
            <ChevronRight className="h-4 w-4" />
          ) : (
            <ChevronLeft className="h-4 w-4" />
          )}
        </button>
      </div>

      {/* Menu Items */}
      <nav className="flex flex-col gap-0.5 p-2 overflow-y-auto flex-1">
        {menuEntries.map((entry) => {
          if (isGroup(entry)) {
            const groupActive = isChildActive(entry);
            const open = isGroupOpen(entry);
            const GroupIcon = entry.icon;

            if (collapsed) {
              // In collapsed mode, show group icon with active state if any child is active
              return (
                <div key={entry.label} className="relative group">
                  <div
                    className={cn(
                      "flex items-center justify-center rounded px-3 py-2.5 cursor-pointer transition-colors",
                      groupActive
                        ? "bg-amber-500/20 text-amber-500"
                        : "text-slate-400 hover:bg-slate-800 hover:text-white"
                    )}
                  >
                    <GroupIcon className="h-5 w-5 flex-shrink-0" />
                  </div>
                  {/* Hover popover for collapsed sidebar */}
                  <div className="absolute left-full top-0 ml-1 hidden group-hover:block z-50">
                    <div className="bg-slate-800 rounded-md border border-slate-700 py-1 shadow-lg min-w-[160px]">
                      <div className="px-3 py-1.5 text-xs font-semibold text-slate-400 uppercase">
                        {entry.label}
                      </div>
                      {entry.children.map((child) => {
                        const childActive =
                          currentPath === child.href ||
                          currentPath.startsWith(child.href + "/");
                        const ChildIcon = child.icon;
                        return (
                          <Link
                            key={child.href}
                            href={child.href}
                            className={cn(
                              "flex items-center gap-2 px-3 py-2 text-sm transition-colors",
                              childActive
                                ? "text-amber-500 bg-amber-500/10"
                                : "text-slate-300 hover:bg-slate-700 hover:text-white"
                            )}
                          >
                            <ChildIcon className="h-4 w-4" />
                            <span>{child.label}</span>
                          </Link>
                        );
                      })}
                    </div>
                  </div>
                </div>
              );
            }

            return (
              <div key={entry.label}>
                <button
                  onClick={() => toggleGroup(entry.label)}
                  className={cn(
                    "flex w-full items-center gap-3 rounded px-3 py-2.5 text-sm font-medium transition-colors",
                    groupActive
                      ? "text-amber-500"
                      : "text-slate-400 hover:bg-slate-800 hover:text-white"
                  )}
                >
                  <GroupIcon className="h-5 w-5 flex-shrink-0" />
                  <span className="flex-1 text-left">{entry.label}</span>
                  <ChevronDown
                    className={cn(
                      "h-4 w-4 transition-transform",
                      open && "rotate-180"
                    )}
                  />
                </button>
                {open && (
                  <div className="ml-4 flex flex-col gap-0.5 border-l border-slate-700 pl-2">
                    {entry.children.map((child) => {
                      const childActive =
                        currentPath === child.href ||
                        currentPath.startsWith(child.href + "/");
                      const ChildIcon = child.icon;
                      return (
                        <Link
                          key={child.href}
                          href={child.href}
                          className={cn(
                            "flex items-center gap-3 rounded px-3 py-2 text-sm transition-colors",
                            childActive
                              ? "bg-amber-500/20 text-amber-500"
                              : "text-slate-400 hover:bg-slate-800 hover:text-white"
                          )}
                        >
                          <ChildIcon className="h-4 w-4 flex-shrink-0" />
                          <span>{child.label}</span>
                        </Link>
                      );
                    })}
                  </div>
                )}
              </div>
            );
          }

          // Regular link item
          const isActive =
            currentPath === entry.href ||
            currentPath.startsWith(entry.href + "/");
          const Icon = entry.icon;

          return (
            <Link
              key={entry.href}
              href={entry.href}
              className={cn(
                "flex items-center gap-3 rounded px-3 py-2.5 text-sm font-medium transition-colors",
                isActive
                  ? "bg-amber-500/20 text-amber-500"
                  : "text-slate-400 hover:bg-slate-800 hover:text-white"
              )}
            >
              <Icon className="h-5 w-5 flex-shrink-0" />
              {!collapsed && <span>{entry.label}</span>}
            </Link>
          );
        })}
      </nav>

      {/* Back to App */}
      <div className="shrink-0 border-t border-slate-700 p-2">
        <Link
          href="/dashboard"
          className={cn(
            "flex items-center gap-3 rounded px-3 py-2.5 text-sm font-medium text-slate-400 hover:bg-slate-800 hover:text-white transition-colors",
            collapsed && "justify-center"
          )}
        >
          <ArrowLeft className="h-5 w-5 flex-shrink-0" />
          {!collapsed && <span>Uygulamaya Dön</span>}
        </Link>
      </div>
    </aside>
  );
}
