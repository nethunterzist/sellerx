"use client";

const isDev = process.env.NODE_ENV === "development";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { useMyStores, useDeleteStore } from "@/hooks/queries/use-stores";
import {
  useWebhookStatus,
  useEnableWebhooks,
  useDisableWebhooks,
} from "@/hooks/queries/use-webhooks";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Switch } from "@/components/ui/switch";
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
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@/components/ui/collapsible";
import { SettingsSection } from "./settings-section";
import {
  Store,
  Trash2,
  Loader2,
  Plus,
  ExternalLink,
  ChevronDown,
  Webhook,
  CheckCircle2,
  Copy,
  Zap,
  Settings2,
} from "lucide-react";
import { cn } from "@/lib/utils";
import { toast } from "sonner";

interface StoreWebhookStatusProps {
  storeId: string;
  storeName: string;
}

function StoreWebhookStatus({ storeId, storeName }: StoreWebhookStatusProps) {
  const { data: status, isLoading } = useWebhookStatus(storeId);
  const enableMutation = useEnableWebhooks();
  const disableMutation = useDisableWebhooks();
  const [copiedUrl, setCopiedUrl] = useState(false);

  const handleToggle = async (enabled: boolean) => {
    try {
      if (enabled) {
        await enableMutation.mutateAsync(storeId);
        toast.success("Webhook etkinlestirildi");
      } else {
        await disableMutation.mutateAsync(storeId);
        toast.success("Webhook devre disi birakildi");
      }
    } catch (error) {
      if (isDev) console.error("Webhook toggle error:", error);
      toast.error("Islem basarisiz oldu");
    }
  };

  const handleCopyUrl = () => {
    if (status?.webhookUrl) {
      navigator.clipboard.writeText(window.location.origin + status.webhookUrl);
      setCopiedUrl(true);
      toast.success("URL kopyalandi");
      setTimeout(() => setCopiedUrl(false), 2000);
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center gap-2 text-sm text-muted-foreground">
        <Loader2 className="h-4 w-4 animate-spin" />
        Yukleniyor...
      </div>
    );
  }

  const isToggling = enableMutation.isPending || disableMutation.isPending;

  return (
    <div className="space-y-4 pt-4 border-t border-border">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div
            className={cn(
              "h-10 w-10 rounded-lg flex items-center justify-center",
              status?.enabled
                ? "bg-green-100 dark:bg-green-900/30 text-green-600 dark:text-green-400"
                : "bg-muted text-muted-foreground"
            )}
          >
            <Webhook className="h-5 w-5" />
          </div>
          <div>
            <p className="font-medium text-sm">Gercek Zamanli Bildirimler</p>
            <p className="text-xs text-muted-foreground">
              {status?.enabled
                ? "Siparisler aninda bildirilir"
                : "Manuel senkronizasyon gerekli"}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-3">
          {status?.enabled && (
            <Badge className="bg-green-100 text-green-700 border-green-200">
              <span className="flex h-1.5 w-1.5 rounded-full bg-green-500 mr-1.5" />
              Aktif
            </Badge>
          )}
          <Switch
            checked={status?.enabled || false}
            onCheckedChange={handleToggle}
            disabled={isToggling}
          />
        </div>
      </div>

      {status?.enabled && status?.webhookUrl && (
        <div className="p-3 rounded-lg bg-muted/50 border border-border">
          <div className="flex items-center justify-between gap-2">
            <div className="min-w-0 flex-1">
              <p className="text-xs text-muted-foreground mb-1">Webhook URL</p>
              <code className="text-xs text-foreground font-mono block truncate">
                {window.location.origin}
                {status.webhookUrl}
              </code>
            </div>
            <Button
              variant="ghost"
              size="sm"
              onClick={handleCopyUrl}
              className="shrink-0"
            >
              {copiedUrl ? (
                <CheckCircle2 className="h-4 w-4 text-green-600" />
              ) : (
                <Copy className="h-4 w-4" />
              )}
            </Button>
          </div>
        </div>
      )}

      {status?.enabled && (
        <div className="grid grid-cols-3 gap-3">
          <div className="p-3 rounded-lg bg-muted/50 text-center">
            <p className="text-lg font-semibold">{status?.totalEvents || 0}</p>
            <p className="text-xs text-muted-foreground">Toplam</p>
          </div>
          <div className="p-3 rounded-lg bg-green-50 dark:bg-green-900/20 text-center">
            <p className="text-lg font-semibold text-green-600">
              {status?.eventStats?.COMPLETED || 0}
            </p>
            <p className="text-xs text-muted-foreground">Basarili</p>
          </div>
          <div className="p-3 rounded-lg bg-red-50 dark:bg-red-900/20 text-center">
            <p className="text-lg font-semibold text-red-600">
              {status?.eventStats?.FAILED || 0}
            </p>
            <p className="text-xs text-muted-foreground">Basarisiz</p>
          </div>
        </div>
      )}
    </div>
  );
}

