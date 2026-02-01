"use client";

const isDev = process.env.NODE_ENV === "development";

import { useState } from "react";
import { useSelectedStore } from "@/hooks/queries/use-stores";
import {
  useWebhookStatus,
  useWebhookEvents,
  useEnableWebhooks,
  useDisableWebhooks,
  useTestWebhook,
} from "@/hooks/queries/use-webhooks";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  Loader2,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  Webhook,
  Play,
  Power,
  PowerOff,
  RefreshCw,
  Clock,
  Zap,
  Copy,
  ChevronLeft,
  ChevronRight,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { format } from "date-fns";
import { tr } from "date-fns/locale";

export function WebhookSettings() {
  const { data: selectedStore } = useSelectedStore();
  const storeId = selectedStore?.selectedStoreId;

  const { data: status, isLoading: statusLoading, refetch: refetchStatus } = useWebhookStatus(storeId || undefined);
  const [page, setPage] = useState(0);
  const { data: eventsData, isLoading: eventsLoading, refetch: refetchEvents } = useWebhookEvents(storeId || undefined, page, 10);

  const enableMutation = useEnableWebhooks();
  const disableMutation = useDisableWebhooks();
  const testMutation = useTestWebhook();

  const [copiedUrl, setCopiedUrl] = useState(false);

  const handleEnable = async () => {
    if (!storeId) return;
    try {
      await enableMutation.mutateAsync(storeId);
    } catch (error) {
      if (isDev) console.error("Webhook etkinleştirme hatası:", error);
    }
  };

  const handleDisable = async () => {
    if (!storeId) return;
    try {
      await disableMutation.mutateAsync(storeId);
    } catch (error) {
      if (isDev) console.error("Webhook devre dışı bırakma hatası:", error);
    }
  };

  const handleTest = async () => {
    if (!storeId) return;
    try {
      await testMutation.mutateAsync(storeId);
    } catch (error) {
      if (isDev) console.error("Webhook test hatası:", error);
    }
  };

  const handleCopyUrl = () => {
    if (status?.webhookUrl) {
      navigator.clipboard.writeText(window.location.origin + status.webhookUrl);
      setCopiedUrl(true);
      setTimeout(() => setCopiedUrl(false), 2000);
    }
  };

  const getStatusBadge = (processingStatus: string) => {
    switch (processingStatus) {
      case "COMPLETED":
        return <Badge className="bg-green-100 text-green-700">Tamamlandı</Badge>;
      case "FAILED":
        return <Badge className="bg-red-100 text-red-700">Başarısız</Badge>;
      case "DUPLICATE":
        return <Badge className="bg-yellow-100 text-yellow-700">Tekrar</Badge>;
      case "PROCESSING":
        return <Badge className="bg-blue-100 text-blue-700">İşleniyor</Badge>;
      default:
        return <Badge className="bg-muted text-muted-foreground">{processingStatus}</Badge>;
    }
  };

  if (!storeId) {
    return (
      <div className="space-y-6">
        <div>
          <h2 className="text-lg font-semibold text-foreground">Webhook Ayarları</h2>
          <p className="text-sm text-muted-foreground mt-1">
            Trendyol sipariş bildirimlerini gerçek zamanlı alın
          </p>
        </div>

        <div className="bg-yellow-50 dark:bg-yellow-900/20 rounded-xl border border-yellow-200 dark:border-yellow-800 p-6">
          <div className="flex items-center gap-3">
            <AlertTriangle className="h-5 w-5 text-yellow-600" />
            <div>
              <p className="font-medium text-yellow-800">Mağaza Seçilmedi</p>
              <p className="text-sm text-yellow-700 mt-1">
                Webhook ayarlarını görmek için lütfen bir mağaza seçin.
              </p>
            </div>
          </div>
        </div>
      </div>
    );
  }

  if (statusLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-lg font-semibold text-foreground">Webhook Ayarları</h2>
        <p className="text-sm text-muted-foreground mt-1">
          Trendyol sipariş bildirimlerini gerçek zamanlı alın
        </p>
      </div>

      {/* Status Card */}
      <div className="bg-card rounded-xl border border-border p-6">
        <div className="flex items-center justify-between mb-6">
          <div className="flex items-center gap-3">
            <div
              className={cn(
                "h-12 w-12 rounded-xl flex items-center justify-center",
                status?.enabled
                  ? "bg-green-100 dark:bg-green-900/30 text-green-600 dark:text-green-400"
                  : "bg-muted text-muted-foreground"
              )}
            >
              <Webhook className="h-6 w-6" />
            </div>
            <div>
              <p className="font-medium text-foreground">Webhook Durumu</p>
              <div className="flex items-center gap-2 mt-1">
                {status?.enabled ? (
                  <>
                    <span className="flex h-2 w-2 rounded-full bg-green-500" />
                    <span className="text-sm text-green-600">Aktif</span>
                  </>
                ) : (
                  <>
                    <span className="flex h-2 w-2 rounded-full bg-muted-foreground" />
                    <span className="text-sm text-muted-foreground">Devre Dışı</span>
                  </>
                )}
              </div>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => {
                refetchStatus();
                refetchEvents();
              }}
            >
              <RefreshCw className="h-4 w-4 mr-2" />
              Yenile
            </Button>

            {status?.enabled ? (
              <AlertDialog>
                <AlertDialogTrigger asChild>
                  <Button
                    variant="outline"
                    size="sm"
                    className="text-red-600 hover:text-red-700 border-red-200 hover:bg-red-50"
                    disabled={disableMutation.isPending}
                  >
                    {disableMutation.isPending ? (
                      <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                    ) : (
                      <PowerOff className="h-4 w-4 mr-2" />
                    )}
                    Devre Dışı Bırak
                  </Button>
                </AlertDialogTrigger>
                <AlertDialogContent>
                  <AlertDialogHeader>
                    <AlertDialogTitle>Webhook'u Devre Dışı Bırak</AlertDialogTitle>
                    <AlertDialogDescription>
                      Webhook'u devre dışı bırakırsanız, yeni siparişler hakkında
                      gerçek zamanlı bildirim almayacaksınız. Siparişleri görmek için
                      manuel senkronizasyon yapmanız gerekecek.
                    </AlertDialogDescription>
                  </AlertDialogHeader>
                  <AlertDialogFooter>
                    <AlertDialogCancel>İptal</AlertDialogCancel>
                    <AlertDialogAction
                      onClick={handleDisable}
                      className="bg-red-600 hover:bg-red-700"
                    >
                      Devre Dışı Bırak
                    </AlertDialogAction>
                  </AlertDialogFooter>
                </AlertDialogContent>
              </AlertDialog>
            ) : (
              <Button
                onClick={handleEnable}
                disabled={enableMutation.isPending}
                className="bg-[#1D70F1] hover:bg-[#1560d1]"
              >
                {enableMutation.isPending ? (
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                ) : (
                  <Power className="h-4 w-4 mr-2" />
                )}
                Webhook'u Etkinleştir
              </Button>
            )}
          </div>
        </div>

        {/* Webhook URL */}
        {status?.webhookUrl && (
          <div className="p-4 rounded-lg bg-muted border border-border">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-xs text-muted-foreground mb-1">Webhook URL</p>
                <code className="text-sm text-foreground font-mono">
                  {window.location.origin}{status.webhookUrl}
                </code>
              </div>
              <Button variant="ghost" size="sm" onClick={handleCopyUrl}>
                {copiedUrl ? (
                  <CheckCircle2 className="h-4 w-4 text-green-600" />
                ) : (
                  <Copy className="h-4 w-4" />
                )}
              </Button>
            </div>
          </div>
        )}

        {/* Stats */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 mt-6">
          <div className="p-4 rounded-lg bg-muted">
            <p className="text-2xl font-semibold text-foreground">
              {status?.totalEvents || 0}
            </p>
            <p className="text-sm text-muted-foreground mt-1">Toplam Olay</p>
          </div>
          <div className="p-4 rounded-lg bg-green-50 dark:bg-green-900/20">
            <p className="text-2xl font-semibold text-green-600">
              {status?.eventStats?.COMPLETED || 0}
            </p>
            <p className="text-sm text-muted-foreground mt-1">Başarılı</p>
          </div>
          <div className="p-4 rounded-lg bg-red-50 dark:bg-red-900/20">
            <p className="text-2xl font-semibold text-red-600">
              {status?.eventStats?.FAILED || 0}
            </p>
            <p className="text-sm text-muted-foreground mt-1">Başarısız</p>
          </div>
          <div className="p-4 rounded-lg bg-yellow-50 dark:bg-yellow-900/20">
            <p className="text-2xl font-semibold text-yellow-600">
              {status?.eventStats?.DUPLICATE || 0}
            </p>
            <p className="text-sm text-muted-foreground mt-1">Tekrar</p>
          </div>
        </div>

        {/* Test Button */}
        <div className="pt-6 mt-6 border-t border-border">
          <div className="flex items-center justify-between">
            <div>
              <p className="font-medium text-foreground">Webhook Testi</p>
              <p className="text-sm text-muted-foreground">
                Webhook bağlantısını test etmek için bir test olayı oluşturun
              </p>
            </div>
            <Button
              variant="outline"
              onClick={handleTest}
              disabled={testMutation.isPending}
            >
              {testMutation.isPending ? (
                <Loader2 className="h-4 w-4 mr-2 animate-spin" />
              ) : (
                <Play className="h-4 w-4 mr-2" />
              )}
              Test Et
            </Button>
          </div>
        </div>
      </div>

      {/* Info Card */}
      <div className="bg-blue-50 dark:bg-blue-900/20 rounded-xl border border-blue-200 dark:border-blue-800 p-6">
        <div className="flex gap-4">
          <Zap className="h-5 w-5 text-blue-600 flex-shrink-0 mt-0.5" />
          <div>
            <p className="font-medium text-blue-800">Nasıl Çalışır?</p>
            <ul className="text-sm text-blue-700 mt-2 space-y-1">
              <li>• Webhook etkinleştirildiğinde, Trendyol yeni siparişleri anında bildirir</li>
              <li>• Siparişler 2-3 saniye içinde sisteminize eklenir</li>
              <li>• Manuel senkronizasyon yerine otomatik güncelleme alırsınız</li>
              <li>• Her bildirim güvenli bir şekilde doğrulanır ve kaydedilir</li>
            </ul>
          </div>
        </div>
      </div>

      {/* Event Logs */}
      <div className="bg-card rounded-xl border border-border">
        <div className="flex items-center justify-between p-6 border-b border-border">
          <div>
            <h3 className="font-medium text-foreground">Olay Geçmişi</h3>
            <p className="text-sm text-muted-foreground mt-1">
              Son webhook bildirimleri ve işlem durumları
            </p>
          </div>
        </div>

        {eventsLoading ? (
          <div className="flex items-center justify-center py-12">
            <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
          </div>
        ) : eventsData?.content && eventsData.content.length > 0 ? (
          <>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Tarih</TableHead>
                  <TableHead>Olay Tipi</TableHead>
                  <TableHead>Sipariş No</TableHead>
                  <TableHead>Durum</TableHead>
                  <TableHead className="text-right">Süre</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {eventsData.content.map((event) => (
                  <TableRow key={event.id}>
                    <TableCell className="font-mono text-xs">
                      {format(new Date(event.createdAt), "dd MMM yyyy HH:mm:ss", {
                        locale: tr,
                      })}
                    </TableCell>
                    <TableCell>
                      <Badge variant="outline">{event.eventType}</Badge>
                    </TableCell>
                    <TableCell className="font-mono">
                      {event.orderNumber || "-"}
                    </TableCell>
                    <TableCell>{getStatusBadge(event.processingStatus)}</TableCell>
                    <TableCell className="text-right text-sm text-muted-foreground">
                      {event.processingTimeMs ? `${event.processingTimeMs}ms` : "-"}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>

            {/* Pagination */}
            {eventsData.totalPages > 1 && (
              <div className="flex items-center justify-between p-4 border-t border-border">
                <p className="text-sm text-muted-foreground">
                  {eventsData.totalElements} olaydan{" "}
                  {page * eventsData.size + 1}-
                  {Math.min((page + 1) * eventsData.size, eventsData.totalElements)}{" "}
                  arası gösteriliyor
                </p>
                <div className="flex items-center gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                    disabled={page === 0}
                  >
                    <ChevronLeft className="h-4 w-4" />
                  </Button>
                  <span className="text-sm text-muted-foreground px-2">
                    {page + 1} / {eventsData.totalPages}
                  </span>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setPage((p) => p + 1)}
                    disabled={page >= eventsData.totalPages - 1}
                  >
                    <ChevronRight className="h-4 w-4" />
                  </Button>
                </div>
              </div>
            )}
          </>
        ) : (
          <div className="text-center py-12">
            <Clock className="h-12 w-12 mx-auto text-muted-foreground mb-3" />
            <p className="text-muted-foreground mb-1">Henüz olay yok</p>
            <p className="text-sm text-muted-foreground">
              Webhook etkinleştirildiğinde olaylar burada görünecek
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
