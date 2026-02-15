"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { motion, AnimatePresence } from "motion/react";
import { cn } from "@/lib/utils";
import { useSidebar } from "@/lib/contexts/sidebar-context";
import {
  LayoutDashboard,
  Package,
  Trophy,
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
  { icon: Trophy, label: "Buybox", href: "/buybox" },
  { icon: ShoppingCart, label: "Siparişler", href: "/orders" },
  { icon: Wallet, label: "Giderler", href: "/expenses" },
  { icon: TrendingUp, label: "Kâr Analizi", href: "/profit" },
  { icon: FileText, label: "Faturalar", href: "/financial/invoices" },
  { icon: Calculator, label: "KDV", href: "/kdv" },
  { icon: Calculator, label: "Kâr Hesaplama", href: "/kar-hesaplama" },
  { icon: Compass, label: "Dümen", href: "/dumen" },
  { icon: Truck, label: "Satın Alma", href: "/purchasing" },
  { icon: RotateCcw, label: "İadeler", href: "/returns" },
  { icon: Sparkles, label: "AI Asistan", href: "/qa", badge: "AI" },
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
        <AnimatePresence mode="wait">
          {!collapsed && (
            <motion.div
              initial={{ opacity: 0, width: 0 }}
              animate={{ opacity: 1, width: "auto" }}
              exit={{ opacity: 0, width: 0 }}
              transition={{ duration: 0.2 }}
            >
              <Link href="/dashboard" className="flex items-center gap-2">
                <span className="text-lg font-semibold text-sidebar-foreground whitespace-nowrap">sellerx</span>
              </Link>
            </motion.div>
          )}
        </AnimatePresence>
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
        {menuItems.map((item, index) => {
          const isActive = currentPath === item.href || currentPath.startsWith(item.href + "/");
          const Icon = item.icon;

          return (
            <motion.div
              key={item.href}
              initial={{ opacity: 0, x: -12 }}
              animate={{ opacity: 1, x: 0 }}
              transition={{ duration: 0.25, delay: index * 0.03, ease: [0.25, 0.1, 0.25, 1] as const }}
            >
              <Link
                href={item.href}
                className={cn(
                  "relative flex items-center gap-3 rounded px-3 py-2.5 text-sm font-medium transition-colors",
                  isActive
                    ? "bg-sidebar-accent text-sidebar-foreground"
                    : "text-sidebar-foreground/70 hover:bg-sidebar-accent/50 hover:text-sidebar-foreground"
                )}
              >
                <Icon className="h-5 w-5 flex-shrink-0" />
                <AnimatePresence mode="wait">
                  {!collapsed && (
                    <motion.span
                      initial={{ opacity: 0, width: 0 }}
                      animate={{ opacity: 1, width: "auto" }}
                      exit={{ opacity: 0, width: 0 }}
                      transition={{ duration: 0.2, ease: [0.25, 0.1, 0.25, 1] as const }}
                      className="flex items-center gap-2 overflow-hidden whitespace-nowrap"
                    >
                      {item.label}
                      {item.badge && (
                        <Badge variant="default" className="bg-gradient-to-r from-violet-500 to-purple-600 text-[10px] px-1.5 py-0 h-4 font-medium">
                          {item.badge}
                        </Badge>
                      )}
                    </motion.span>
                  )}
                </AnimatePresence>
              </Link>
            </motion.div>
          );
        })}
      </nav>

    </aside>
  );
}
