"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/utils";
import { useSidebar } from "@/lib/contexts/sidebar-context";
import {
  LayoutDashboard,
  Package,
  ChevronLeft,
  ChevronRight,
  Store,
  ShoppingCart,
  Wallet,
  RotateCcw,
  Sparkles,
  Megaphone,
  Truck,
  Compass,
  FileText,
  Calculator,
  Bell,
  TrendingUp,
  Activity,
  Users,
} from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { useImpersonation } from "@/hooks/use-impersonation";

interface SidebarItem {
  icon: React.ElementType;
  label: string;
  href: string;
  badge?: string;
}

const menuItems: SidebarItem[] = [
  { icon: LayoutDashboard, label: "Kontrol Paneli", href: "/dashboard" },
  { icon: Package, label: "Ürünler", href: "/products" },
  { icon: Activity, label: "Stok Takip", href: "/stock-tracking" },
  { icon: ShoppingCart, label: "Siparişler", href: "/orders" },
  { icon: Wallet, label: "Giderler", href: "/expenses" },
  { icon: TrendingUp, label: "Kâr Analizi", href: "/profit" },
  { icon: FileText, label: "Faturalar", href: "/financial/invoices" },
  { icon: Calculator, label: "KDV", href: "/kdv" },
  { icon: Compass, label: "Dümen", href: "/dumen" },
  { icon: Truck, label: "Satın Alma", href: "/purchasing" },
  { icon: RotateCcw, label: "İadeler", href: "/returns" },
  { icon: Sparkles, label: "Müşteri Soruları", href: "/qa", badge: "AI" },
  { icon: Users, label: "Müşteri Analizi", href: "/customer-analytics" },
  { icon: Bell, label: "Uyarılar", href: "/alerts" },
  { icon: Store, label: "Mağazalar", href: "/new-store" },
];

export function Sidebar() {
  const pathname = usePathname();
  const { collapsed, toggleCollapsed } = useSidebar();
  const { isImpersonating } = useImpersonation();

  // Extract locale-independent path for comparison
  const currentPath = pathname.replace(/^\/(tr|en)/, "") || "/dashboard";

  return (
    <aside
      className={cn(
        "fixed left-0 z-40 h-screen bg-sidebar transition-all duration-300",
        collapsed ? "w-[60px]" : "w-[220px]",
        isImpersonating ? "top-10" : "top-0"
      )}
    >
      {/* Logo Area */}
      <div className="flex h-14 items-center justify-between border-b border-sidebar-border px-4">
        {!collapsed && (
          <Link href="/dashboard" className="flex items-center gap-2">
            <span className="text-lg font-semibold text-sidebar-foreground">sellerx</span>
          </Link>
        )}
        <button
          onClick={toggleCollapsed}
          className="flex h-8 w-8 items-center justify-center rounded bg-sidebar-accent text-sidebar-foreground hover:bg-sidebar-accent/80 transition-colors"
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
      <nav className="flex flex-col gap-1 p-2 overflow-y-auto" style={{ maxHeight: "calc(100vh - 3.5rem)" }}>
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
                  ? "bg-sidebar-accent text-sidebar-foreground"
                  : "text-sidebar-foreground/70 hover:bg-sidebar-accent/50 hover:text-sidebar-foreground"
              )}
            >
              <Icon className="h-5 w-5 flex-shrink-0" />
              {!collapsed && (
                <span className="flex items-center gap-2">
                  {item.label}
                  {item.badge && (
                    <Badge variant="default" className="bg-gradient-to-r from-violet-500 to-purple-600 text-[10px] px-1.5 py-0 h-4 font-medium">
                      {item.badge}
                    </Badge>
                  )}
                </span>
              )}
            </Link>
          );
        })}
      </nav>

    </aside>
  );
}
