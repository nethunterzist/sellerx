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
import { ScrollArea } from "@/components/ui/scroll-area";
import { Loader2, Search, X, Package, Check } from "lucide-react";
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

const ALERT_TYPES: AlertType[] = ["STOCK", "PROFIT", "PRICE", "ORDER", "RETURN"];
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
  const [selectedProducts, setSelectedProducts] = useState<SelectedProduct[]>([]);
  const [categoryName, setCategoryName] = useState("");
  const [emailEnabled, setEmailEnabled] = useState(true);
  const [inAppEnabled, setInAppEnabled] = useState(true);

  // Product search state
  const [productSearchQuery, setProductSearchQuery] = useState("");

  const [error, setError] = useState<string | null>(null);

  // Fetch products for selected store
  const { data: productsData, isLoading: productsLoading } = useProductsByStore(storeId);
  const products = Array.isArray(productsData) ? productsData : (productsData as any)?.products ?? [];

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

      // Handle selected products from comma-separated barcodes
      if (rule.productBarcode) {
        const barcodes = rule.productBarcode.split(",").map(b => b.trim());
        const selected: SelectedProduct[] = barcodes.map(barcode => {
          const product = products.find((p: any) => p.barcode === barcode);
          return product
            ? { barcode: product.barcode, title: product.title, image: product.image }
            : { barcode, title: barcode };
        });
        setSelectedProducts(selected);
      } else {
        setSelectedProducts([]);
      }
    } else {
      // Reset form for new rule
      setName("");
      setAlertType("STOCK");
      setConditionType("BELOW");
      setThreshold("");
      setStoreId("");
      setSelectedProducts([]);
      setCategoryName("");
      setEmailEnabled(true);
      setInAppEnabled(true);
    }
    setError(null);
    setProductSearchQuery("");
  }, [rule, open]);

  // Update selected products titles when products load (for editing)
  useEffect(() => {
    if (rule?.productBarcode && products.length > 0 && selectedProducts.length > 0) {
      const updated = selectedProducts.map(sp => {
        if (sp.title === sp.barcode) {
          const product = products.find((p: any) => p.barcode === sp.barcode);
          if (product) {
            return { barcode: product.barcode, title: product.title, image: product.image };
          }
        }
        return sp;
      });
      const hasChanges = updated.some((u, i) => u.title !== selectedProducts[i].title);
      if (hasChanges) {
        setSelectedProducts(updated);
      }
    }
  }, [products, rule?.productBarcode]);

  // Clear selected products when store changes
  useEffect(() => {
    if (!isEditing) {
      setSelectedProducts([]);
    }
  }, [storeId, isEditing]);

  const requiresThreshold = CONDITIONS_WITH_THRESHOLD.includes(conditionType);

  const isProductSelected = (barcode: string) =>
    selectedProducts.some(sp => sp.barcode === barcode);

  const handleProductToggle = (product: any) => {
    if (isProductSelected(product.barcode)) {
      setSelectedProducts(prev => prev.filter(sp => sp.barcode !== product.barcode));
    } else {
      setSelectedProducts(prev => [...prev, {
        barcode: product.barcode,
        title: product.title,
        image: product.image,
      }]);
    }
  };

  const handleRemoveProduct = (barcode: string) => {
    setSelectedProducts(prev => prev.filter(sp => sp.barcode !== barcode));
  };

  const handleClearAll = () => {
    setSelectedProducts([]);
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

    // Build comma-separated barcodes
    const productBarcode = selectedProducts.length > 0
      ? selectedProducts.map(sp => sp.barcode).join(",")
      : undefined;

    try {
      if (isEditing && rule) {
        const updateData: UpdateAlertRuleRequest = {
          name: name.trim(),
          alertType,
          conditionType,
          threshold: requiresThreshold ? parseFloat(threshold) : undefined,
          storeId: storeId || undefined,
          productBarcode,
          categoryName: categoryName.trim() || undefined,
          emailEnabled,
          inAppEnabled,
          cooldownMinutes: 60,
        };
        await updateMutation.mutateAsync({ id: rule.id, data: updateData });
      } else {
        const createData: CreateAlertRuleRequest = {
          name: name.trim(),
          alertType,
          conditionType,
          threshold: requiresThreshold ? parseFloat(threshold) : undefined,
          storeId: storeId || undefined,
          productBarcode,
          categoryName: categoryName.trim() || undefined,
          emailEnabled,
          inAppEnabled,
          cooldownMinutes: 60,
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
      <DialogContent className="sm:max-w-[650px] max-h-[90vh] overflow-y-auto overflow-x-hidden">
        <form onSubmit={handleSubmit}>
          <DialogHeader>
            <DialogTitle>{isEditing ? "Kuralı Düzenle" : "Yeni Uyarı Kuralı"}</DialogTitle>
            <DialogDescription>
              {isEditing
                ? "Uyarı kuralının ayarlarını güncelleyin."
                : "Stok, kar veya sipariş değişikliklerinde bildirim almak için bir kural tanımlayın."}
            </DialogDescription>
          </DialogHeader>

          <div className="grid gap-4 py-4 overflow-hidden">
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

            {/* Threshold */}
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
                {alertType === "RETURN" && "Adet (Son 7 günde)"}
              </p>
            </div>

            {/* Store - Full Width */}
            <div className="grid gap-2">
              <Label htmlFor="storeId">Mağaza</Label>
              <Select value={storeId || "all"} onValueChange={(v) => setStoreId(v === "all" ? "" : v)}>
                <SelectTrigger className="w-full">
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

            {/* Product - Full Width Inline Multi-Select */}
            <div className="grid gap-2">
              <div className="flex items-center justify-between">
                <Label>Ürün {selectedProducts.length > 0 && `(${selectedProducts.length} seçili)`}</Label>
                {selectedProducts.length > 0 && (
                  <button
                    type="button"
                    onClick={handleClearAll}
                    className="text-xs text-muted-foreground hover:text-foreground"
                  >
                    Tümünü kaldır
                  </button>
                )}
              </div>
              {!storeId ? (
                <p className="text-sm text-muted-foreground py-3 text-center border rounded-md bg-muted/30">
                  Önce mağaza seçin
                </p>
              ) : (
                <div className="border rounded-md overflow-hidden">
                  {/* Search + Selected Chips */}
                  <div className="p-2 border-b">
                    <div className="flex items-center gap-2 flex-wrap">
                      <Search className="h-4 w-4 text-muted-foreground shrink-0" />
                      {selectedProducts.map(sp => (
                        <span
                          key={sp.barcode}
                          className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-primary/10 text-primary text-xs max-w-[200px]"
                        >
                          <span className="truncate">{sp.title}</span>
                          <button
                            type="button"
                            onClick={() => handleRemoveProduct(sp.barcode)}
                            className="shrink-0 hover:text-primary/70"
                          >
                            <X className="h-3 w-3" />
                          </button>
                        </span>
                      ))}
                      <Input
                        placeholder={selectedProducts.length === 0 ? "Ürün ara..." : ""}
                        value={productSearchQuery}
                        onChange={(e) => setProductSearchQuery(e.target.value)}
                        className="flex-1 min-w-[100px] border-0 shadow-none focus-visible:ring-0 h-8 px-1"
                      />
                    </div>
                  </div>
                  <ScrollArea className="h-[200px]">
                    <div className="p-1">
                      {/* All products option */}
                      <button
                        type="button"
                        onClick={handleClearAll}
                        className={`w-full flex items-center gap-3 px-2 py-2 rounded-md text-sm transition-colors ${
                          selectedProducts.length === 0
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
                        <div className="py-4 text-center text-sm text-muted-foreground">
                          {productSearchQuery ? "Ürün bulunamadı" : "Ürün yok"}
                        </div>
                      ) : (
                        <div className="mt-1 border-t pt-1">
                          {filteredProducts.slice(0, 50).map((product: any) => {
                            const selected = isProductSelected(product.barcode);
                            return (
                              <button
                                type="button"
                                key={product.barcode}
                                onClick={() => handleProductToggle(product)}
                                className={`w-full flex items-center gap-3 px-2 py-2 rounded-md text-sm transition-colors ${
                                  selected
                                    ? "bg-primary/10 text-primary"
                                    : "hover:bg-muted"
                                }`}
                              >
                                {/* Checkbox indicator */}
                                <div className={`h-4 w-4 rounded border shrink-0 flex items-center justify-center ${
                                  selected
                                    ? "bg-primary border-primary text-primary-foreground"
                                    : "border-input"
                                }`}>
                                  {selected && <Check className="h-3 w-3" />}
                                </div>
                                {product.image ? (
                                  <img
                                    src={product.image}
                                    alt=""
                                    className="h-8 w-8 rounded object-cover shrink-0"
                                  />
                                ) : (
                                  <div className="h-8 w-8 rounded bg-muted flex items-center justify-center shrink-0">
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
                            );
                          })}
                          {filteredProducts.length > 50 && (
                            <p className="text-xs text-center text-muted-foreground py-2">
                              +{filteredProducts.length - 50} daha fazla ürün
                            </p>
                          )}
                        </div>
                      )}
                    </div>
                  </ScrollArea>
                </div>
              )}
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
