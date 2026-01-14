"use client";

import { useOrderStatistics } from "@/hooks/queries/use-orders";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";
import {
  ShoppingCart,
  Clock,
  Truck,
  CheckCircle,
  XCircle,
  RotateCcw,
  TrendingUp,
  Receipt,
} from "lucide-react";

interface OrderStatsCardsProps {
  storeId: string | undefined;
}

function formatCurrency(value: number): string {
  return new Intl.NumberFormat("tr-TR", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value);
}

function StatCard({
  title,
  value,
  icon: Icon,
  valuePrefix,
  valueSuffix,
  colorClass,
}: {
  title: string;
  value: number | string;
  icon: React.ElementType;
  valuePrefix?: string;
  valueSuffix?: string;
  colorClass?: string;
}) {
  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
        <CardTitle className="text-sm font-medium text-muted-foreground">
          {title}
        </CardTitle>
        <Icon className={`h-4 w-4 ${colorClass || "text-muted-foreground"}`} />
      </CardHeader>
      <CardContent>
        <div className={`text-2xl font-bold ${colorClass || ""}`}>
          {valuePrefix}
          {value}
          {valueSuffix}
        </div>
      </CardContent>
    </Card>
  );
}

function StatCardSkeleton() {
  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
        <Skeleton className="h-4 w-24" />
        <Skeleton className="h-4 w-4 rounded" />
      </CardHeader>
      <CardContent>
        <Skeleton className="h-8 w-20" />
      </CardContent>
    </Card>
  );
}

export function OrderStatsCards({ storeId }: OrderStatsCardsProps) {
  const { data, isLoading, error } = useOrderStatistics(storeId);

  if (error) {
    return (
      <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-6">
        <p className="text-red-800 text-sm">
          İstatistikler yüklenirken hata: {error.message}
        </p>
      </div>
    );
  }

  if (isLoading || !data) {
    return (
      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4 mb-6">
        {Array.from({ length: 8 }).map((_, i) => (
          <StatCardSkeleton key={i} />
        ))}
      </div>
    );
  }

  return (
    <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4 mb-6">
      <StatCard
        title="Toplam Sipariş"
        value={data.totalOrders}
        icon={ShoppingCart}
        colorClass="text-blue-600"
      />
      <StatCard
        title="Bekleyen"
        value={data.pendingOrders}
        icon={Clock}
        colorClass="text-yellow-600"
      />
      <StatCard
        title="Kargoda"
        value={data.shippedOrders}
        icon={Truck}
        colorClass="text-blue-500"
      />
      <StatCard
        title="Teslim Edildi"
        value={data.deliveredOrders}
        icon={CheckCircle}
        colorClass="text-green-600"
      />
      <StatCard
        title="İptal Edilen"
        value={data.cancelledOrders}
        icon={XCircle}
        colorClass="text-red-600"
      />
      <StatCard
        title="İade Edilen"
        value={data.returnedOrders}
        icon={RotateCcw}
        colorClass="text-orange-600"
      />
      <StatCard
        title="Toplam Ciro"
        value={formatCurrency(data.totalRevenue)}
        icon={TrendingUp}
        valueSuffix=" TL"
        colorClass="text-green-700"
      />
      <StatCard
        title="Ortalama Sipariş"
        value={formatCurrency(data.averageOrderValue)}
        icon={Receipt}
        valueSuffix=" TL"
        colorClass="text-purple-600"
      />
    </div>
  );
}
