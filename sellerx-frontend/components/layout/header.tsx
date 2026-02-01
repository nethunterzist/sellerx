"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import { usePathname, useSearchParams, useRouter } from "next/navigation";
import { useAuth } from "@/hooks/useAuth";
import { cn } from "@/lib/utils";
import { useSidebar } from "@/lib/contexts/sidebar-context";
import {
  Bell,
  GraduationCap,
  ChevronDown,
  LayoutGrid,
  LineChart,
  FileText,
  TrendingUp,
  MapPin,
  LogOut,
  User,
  Settings,
  ShoppingCart,
  Package,
  Tag,
  Info,
  CheckCircle,
  AlertTriangle,
  PlayCircle,
  Clock,
  Sun,
  Moon,
  Monitor,
  LayoutDashboard,
  DollarSign,
  Wallet,
  RotateCcw,
  Sparkles,
  Megaphone,
  BarChart3,
  Store,
  Trophy,
  Activity,
  Calculator,
  Compass,
  Truck,
  Users,
  LifeBuoy,
} from "lucide-react";
import { useTheme } from "next-themes";
import { getRecentNotifications, getUnreadCount } from "@/lib/mock/notifications";
import { NotificationType, Notification } from "@/types/notification";
import { EducationVideo } from "@/types/education";
import { useEducationVideos, useMyWatchStatus } from "@/hooks/queries/use-education";
import { useNotifications, useUnreadCount as useNotificationsUnreadCount } from "@/hooks/queries/use-notifications";
import { useMemo } from "react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Button } from "@/components/ui/button";
import {
  Tooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import { useSelectedStore, useStore } from "@/hooks/queries/use-stores";
import { useDashboardStats } from "@/hooks/useDashboardStats";
import { format } from "date-fns";
import { tr } from "date-fns/locale";
import { StoreSelectorDropdown } from "./store-selector-dropdown";
import { NotificationCenter } from "@/components/alerts/notification-center";

interface HeaderTab {
  icon: React.ElementType;
  label: string;
  value: string;
}

const tabs: HeaderTab[] = [
  { icon: LayoutGrid, label: "Kartlar", value: "tiles" },
  { icon: LineChart, label: "Grafik", value: "chart" },
  { icon: FileText, label: "Kar/Zarar", value: "pl" },
  { icon: TrendingUp, label: "Trendler", value: "trends" },
  { icon: MapPin, label: "Şehirler", value: "cities" },
];

interface HeaderLink {
  icon: React.ElementType;
  label: string;
  href: string;
}

const purchasingLinks: HeaderLink[] = [
  { icon: Truck, label: "Genel Bakış", href: "/purchasing" },
  { icon: Clock, label: "Maliyet Geçmişi", href: "/purchasing/reports/cost-history" },
  { icon: TrendingUp, label: "Karlılık Analizi", href: "/purchasing/reports/profitability" },
  { icon: Package, label: "Stok Değerleme", href: "/purchasing/reports/stock-valuation" },
  { icon: Users, label: "Tedarikçiler", href: "/purchasing/suppliers" },
];

// Page titles mapping
interface PageTitle {
  icon: React.ElementType;
  title: string;
}

const pageTitles: Record<string, PageTitle> = {
  "/dashboard": { icon: LayoutDashboard, title: "Kontrol Paneli" },
  "/products": { icon: Package, title: "Ürünler" },
  "/buybox": { icon: Trophy, title: "Buybox Takip" },
  "/stock-tracking": { icon: Activity, title: "Stok Takip" },
  "/orders": { icon: ShoppingCart, title: "Siparişler" },
  "/expenses": { icon: Wallet, title: "Giderler" },
  "/profit": { icon: TrendingUp, title: "Kâr Analizi" },
  "/financial": { icon: DollarSign, title: "Finansal" },
  "/financial/invoices": { icon: FileText, title: "Faturalar" },
  "/kdv": { icon: Calculator, title: "KDV" },
  "/dumen": { icon: Compass, title: "Dümen" },
  "/purchasing": { icon: Truck, title: "Satın Alma" },
  "/purchasing/suppliers": { icon: Users, title: "Tedarikçiler" },
  "/returns": { icon: RotateCcw, title: "İadeler" },
  "/qa": { icon: Sparkles, title: "Müşteri Soruları" },
  "/alerts": { icon: Bell, title: "Uyarılar" },
  "/new-store": { icon: Store, title: "Mağazalar" },
  "/support": { icon: LifeBuoy, title: "Destek" },
  "/settings": { icon: Settings, title: "Ayarlar" },
  "/notifications": { icon: Bell, title: "Bildirimler" },
  "/education": { icon: GraduationCap, title: "Eğitim Videoları" },
  "/analytics": { icon: BarChart3, title: "Analitik" },
  "/profile": { icon: User, title: "Profil" },
};

const notificationIcons: Record<NotificationType, { icon: React.ElementType; color: string }> = {
  VIDEO_ADDED: { icon: Info, color: "text-purple-500" },
  ORDER_UPDATE: { icon: ShoppingCart, color: "text-blue-500" },
  STOCK_ALERT: { icon: Package, color: "text-orange-500" },
  SYSTEM: { icon: Info, color: "text-gray-500" },
  SUCCESS: { icon: CheckCircle, color: "text-green-500" },
  WARNING: { icon: AlertTriangle, color: "text-yellow-500" },
};

// Format datetime helper
function formatSyncDateTime(isoString: string | null | undefined): string {
  if (!isoString) return "Veri yok";

  try {
    const date = new Date(isoString);
    return format(date, "dd MMMM yyyy, HH:mm", { locale: tr });
  } catch {
    return "Bilinmiyor";
  }
}

export function Header() {
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const router = useRouter();
  const { user, logout } = useAuth();
  const { theme, setTheme, resolvedTheme } = useTheme();
  const { collapsed } = useSidebar();

  // Client-side only notification data to prevent hydration mismatch
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [mounted, setMounted] = useState(false);

  // Store and dashboard data
  const { data: selectedStoreData } = useSelectedStore();
  const selectedStoreId = selectedStoreData?.selectedStoreId;
  const { data: selectedStore } = useStore(selectedStoreId || "");
  const { data: dashboardStats } = useDashboardStats(selectedStoreId);

  // API hooks
  const { data: allVideos = [] } = useEducationVideos();
  const { data: watchStatus } = useMyWatchStatus();
  const { data: apiNotifications = [] } = useNotifications();
  const { data: unreadCount = 0 } = useNotificationsUnreadCount();

  // Get recent 3 videos with watch status
  const educationVideos = useMemo(() => {
    const watchedIds = new Set(watchStatus?.watchedVideoIds || []);
    return allVideos
      .slice(0, 3)
      .map((video) => ({
        ...video,
        watched: watchedIds.has(video.id),
      }));
  }, [allVideos, watchStatus]);

  // Calculate unwatched video count
  const unwatchedVideoCount = useMemo(() => {
    const watchedIds = new Set(watchStatus?.watchedVideoIds || []);
    return allVideos.filter((v) => !watchedIds.has(v.id)).length;
  }, [allVideos, watchStatus]);

  useEffect(() => {
    setMounted(true);
    // Keep mock notifications for backward compatibility
    setNotifications(getRecentNotifications(5));
  }, []);

  // Get active tab from URL or default to "tiles"
  const activeTab = searchParams.get("view") || "tiles";

  // Get current page path (without locale prefix)
  const currentPath = pathname?.replace(/^\/(tr|en)/, "") || "/dashboard";

  // Check if we're on the dashboard page
  const isDashboard = pathname === "/dashboard" || pathname?.endsWith("/dashboard");

  // Check if we're on the purchasing page (any sub-route)
  const isPurchasing = currentPath === "/purchasing" || currentPath.startsWith("/purchasing/");

  // Get current page title - exact match first, then prefix match for sub-routes
  const currentPageTitle =
    pageTitles[currentPath] ||
    Object.entries(pageTitles)
      .filter(([key]) => key !== "/")
      .sort((a, b) => b[0].length - a[0].length)
      .find(([key]) => currentPath.startsWith(key + "/"))?.[1] ||
    pageTitles["/dashboard"];
  const PageIcon = currentPageTitle.icon;

  const handleTabChange = (value: string) => {
    const params = new URLSearchParams(searchParams.toString());
    params.set("view", value);
    router.push(`${pathname}?${params.toString()}`);
  };

  return (
    <header className={cn(
      "fixed top-0 right-0 z-30 flex h-14 items-center justify-between border-b border-border bg-background px-4 transition-all duration-300",
      collapsed ? "left-[60px]" : "left-[220px]"
    )}>
      {/* Left Section - Page Title & Tabs */}
      <div className="flex items-center gap-4">
        {/* Page Title */}
        <div className="flex items-center gap-2">
          <PageIcon className="h-5 w-5 text-[#1D70F1]" />
          <h1 className="text-lg font-semibold text-foreground">{currentPageTitle.title}</h1>
        </div>

        {/* Dashboard Tabs (only on dashboard) */}
        {isDashboard && (
          <div className="flex items-center gap-1 ml-4 pl-4 border-l border-border">
            {tabs.map((tab) => {
              const Icon = tab.icon;
              const isActive = activeTab === tab.value;
              return (
                <button
                  key={tab.value}
                  onClick={() => handleTabChange(tab.value)}
                  className={cn(
                    "flex items-center gap-1.5 rounded px-3 py-1.5 text-sm transition-colors",
                    isActive
                      ? "bg-[#E8F1FE] dark:bg-[#1D70F1]/20 text-[#1D70F1]"
                      : "text-foreground/70 hover:text-foreground hover:bg-muted/50"
                  )}
                >
                  <Icon className="h-4 w-4" />
                  <span className="hidden sm:inline">{tab.label}</span>
                </button>
              );
            })}
          </div>
        )}

        {/* Purchasing Tabs (only on purchasing pages) */}
        {isPurchasing && (
          <div className="flex items-center gap-1 ml-4 pl-4 border-l border-border">
            {purchasingLinks.map((link) => {
              const Icon = link.icon;
              const isActive = currentPath === link.href;
              return (
                <Link
                  key={link.href}
                  href={link.href}
                  className={cn(
                    "flex items-center gap-1.5 rounded px-3 py-1.5 text-sm transition-colors",
                    isActive
                      ? "bg-[#E8F1FE] dark:bg-[#1D70F1]/20 text-[#1D70F1]"
                      : "text-foreground/70 hover:text-foreground hover:bg-muted/50"
                  )}
                >
                  <Icon className="h-4 w-4" />
                  <span className="hidden sm:inline">{link.label}</span>
                </Link>
              );
            })}
          </div>
        )}
      </div>

      {/* Store Selector - Center/Right */}
      {mounted && (
        <div className="ml-auto mr-4">
          <StoreSelectorDropdown
            selectedStoreId={selectedStoreId}
            selectedStore={selectedStore}
          />
        </div>
      )}

      {/* Right Section */}
      <div className="flex items-center gap-3">
        {/* Education / Help */}
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button
              variant="ghost"
              size="icon"
              className="relative h-8 w-8 rounded-full bg-[#E8F1FE] dark:bg-[#1D70F1]/20 text-[#1D70F1] hover:bg-[#d0e3fc] dark:hover:bg-[#1D70F1]/30"
            >
              <GraduationCap className="h-4 w-4" />
              {mounted && unwatchedVideoCount > 0 && (
                <span className="absolute -right-1 -top-1 flex h-4 w-4 items-center justify-center rounded-full bg-[#FEB72B] text-[10px] font-medium text-black">
                  {unwatchedVideoCount}
                </span>
              )}
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-80">
            <div className="flex items-center justify-between px-3 py-2 border-b border-border">
              <span className="font-medium text-sm text-foreground">Eğitim Videoları</span>
              {mounted && unwatchedVideoCount > 0 && (
                <span className="text-xs text-muted-foreground">
                  {unwatchedVideoCount} izlenmemiş
                </span>
              )}
            </div>
            <div className="max-h-[300px] overflow-y-auto">
              {educationVideos.map((video) => (
                <DropdownMenuItem
                  key={video.id}
                  className={cn(
                    "flex items-start gap-3 p-3 cursor-pointer",
                    !video.watched && "bg-blue-50/50 dark:bg-blue-900/20"
                  )}
                  asChild
                >
                  <Link href={`/education?video=${video.id}`}>
                    <div className="mt-0.5 text-[#1D70F1]">
                      <PlayCircle className="h-4 w-4" />
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className={cn(
                        "text-sm truncate text-foreground",
                        !video.watched && "font-medium"
                      )}>
                        {video.title}
                      </p>
                      <p className="text-xs text-muted-foreground truncate">
                        {video.description}
                      </p>
                      <div className="flex items-center gap-1 mt-1 text-xs text-muted-foreground">
                        <Clock className="h-3 w-3" />
                        <span>{video.duration}</span>
                      </div>
                    </div>
                    {video.watched && (
                      <CheckCircle className="h-4 w-4 text-green-500 mt-1.5" />
                    )}
                  </Link>
                </DropdownMenuItem>
              ))}
            </div>
            <DropdownMenuSeparator />
            <DropdownMenuItem asChild className="justify-center">
              <Link href="/education" className="text-sm text-[#1D70F1] font-medium w-full text-center py-2">
                Tüm Videoları Gör
              </Link>
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>

        {/* Theme Toggle */}
        <Button
          variant="ghost"
          size="icon"
          className="h-8 w-8 text-foreground"
          onClick={() => setTheme(resolvedTheme === "dark" ? "light" : "dark")}
        >
          {mounted ? (
            resolvedTheme === "dark" ? (
              <Sun className="h-4 w-4" />
            ) : (
              <Moon className="h-4 w-4" />
            )
          ) : (
            <Sun className="h-4 w-4" />
          )}
        </Button>

        {/* Last Sync Time */}
        {mounted && selectedStoreId && (
          <Tooltip>
            <TooltipTrigger asChild>
              <Button
                variant="ghost"
                size="icon"
                className="h-8 w-8 text-muted-foreground hover:text-foreground"
              >
                <Clock className="h-4 w-4" />
              </Button>
            </TooltipTrigger>
            <TooltipContent side="bottom" className="w-64 p-3 bg-gray-800 text-white border-gray-700 [&>span]:hidden">
              <div className="space-y-2">
                <div className="font-medium text-sm border-b border-gray-600 pb-2 mb-2 text-white">
                  Son Güncelleme
                </div>
                <div className="flex justify-between text-xs">
                  <span className="text-gray-300">Veri tarihi:</span>
                  <span className="text-white">
                    {formatSyncDateTime(dashboardStats?.calculatedAt)}
                  </span>
                </div>
                <div className="flex justify-between text-xs">
                  <span className="text-gray-300">Son sipariş:</span>
                  <span className="text-white">
                    {dashboardStats?.today?.orders?.[0]?.orderDate
                      ? formatSyncDateTime(dashboardStats.today.orders[0].orderDate)
                      : "Veri yok"}
                  </span>
                </div>
              </div>
            </TooltipContent>
          </Tooltip>
        )}

        {/* Notifications - Alert System */}
        <NotificationCenter />

        {/* User Menu */}
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button
              variant="ghost"
              size="sm"
              className="h-8 gap-2 text-sm font-normal"
            >
              <div className="flex h-6 w-6 items-center justify-center rounded-full bg-[#1D70F1] text-white text-xs">
                {user?.name?.charAt(0).toUpperCase() || "U"}
              </div>
              <span className="hidden md:inline">
                {user?.name || "Kullanıcı"}
              </span>
              <ChevronDown className="h-3.5 w-3.5" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-48">
            <DropdownMenuItem asChild>
              <Link href="/profile" className="flex items-center gap-2">
                <User className="h-4 w-4" />
                Profil
              </Link>
            </DropdownMenuItem>
            <DropdownMenuItem asChild>
              <Link href="/settings" className="flex items-center gap-2">
                <Settings className="h-4 w-4" />
                Ayarlar
              </Link>
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            <div className="px-2 py-1.5 text-xs font-medium text-muted-foreground">
              Tema
            </div>
            <DropdownMenuItem
              onClick={() => setTheme("light")}
              className={cn(theme === "light" && "bg-accent")}
            >
              <Sun className="h-4 w-4 mr-2" />
              Açık
            </DropdownMenuItem>
            <DropdownMenuItem
              onClick={() => setTheme("dark")}
              className={cn(theme === "dark" && "bg-accent")}
            >
              <Moon className="h-4 w-4 mr-2" />
              Koyu
            </DropdownMenuItem>
            <DropdownMenuItem
              onClick={() => setTheme("system")}
              className={cn(theme === "system" && "bg-accent")}
            >
              <Monitor className="h-4 w-4 mr-2" />
              Sistem
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem
              onClick={() => logout()}
              className="text-[#F34E1B] focus:text-[#F34E1B]"
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
