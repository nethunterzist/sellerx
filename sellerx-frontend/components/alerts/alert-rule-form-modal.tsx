"use client";

import { useState, useEffect } from "react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from "@/components/ui/popover";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Loader2, Search, X, Package } from "lucide-react";
import { useMyStores } from "@/hooks/queries/use-stores";
import { useProductsByStore } from "@/hooks/queries/use-products";
import { useCreateAlertRule, useUpdateAlertRule } from "@/hooks/queries/use-alerts";
import {
  AlertRule,
  AlertType,
  AlertConditionType,
  CreateAlertRuleRequest,
  UpdateAlertRuleRequest,
  ALERT_TYPE_LABELS,
  ALERT_CONDITION_LABELS,
} from "@/types/alert";

interface AlertRuleFormModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  rule?: AlertRule | null;
}

interface SelectedProduct {
  barcode: string;
  title: string;
  image?: string;
}

const ALERT_TYPES: AlertType[] = ["STOCK", "PROFIT", "PRICE", "ORDER"];
const CONDITION_TYPES: AlertConditionType[] = ["BELOW", "ABOVE", "EQUALS", "ZERO", "CHANGED"];

// Conditions that require a threshold value
const CONDITIONS_WITH_THRESHOLD: AlertConditionType[] = ["BELOW", "ABOVE", "EQUALS"];

