"use client";

import { useState } from "react";
import { useSearchParams } from "next/navigation";
import { useSelectedStore, useMyStores } from "@/hooks/queries/use-stores";
import {
  useCustomerAnalyticsSummary,
  useCustomerList,
  useProductRepeatAnalysis,
  useCrossSellAnalysis,
  useBackfillStatus,
  useTriggerBackfill,
  useLifecycleStages,
  useCohortAnalysis,
  useFrequencyDistribution,
  useClvSummary,
} from "@/hooks/queries/use-customer-analytics";
import { useCurrency } from "@/lib/contexts/currency-context";
import { cn } from "@/lib/utils";
import {
  Users,
  UserCheck,
  Repeat,
  TrendingUp,
  ShoppingBag,
  ArrowRightLeft,
  RefreshCw,
  ChevronLeft,
  ChevronRight,
  Info,
  BarChart3,
  PieChart as PieChartIcon,
  Crown,
  AlertTriangle,
  UserPlus,
  Heart,
  Moon,
  UserX,
  RotateCcw,
  Banknote,
  Target,
  Award,
  Percent,
  Hash,
  Calendar,
  Search,
} from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Tooltip as UITooltip,
  TooltipContent,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import {
  PieChart,
  Pie,
  Cell,
  ResponsiveContainer,
  Tooltip,
  Legend,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
} from "recharts";
import type {
  CustomerListItem,
  SegmentData,
  MonthlyTrend,
  LifecycleStageData,
  CohortData,
  FrequencyDistributionData,
  ClvSummaryData,
} from "@/types/customer-analytics";

type Tab = "overview" | "products" | "customers" | "cross-sell";

const SEGMENT_COLORS = [
  "#6366f1", // indigo
  "#8b5cf6", // violet
  "#a78bfa", // lighter violet
  "#c4b5fd", // light violet
  "#e0e7ff", // very light
];

const RFM_BADGE_COLORS: Record<string, string> = {
  "Sampiyonlar": "bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400",
  "Sadik Musteriler": "bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400",
  "Yeni Musteriler": "bg-cyan-100 text-cyan-700 dark:bg-cyan-900/30 dark:text-cyan-400",
  "Potansiyel Sadik": "bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-400",
  "Risk Altinda": "bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-400",
  "Kaybedilmek Uzere": "bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400",
  "Kayip": "bg-gray-100 text-gray-700 dark:bg-gray-900/30 dark:text-gray-400",
  "Diger": "bg-gray-100 text-gray-600 dark:bg-gray-900/30 dark:text-gray-400",
};

// Lifecycle stage icons mapping
const LIFECYCLE_ICONS: Record<string, React.ComponentType<{ className?: string; color?: string }>> = {
  new: UserPlus,
  active: Heart,
  loyal: Crown,
  at_risk: AlertTriangle,
  dormant: Moon,
  lost: UserX,
  returning: RotateCcw,
};

// Frequency bucket colors
const FREQUENCY_COLORS = [
  "#94a3b8", // 1x - gray
  "#6366f1", // 2x - indigo
  "#8b5cf6", // 3x - violet
  "#a855f7", // 4x - purple
  "#22c55e", // 5x+ - green
];

function getRfmBadgeClass(segment: string): string {
  // Normalize Turkish characters for matching
  const normalized = segment
    .replace(/\u015e/g, "S").replace(/\u015f/g, "s")
    .replace(/\u00c7/g, "C").replace(/\u00e7/g, "c")
    .replace(/\u011e/g, "G").replace(/\u011f/g, "g")
    .replace(/\u0130/g, "I").replace(/\u0131/g, "i")
    .replace(/\u00d6/g, "O").replace(/\u00f6/g, "o")
    .replace(/\u00dc/g, "U").replace(/\u00fc/g, "u");

  return RFM_BADGE_COLORS[normalized] || RFM_BADGE_COLORS["Diger"];
}

