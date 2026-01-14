"use client";

import { useState } from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/utils";
import {
  LayoutDashboard,
  Package,
  Settings,
  ChevronLeft,
  ChevronRight,
  Store,
  TrendingUp,
  BarChart3,
  ShoppingCart,
  Receipt,
} from "lucide-react";

interface SidebarItem {
  icon: React.ElementType;
  label: string;
  href: string;
}

const menuItems: SidebarItem[] = [
  { icon: LayoutDashboard, label: "Kontrol Paneli", href: "/dashboard" },
  { icon: Package, label: "Ürünler", href: "/products" },
  { icon: ShoppingCart, label: "Siparişler", href: "/orders" },
  { icon: Receipt, label: "Giderler", href: "/expenses" },
  { icon: TrendingUp, label: "Kârlılık", href: "/profit" },
  { icon: BarChart3, label: "Analitik", href: "/analytics" },
  { icon: Store, label: "Mağazalar", href: "/new-store" },
  { icon: Settings, label: "Ayarlar", href: "/settings" },
];

export function Sidebar() {
  const pathname = usePathname();
  const [collapsed, setCollapsed] = useState(false);

  // Extract locale-independent path for comparison
  const currentPath = pathname.replace(/^\/(tr|en)/, "") || "/dashboard";

  return (
    <aside
      className={cn(
        "fixed left-0 top-0 z-40 h-screen bg-[#333333] transition-all duration-300",
        collapsed ? "w-[60px]" : "w-[220px]"
      )}
    >
      {/* Logo Area */}
      <div className="flex h-14 items-center justify-between border-b border-[#444444] px-4">
        {!collapsed && (
          <Link href="/dashboard" className="flex items-center gap-2">
            <span className="text-lg font-semibold text-white">sellerx</span>
          </Link>
        )}
        <button
          onClick={() => setCollapsed(!collapsed)}
          className="flex h-8 w-8 items-center justify-center rounded bg-white/10 text-white hover:bg-white/20 transition-colors"
          aria-label={collapsed ? "Kenar çubuğunu genişlet" : "Kenar çubuğunu daralt"}
        >
          {collapsed ? (
            <ChevronRight className="h-4 w-4" />
          ) : (
            <ChevronLeft className="h-4 w-4" />
          )}
        </button>
      </div>

      {/* Menu Items */}
      <nav className="flex flex-col gap-1 p-2">
        {menuItems.map((item) => {
          const isActive = currentPath === item.href || currentPath.startsWith(item.href + "/");
          const Icon = item.icon;

          return (
            <Link
              key={item.href}
              href={item.href}
              className={cn(
                "flex items-center gap-3 rounded px-3 py-2.5 text-sm font-medium transition-colors",
                isActive
                  ? "bg-white/10 text-white"
                  : "text-white/70 hover:bg-white/5 hover:text-white"
              )}
            >
              <Icon className="h-5 w-5 flex-shrink-0" />
              {!collapsed && <span>{item.label}</span>}
            </Link>
          );
        })}
      </nav>

      {/* Bottom Section - Marketplace Links */}
      <div className="absolute bottom-4 left-0 right-0 px-2">
        <div className="border-t border-[#444444] pt-4">
          {!collapsed && (
            <div className="px-3 mb-2">
              <span className="text-xs text-white/50 uppercase">Pazaryerleri</span>
            </div>
          )}
          <Link
            href="/trendyol"
            className="flex items-center gap-3 rounded px-3 py-2 text-sm text-white/70 hover:bg-white/5 hover:text-white transition-colors"
          >
            <div className="h-5 w-5 rounded bg-[#F27A1A] flex items-center justify-center text-[10px] font-bold text-white flex-shrink-0">
              T
            </div>
            {!collapsed && <span>Trendyol</span>}
          </Link>
          <Link
            href="/hepsiburada"
            className="flex items-center gap-3 rounded px-3 py-2 text-sm text-white/70 hover:bg-white/5 hover:text-white transition-colors"
          >
            <div className="h-5 w-5 rounded bg-[#FF6000] flex items-center justify-center text-[10px] font-bold text-white flex-shrink-0">
              H
            </div>
            {!collapsed && <span>Hepsiburada</span>}
          </Link>
        </div>
      </div>
    </aside>
  );
}