export function AlertRuleFormModal({ open, onOpenChange, rule }: AlertRuleFormModalProps) {
  const { data: stores } = useMyStores();
  const createMutation = useCreateAlertRule();
  const updateMutation = useUpdateAlertRule();

  const isEditing = !!rule;

  // Form state
  const [name, setName] = useState("");
  const [alertType, setAlertType] = useState<AlertType>("STOCK");
  const [conditionType, setConditionType] = useState<AlertConditionType>("BELOW");
  const [threshold, setThreshold] = useState<string>("");
  const [storeId, setStoreId] = useState<string>("");
  const [selectedProduct, setSelectedProduct] = useState<SelectedProduct | null>(null);
  const [categoryName, setCategoryName] = useState("");
  const [emailEnabled, setEmailEnabled] = useState(true);
  const [inAppEnabled, setInAppEnabled] = useState(true);
  const [cooldownMinutes, setCooldownMinutes] = useState<string>("60");

  // Product search state
  const [productSearchOpen, setProductSearchOpen] = useState(false);
  const [productSearchQuery, setProductSearchQuery] = useState("");

  const [error, setError] = useState<string | null>(null);

  // Fetch products for selected store
  const { data: productsData, isLoading: productsLoading } = useProductsByStore(storeId);
  const products = productsData || [];

  // Filter products by search query
  const filteredProducts = products.filter((p: any) =>
    p.title?.toLowerCase().includes(productSearchQuery.toLowerCase()) ||
    p.barcode?.toLowerCase().includes(productSearchQuery.toLowerCase())
  );

  // Populate form when editing
  useEffect(() => {
    if (rule) {
      setName(rule.name);
      setAlertType(rule.alertType);
      setConditionType(rule.conditionType);
      setThreshold(rule.threshold?.toString() || "");
      setStoreId(rule.storeId || "");
      setCategoryName(rule.categoryName || "");
      setEmailEnabled(rule.emailEnabled);
      setInAppEnabled(rule.inAppEnabled);
      setCooldownMinutes(rule.cooldownMinutes.toString());

      // Handle selected product from barcode
      if (rule.productBarcode) {
        // Try to find product in current products list
        const product = products.find((p: any) => p.barcode === rule.productBarcode);
        if (product) {
          setSelectedProduct({
            barcode: product.barcode,
            title: product.title,
            image: product.image,
          });
        } else {
          // If product not found, just set barcode
          setSelectedProduct({
            barcode: rule.productBarcode,
            title: rule.productBarcode, // Show barcode as title
          });
        }
      } else {
        setSelectedProduct(null);
      }
    } else {
      // Reset form for new rule
      setName("");
      setAlertType("STOCK");
      setConditionType("BELOW");
      setThreshold("");
      setStoreId("");
      setSelectedProduct(null);
      setCategoryName("");
      setEmailEnabled(true);
      setInAppEnabled(true);
      setCooldownMinutes("60");
    }
    setError(null);
    setProductSearchQuery("");
  }, [rule, open]);

  // Update selected product when products load (for editing)
  useEffect(() => {
    if (rule?.productBarcode && products.length > 0 && selectedProduct?.barcode === rule.productBarcode) {
      const product = products.find((p: any) => p.barcode === rule.productBarcode);
      if (product && selectedProduct.title === rule.productBarcode) {
        setSelectedProduct({
          barcode: product.barcode,
          title: product.title,
          image: product.image,
        });
      }
    }
  }, [products, rule?.productBarcode]);

  // Clear selected product when store changes
  useEffect(() => {
    if (!isEditing) {
      setSelectedProduct(null);
    }
  }, [storeId, isEditing]);

  const requiresThreshold = CONDITIONS_WITH_THRESHOLD.includes(conditionType);

  const handleProductSelect = (product: any) => {
    setSelectedProduct({
      barcode: product.barcode,
      title: product.title,
      image: product.image,
    });
    setProductSearchOpen(false);
    setProductSearchQuery("");
  };

  const handleClearProduct = () => {
    setSelectedProduct(null);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    // Validation
    if (!name.trim()) {
      setError("Kural adı zorunludur");
      return;
    }
    if (requiresThreshold && !threshold) {
      setError("Eşik değeri zorunludur");
      return;
    }

    try {
      if (isEditing && rule) {
        const updateData: UpdateAlertRuleRequest = {
          name: name.trim(),
          alertType,
          conditionType,
          threshold: requiresThreshold ? parseFloat(threshold) : undefined,
          storeId: storeId || undefined,
          productBarcode: selectedProduct?.barcode || undefined,
          categoryName: categoryName.trim() || undefined,
          emailEnabled,
          inAppEnabled,
          cooldownMinutes: parseInt(cooldownMinutes) || 60,
        };
        await updateMutation.mutateAsync({ id: rule.id, data: updateData });
      } else {
        const createData: CreateAlertRuleRequest = {
          name: name.trim(),
          alertType,
          conditionType,
          threshold: requiresThreshold ? parseFloat(threshold) : undefined,
          storeId: storeId || undefined,
          productBarcode: selectedProduct?.barcode || undefined,
          categoryName: categoryName.trim() || undefined,
          emailEnabled,
          inAppEnabled,
          cooldownMinutes: parseInt(cooldownMinutes) || 60,
        };
        await createMutation.mutateAsync(createData);
      }
      onOpenChange(false);
    } catch (err: any) {
      setError(err.message || "Bir hata oluştu");
    }
  };

  const isPending = createMutation.isPending || updateMutation.isPending;

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[650px]">
        <form onSubmit={handleSubmit}>
          <DialogHeader>
            <DialogTitle>{isEditing ? "Kuralı Düzenle" : "Yeni Uyarı Kuralı"}</DialogTitle>
            <DialogDescription>
              {isEditing
                ? "Uyarı kuralının ayarlarını güncelleyin."
                : "Stok, kar veya sipariş değişikliklerinde bildirim almak için bir kural tanımlayın."}
            </DialogDescription>
          </DialogHeader>

          <div className="grid gap-4 py-4">
            {/* Rule Name */}
            <div className="grid gap-2">
              <Label htmlFor="name">Kural Adı *</Label>
              <Input
                id="name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="Örn: Düşük Stok Uyarısı"
              />
            </div>

            {/* Alert Type & Condition Type - Side by Side */}
            <div className="grid grid-cols-2 gap-4">
              <div className="grid gap-2">
                <Label htmlFor="alertType">Uyarı Tipi *</Label>
                <Select value={alertType} onValueChange={(v) => setAlertType(v as AlertType)}>
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {ALERT_TYPES.map((type) => (
                      <SelectItem key={type} value={type}>
                        {ALERT_TYPE_LABELS[type]}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div className="grid gap-2">
                <Label htmlFor="conditionType">Koşul *</Label>
                <Select value={conditionType} onValueChange={(v) => setConditionType(v as AlertConditionType)}>
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {CONDITION_TYPES.map((type) => (
                      <SelectItem key={type} value={type}>
                        {ALERT_CONDITION_LABELS[type]}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>

            {/* Threshold & Cooldown - Side by Side */}
            <div className="grid grid-cols-2 gap-4">
              <div className="grid gap-2">
                <Label htmlFor="threshold">Eşik Değeri {requiresThreshold && "*"}</Label>
                <Input
                  id="threshold"
                  type="number"
                  value={threshold}
                  onChange={(e) => setThreshold(e.target.value)}
                  placeholder={alertType === "PROFIT" ? "Örn: 5 (%)" : "Örn: 10"}
                  min="0"
                  disabled={!requiresThreshold}
                />
                <p className="text-xs text-muted-foreground">
                  {alertType === "PROFIT" && "Yüzde (%)"}
                  {alertType === "STOCK" && "Adet"}
                  {alertType === "PRICE" && "TL"}
                  {alertType === "ORDER" && "Adet"}
                </p>
              </div>

              <div className="grid gap-2">
                <Label htmlFor="cooldown">Cooldown (dakika)</Label>
                <Input
                  id="cooldown"
                  type="number"
                  value={cooldownMinutes}
                  onChange={(e) => setCooldownMinutes(e.target.value)}
                  min="1"
                  max="1440"
                />
                <p className="text-xs text-muted-foreground">
                  Tekrar bildirim bekleme süresi
                </p>
              </div>
            </div>

            {/* Store & Product - Side by Side */}
            <div className="grid grid-cols-2 gap-4">
              <div className="grid gap-2">
                <Label htmlFor="storeId">Mağaza</Label>
                <Select value={storeId || "all"} onValueChange={(v) => setStoreId(v === "all" ? "" : v)}>
                  <SelectTrigger>
                    <SelectValue placeholder="Tüm mağazalar" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="all">Tüm mağazalar</SelectItem>
                    {stores?.map((store) => (
                      <SelectItem key={store.id} value={store.id}>
                        {store.storeName}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              {/* Product Search Popover */}
              <div className="grid gap-2">
                <Label>Ürün</Label>
                <Popover open={productSearchOpen} onOpenChange={setProductSearchOpen}>
                  <PopoverTrigger asChild>
                    <button
                      type="button"
                      className="flex h-10 w-full items-center justify-between rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                      disabled={!storeId}
                    >
                      {selectedProduct ? (
                        <div className="flex items-center gap-2 flex-1 min-w-0">
                          <span className="truncate text-foreground">{selectedProduct.title}</span>
                          <button
                            type="button"
                            onClick={(e) => {
                              e.stopPropagation();
                              handleClearProduct();
                            }}
                            className="ml-auto shrink-0 rounded-full p-0.5 hover:bg-muted"
                          >
                            <X className="h-3.5 w-3.5 text-muted-foreground" />
                          </button>
                        </div>
                      ) : (
                        <span className="text-muted-foreground">
                          {!storeId ? "Önce mağaza seçin" : "Tüm ürünler"}
                        </span>
                      )}
                      <Search className="h-4 w-4 shrink-0 text-muted-foreground ml-2" />
                    </button>
                  </PopoverTrigger>
                  <PopoverContent className="w-[300px] p-0" align="start">
                    <div className="p-2 border-b">
                      <div className="relative">
                        <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
                        <Input
                          placeholder="Ürün ara..."
                          value={productSearchQuery}
                          onChange={(e) => setProductSearchQuery(e.target.value)}
                          className="pl-8"
                          autoFocus
                        />
                      </div>
                    </div>
                    <ScrollArea className="h-[250px]">
                      <div className="p-2">
                        {/* All products option */}
                        <button
                          type="button"
                          onClick={() => {
                            setSelectedProduct(null);
                            setProductSearchOpen(false);
                            setProductSearchQuery("");
                          }}
                          className={`w-full flex items-center gap-3 px-2 py-2 rounded-md text-sm transition-colors ${
                            !selectedProduct
                              ? "bg-primary/10 text-primary"
                              : "hover:bg-muted"
                          }`}
                        >
                          <Package className="h-4 w-4" />
                          <span>Tüm ürünler</span>
                        </button>

                        {productsLoading ? (
                          <div className="flex items-center justify-center py-6">
                            <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
                          </div>
                        ) : filteredProducts.length === 0 ? (
                          <div className="py-6 text-center text-sm text-muted-foreground">
                            {productSearchQuery ? "Ürün bulunamadı" : "Ürün yok"}
                          </div>
                        ) : (
                          <div className="mt-1 border-t pt-1">
                            {filteredProducts.slice(0, 50).map((product: any) => (
                              <button
                                type="button"
                                key={product.barcode}
                                onClick={() => handleProductSelect(product)}
                                className={`w-full flex items-center gap-3 px-2 py-2 rounded-md text-sm transition-colors ${
                                  selectedProduct?.barcode === product.barcode
                                    ? "bg-primary/10 text-primary"
                                    : "hover:bg-muted"
                                }`}
                              >
                                {product.image ? (
                                  <img
                                    src={product.image}
                                    alt=""
                                    className="h-8 w-8 rounded object-cover"
                                  />
                                ) : (
                                  <div className="h-8 w-8 rounded bg-muted flex items-center justify-center">
                                    <Package className="h-4 w-4 text-muted-foreground" />
                                  </div>
                                )}
                                <div className="flex-1 min-w-0 text-left">
                                  <p className="truncate font-medium">{product.title}</p>
                                  <p className="text-xs text-muted-foreground truncate">
                                    {product.barcode}
                                  </p>
                                </div>
                              </button>
                            ))}
                            {filteredProducts.length > 50 && (
                              <p className="text-xs text-center text-muted-foreground py-2">
                                +{filteredProducts.length - 50} daha fazla ürün
                              </p>
                            )}
                          </div>
                        )}
                      </div>
                    </ScrollArea>
                  </PopoverContent>
                </Popover>
              </div>
            </div>

            {/* Category */}
            <div className="grid gap-2">
              <Label htmlFor="categoryName">Kategori</Label>
              <Input
                id="categoryName"
                value={categoryName}
                onChange={(e) => setCategoryName(e.target.value)}
                placeholder="Belirli bir kategori için isim girin (opsiyonel)"
              />
            </div>

            {/* Notification Channels - Side by Side */}
            <div className="pt-2 border-t">
              <p className="text-sm font-medium text-foreground mb-3">Bildirim Kanalları</p>
              <div className="grid grid-cols-2 gap-4">
                <div className="flex items-center justify-between p-3 rounded-lg bg-muted/50">
                  <div>
                    <Label htmlFor="emailEnabled" className="text-sm">Email</Label>
                    <p className="text-xs text-muted-foreground">Email gönder</p>
                  </div>
                  <Switch
                    id="emailEnabled"
                    checked={emailEnabled}
                    onCheckedChange={setEmailEnabled}
                  />
                </div>

                <div className="flex items-center justify-between p-3 rounded-lg bg-muted/50">
                  <div>
                    <Label htmlFor="inAppEnabled" className="text-sm">Uygulama</Label>
                    <p className="text-xs text-muted-foreground">Bildirim merkezinde</p>
                  </div>
                  <Switch
                    id="inAppEnabled"
                    checked={inAppEnabled}
                    onCheckedChange={setInAppEnabled}
                  />
                </div>
              </div>
            </div>

            {/* Error Message */}
            {error && (
              <div className="bg-red-50 dark:bg-red-900/20 text-red-600 text-sm p-3 rounded-lg">
                {error}
              </div>
            )}
          </div>

          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
              disabled={isPending}
            >
              İptal
            </Button>
            <Button
              type="submit"
              disabled={isPending}
              className="bg-[#1D70F1] hover:bg-[#1560d1]"
            >
              {isPending ? (
                <>
                  <Loader2 className="h-4 w-4 mr-2 animate-spin" />
                  {isEditing ? "Güncelleniyor..." : "Oluşturuluyor..."}
                </>
              ) : (
                isEditing ? "Güncelle" : "Oluştur"
              )}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