export default function CustomerAnalyticsPage() {
  const { formatCurrency } = useCurrency();
  const searchParams = useSearchParams();
  const activeTab = (searchParams.get("view") as Tab) || "overview";
  const [customerPage, setCustomerPage] = useState(0);
  const [productSearch, setProductSearch] = useState("");
  const [customerSearch, setCustomerSearch] = useState("");

  const { data: selectedStore, isLoading: storeLoading } = useSelectedStore();
  const storeId = selectedStore?.selectedStoreId;

  // Mağaza detayları ve tarih hesaplamaları için
  const { data: myStores } = useMyStores();
  const currentStore = myStores?.find(s => s.id === storeId);

  // İlk senkronizasyon tarihi ve 90 gün öncesi
  const firstSyncDate = currentStore?.createdAt
    ? new Date(currentStore.createdAt)
    : null;
  const dataStartDate = firstSyncDate
    ? new Date(firstSyncDate.getTime() - 90 * 24 * 60 * 60 * 1000)
    : null;

  // Türkçe tarih formatlama
  const formatDate = (date: Date) => date.toLocaleDateString('tr-TR', {
    day: 'numeric',
    month: 'long',
    year: 'numeric'
  });

  const { data: analytics, isLoading: analyticsLoading } =
    useCustomerAnalyticsSummary(storeId || undefined);
  const { data: customerList, isLoading: customersLoading } =
    useCustomerList(storeId || undefined, customerPage, 20, customerSearch);
  const { data: productRepeat, isLoading: productRepeatLoading } =
    useProductRepeatAnalysis(storeId || undefined);
  const { data: crossSell, isLoading: crossSellLoading } =
    useCrossSellAnalysis(storeId || undefined);
  const { data: backfillStatus } = useBackfillStatus(storeId || undefined);
  const triggerBackfill = useTriggerBackfill();

  // New analytics hooks
  const { data: lifecycleStages, isLoading: lifecycleLoading } =
    useLifecycleStages(storeId || undefined);
  const { data: cohortData, isLoading: cohortLoading } =
    useCohortAnalysis(storeId || undefined);
  const { data: frequencyData, isLoading: frequencyLoading } =
    useFrequencyDistribution(storeId || undefined);
  const { data: clvSummary, isLoading: clvLoading } =
    useClvSummary(storeId || undefined);

  // Client-side filtering for products
  const filteredProducts = productRepeat?.filter((p) => {
    if (!productSearch.trim()) return true;
    const search = productSearch.toLowerCase();
    return (
      p.productName.toLowerCase().includes(search) ||
      p.barcode.toLowerCase().includes(search)
    );
  });

  // No store selected
  if (!storeId && !storeLoading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[400px] text-center">
        <Users className="h-16 w-16 text-muted-foreground/30 mb-4" />
        <h2 className="text-lg font-medium text-foreground mb-2">
          Magaza Secilmedi
        </h2>
        <p className="text-sm text-muted-foreground max-w-md">
          Musteri analizini goruntulemek icin lutfen bir magaza secin.
        </p>
      </div>
    );
  }

  // Loading
  if (analyticsLoading) {
    return (
      <div className="space-y-6">
        <div className="h-8 w-64 bg-muted rounded animate-pulse" />
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {[...Array(4)].map((_, i) => (
            <div key={i} className="h-28 bg-muted rounded-xl animate-pulse" />
          ))}
        </div>
        <div className="h-10 w-96 bg-muted rounded animate-pulse" />
        <div className="h-96 bg-muted rounded-xl animate-pulse" />
      </div>
    );
  }

  const summary = analytics?.summary;

  return (
    <div className="space-y-6">

      {/* Summary Cards */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {/* Total Customers */}
        <div className="bg-card rounded-xl border border-border p-4">
          <div className="flex items-center gap-2 mb-2">
            <div className="p-2 rounded-lg bg-blue-50 dark:bg-blue-900/20">
              <Users className="h-4 w-4 text-blue-600 dark:text-blue-400" />
            </div>
            <span className="text-xs text-muted-foreground">
              Toplam Musteri
            </span>
          </div>
          <p className="text-2xl font-bold text-foreground">
            {(summary?.totalCustomers || 0).toLocaleString("tr-TR")}
          </p>
          <p className="text-xs text-muted-foreground mt-1">
            Benzersiz alici
          </p>
        </div>

        {/* Repeat Rate */}
        <div className="bg-card rounded-xl border border-border p-4">
          <div className="flex items-center gap-2 mb-2">
            <div className="p-2 rounded-lg bg-green-50 dark:bg-green-900/20">
              <Repeat className="h-4 w-4 text-green-600 dark:text-green-400" />
            </div>
            <span className="text-xs text-muted-foreground">Tekrar Orani</span>
          </div>
          <p className="text-2xl font-bold text-green-600 dark:text-green-400">
            %{(summary?.repeatRate || 0).toFixed(1)}
          </p>
          <p className="text-xs text-muted-foreground mt-1">
            {(summary?.repeatCustomers || 0).toLocaleString("tr-TR")} tekrar
            musteri
          </p>
        </div>

        {/* Avg Items */}
        <div className="bg-card rounded-xl border border-border p-4">
          <div className="flex items-center gap-2 mb-2">
            <div className="p-2 rounded-lg bg-purple-50 dark:bg-purple-900/20">
              <ShoppingBag className="h-4 w-4 text-purple-600 dark:text-purple-400" />
            </div>
            <span className="text-xs text-muted-foreground">
              Ort. Urun Adedi
            </span>
          </div>
          <p className="text-2xl font-bold text-foreground">
            {(summary?.avgItemsPerCustomer || 0).toFixed(1)}
          </p>
          <p className="text-xs text-muted-foreground mt-1">
            Musteri basina urun
          </p>
        </div>

        {/* Repeat Revenue Share */}
        <div className="bg-card rounded-xl border border-border p-4">
          <div className="flex items-center gap-2 mb-2">
            <div className="p-2 rounded-lg bg-indigo-50 dark:bg-indigo-900/20">
              <TrendingUp className="h-4 w-4 text-indigo-600 dark:text-indigo-400" />
            </div>
            <span className="text-xs text-muted-foreground">
              Tekrar Gelir Payi
            </span>
          </div>
          <p className="text-2xl font-bold text-indigo-600 dark:text-indigo-400">
            %{(summary?.repeatRevenueShare || 0).toFixed(1)}
          </p>
          <p className="text-xs text-muted-foreground mt-1">
            {formatCurrency(summary?.repeatCustomerRevenue || 0)}
          </p>
        </div>
      </div>

      {/* ===== OVERVIEW TAB ===== */}
      {activeTab === "overview" && (
        <div className="space-y-6">
          {/* Segmentation + Monthly Trend Row */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Segmentation Donut */}
            <div className="bg-card rounded-xl border border-border">
              <div className="p-4 border-b border-border">
                <h3 className="font-semibold text-foreground">
                  Musteri Segmentasyonu
                </h3>
                <p className="text-xs text-muted-foreground mt-1">
                  Siparis sayisina gore musteri dagilimi (1, 2-3, 4-6, 7+ siparis)
                </p>
              </div>
              <div className="p-4">
                {analytics?.segmentation &&
                analytics.segmentation.length > 0 ? (
                  <div className="flex flex-col items-center">
                    <ResponsiveContainer width="100%" height={280}>
                      <PieChart>
                        <Pie
                          data={analytics.segmentation.map(
                            (s: SegmentData) => ({
                              name: s.segment,
                              value: s.customerCount,
                              revenue: s.totalRevenue,
                              percentage: s.percentage,
                            })
                          )}
                          cx="50%"
                          cy="50%"
                          innerRadius={65}
                          outerRadius={110}
                          paddingAngle={3}
                          dataKey="value"
                        >
                          {analytics.segmentation.map(
                            (_: SegmentData, index: number) => (
                              <Cell
                                key={`cell-${index}`}
                                fill={
                                  SEGMENT_COLORS[
                                    index % SEGMENT_COLORS.length
                                  ]
                                }
                              />
                            )
                          )}
                        </Pie>
                        <Tooltip
                          content={({ active, payload }) => {
                            if (active && payload && payload.length) {
                              const d = payload[0].payload;
                              return (
                                <div className="bg-popover border border-border rounded-lg shadow-lg p-3 text-sm">
                                  <p className="font-medium">{d.name}</p>
                                  <p className="text-muted-foreground">
                                    {d.value.toLocaleString("tr-TR")} musteri (%
                                    {d.percentage.toFixed(1)})
                                  </p>
                                  <p className="text-muted-foreground">
                                    Gelir: {formatCurrency(d.revenue)}
                                  </p>
                                </div>
                              );
                            }
                            return null;
                          }}
                        />
                        <Legend
                          verticalAlign="bottom"
                          height={36}
                          formatter={(value: string) => (
                            <span className="text-xs text-foreground">
                              {value}
                            </span>
                          )}
                        />
                      </PieChart>
                    </ResponsiveContainer>
                  </div>
                ) : (
                  <div className="flex flex-col items-center justify-center h-[280px] text-muted-foreground">
                    <PieChartIcon className="h-12 w-12 mb-2 opacity-30" />
                    <p className="text-sm">Segmentasyon verisi bulunamadi</p>
                  </div>
                )}
              </div>
            </div>

            {/* Monthly Trend Bar Chart */}
            <div className="bg-card rounded-xl border border-border">
              <div className="p-4 border-b border-border">
                <h3 className="font-semibold text-foreground">
                  Aylik Yeni vs Tekrar Musteri
                </h3>
                <p className="text-xs text-muted-foreground mt-1">
                  Son 12 ay musteri kazanim trendi
                </p>
              </div>
              <div className="p-4">
                {analytics?.monthlyTrend &&
                analytics.monthlyTrend.length > 0 ? (
                  <ResponsiveContainer width="100%" height={280}>
                    <BarChart
                      data={analytics.monthlyTrend.map(
                        (t: MonthlyTrend) => ({
                          month: t.month,
                          Yeni: t.newCustomers,
                          Tekrar: t.repeatCustomers,
                        })
                      )}
                    >
                      <CartesianGrid
                        strokeDasharray="3 3"
                        className="stroke-border"
                      />
                      <XAxis
                        dataKey="month"
                        tick={{ fontSize: 11 }}
                        className="fill-muted-foreground"
                      />
                      <YAxis
                        tick={{ fontSize: 11 }}
                        className="fill-muted-foreground"
                      />
                      <Tooltip
                        content={({ active, payload, label }) => {
                          if (active && payload && payload.length) {
                            return (
                              <div className="bg-popover border border-border rounded-lg shadow-lg p-3 text-sm">
                                <p className="font-medium mb-1">{label}</p>
                                {payload.map((p, i) => (
                                  <p
                                    key={i}
                                    style={{ color: p.color }}
                                  >
                                    {p.name}:{" "}
                                    {(p.value as number).toLocaleString(
                                      "tr-TR"
                                    )}
                                  </p>
                                ))}
                              </div>
                            );
                          }
                          return null;
                        }}
                      />
                      <Legend
                        verticalAlign="bottom"
                        height={36}
                        formatter={(value: string) => (
                          <span className="text-xs text-foreground">
                            {value}
                          </span>
                        )}
                      />
                      <Bar
                        dataKey="Yeni"
                        fill="#6366f1"
                        radius={[4, 4, 0, 0]}
                      />
                      <Bar
                        dataKey="Tekrar"
                        fill="#22c55e"
                        radius={[4, 4, 0, 0]}
                      />
                    </BarChart>
                  </ResponsiveContainer>
                ) : (
                  <div className="flex flex-col items-center justify-center h-[280px] text-muted-foreground">
                    <BarChart3 className="h-12 w-12 mb-2 opacity-30" />
                    <p className="text-sm">Trend verisi bulunamadi</p>
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* CLV Summary Cards */}
          {clvSummary && (
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              {/* Average CLV */}
              <div className="bg-card rounded-xl border border-border p-4">
                <div className="flex items-center gap-2 mb-2">
                  <div className="p-2 rounded-lg bg-emerald-50 dark:bg-emerald-900/20">
                    <Banknote className="h-4 w-4 text-emerald-600 dark:text-emerald-400" />
                  </div>
                  <div className="flex items-center gap-1">
                    <span className="text-xs text-muted-foreground">
                      Ortalama CLV
                    </span>
                    <UITooltip>
                      <TooltipTrigger asChild>
                        <Info className="h-3 w-3 text-muted-foreground/60 cursor-help hover:text-muted-foreground" />
                      </TooltipTrigger>
                      <TooltipContent side="top" className="max-w-[280px]">
                        <p className="text-xs">Musteri Yasam Boyu Degeri (CLV), bir musterinin sizinle olan tum alisveris surecinde biraktigi toplam geliri temsil eder. Ortalama CLV, tum musterilerinizin degerinin ortalamasidir.</p>
                      </TooltipContent>
                    </UITooltip>
                  </div>
                </div>
                <p className="text-2xl font-bold text-emerald-600 dark:text-emerald-400">
                  {formatCurrency(clvSummary.avgClv || 0)}
                </p>
                <p className="text-xs text-muted-foreground mt-1">
                  Musteri yasam boyu degeri
                </p>
              </div>

              {/* Median CLV */}
              <div className="bg-card rounded-xl border border-border p-4">
                <div className="flex items-center gap-2 mb-2">
                  <div className="p-2 rounded-lg bg-cyan-50 dark:bg-cyan-900/20">
                    <Target className="h-4 w-4 text-cyan-600 dark:text-cyan-400" />
                  </div>
                  <div className="flex items-center gap-1">
                    <span className="text-xs text-muted-foreground">
                      Medyan CLV
                    </span>
                    <UITooltip>
                      <TooltipTrigger asChild>
                        <Info className="h-3 w-3 text-muted-foreground/60 cursor-help hover:text-muted-foreground" />
                      </TooltipTrigger>
                      <TooltipContent side="top" className="max-w-[280px]">
                        <p className="text-xs">Medyan, musterilerinizi degere gore siraladiginizda tam ortadaki degerdir. Ortalamadan farkli olarak asiri degerlerden etkilenmez.</p>
                      </TooltipContent>
                    </UITooltip>
                  </div>
                </div>
                <p className="text-2xl font-bold text-foreground">
                  {formatCurrency(clvSummary.medianClv || 0)}
                </p>
                <p className="text-xs text-muted-foreground mt-1">
                  Ortanca deger
                </p>
              </div>

              {/* Top 10% CLV */}
              <div className="bg-card rounded-xl border border-border p-4">
                <div className="flex items-center gap-2 mb-2">
                  <div className="p-2 rounded-lg bg-amber-50 dark:bg-amber-900/20">
                    <Award className="h-4 w-4 text-amber-600 dark:text-amber-400" />
                  </div>
                  <div className="flex items-center gap-1">
                    <span className="text-xs text-muted-foreground">
                      Top %10 CLV
                    </span>
                    <UITooltip>
                      <TooltipTrigger asChild>
                        <Info className="h-3 w-3 text-muted-foreground/60 cursor-help hover:text-muted-foreground" />
                      </TooltipTrigger>
                      <TooltipContent side="top" className="max-w-[280px]">
                        <p className="text-xs">En degerli %10 musterinizin ortalama yasam boyu degeri. Bu musteriler isletmeniz icin en kritik oneme sahiptir.</p>
                      </TooltipContent>
                    </UITooltip>
                  </div>
                </div>
                <p className="text-2xl font-bold text-amber-600 dark:text-amber-400">
                  {formatCurrency(clvSummary.top10PercentClv || 0)}
                </p>
                <p className="text-xs text-muted-foreground mt-1">
                  En degerli musteriler
                </p>
              </div>

              {/* Top 10% Revenue Share */}
              <div className="bg-card rounded-xl border border-border p-4">
                <div className="flex items-center gap-2 mb-2">
                  <div className="p-2 rounded-lg bg-rose-50 dark:bg-rose-900/20">
                    <Percent className="h-4 w-4 text-rose-600 dark:text-rose-400" />
                  </div>
                  <div className="flex items-center gap-1">
                    <span className="text-xs text-muted-foreground">
                      Top %10 Gelir Payi
                    </span>
                    <UITooltip>
                      <TooltipTrigger asChild>
                        <Info className="h-3 w-3 text-muted-foreground/60 cursor-help hover:text-muted-foreground" />
                      </TooltipTrigger>
                      <TooltipContent side="top" className="max-w-[280px]">
                        <p className="text-xs">En degerli %10 musterinizin toplam geliriniz icindeki payi. Yuksek oran, musteri tabaninin konsantre oldugunu gosterir.</p>
                      </TooltipContent>
                    </UITooltip>
                  </div>
                </div>
                <p className="text-2xl font-bold text-rose-600 dark:text-rose-400">
                  %{(clvSummary.top10PercentRevenueShare || 0).toFixed(1)}
                </p>
                <p className="text-xs text-muted-foreground mt-1">
                  Toplam gelirin yuzde kaci
                </p>
              </div>
            </div>
          )}

          {/* Lifecycle Stages + Frequency Distribution Row */}
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
            {/* Lifecycle Stages */}
            <div className="bg-card rounded-xl border border-border">
              <div className="p-4 border-b border-border">
                <h3 className="font-semibold text-foreground">
                  Musteri Yasam Dongusu
                </h3>
                <p className="text-xs text-muted-foreground mt-1">
                  Musterilerinizin aktivite durumuna gore dagilimi
                </p>
              </div>
              <div className="p-4">
                {lifecycleLoading ? (
                  <div className="space-y-3">
                    {[...Array(6)].map((_, i) => (
                      <div key={i} className="h-10 bg-muted rounded animate-pulse" />
                    ))}
                  </div>
                ) : lifecycleStages && lifecycleStages.length > 0 ? (
                  <div className="space-y-3">
                    {lifecycleStages.map((stage: LifecycleStageData) => {
                      const IconComponent = LIFECYCLE_ICONS[stage.stage] || Users;
                      return (
                        <div key={stage.stage} className="flex items-center gap-3">
                          <div
                            className="p-2 rounded-lg"
                            style={{ backgroundColor: `${stage.color}20` }}
                          >
                            <IconComponent
                              className="h-4 w-4"
                              color={stage.color}
                            />
                          </div>
                          <div className="flex-1 min-w-0">
                            <div className="flex items-center justify-between mb-1">
                              <span className="text-sm font-medium text-foreground">
                                {stage.label}
                              </span>
                              <span className="text-sm text-muted-foreground">
                                {stage.customerCount.toLocaleString("tr-TR")} (%{stage.percentage.toFixed(1)})
                              </span>
                            </div>
                            <div className="h-2 bg-muted rounded-full overflow-hidden">
                              <div
                                className="h-full rounded-full transition-all duration-500"
                                style={{
                                  width: `${Math.min(stage.percentage, 100)}%`,
                                  backgroundColor: stage.color
                                }}
                              />
                            </div>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                ) : (
                  <div className="flex flex-col items-center justify-center h-[280px] text-muted-foreground">
                    <Users className="h-12 w-12 mb-2 opacity-30" />
                    <p className="text-sm">Yasam dongusu verisi bulunamadi</p>
                  </div>
                )}
              </div>
            </div>

            {/* Purchase Frequency Distribution */}
            <div className="bg-card rounded-xl border border-border">
              <div className="p-4 border-b border-border">
                <h3 className="font-semibold text-foreground">
                  Siparis Sayisi Dagilimi
                </h3>
                <p className="text-xs text-muted-foreground mt-1">
                  Musteriler toplam kac siparis verdi?
                </p>
              </div>
              <div className="p-4">
                {frequencyLoading ? (
                  <div className="h-[280px] bg-muted rounded animate-pulse" />
                ) : frequencyData && frequencyData.length > 0 ? (
                  <ResponsiveContainer width="100%" height={280}>
                    <BarChart
                      data={frequencyData.map((f: FrequencyDistributionData, idx: number) => ({
                        name: f.bucket,
                        Musteri: f.customerCount,
                        Gelir: f.totalRevenue,
                        fill: FREQUENCY_COLORS[idx % FREQUENCY_COLORS.length],
                      }))}
                      layout="vertical"
                    >
                      <CartesianGrid strokeDasharray="3 3" className="stroke-border" />
                      <XAxis
                        type="number"
                        tick={{ fontSize: 11 }}
                        className="fill-muted-foreground"
                      />
                      <YAxis
                        dataKey="name"
                        type="category"
                        width={80}
                        tick={{ fontSize: 11 }}
                        className="fill-muted-foreground"
                      />
                      <Tooltip
                        content={({ active, payload }) => {
                          if (active && payload && payload.length) {
                            const d = payload[0].payload;
                            const freq = frequencyData.find((f: FrequencyDistributionData) => f.bucket === d.name);
                            return (
                              <div className="bg-popover border border-border rounded-lg shadow-lg p-3 text-sm">
                                <p className="font-medium">{d.name} siparis</p>
                                <p className="text-muted-foreground">
                                  {d.Musteri.toLocaleString("tr-TR")} musteri
                                </p>
                                <p className="text-muted-foreground">
                                  Gelir: {formatCurrency(d.Gelir)}
                                </p>
                                {freq && (
                                  <p className="text-muted-foreground">
                                    Gelir payi: %{freq.revenueShare.toFixed(1)}
                                  </p>
                                )}
                              </div>
                            );
                          }
                          return null;
                        }}
                      />
                      <Bar dataKey="Musteri" radius={[0, 4, 4, 0]}>
                        {frequencyData.map((_: FrequencyDistributionData, idx: number) => (
                          <Cell key={idx} fill={FREQUENCY_COLORS[idx % FREQUENCY_COLORS.length]} />
                        ))}
                      </Bar>
                    </BarChart>
                  </ResponsiveContainer>
                ) : (
                  <div className="flex flex-col items-center justify-center h-[280px] text-muted-foreground">
                    <Hash className="h-12 w-12 mb-2 opacity-30" />
                    <p className="text-sm">Siklik verisi bulunamadi</p>
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* Cohort Retention Heatmap */}
          <div className="bg-card rounded-xl border border-border">
            <div className="p-4 border-b border-border">
              <div className="flex items-center gap-2">
                <Calendar className="h-5 w-5 text-muted-foreground" />
                <div>
                  <div className="flex items-center gap-1">
                    <h3 className="font-semibold text-foreground">
                      Kohort Retention Analizi
                    </h3>
                    <UITooltip>
                      <TooltipTrigger asChild>
                        <Info className="h-4 w-4 text-muted-foreground/60 cursor-help hover:text-muted-foreground" />
                      </TooltipTrigger>
                      <TooltipContent side="top" className="max-w-[350px]">
                        <div className="text-xs space-y-2">
                          <p><strong>Kohort nedir?</strong> Ayni ayda ilk siparisini veren musteriler bir kohort olusturur. Ornegin "2025-11" kohortu, Kasim 2025'te ilk kez alisveris yapan musterilerdir.</p>
                          <p><strong>M1, M2, M3... ne anlama gelir?</strong></p>
                          <ul className="list-disc list-inside space-y-1">
                            <li><strong>M1:</strong> Kohortun ilk ayi (her zaman %100)</li>
                            <li><strong>M2:</strong> 1 ay sonra tekrar alisveris yapan musteri orani</li>
                            <li><strong>M3:</strong> 2 ay sonra tekrar alisveris yapan musteri orani</li>
                          </ul>
                          <p className="text-foreground font-medium">Yuksek retention, musteri sadakatinin gostergesidir.</p>
                        </div>
                      </TooltipContent>
                    </UITooltip>
                  </div>
                  <p className="text-xs text-muted-foreground mt-1">
                    Her ay ilk kez alisveris yapan musterilerin sonraki aylarda tekrar satin alma oranlari
                  </p>
                </div>
              </div>
            </div>
            <div className="p-4">
              {cohortLoading ? (
                <div className="h-[300px] bg-muted rounded animate-pulse" />
              ) : cohortData && cohortData.length > 0 ? (
                <div className="overflow-x-auto">
                  <table className="w-full min-w-[600px]">
                    <thead>
                      <tr className="border-b border-border">
                        <th className="text-left p-2 text-xs font-medium text-muted-foreground">
                          Kohort
                        </th>
                        <th className="text-center p-2 text-xs font-medium text-muted-foreground">
                          Boyut
                        </th>
                        {Array.from({ length: 6 }, (_, i) => (
                          <th key={i} className="text-center p-2 text-xs font-medium text-muted-foreground">
                            M{i + 1}
                          </th>
                        ))}
                      </tr>
                    </thead>
                    <tbody>
                      {cohortData.slice(0, 8).map((cohort: CohortData) => (
                        <tr key={cohort.cohortMonth} className="border-b border-border last:border-0">
                          <td className="p-2 text-sm font-medium text-foreground">
                            {cohort.cohortMonth}
                          </td>
                          <td className="p-2 text-center text-sm text-muted-foreground">
                            {cohort.cohortSize.toLocaleString("tr-TR")}
                          </td>
                          {Array.from({ length: 6 }, (_, i) => {
                            const monthKey = `M${i + 1}`;
                            const rate = cohort.retentionRates[monthKey];
                            const hasValue = rate !== undefined && rate !== null;

                            // Color intensity based on retention rate
                            const getHeatmapColor = (value: number) => {
                              if (value >= 50) return "bg-green-500 text-white";
                              if (value >= 35) return "bg-green-400 text-white";
                              if (value >= 25) return "bg-green-300 text-green-900";
                              if (value >= 15) return "bg-yellow-300 text-yellow-900";
                              if (value >= 10) return "bg-orange-300 text-orange-900";
                              if (value >= 5) return "bg-orange-200 text-orange-800";
                              return "bg-red-100 text-red-700";
                            };

                            return (
                              <td key={i} className="p-1 text-center">
                                {hasValue ? (
                                  <span
                                    className={cn(
                                      "inline-flex items-center justify-center w-12 h-8 rounded text-xs font-medium",
                                      i === 0 ? "bg-blue-500 text-white" : getHeatmapColor(rate)
                                    )}
                                  >
                                    {i === 0 ? "100%" : `${rate.toFixed(0)}%`}
                                  </span>
                                ) : (
                                  <span className="inline-flex items-center justify-center w-12 h-8 text-xs text-muted-foreground">
                                    -
                                  </span>
                                )}
                              </td>
                            );
                          })}
                        </tr>
                      ))}
                    </tbody>
                  </table>

                  {/* Legend */}
                  <div className="mt-4 flex items-center justify-center gap-4 text-xs text-muted-foreground">
                    <span className="flex items-center gap-1">
                      <span className="w-4 h-4 rounded bg-green-500" /> Yuksek (&gt;35%)
                    </span>
                    <span className="flex items-center gap-1">
                      <span className="w-4 h-4 rounded bg-yellow-300" /> Orta (15-35%)
                    </span>
                    <span className="flex items-center gap-1">
                      <span className="w-4 h-4 rounded bg-red-100" /> Dusuk (&lt;15%)
                    </span>
                  </div>
                </div>
              ) : (
                <div className="flex flex-col items-center justify-center h-[200px] text-muted-foreground">
                  <Calendar className="h-12 w-12 mb-2 opacity-30" />
                  <p className="text-sm">Kohort verisi bulunamadi</p>
                  <p className="text-xs mt-1">Yeterli siparis gecmisi gerekiyor</p>
                </div>
              )}
            </div>
          </div>

        </div>
      )}

      {/* ===== PRODUCTS TAB ===== */}
      {activeTab === "products" && (
        <div className="space-y-6">
          {/* Product Repeat Table */}
          <div className="bg-card rounded-xl border border-border">
            <div className="p-4 border-b border-border">
              <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
                <div>
                  <h3 className="font-semibold text-foreground">
                    Urun Bazli Tekrar Analizi
                  </h3>
                  <p className="text-xs text-muted-foreground mt-1">
                    Hangi urunler tekrar tekrar satiliyor? Ortalama kac gunde tekrar
                    alinyor?
                  </p>
                </div>
                <div className="relative w-full sm:w-64">
                  <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                  <input
                    type="text"
                    placeholder="Urun adi veya barkod ara..."
                    value={productSearch}
                    onChange={(e) => setProductSearch(e.target.value)}
                    className="w-full pl-9 pr-4 py-2 text-sm border border-border rounded-lg bg-background focus:outline-none focus:ring-2 focus:ring-primary/20"
                  />
                </div>
              </div>
              {productSearch && filteredProducts && (
                <p className="text-xs text-muted-foreground mt-2">
                  {filteredProducts.length} urun bulundu
                </p>
              )}
            </div>
            {productRepeatLoading ? (
              <div className="p-8">
                <div className="space-y-3">
                  {[...Array(5)].map((_, i) => (
                    <div
                      key={i}
                      className="h-12 bg-muted rounded animate-pulse"
                    />
                  ))}
                </div>
              </div>
            ) : filteredProducts && filteredProducts.length > 0 ? (
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead>
                    <tr className="border-b border-border bg-muted/30">
                      <th className="text-left p-3 text-xs font-medium text-muted-foreground">
                        Urun
                      </th>
                      <th className="text-right p-3 text-xs font-medium text-muted-foreground">
                        Toplam Alici
                      </th>
                      <th className="text-right p-3 text-xs font-medium text-muted-foreground">
                        Tekrar Alici
                      </th>
                      <th className="text-right p-3 text-xs font-medium text-muted-foreground">
                        Tekrar Orani
                      </th>
                      <th className="text-right p-3 text-xs font-medium text-muted-foreground">
                        Ort. Gun
                      </th>
                      <th className="text-right p-3 text-xs font-medium text-muted-foreground">
                        Satilan Adet
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {filteredProducts.map((p) => (
                      <tr
                        key={p.barcode}
                        className="border-b border-border last:border-0 hover:bg-muted/20"
                      >
                        <td className="p-3">
                          <div className="flex items-center gap-3">
                            <a
                              href={p.productUrl || "#"}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="flex-shrink-0 group"
                            >
                              {p.image ? (
                                <img
                                  src={p.image}
                                  alt={p.productName}
                                  className="h-10 w-10 rounded object-cover border border-border group-hover:border-[#F27A1A] transition-colors"
                                />
                              ) : (
                                <div className="h-10 w-10 rounded bg-[#F27A1A] flex items-center justify-center text-white text-xs font-bold group-hover:ring-2 ring-[#F27A1A] transition-all">
                                  T
                                </div>
                              )}
                            </a>
                            <div className="min-w-0">
                              <UITooltip>
                                <TooltipTrigger asChild>
                                  <a
                                    href={p.productUrl || "#"}
                                    target="_blank"
                                    rel="noopener noreferrer"
                                    className="text-sm font-medium text-foreground line-clamp-1 hover:text-[#F27A1A] hover:underline cursor-pointer block"
                                  >
                                    {p.productName}
                                  </a>
                                </TooltipTrigger>
                                <TooltipContent side="top" className="max-w-[300px]">
                                  <p>{p.productName}</p>
                                </TooltipContent>
                              </UITooltip>
                              <p className="text-xs text-muted-foreground font-mono">
                                {p.barcode}
                              </p>
                            </div>
                          </div>
                        </td>
                        <td className="p-3 text-sm text-muted-foreground text-right">
                          {p.totalBuyers.toLocaleString("tr-TR")}
                        </td>
                        <td className="p-3 text-sm text-foreground text-right font-medium">
                          {p.repeatBuyers.toLocaleString("tr-TR")}
                        </td>
                        <td className="p-3 text-right">
                          <span
                            className={cn(
                              "inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium",
                              p.repeatRate >= 30
                                ? "bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400"
                                : p.repeatRate >= 15
                                  ? "bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400"
                                  : "bg-gray-100 text-gray-600 dark:bg-gray-900/30 dark:text-gray-400"
                            )}
                          >
                            %{p.repeatRate.toFixed(1)}
                          </span>
                        </td>
                        <td className="p-3 text-sm text-foreground text-right">
                          {p.avgDaysBetweenRepurchase > 0
                            ? `${Math.round(p.avgDaysBetweenRepurchase)} gun`
                            : "-"}
                        </td>
                        <td className="p-3 text-sm text-muted-foreground text-right">
                          {p.totalQuantitySold.toLocaleString("tr-TR")}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <div className="p-8 text-center">
                <Repeat className="h-12 w-12 mx-auto text-muted-foreground/50 mb-3" />
                <p className="text-sm text-muted-foreground">
                  Urun tekrar verisi bulunamadi. Musteri verisi
                  guncellendiginde burada analiz gorunecektir.
                </p>
              </div>
            )}
          </div>

          {/* Average Repeat Interval Info */}
          {summary && summary.avgRepeatIntervalDays > 0 && (
            <div className="bg-indigo-50 dark:bg-indigo-900/20 rounded-xl border border-indigo-200 dark:border-indigo-800 p-4">
              <div className="flex items-start gap-3">
                <Repeat className="h-5 w-5 text-indigo-600 dark:text-indigo-400 mt-0.5" />
                <div>
                  <p className="font-medium text-indigo-900 dark:text-indigo-100">
                    Ortalama Tekrar Suresi
                  </p>
                  <p className="text-sm text-indigo-700 dark:text-indigo-300 mt-1">
                    Musterileriniz ortalama{" "}
                    <span className="font-bold">
                      {Math.round(summary.avgRepeatIntervalDays)} gun
                    </span>{" "}
                    sonra tekrar alis yapiyor. Bu veriyi stok planlamaniz ve
                    kampanya zamanlamaniz icin kullanabilirsiniz.
                  </p>
                </div>
              </div>
            </div>
          )}
        </div>
      )}

      {/* ===== CUSTOMERS TAB ===== */}
      {activeTab === "customers" && (
        <div className="bg-card rounded-xl border border-border">
          <div className="p-4 border-b border-border">
            <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
              <div>
                <h3 className="font-semibold text-foreground">Musteri Listesi</h3>
                <p className="text-xs text-muted-foreground mt-1">
                  RFM (Recency-Frequency-Monetary) skoruna gore segmentlenmis
                  musteri listesi
                </p>
              </div>
              <div className="relative w-full sm:w-64">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <input
                  type="text"
                  placeholder="Musteri adi veya sehir ara..."
                  value={customerSearch}
                  onChange={(e) => {
                    setCustomerSearch(e.target.value);
                    setCustomerPage(0); // Reset to first page on search
                  }}
                  className="w-full pl-9 pr-4 py-2 text-sm border border-border rounded-lg bg-background focus:outline-none focus:ring-2 focus:ring-primary/20"
                />
              </div>
            </div>
          </div>
          {customersLoading ? (
            <div className="p-8">
              <div className="space-y-3">
                {[...Array(5)].map((_, i) => (
                  <div
                    key={i}
                    className="h-12 bg-muted rounded animate-pulse"
                  />
                ))}
              </div>
            </div>
          ) : customerList &&
            customerList.content &&
            customerList.content.length > 0 ? (
            <>
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead>
                    <tr className="border-b border-border bg-muted/30">
                      <th className="text-left p-3 text-xs font-medium text-muted-foreground">
                        Musteri
                      </th>
                      <th className="text-left p-3 text-xs font-medium text-muted-foreground">
                        Sehir
                      </th>
                      <th className="text-right p-3 text-xs font-medium text-muted-foreground">
                        Siparis
                      </th>
                      <th className="text-right p-3 text-xs font-medium text-muted-foreground">
                        Urun Adedi
                      </th>
                      <th className="text-right p-3 text-xs font-medium text-muted-foreground">
                        Toplam Harcama
                      </th>
                      <th className="text-right p-3 text-xs font-medium text-muted-foreground">
                        Ort. Siparis
                      </th>
                      <th className="text-center p-3 text-xs font-medium text-muted-foreground">
                        RFM Segment
                      </th>
                      <th className="text-center p-3 text-xs font-medium text-muted-foreground">
                        Skor
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {customerList.content.map(
                      (c: CustomerListItem, idx: number) => (
                        <tr
                          key={`${c.customerKey}-${idx}`}
                          className="border-b border-border last:border-0 hover:bg-muted/20"
                        >
                          <td className="p-3">
                            <p className="text-sm font-medium text-foreground">
                              {c.displayName || "Anonim"}
                            </p>
                            <p className="text-xs text-muted-foreground font-mono truncate max-w-[200px]">
                              {c.customerKey}
                            </p>
                          </td>
                          <td className="p-3 text-sm text-muted-foreground">
                            {c.city || "-"}
                          </td>
                          <td className="p-3 text-sm text-muted-foreground text-right">
                            {c.orderCount}
                          </td>
                          <td className="p-3 text-sm text-foreground text-right font-medium">
                            {c.itemCount}
                          </td>
                          <td className="p-3 text-sm text-foreground text-right">
                            {formatCurrency(c.totalSpend)}
                          </td>
                          <td className="p-3 text-sm text-muted-foreground text-right">
                            {formatCurrency(c.avgOrderValue)}
                          </td>
                          <td className="p-3 text-center">
                            <Badge
                              variant="secondary"
                              className={cn(
                                "text-[10px] px-2 py-0.5 font-medium",
                                getRfmBadgeClass(c.rfmSegment)
                              )}
                            >
                              {c.rfmSegment}
                            </Badge>
                          </td>
                          <td className="p-3 text-center">
                            <span className="text-xs font-mono text-muted-foreground">
                              {c.recencyScore}-{c.frequencyScore}-
                              {c.monetaryScore}
                            </span>
                          </td>
                        </tr>
                      )
                    )}
                  </tbody>
                </table>
              </div>

              {/* Pagination */}
              {customerList.totalPages > 1 && (
                <div className="flex items-center justify-between p-4 border-t border-border">
                  <p className="text-xs text-muted-foreground">
                    Toplam {customerList.totalElements.toLocaleString("tr-TR")}{" "}
                    musteri
                  </p>
                  <div className="flex items-center gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() =>
                        setCustomerPage((p) => Math.max(0, p - 1))
                      }
                      disabled={customerPage === 0}
                    >
                      <ChevronLeft className="h-4 w-4" />
                    </Button>
                    <span className="text-sm text-muted-foreground">
                      {customerPage + 1} / {customerList.totalPages}
                    </span>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() =>
                        setCustomerPage((p) =>
                          Math.min(customerList.totalPages - 1, p + 1)
                        )
                      }
                      disabled={customerPage >= customerList.totalPages - 1}
                    >
                      <ChevronRight className="h-4 w-4" />
                    </Button>
                  </div>
                </div>
              )}
            </>
          ) : (
            <div className="p-8 text-center">
              <Users className="h-12 w-12 mx-auto text-muted-foreground/50 mb-3" />
              <p className="text-sm text-muted-foreground">
                Musteri verisi bulunamadi. Veri guncelleme baslatarak
                musteri bilgilerini yukleyebilirsiniz.
              </p>
            </div>
          )}
        </div>
      )}

      {/* ===== CROSS-SELL TAB ===== */}
      {activeTab === "cross-sell" && (
        <div className="bg-card rounded-xl border border-border">
          <div className="p-4 border-b border-border">
            <h3 className="font-semibold text-foreground">
              Capraz Satis Analizi
            </h3>
            <p className="text-xs text-muted-foreground mt-1">
              Birlikte satin alinan urun ciftleri - &quot;Bu urunu alan
              sunlari da almis&quot;
            </p>
          </div>
          {crossSellLoading ? (
            <div className="p-8">
              <div className="space-y-3">
                {[...Array(5)].map((_, i) => (
                  <div
                    key={i}
                    className="h-12 bg-muted rounded animate-pulse"
                  />
                ))}
              </div>
            </div>
          ) : crossSell && crossSell.length > 0 ? (
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="border-b border-border bg-muted/30">
                    <th className="text-left p-3 text-xs font-medium text-muted-foreground">
                      Kaynak Urun
                    </th>
                    <th className="text-center p-3 text-xs font-medium text-muted-foreground w-10">
                      &rarr;
                    </th>
                    <th className="text-left p-3 text-xs font-medium text-muted-foreground">
                      Hedef Urun
                    </th>
                    <th className="text-right p-3 text-xs font-medium text-muted-foreground">
                      Birlikte Alim
                    </th>
                    <th className="text-right p-3 text-xs font-medium text-muted-foreground">
                      Guven %
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {crossSell.map((pair, idx) => (
                    <tr
                      key={`${pair.sourceBarcode}-${pair.targetBarcode}-${idx}`}
                      className="border-b border-border last:border-0 hover:bg-muted/20"
                    >
                      <td className="p-3">
                        <div className="flex items-center gap-3">
                          <a
                            href={pair.sourceProductUrl || "#"}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="flex-shrink-0 group"
                          >
                            {pair.sourceImage ? (
                              <img
                                src={pair.sourceImage}
                                alt={pair.sourceProductName}
                                className="h-10 w-10 rounded object-cover border border-border group-hover:border-[#F27A1A] transition-colors"
                              />
                            ) : (
                              <div className="h-10 w-10 rounded bg-[#F27A1A] flex items-center justify-center text-white text-xs font-bold group-hover:ring-2 ring-[#F27A1A] transition-all">
                                T
                              </div>
                            )}
                          </a>
                          <div className="min-w-0">
                            <UITooltip>
                              <TooltipTrigger asChild>
                                <a
                                  href={pair.sourceProductUrl || "#"}
                                  target="_blank"
                                  rel="noopener noreferrer"
                                  className="text-sm font-medium text-foreground line-clamp-1 hover:text-[#F27A1A] hover:underline cursor-pointer block"
                                >
                                  {pair.sourceProductName}
                                </a>
                              </TooltipTrigger>
                              <TooltipContent side="top" className="max-w-[300px]">
                                <p>{pair.sourceProductName}</p>
                              </TooltipContent>
                            </UITooltip>
                            <p className="text-xs text-muted-foreground font-mono">
                              {pair.sourceBarcode}
                            </p>
                          </div>
                        </div>
                      </td>
                      <td className="p-3 text-center">
                        <ArrowRightLeft className="h-4 w-4 text-muted-foreground mx-auto" />
                      </td>
                      <td className="p-3">
                        <div className="flex items-center gap-3">
                          <a
                            href={pair.targetProductUrl || "#"}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="flex-shrink-0 group"
                          >
                            {pair.targetImage ? (
                              <img
                                src={pair.targetImage}
                                alt={pair.targetProductName}
                                className="h-10 w-10 rounded object-cover border border-border group-hover:border-[#F27A1A] transition-colors"
                              />
                            ) : (
                              <div className="h-10 w-10 rounded bg-[#F27A1A] flex items-center justify-center text-white text-xs font-bold group-hover:ring-2 ring-[#F27A1A] transition-all">
                                T
                              </div>
                            )}
                          </a>
                          <div className="min-w-0">
                            <UITooltip>
                              <TooltipTrigger asChild>
                                <a
                                  href={pair.targetProductUrl || "#"}
                                  target="_blank"
                                  rel="noopener noreferrer"
                                  className="text-sm font-medium text-foreground line-clamp-1 hover:text-[#F27A1A] hover:underline cursor-pointer block"
                                >
                                  {pair.targetProductName}
                                </a>
                              </TooltipTrigger>
                              <TooltipContent side="top" className="max-w-[300px]">
                                <p>{pair.targetProductName}</p>
                              </TooltipContent>
                            </UITooltip>
                            <p className="text-xs text-muted-foreground font-mono">
                              {pair.targetBarcode}
                            </p>
                          </div>
                        </div>
                      </td>
                      <td className="p-3 text-sm text-foreground text-right font-medium">
                        {pair.coOccurrenceCount.toLocaleString("tr-TR")}
                      </td>
                      <td className="p-3 text-right">
                        <span
                          className={cn(
                            "inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium",
                            pair.confidence >= 5
                              ? "bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400"
                              : pair.confidence >= 2
                                ? "bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400"
                                : "bg-gray-100 text-gray-600 dark:bg-gray-900/30 dark:text-gray-400"
                          )}
                        >
                          %{pair.confidence.toFixed(1)}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <div className="p-8 text-center">
              <ArrowRightLeft className="h-12 w-12 mx-auto text-muted-foreground/50 mb-3" />
              <p className="text-sm text-muted-foreground">
                Capraz satis verisi bulunamadi. Yeterli siparis verisi
                toplandikca burada analiz gorunecektir.
              </p>
            </div>
          )}
        </div>
      )}

      {/* Info Box */}
      <div className="bg-blue-50 dark:bg-blue-900/20 rounded-xl border border-blue-200 dark:border-blue-800 p-4">
        <div className="flex items-start gap-3">
          <Info className="h-5 w-5 text-blue-600 dark:text-blue-400 mt-0.5" />
          <div>
            <p className="font-medium text-blue-900 dark:text-blue-100">
              Musteri Analizi Hakkinda
            </p>
            <div className="text-sm text-blue-700 dark:text-blue-300 mt-1 space-y-2">
              {currentStore && firstSyncDate && dataStartDate ? (
                <>
                  <p>
                    <strong>{currentStore.storeName}</strong> magazasi icin musteri verileri{" "}
                    <strong>{formatDate(firstSyncDate)}</strong> tarihinde baslatilan ilk
                    senkronizasyonla olusturulmustur. Veriler{" "}
                    <strong>{formatDate(dataStartDate)}</strong> tarihinden itibaren
                    (senkronizasyon tarihinden 90 gun oncesi) siparis bilgilerini icermektedir.
                  </p>
                  <p>
                    SellerX abonesi oldugunuz surece tum yeni siparisler anlik olarak
                    senkronize edilir ve musteri analitikleri otomatik guncellenir.
                  </p>
                </>
              ) : (
                <p>
                  Musteri verileri Trendyol API&apos;sinden alinan siparis bilgilerinden
                  olusturulmaktadir. Ilk senkronizasyon tarihinden itibaren 90 gunluk gecmis
                  veriye erisim saglanir ve aboneliginiz surece veriler surekli guncellenir.
                </p>
              )}
              <p className="text-blue-600 dark:text-blue-400 text-xs">
                Musteri kimlikleri sifrelenmis olup, benzersiz e-posta adresleri uzerinden
                eslestirme yapilmaktadir.
              </p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