export function StoresSettings() {
  const router = useRouter();
  const { data: stores, isLoading: storesLoading } = useMyStores();
  const deleteStoreMutation = useDeleteStore();
  const [expandedStore, setExpandedStore] = useState<string | null>(null);

  const handleDeleteStore = (storeId: string) => {
    deleteStoreMutation.mutate(storeId);
  };

  return (
    <div className="space-y-6">
      <SettingsSection
        title="Bagli Magazalar"
        description="Hesabiniza bagli pazaryeri magazalarini yonetin"
        action={
          <Button
            onClick={() => router.push("/new-store")}
            className="bg-[#F27A1A] hover:bg-[#E06A0A]"
          >
            <Plus className="h-4 w-4 mr-2" />
            Magaza Ekle
          </Button>
        }
        noPadding
      >
        {storesLoading ? (
          <div className="flex items-center justify-center py-12">
            <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
          </div>
        ) : stores && stores.length > 0 ? (
          <div className="divide-y divide-border">
            {stores.map((store: any) => {
              const isExpanded = expandedStore === store.id;

              return (
                <Collapsible
                  key={store.id}
                  open={isExpanded}
                  onOpenChange={() =>
                    setExpandedStore(isExpanded ? null : store.id)
                  }
                >
                  <div className="p-5">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-4">
                        <div
                          className={cn(
                            "h-12 w-12 rounded-xl flex items-center justify-center text-lg font-bold text-white shadow-sm",
                            store.marketplace === "trendyol"
                              ? "bg-gradient-to-br from-[#F27A1A] to-[#E06A0A]"
                              : "bg-gradient-to-br from-[#FF6000] to-[#E05500]"
                          )}
                        >
                          {store.marketplace === "trendyol" ? "T" : "H"}
                        </div>
                        <div>
                          <p className="font-medium text-foreground">
                            {store.storeName || store.store_name}
                          </p>
                          <div className="flex items-center gap-2 text-sm text-muted-foreground">
                            <span className="capitalize">
                              {store.marketplace}
                            </span>
                            <span className="text-muted-foreground/50">•</span>
                            <span>Seller ID: {store.sellerId}</span>
                          </div>
                        </div>
                      </div>

                      <div className="flex items-center gap-2">
                        <CollapsibleTrigger asChild>
                          <Button
                            variant="ghost"
                            size="sm"
                            className="text-muted-foreground hover:text-foreground"
                          >
                            <Settings2 className="h-4 w-4 mr-1" />
                            Ayarlar
                            <ChevronDown
                              className={cn(
                                "h-4 w-4 ml-1 transition-transform",
                                isExpanded && "rotate-180"
                              )}
                            />
                          </Button>
                        </CollapsibleTrigger>

                        <Button
                          variant="ghost"
                          size="sm"
                          className="text-muted-foreground hover:text-foreground"
                          onClick={() => {
                            if (store.marketplace === "trendyol") {
                              window.open(
                                "https://partner.trendyol.com",
                                "_blank"
                              );
                            }
                          }}
                        >
                          <ExternalLink className="h-4 w-4" />
                        </Button>

                        <AlertDialog>
                          <AlertDialogTrigger asChild>
                            <Button
                              variant="ghost"
                              size="sm"
                              className="text-red-600 hover:text-red-700 hover:bg-red-50"
                            >
                              <Trash2 className="h-4 w-4" />
                            </Button>
                          </AlertDialogTrigger>
                          <AlertDialogContent>
                            <AlertDialogHeader>
                              <AlertDialogTitle>Magazayi Sil</AlertDialogTitle>
                              <AlertDialogDescription>
                                <strong>
                                  {store.storeName || store.store_name}
                                </strong>{" "}
                                magazasini silmek istediginizden emin misiniz?
                                Bu islem geri alinamaz ve magazaya ait tum
                                veriler silinecektir.
                              </AlertDialogDescription>
                            </AlertDialogHeader>
                            <AlertDialogFooter>
                              <AlertDialogCancel>Iptal</AlertDialogCancel>
                              <AlertDialogAction
                                onClick={() => handleDeleteStore(store.id)}
                                className="bg-red-600 hover:bg-red-700"
                              >
                                Magazayi Sil
                              </AlertDialogAction>
                            </AlertDialogFooter>
                          </AlertDialogContent>
                        </AlertDialog>
                      </div>
                    </div>

                    <CollapsibleContent>
                      <StoreWebhookStatus
                        storeId={store.id}
                        storeName={store.storeName || store.store_name}
                      />
                    </CollapsibleContent>
                  </div>
                </Collapsible>
              );
            })}
          </div>
        ) : (
          <div className="text-center py-12">
            <div className="h-16 w-16 rounded-2xl bg-muted flex items-center justify-center mx-auto mb-4">
              <Store className="h-8 w-8 text-muted-foreground" />
            </div>
            <p className="text-foreground font-medium mb-1">
              Henuz magaza yok
            </p>
            <p className="text-sm text-muted-foreground mb-4">
              Ilk magazanizi eklemek icin asagidaki butonu kullanin
            </p>
            <Button
              onClick={() => router.push("/new-store")}
              className="bg-[#F27A1A] hover:bg-[#E06A0A]"
            >
              <Plus className="h-4 w-4 mr-2" />
              Magaza Ekle
            </Button>
          </div>
        )}
      </SettingsSection>

      {/* Info Card */}
      <div className="bg-blue-50 dark:bg-blue-900/20 rounded-xl border border-blue-200 dark:border-blue-800 p-6">
        <div className="flex gap-4">
          <Zap className="h-5 w-5 text-blue-600 flex-shrink-0 mt-0.5" />
          <div>
            <p className="font-medium text-blue-800 dark:text-blue-200">
              Gercek Zamanli Bildirimler
            </p>
            <ul className="text-sm text-blue-700 dark:text-blue-300 mt-2 space-y-1">
              <li>
                • Her magaza icin ayri webhook ayari yapabilirsiniz
              </li>
              <li>
                • Webhook etkinlestirildiginde, siparisler 2-3 saniye icinde
                sisteme eklenir
              </li>
              <li>
                • Her bildirim guvenli bir sekilde dogrulanir ve kaydedilir
              </li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
}
