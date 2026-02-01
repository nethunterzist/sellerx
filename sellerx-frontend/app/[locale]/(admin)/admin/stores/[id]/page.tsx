"use client";

import { use } from "react";
import Link from "next/link";
import { useAdminStore, useTriggerStoreSync } from "@/hooks/queries/use-admin";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  ArrowLeft,
  Store,
  User,
  Package,
  ShoppingCart,
  AlertTriangle,
  CheckCircle,
  RefreshCw,
  Clock,
  Key,
  Webhook,
  Calendar,
  Loader2,
} from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import { format } from "date-fns";
import { tr } from "date-fns/locale";
import { toast } from "sonner";

export default function AdminStoreDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const { data: store, isLoading, error } = useAdminStore(id);
  const triggerSync = useTriggerStoreSync();

  const formatDate = (date: string | null) => {
    if (!date) return "-";
    try {
      return format(new Date(date), "dd MMMM yyyy, HH:mm", { locale: tr });
    } catch {
      return "-";
    }
  };

  const handleTriggerSync = async () => {
    try {
      await triggerSync.mutateAsync(id);
      toast.success("Sync başlatıldı");
    } catch {
      toast.error("Sync başlatılamadı");
    }
  };

  const getPhaseStatusBadge = (status: string) => {
    switch (status) {
      case "COMPLETED":
        return (
          <Badge className="bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400">
            <CheckCircle className="h-3 w-3 mr-1" />
            Tamamlandı
          </Badge>
        );
      case "ACTIVE":
        return (
          <Badge className="bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400">
            <RefreshCw className="h-3 w-3 mr-1 animate-spin" />
            Çalışıyor
          </Badge>
        );
      case "FAILED":
        return (
          <Badge className="bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400">
            <AlertTriangle className="h-3 w-3 mr-1" />
            Hata
          </Badge>
        );
      case "PENDING":
        return (
          <Badge className="bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400">
            <Clock className="h-3 w-3 mr-1" />
            Bekliyor
          </Badge>
        );
      default:
        return <Badge variant="secondary">{status}</Badge>;
    }
  };

  if (isLoading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-8 w-48" />
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <Skeleton className="h-64" />
          <Skeleton className="h-64" />
        </div>
      </div>
    );
  }

  if (error || !store) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-center">
          <AlertTriangle className="h-12 w-12 text-red-500 mx-auto mb-4" />
          <p className="text-lg text-slate-600 dark:text-slate-400">
            Mağaza bulunamadı
          </p>
          <Button variant="outline" asChild className="mt-4">
            <Link href="/admin/stores">
              <ArrowLeft className="h-4 w-4 mr-2" />
              Listeye Dön
            </Link>
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Back Button */}
      <Button variant="ghost" asChild>
        <Link href="/admin/stores">
          <ArrowLeft className="h-4 w-4 mr-2" />
          Mağazalara Dön
        </Link>
      </Button>

      {/* Store Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <div className="flex h-16 w-16 items-center justify-center rounded-full bg-purple-100 dark:bg-purple-900/30">
            <Store className="h-8 w-8 text-purple-600 dark:text-purple-400" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-slate-900 dark:text-white">
              {store.storeName}
            </h1>
            <p className="text-slate-500">{store.marketplace}</p>
          </div>
        </div>
        <Button
          onClick={handleTriggerSync}
          disabled={triggerSync.isPending}
        >
          {triggerSync.isPending ? (
            <Loader2 className="h-4 w-4 mr-2 animate-spin" />
          ) : (
            <RefreshCw className="h-4 w-4 mr-2" />
          )}
          Sync Başlat
        </Button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Store Info */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Store className="h-5 w-5" />
              Mağaza Bilgileri
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex items-center gap-3">
              <User className="h-5 w-5 text-slate-400" />
              <div>
                <p className="text-sm text-slate-500">Sahip</p>
                <p className="font-medium text-slate-900 dark:text-white">{store.userName}</p>
                <p className="text-sm text-slate-500">{store.userEmail}</p>
              </div>
            </div>
            <div className="flex items-center gap-3">
              <Calendar className="h-5 w-5 text-slate-400" />
              <div>
                <p className="text-sm text-slate-500">Oluşturulma Tarihi</p>
                <p className="font-medium text-slate-900 dark:text-white">
                  {formatDate(store.createdAt)}
                </p>
              </div>
            </div>
            <div className="flex items-center gap-3">
              <Package className="h-5 w-5 text-slate-400" />
              <div>
                <p className="text-sm text-slate-500">Ürün Sayısı</p>
                <p className="font-medium text-slate-900 dark:text-white">{store.productCount}</p>
              </div>
            </div>
            <div className="flex items-center gap-3">
              <ShoppingCart className="h-5 w-5 text-slate-400" />
              <div>
                <p className="text-sm text-slate-500">Sipariş Sayısı</p>
                <p className="font-medium text-slate-900 dark:text-white">{store.orderCount}</p>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Credentials */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Key className="h-5 w-5" />
              API Bilgileri
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div>
              <p className="text-sm text-slate-500">Seller ID</p>
              <p className="font-mono text-slate-900 dark:text-white">
                {store.sellerId || "-"}
              </p>
            </div>
            <div className="flex items-center gap-3">
              <div>
                <p className="text-sm text-slate-500">API Key</p>
                <Badge variant={store.hasApiKey ? "default" : "secondary"}>
                  {store.hasApiKey ? "Tanımlı" : "Yok"}
                </Badge>
              </div>
              <div>
                <p className="text-sm text-slate-500">API Secret</p>
                <Badge variant={store.hasApiSecret ? "default" : "secondary"}>
                  {store.hasApiSecret ? "Tanımlı" : "Yok"}
                </Badge>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Sync Status */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <RefreshCw className="h-5 w-5" />
            Sync Durumu
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-6">
            <div>
              <p className="text-sm text-slate-500">Genel Durum</p>
              {getPhaseStatusBadge(store.overallSyncStatus || store.syncStatus)}
            </div>
            <div>
              <p className="text-sm text-slate-500">Initial Sync</p>
              <Badge variant={store.initialSyncCompleted ? "default" : "secondary"}>
                {store.initialSyncCompleted ? "Tamamlandı" : "Bekliyor"}
              </Badge>
            </div>
            <div>
              <p className="text-sm text-slate-500">Son Güncelleme</p>
              <p className="text-sm text-slate-900 dark:text-white">
                {formatDate(store.updatedAt)}
              </p>
            </div>
            <div>
              <p className="text-sm text-slate-500">Hata Mesajı</p>
              <p className="text-sm text-red-500">
                {store.syncErrorMessage || "-"}
              </p>
            </div>
          </div>

          {/* Sync Phases */}
          {store.syncPhases && Object.keys(store.syncPhases).length > 0 && (
            <div>
              <h4 className="font-medium text-slate-900 dark:text-white mb-3">
                Sync Fazları
              </h4>
              <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-7 gap-3">
                {Object.entries(store.syncPhases).map(([phase, info]) => (
                  <div
                    key={phase}
                    className="p-3 rounded-lg bg-slate-50 dark:bg-slate-800/50"
                  >
                    <p className="text-xs text-slate-500 uppercase mb-1">{phase}</p>
                    {getPhaseStatusBadge((info as { status: string }).status)}
                  </div>
                ))}
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Webhook Status */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Webhook className="h-5 w-5" />
            Webhook Durumu
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <div>
              <p className="text-sm text-slate-500">Webhook ID</p>
              <p className="font-mono text-sm text-slate-900 dark:text-white truncate">
                {store.webhookId || "-"}
              </p>
            </div>
            <div>
              <p className="text-sm text-slate-500">Durum</p>
              {store.webhookStatus === "active" ? (
                <Badge className="bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400">
                  <CheckCircle className="h-3 w-3 mr-1" />
                  Aktif
                </Badge>
              ) : store.webhookStatus === "failed" ? (
                <Badge className="bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400">
                  <AlertTriangle className="h-3 w-3 mr-1" />
                  Hata
                </Badge>
              ) : (
                <Badge variant="secondary">{store.webhookStatus || "-"}</Badge>
              )}
            </div>
            <div className="col-span-2">
              <p className="text-sm text-slate-500">Hata Mesajı</p>
              <p className="text-sm text-red-500">
                {store.webhookErrorMessage || "-"}
              </p>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Historical Sync */}
      {store.historicalSyncStatus && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Clock className="h-5 w-5" />
              Tarihsel Veri Sync
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              <div>
                <p className="text-sm text-slate-500">Durum</p>
                {getPhaseStatusBadge(store.historicalSyncStatus)}
              </div>
              <div>
                <p className="text-sm text-slate-500">Tarih</p>
                <p className="text-sm text-slate-900 dark:text-white">
                  {formatDate(store.historicalSyncDate)}
                </p>
              </div>
              <div>
                <p className="text-sm text-slate-500">İlerleme</p>
                <p className="text-sm text-slate-900 dark:text-white">
                  {store.historicalSyncCompletedChunks || 0} / {store.historicalSyncTotalChunks || 0}
                </p>
              </div>
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
