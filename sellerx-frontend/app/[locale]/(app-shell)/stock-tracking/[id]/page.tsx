"use client";

import { use, useMemo } from "react";
import Image from "next/image";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  ArrowLeft,
  ExternalLink,
  RefreshCw,
  Loader2,
  Package,
  TrendingDown,
  TrendingUp,
  Activity,
  FlaskConical,
} from "lucide-react";
import {
  StockHistoryChart,
  StockAlertSettings,
  StockAlertsList,
} from "@/components/stock-tracking";
import { useTrackedProductDetail, useCheckStockNow } from "@/hooks/queries/use-stock-tracking";
import { useSelectedStore } from "@/hooks/queries/use-stores";
import { useTranslations } from "next-intl";
import { useLocale } from "next-intl";
import { toast } from "sonner";
import { formatDistanceToNow } from "date-fns";
import { tr, enUS } from "date-fns/locale";
import { getMockProductDetail } from "@/lib/mock/stock-tracking-mock-data";

interface PageProps {
  params: Promise<{ id: string }>;
}

export default function StockTrackingDetailPage({ params }: PageProps) {
  const { id } = use(params);
  const t = useTranslations("stockTracking");
  const locale = useLocale();
  const { data: selectedStore } = useSelectedStore();
  const storeId = selectedStore?.selectedStoreId;

  // Detect if this is a mock product
  const isMockProduct = id.startsWith("mock-");

  // Get mock data if it's a mock product
  const mockDetail = useMemo(
    () => (isMockProduct ? getMockProductDetail(id) : null),
    [id, isMockProduct]
  );

  // Only call API for real products
  const { data: realDetail, isLoading: realLoading } = useTrackedProductDetail(
    isMockProduct ? undefined : id,
    storeId
  );

  // Use mock or real data
  const detail = isMockProduct ? mockDetail : realDetail;
  const isLoading = isMockProduct ? false : realLoading;

  const checkStock = useCheckStockNow(storeId);

  const handleCheckStock = async () => {
    if (isMockProduct) {
      toast.success(t("table.checkSuccess"));
      return;
    }
    try {
      await checkStock.mutateAsync(id);
      toast.success(t("table.checkSuccess"));
    } catch (error) {
      toast.error(error instanceof Error ? error.message : t("table.checkError"));
    }
  };

  if (!storeId) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <p className="text-muted-foreground">{t("noStore")}</p>
      </div>
    );
  }

  if (isLoading) {
    return (
      <div className="space-y-6">
        <div className="flex items-center gap-4">
          <div className="h-10 w-10 bg-gray-200 rounded animate-pulse" />
          <div className="space-y-2">
            <div className="h-6 w-64 bg-gray-200 rounded animate-pulse" />
            <div className="h-4 w-32 bg-gray-200 rounded animate-pulse" />
          </div>
        </div>
        <div className="grid gap-6 lg:grid-cols-3">
          <div className="lg:col-span-2 h-[400px] bg-gray-100 rounded animate-pulse" />
          <div className="h-[400px] bg-gray-100 rounded animate-pulse" />
        </div>
      </div>
    );
  }

  if (!detail?.product) {
    return (
      <div className="space-y-6">
        <Link href={`/${locale}/stock-tracking`}>
          <Button variant="ghost" size="sm">
            <ArrowLeft className="h-4 w-4 mr-2" />
            {t("backToList")}
          </Button>
        </Link>
        <div className="flex items-center justify-center min-h-[300px]">
          <p className="text-muted-foreground">{t("productNotFound")}</p>
        </div>
      </div>
    );
  }

  const { product, snapshots, alerts, statistics } = detail;

  const getStockStatusBadge = () => {
    if (product.lastStockQuantity === null) {
      return <Badge variant="outline">{t("table.noData")}</Badge>;
    }
    if (product.lastStockQuantity === 0) {
      return <Badge variant="destructive">{t("table.outOfStock")}</Badge>;
    }
    if (product.lastStockQuantity <= product.lowStockThreshold) {
      return (
        <Badge variant="outline" className="border-orange-500 text-orange-600">
          {t("table.lowStock")}
        </Badge>
      );
    }
    return (
      <Badge variant="outline" className="border-green-500 text-green-600">
        {t("table.inStock")}
      </Badge>
    );
  };

  return (
    <div className="space-y-6">
      {/* Back Button */}
      <Link href={`/${locale}/stock-tracking`}>
        <Button variant="ghost" size="sm">
          <ArrowLeft className="h-4 w-4 mr-2" />
          {t("backToList")}
        </Button>
      </Link>

      {/* Mock Mode Banner */}
      {isMockProduct && (
        <div className="bg-amber-100 dark:bg-amber-900/30 border border-amber-300 dark:border-amber-800 rounded-lg p-3 flex items-center gap-2">
          <FlaskConical className="h-4 w-4 text-amber-600 dark:text-amber-400" />
          <span className="text-sm text-amber-800 dark:text-amber-200">
            {t("mockModeEnabled")}
          </span>
        </div>
      )}

      {/* Product Header */}
      <div className="flex flex-col sm:flex-row gap-4 items-start">
        {product.imageUrl ? (
          <Image
            src={product.imageUrl}
            alt={product.productName}
            width={80}
            height={80}
            className="rounded-lg object-cover"
          />
        ) : (
          <div className="h-20 w-20 bg-gray-100 rounded-lg flex items-center justify-center">
            <Package className="h-8 w-8 text-gray-400" />
          </div>
        )}
        <div className="flex-1">
          <div className="flex items-start justify-between gap-4">
            <div>
              <h1 className="text-xl font-bold">{product.productName}</h1>
              <p className="text-muted-foreground">{product.brandName}</p>
              <div className="flex items-center gap-2 mt-2">
                {getStockStatusBadge()}
                {!product.isActive && (
                  <Badge variant="secondary">{t("detail.paused")}</Badge>
                )}
              </div>
            </div>
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={handleCheckStock}
                disabled={checkStock.isPending}
              >
                {checkStock.isPending ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  <RefreshCw className="h-4 w-4" />
                )}
                <span className="ml-2">{t("table.checkNow")}</span>
              </Button>
              <Button variant="outline" size="sm" asChild>
                <a href={product.productUrl} target="_blank" rel="noopener noreferrer">
                  <ExternalLink className="h-4 w-4 mr-2" />
                  Trendyol
                </a>
              </Button>
            </div>
          </div>
        </div>
      </div>

      {/* Current Stock Info */}
      <div className="grid gap-4 md:grid-cols-4">
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              {t("detail.currentStock")}
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {product.lastStockQuantity ?? "-"} {t("table.unit")}
            </div>
            {product.lastCheckedAt && (
              <p className="text-xs text-muted-foreground mt-1">
                {formatDistanceToNow(new Date(product.lastCheckedAt), {
                  addSuffix: true,
                  locale: locale === "tr" ? tr : enUS,
                })}
              </p>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              {t("detail.price")}
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {product.lastPrice
                ? `${product.lastPrice.toLocaleString(locale === "tr" ? "tr-TR" : "en-US")} TL`
                : "-"}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground flex items-center gap-1">
              <TrendingDown className="h-4 w-4" />
              {t("detail.minStock")}
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {statistics.minStock} {t("table.unit")}
            </div>
            <p className="text-xs text-muted-foreground">
              {t("detail.last30Days")}
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground flex items-center gap-1">
              <TrendingUp className="h-4 w-4" />
              {t("detail.maxStock")}
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {statistics.maxStock} {t("table.unit")}
            </div>
            <p className="text-xs text-muted-foreground">
              {t("detail.last30Days")}
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Statistics Row */}
      <div className="grid gap-4 md:grid-cols-3">
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground flex items-center gap-1">
              <Activity className="h-4 w-4" />
              {t("detail.avgStock")}
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-xl font-bold">
              {statistics.avgStock.toFixed(1)} {t("table.unit")}
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              {t("detail.totalChecks")}
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-xl font-bold">{statistics.totalChecks}</div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              {t("detail.outOfStockCount")}
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-xl font-bold text-red-600">
              {statistics.outOfStockCount}
            </div>
            {statistics.lastOutOfStock && (
              <p className="text-xs text-muted-foreground">
                {t("detail.lastOutOfStock")}:{" "}
                {formatDistanceToNow(new Date(statistics.lastOutOfStock), {
                  addSuffix: true,
                  locale: locale === "tr" ? tr : enUS,
                })}
              </p>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Main Content */}
      <div className="grid gap-6 lg:grid-cols-3">
        {/* Chart */}
        <div className="lg:col-span-2">
          <StockHistoryChart
            snapshots={snapshots}
            lowStockThreshold={product.lowStockThreshold}
            isLoading={false}
          />
        </div>

        {/* Settings */}
        <div>
          <StockAlertSettings product={product} storeId={storeId} isLoading={false} />
        </div>
      </div>

      {/* Alerts */}
      <StockAlertsList
        alerts={alerts}
        storeId={storeId}
        isLoading={false}
        showMarkAllRead={false}
        maxHeight="300px"
      />
    </div>
  );
}
