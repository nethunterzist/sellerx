"use client";

import { useState, useEffect, useCallback } from "react";
import { useRouter } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";
import {
  useCreateStore,
  useMyStores,
  useSetSelectedStore,
  useDeleteStore,
  useStore,
  storeKeys,
} from "@/hooks/queries/use-stores";
import { storeApi } from "@/lib/api/client";
import { SyncStatusDisplay } from "@/components/stores/sync-status-display";
import { Store as StoreType, isSyncInProgress } from "@/types/store";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Store,
  Plus,
  CheckCircle2,
  XCircle,
  Loader2,
  Trash2,
  ExternalLink,
  Eye,
  EyeOff,
} from "lucide-react";
import { cn } from "@/lib/utils";

type Marketplace = "trendyol" | "hepsiburada";

interface TestResult {
  connected: boolean;
  message: string;
  storeName?: string;
}

export default function NewStorePage() {
  const router = useRouter();
  const [storeName, setStoreName] = useState("");
  const [marketplace, setMarketplace] = useState<Marketplace>("trendyol");
  const [sellerId, setSellerId] = useState("");
  const [apiKey, setApiKey] = useState("");
  const [apiSecret, setApiSecret] = useState("");
  const [showApiKey, setShowApiKey] = useState(false);
  const [showApiSecret, setShowApiSecret] = useState(false);
  const [testResult, setTestResult] = useState<TestResult | null>(null);
  const [isTesting, setIsTesting] = useState(false);
  const [createdStoreId, setCreatedStoreId] = useState<string | null>(null);

  const queryClient = useQueryClient();
  const { data: myStores, isLoading: storesLoading } = useMyStores();
  const { data: createdStore } = useStore(createdStoreId || "");
  const createStoreMutation = useCreateStore();
  const setSelectedStoreMutation = useSetSelectedStore();
  const deleteStoreMutation = useDeleteStore();

  // Poll for store updates while sync is in progress
  useEffect(() => {
    if (!createdStoreId || !createdStore) return;

    const isInProgress = isSyncInProgress(createdStore.syncStatus);

    if (isInProgress) {
      const interval = setInterval(() => {
        queryClient.invalidateQueries({ queryKey: storeKeys.detail(createdStoreId) });
      }, 3000); // Poll every 3 seconds

      return () => clearInterval(interval);
    }
  }, [createdStoreId, createdStore, queryClient]);

  // Handle sync completion
  const handleSyncComplete = useCallback(() => {
    // Small delay to show completion message
    setTimeout(() => {
      router.push("/dashboard");
    }, 1500);
  }, [router]);

  const handleTestConnection = async () => {
    if (!sellerId || !apiKey || !apiSecret) {
      setTestResult({
        connected: false,
        message: "Lütfen tüm kimlik bilgilerini doldurun",
      });
      return;
    }

    setIsTesting(true);
    setTestResult(null);

    try {
      const result = await storeApi.testCredentials({
        sellerId,
        apiKey,
        apiSecret,
      });
      setTestResult({
        connected: result.connected,
        message: result.message || (result.connected ? "Bağlantı başarılı!" : "Bağlantı başarısız"),
        storeName: result.storeName,
      });

      // Auto-fill store name if empty and connection successful
      if (result.connected && result.storeName && !storeName) {
        setStoreName(result.storeName);
      }
    } catch (error: any) {
      setTestResult({
        connected: false,
        message: error.message || "Bağlantı testi başarısız oldu",
      });
    } finally {
      setIsTesting(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!storeName || !sellerId || !apiKey || !apiSecret) {
      return;
    }

    createStoreMutation.mutate(
      {
        storeName,
        marketplace,
        credentials: {
          sellerId,
          apiKey,
          apiSecret,
        },
      },
      {
        onSuccess: (data) => {
          // Set as selected store - backend automatically starts sync via StoreOnboardingService
          if (data?.id) {
            setSelectedStoreMutation.mutate(data.id, {
              onSuccess: () => {
                // Store the created store ID to trigger sync status display
                setCreatedStoreId(data.id);
              },
            });
          } else {
            router.push("/dashboard");
          }
        },
      }
    );
  };

  const handleSelectStore = (storeId: string) => {
    setSelectedStoreMutation.mutate(storeId, {
      onSuccess: () => {
        router.push("/dashboard");
      },
    });
  };

  const handleDeleteStore = (storeId: string, e: React.MouseEvent) => {
    e.stopPropagation();
    if (confirm("Bu mağazayı silmek istediğinizden emin misiniz? Bu işlem geri alınamaz.")) {
      deleteStoreMutation.mutate(storeId);
    }
  };

  const isFormValid = storeName && sellerId && apiKey && apiSecret;

  // Show sync overlay when a store is being synced
  if (createdStoreId && createdStore) {
    return (
      <div className="fixed inset-0 bg-background/90 z-50 flex items-center justify-center">
        <div className="max-w-md mx-auto p-8 w-full">
          <Card>
            <CardHeader className="text-center">
              <CardTitle>Mağaza Senkronizasyonu</CardTitle>
              <CardDescription>
                {createdStore.storeName} mağazanız için veriler senkronize ediliyor
              </CardDescription>
            </CardHeader>
            <CardContent>
              <SyncStatusDisplay
                store={createdStore as StoreType}
                onComplete={handleSyncComplete}
              />
              <p className="text-sm text-muted-foreground text-center mt-4">
                Bu işlem birkaç dakika sürebilir. Lütfen bekleyin...
              </p>
            </CardContent>
          </Card>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-8 max-w-6xl mx-auto px-4">
      {/* Header */}
      <div>
        <p className="text-sm text-muted-foreground">
          Satışlarınızı ve kârlarınızı takip etmek için pazaryeri mağazalarınızı bağlayın
        </p>
      </div>

      <div className="grid gap-8 lg:grid-cols-2">
        {/* Add New Store Form */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Plus className="h-5 w-5" />
              Yeni Mağaza Ekle
            </CardTitle>
            <CardDescription>
              API kimlik bilgilerini kullanarak Trendyol veya Hepsiburada mağazanızı bağlayın
            </CardDescription>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit} className="space-y-6">
              {/* Store Name */}
              <div className="space-y-2">
                <Label htmlFor="storeName">Mağaza Adı</Label>
                <Input
                  id="storeName"
                  placeholder="Trendyol Mağazam"
                  value={storeName}
                  onChange={(e) => setStoreName(e.target.value)}
                />
                <p className="text-xs text-muted-foreground">
                  Mağazanızı tanımlamak için bir isim
                </p>
              </div>

              {/* Marketplace Selection */}
              <div className="space-y-2">
                <Label>Pazaryeri</Label>
                <Select
                  value={marketplace}
                  onValueChange={(v) => setMarketplace(v as Marketplace)}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="trendyol">
                      <div className="flex items-center gap-2">
                        <div className="h-5 w-5 rounded bg-[#F27A1A] flex items-center justify-center text-[10px] font-bold text-white">
                          T
                        </div>
                        Trendyol
                      </div>
                    </SelectItem>
                    <SelectItem value="hepsiburada">
                      <div className="flex items-center gap-2">
                        <div className="h-5 w-5 rounded bg-[#FF6000] flex items-center justify-center text-[10px] font-bold text-white">
                          H
                        </div>
                        Hepsiburada
                      </div>
                    </SelectItem>
                  </SelectContent>
                </Select>
              </div>

              {/* API Credentials */}
              <div className="space-y-4 pt-4 border-t">
                <div className="flex items-center justify-between">
                  <Label className="text-base font-medium">API Bilgileri</Label>
                  <a
                    href={
                      marketplace === "trendyol"
                        ? "https://partner.trendyol.com/account/api-integration"
                        : "https://merchant.hepsiburada.com"
                    }
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-xs text-blue-600 hover:underline flex items-center gap-1"
                  >
                    API anahtarlarını al
                    <ExternalLink className="h-3 w-3" />
                  </a>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="sellerId">Seller ID</Label>
                  <Input
                    id="sellerId"
                    placeholder="123456"
                    value={sellerId}
                    onChange={(e) => setSellerId(e.target.value)}
                  />
                </div>

                <div className="space-y-2">
                  <Label htmlFor="apiKey">API Anahtarı</Label>
                  <div className="relative">
                    <Input
                      id="apiKey"
                      type={showApiKey ? "text" : "password"}
                      placeholder="API Anahtarınız"
                      value={apiKey}
                      onChange={(e) => setApiKey(e.target.value)}
                      className="pr-10"
                    />
                    <button
                      type="button"
                      onClick={() => setShowApiKey(!showApiKey)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                    >
                      {showApiKey ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                    </button>
                  </div>
                </div>

                <div className="space-y-2">
                  <Label htmlFor="apiSecret">API Gizli Anahtarı</Label>
                  <div className="relative">
                    <Input
                      id="apiSecret"
                      type={showApiSecret ? "text" : "password"}
                      placeholder="API Gizli Anahtarınız"
                      value={apiSecret}
                      onChange={(e) => setApiSecret(e.target.value)}
                      className="pr-10"
                    />
                    <button
                      type="button"
                      onClick={() => setShowApiSecret(!showApiSecret)}
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                    >
                      {showApiSecret ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                    </button>
                  </div>
                </div>
              </div>

              {/* Test Connection Result */}
              {testResult && (
                <div
                  className={cn(
                    "p-4 rounded-lg flex items-start gap-3",
                    testResult.connected
                      ? "bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800"
                      : "bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800"
                  )}
                >
                  {testResult.connected ? (
                    <CheckCircle2 className="h-5 w-5 text-green-600 dark:text-green-400 flex-shrink-0 mt-0.5" />
                  ) : (
                    <XCircle className="h-5 w-5 text-red-600 dark:text-red-400 flex-shrink-0 mt-0.5" />
                  )}
                  <div>
                    <p
                      className={cn(
                        "font-medium text-sm",
                        testResult.connected ? "text-green-800 dark:text-green-200" : "text-red-800 dark:text-red-200"
                      )}
                    >
                      {testResult.connected ? "Bağlantı Başarılı" : "Bağlantı Başarısız"}
                    </p>
                    <p
                      className={cn(
                        "text-sm mt-0.5",
                        testResult.connected ? "text-green-700 dark:text-green-300" : "text-red-700 dark:text-red-300"
                      )}
                    >
                      {testResult.message}
                    </p>
                    {testResult.storeName && (
                      <p className="text-sm text-green-600 dark:text-green-400 mt-1">
                        Mağaza: {testResult.storeName}
                      </p>
                    )}
                  </div>
                </div>
              )}

              {/* Action Buttons */}
              <div className="flex gap-3 pt-2">
                <Button
                  type="button"
                  variant="outline"
                  onClick={handleTestConnection}
                  disabled={isTesting || !sellerId || !apiKey || !apiSecret}
                  className="flex-1"
                >
                  {isTesting ? (
                    <>
                      <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                      Test Ediliyor...
                    </>
                  ) : (
                    "Bağlantıyı Test Et"
                  )}
                </Button>
                <Button
                  type="submit"
                  disabled={!isFormValid || createStoreMutation.isPending || setSelectedStoreMutation.isPending}
                  className="flex-1 bg-[#F27A1A] hover:bg-[#E06A0A]"
                >
                  {createStoreMutation.isPending || setSelectedStoreMutation.isPending ? (
                    <>
                      <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                      Mağaza Ekleniyor...
                    </>
                  ) : (
                    <>
                      <Plus className="h-4 w-4 mr-2" />
                      Mağaza Ekle ve Senkronize Et
                    </>
                  )}
                </Button>
              </div>

              {createStoreMutation.isError && (
                <p className="text-sm text-red-600">
                  Hata: {createStoreMutation.error.message}
                </p>
              )}
            </form>
          </CardContent>
        </Card>

        {/* Existing Stores */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Store className="h-5 w-5" />
              Mağazalarınız
            </CardTitle>
            <CardDescription>
              {myStores?.length
                ? `${myStores.length} bağlı mağazanız var`
                : "Henüz bağlı mağaza yok"}
            </CardDescription>
          </CardHeader>
          <CardContent>
            {storesLoading ? (
              <div className="flex items-center justify-center py-8">
                <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
              </div>
            ) : myStores && myStores.length > 0 ? (
              <div className="space-y-3">
                {myStores.map((store: any) => (
                  <div
                    key={store.id}
                    onClick={() => handleSelectStore(store.id)}
                    className="group flex items-center justify-between p-4 rounded-lg border border-border hover:border-[#F27A1A] hover:bg-orange-50/50 dark:hover:bg-orange-900/10 cursor-pointer transition-all"
                  >
                    <div className="flex items-center gap-3">
                      <div
                        className={cn(
                          "h-10 w-10 rounded-lg flex items-center justify-center text-sm font-bold text-white",
                          store.marketplace === "trendyol"
                            ? "bg-[#F27A1A]"
                            : "bg-[#FF6000]"
                        )}
                      >
                        {store.marketplace === "trendyol" ? "T" : "H"}
                      </div>
                      <div>
                        <p className="font-medium text-foreground">
                          {store.storeName || store.store_name}
                        </p>
                        <p className="text-sm text-muted-foreground capitalize">
                          {store.marketplace}
                        </p>
                      </div>
                    </div>
                    <div className="flex items-center gap-2">
                      <Button
                        variant="ghost"
                        size="sm"
                        className="opacity-0 group-hover:opacity-100 text-red-600 hover:text-red-700 hover:bg-red-50"
                        onClick={(e) => handleDeleteStore(store.id, e)}
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                      <Button
                        variant="ghost"
                        size="sm"
                        className="text-[#F27A1A]"
                      >
                        Seç
                      </Button>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="text-center py-8">
                <Store className="h-12 w-12 mx-auto text-muted-foreground opacity-50 mb-3" />
                <p className="text-muted-foreground mb-1">Henüz mağaza yok</p>
                <p className="text-sm text-muted-foreground/70">
                  Formu kullanarak ilk mağazanızı ekleyin
                </p>
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Help Section */}
      <Card className="bg-blue-50 dark:bg-blue-900/20 border-blue-200 dark:border-blue-800">
        <CardContent className="pt-6">
          <div className="flex gap-4">
            <div className="flex-shrink-0">
              <div className="h-10 w-10 rounded-full bg-blue-100 dark:bg-blue-900/30 flex items-center justify-center">
                <ExternalLink className="h-5 w-5 text-blue-600 dark:text-blue-400" />
              </div>
            </div>
            <div>
              <h3 className="font-medium text-blue-900 dark:text-blue-100">
                API bilgilerini nereden alabilirsiniz?
              </h3>
              <p className="text-sm text-blue-700 dark:text-blue-300 mt-1">
                Trendyol için:{" "}
                <a
                  href="https://partner.trendyol.com/account/api-integration"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="underline font-medium"
                >
                  Satıcı Paneli → Entegrasyon → API Ayarları
                </a>
                {" "}sayfasına gidin. Yeni bir API kullanıcısı oluşturun veya mevcut bilgilerinizi kullanın.
              </p>
              <p className="text-sm text-blue-700 dark:text-blue-300 mt-2">
                Hepsiburada için: Satıcı Paneli → Ayarlar → API Entegrasyonu sayfasına gidin
              </p>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
