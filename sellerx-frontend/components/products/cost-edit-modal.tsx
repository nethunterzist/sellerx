"use client";

import { useState, useEffect, useMemo } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import * as z from "zod";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import {
  useUpdateProductCostAndStock,
  useUpdateStockInfoByDate,
  useDeleteStockInfoByDate,
} from "@/hooks/queries/use-products";
import type { TrendyolProduct, CostAndStockInfo } from "@/types/product";
import { AlertTriangle, Loader2, Package, TrendingUp, X, Settings2, Globe, Percent } from "lucide-react";
import {
  Collapsible,
  CollapsibleContent,
  CollapsibleTrigger,
} from "@/components/ui/collapsible";
import { CostHistoryTimeline } from "./cost-history-timeline";
import { useCurrency } from "@/lib/contexts/currency-context";
import { toast } from "sonner";

const formSchema = z.object({
  quantity: z.coerce.number().min(1, "Miktar en az 1 olmalıdır"),
  unitCost: z.coerce.number().min(0, "Maliyet 0'dan küçük olamaz"),
  costVatRate: z.coerce.number().min(0, "KDV 0'dan küçük olamaz").max(100, "KDV 100'den büyük olamaz"),
  stockDate: z.string().min(1, "Tarih gereklidir"),
  // Döviz kuru desteği
  currency: z.enum(["TRY", "USD", "EUR"]).optional().nullable(),
  exchangeRate: z.coerce.number().min(0).optional().nullable(),
  foreignCost: z.coerce.number().min(0).optional().nullable(),
  // ÖTV desteği
  otvRate: z.coerce.number().min(0).max(1).optional().nullable(),
});

type FormValues = z.infer<typeof formSchema>;

// Common VAT rates
const COMMON_VAT_RATES = [0, 1, 10, 20];

// Common ÖTV rates
const COMMON_OTV_RATES = [0, 0.10, 0.20, 0.50];

// Supported foreign currencies (TRY excluded - this is for imports only)
const CURRENCIES = [
  { value: "USD", label: "USD", symbol: "$" },
  { value: "EUR", label: "EUR", symbol: "€" },
] as const;

interface CostEditModalProps {
  product: TrendyolProduct;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function CostEditModal({ product, open, onOpenChange }: CostEditModalProps) {
  const { formatCurrency } = useCurrency();
  const [error, setError] = useState<string | null>(null);
  const [editingEntry, setEditingEntry] = useState<CostAndStockInfo | null>(null);
  const [deletingEntry, setDeletingEntry] = useState<CostAndStockInfo | null>(null);
  const [isCustomVat, setIsCustomVat] = useState(false);
  const [isCustomOtv, setIsCustomOtv] = useState(false);
  const [showAdvanced, setShowAdvanced] = useState(false);

  const { mutate: addCost, isPending: isAdding } = useUpdateProductCostAndStock();
  const { mutate: updateCost, isPending: isUpdating } = useUpdateStockInfoByDate();
  const { mutate: deleteCost, isPending: isDeleting } = useDeleteStockInfoByDate();

  const isPending = isAdding || isUpdating || isDeleting;

  // Memoize today to prevent unnecessary re-renders
  const today = useMemo(() => new Date().toISOString().split("T")[0], []);

  const form = useForm<FormValues>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      quantity: 1,
      unitCost: 0,
      costVatRate: 20,
      stockDate: today,
      currency: "TRY",
      exchangeRate: null,
      foreignCost: null,
      otvRate: null,
    },
  });

  // Watch values for hybrid inputs and calculations
  const currentVatRate = form.watch("costVatRate");
  const currentOtvRate = form.watch("otvRate");
  const currentCurrency = form.watch("currency");
  const currentExchangeRate = form.watch("exchangeRate");
  const currentForeignCost = form.watch("foreignCost");

  // Reset form when modal opens/closes
  useEffect(() => {
    if (open && !editingEntry) {
      form.reset({
        quantity: 1,
        unitCost: 0,
        costVatRate: 20,
        stockDate: today,
        currency: "TRY",
        exchangeRate: null,
        foreignCost: null,
        otvRate: null,
      });
      setIsCustomVat(false);
      setIsCustomOtv(false);
      setShowAdvanced(false);
      setError(null);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, editingEntry]);

  // Update isCustomVat based on whether value is in common rates
  useEffect(() => {
    if (!COMMON_VAT_RATES.includes(currentVatRate)) {
      setIsCustomVat(true);
    }
  }, [currentVatRate]);

  // Update isCustomOtv based on whether value is in common rates
  useEffect(() => {
    if (currentOtvRate !== null && currentOtvRate !== undefined && !COMMON_OTV_RATES.includes(currentOtvRate)) {
      setIsCustomOtv(true);
    }
  }, [currentOtvRate]);

  // Auto-calculate unitCost from foreignCost * exchangeRate
  useEffect(() => {
    if (currentCurrency && currentCurrency !== "TRY" && currentForeignCost && currentExchangeRate) {
      const calculatedCost = currentForeignCost * currentExchangeRate;
      form.setValue("unitCost", Math.round(calculatedCost * 100) / 100);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [currentForeignCost, currentExchangeRate, currentCurrency]);

  const handleVatSelect = (rate: number | "custom") => {
    if (rate === "custom") {
      setIsCustomVat(true);
    } else {
      setIsCustomVat(false);
      form.setValue("costVatRate", rate);
    }
  };

  const handleOtvSelect = (rate: number | "custom" | null) => {
    if (rate === "custom") {
      setIsCustomOtv(true);
    } else if (rate === null) {
      setIsCustomOtv(false);
      form.setValue("otvRate", null);
    } else {
      setIsCustomOtv(false);
      form.setValue("otvRate", rate);
    }
  };

  const handleEdit = (entry: CostAndStockInfo) => {
    setEditingEntry(entry);
    setError(null);

    const vatRate = entry.costVatRate;
    setIsCustomVat(!COMMON_VAT_RATES.includes(vatRate));

    const otvRate = entry.otvRate ?? null;
    setIsCustomOtv(otvRate !== null && !COMMON_OTV_RATES.includes(otvRate));

    // Show advanced section if any advanced fields have values
    const hasAdvancedValues = entry.currency || entry.otvRate;
    setShowAdvanced(!!hasAdvancedValues);

    form.reset({
      quantity: entry.quantity,
      unitCost: entry.unitCost,
      costVatRate: vatRate,
      stockDate: entry.stockDate,
      currency: (entry.currency as "TRY" | "USD" | "EUR") ?? "TRY",
      exchangeRate: entry.exchangeRate ?? null,
      foreignCost: entry.foreignCost ?? null,
      otvRate: otvRate,
    });
  };

  const handleDelete = (entry: CostAndStockInfo) => {
    setDeletingEntry(entry);
  };

  const confirmDelete = () => {
    if (!deletingEntry) return;

    setError(null);
    deleteCost(
      {
        productId: product.id,
        stockDate: deletingEntry.stockDate,
      },
      {
        onSuccess: () => {
          toast.success("Maliyet kaydı silindi");
          setDeletingEntry(null);
        },
        onError: (err: Error) => {
          toast.error(err.message || "Maliyet silinirken bir hata oluştu");
          setError(err.message || "Maliyet silinirken bir hata oluştu");
          setDeletingEntry(null);
        },
      }
    );
  };

  const cancelDelete = () => {
    setDeletingEntry(null);
  };

  const handleCancelEdit = () => {
    setEditingEntry(null);
    setIsCustomVat(false);
    setIsCustomOtv(false);
    setShowAdvanced(false);
    form.reset({
      quantity: 1,
      unitCost: 0,
      costVatRate: 20,
      stockDate: today,
      currency: "TRY",
      exchangeRate: null,
      foreignCost: null,
      otvRate: null,
    });
  };

  const onSubmit = (values: FormValues) => {
    setError(null);

    // Prepare common data with all fields
    const commonData = {
      quantity: values.quantity,
      unitCost: values.unitCost,
      costVatRate: values.costVatRate,
      // Döviz kuru
      currency: values.currency === "TRY" ? null : values.currency,
      exchangeRate: values.currency !== "TRY" ? values.exchangeRate : null,
      foreignCost: values.currency !== "TRY" ? values.foreignCost : null,
      // ÖTV
      otvRate: values.otvRate,
    };

    if (editingEntry) {
      // Update existing entry
      updateCost(
        {
          productId: product.id,
          stockDate: editingEntry.stockDate,
          data: commonData,
        },
        {
          onSuccess: () => {
            toast.success("Maliyet başarıyla güncellendi");
            setEditingEntry(null);
            setIsCustomVat(false);
            setIsCustomOtv(false);
            setShowAdvanced(false);
            form.reset({
              quantity: 1,
              unitCost: 0,
              costVatRate: 20,
              stockDate: today,
              currency: "TRY",
              exchangeRate: null,
              foreignCost: null,
              otvRate: null,
            });
          },
          onError: (err: Error) => {
            toast.error(err.message || "Maliyet güncellenirken bir hata oluştu");
            setError(err.message || "Maliyet güncellenirken bir hata oluştu");
          },
        }
      );
    } else {
      // Add new entry
      addCost(
        {
          productId: product.id,
          data: {
            ...commonData,
            stockDate: values.stockDate,
          },
        },
        {
          onSuccess: () => {
            toast.success("Maliyet başarıyla eklendi");
            form.reset({
              quantity: 1,
              unitCost: 0,
              costVatRate: 20,
              stockDate: today,
              currency: "TRY",
              exchangeRate: null,
              foreignCost: null,
              otvRate: null,
            });
            setIsCustomVat(false);
            setIsCustomOtv(false);
            setShowAdvanced(false);
            onOpenChange(false);
          },
          onError: (err: Error) => {
            toast.error(err.message || "Maliyet eklenirken bir hata oluştu");
            setError(err.message || "Maliyet eklenirken bir hata oluştu");
          },
        }
      );
    }
  };

  // Get cost history from product
  const costHistory = product.costAndStockInfo || [];

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[600px] max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2">
            <Package className="h-5 w-5" />
            Maliyet & Stok Yönetimi
          </DialogTitle>
          <DialogDescription className="line-clamp-2">
            {product.title}
          </DialogDescription>
        </DialogHeader>

        {/* Product Info Summary */}
        <div className="grid grid-cols-3 gap-4 p-4 bg-muted rounded-lg">
          <div className="text-center">
            <p className="text-xs text-muted-foreground">Satış Fiyatı</p>
            <p className="font-semibold text-lg text-foreground">{formatCurrency(product.salePrice)}</p>
          </div>
          <div className="text-center">
            <p className="text-xs text-muted-foreground">Trendyol Stok</p>
            <p className="font-semibold text-lg text-foreground">{product.trendyolQuantity}</p>
          </div>
          <div className="text-center">
            <p className="text-xs text-muted-foreground">Komisyon</p>
            <p className="font-semibold text-lg text-foreground">%{product.commissionRate}</p>
          </div>
        </div>

        {/* Auto-Detected Cost Banner */}
        {product.hasAutoDetectedCost && (
          <div className="flex items-start gap-3 p-3 bg-amber-50 dark:bg-amber-900/20 border border-amber-200 dark:border-amber-800 rounded-lg">
            <AlertTriangle className="h-5 w-5 text-amber-600 dark:text-amber-400 shrink-0 mt-0.5" />
            <div>
              <p className="text-sm font-medium text-amber-800 dark:text-amber-200">
                Otomatik Algilanan Maliyet
              </p>
              <p className="text-xs text-amber-700 dark:text-amber-300 mt-1">
                Bu urun icin stok artisi algilandi ve son bilinen maliyet ile otomatik kayit olusturuldu.
                Lutfen maliyet ve KDV oranini kontrol edin.
              </p>
            </div>
          </div>
        )}

        {/* Cost History Timeline */}
        <div className="space-y-3">
          <h4 className="font-medium text-sm flex items-center gap-2">
            <TrendingUp className="h-4 w-4" />
            Maliyet Geçmişi & Analiz
          </h4>
          <CostHistoryTimeline
            costHistory={costHistory}
            salePrice={product.salePrice}
            vatRate={product.vatRate}
            commissionRate={product.commissionRate}
            lastShippingCostPerUnit={product.lastShippingCostPerUnit}
            onEdit={handleEdit}
            onDelete={handleDelete}
            editingDate={editingEntry?.stockDate}
            deletingDate={deletingEntry?.stockDate}
            onConfirmDelete={confirmDelete}
            onCancelDelete={cancelDelete}
          />
        </div>

        {/* Add/Edit Cost Form */}
        <div className="space-y-4 pt-4 border-t">
          <div className="flex items-center justify-between">
            <h4 className="font-medium text-sm">
              {editingEntry ? "Maliyet Düzenle" : "Yeni Maliyet Ekle"}
            </h4>
            {editingEntry && (
              <Button
                type="button"
                variant="ghost"
                size="sm"
                onClick={handleCancelEdit}
                className="text-muted-foreground hover:text-foreground"
              >
                <X className="h-4 w-4 mr-1" />
                İptal
              </Button>
            )}
          </div>

          {editingEntry && (
            <div className="p-2 bg-blue-50 dark:bg-blue-900/20 border border-blue-200 dark:border-blue-800 rounded-lg">
              <p className="text-xs text-blue-700 dark:text-blue-300">
                <span className="font-medium">{editingEntry.stockDate}</span> tarihli kaydı düzenliyorsunuz
              </p>
            </div>
          )}

          {error && (
            <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 rounded-lg p-3">
              <p className="text-red-800 dark:text-red-200 text-sm">{error}</p>
            </div>
          )}

          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <FormField
                  control={form.control}
                  name="quantity"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Adet</FormLabel>
                      <FormControl>
                        <Input
                          type="number"
                          min="1"
                          placeholder="Adet girin"
                          {...field}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="unitCost"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Birim Maliyet (TL)</FormLabel>
                      <FormControl>
                        <Input
                          type="number"
                          min="0"
                          step="0.01"
                          placeholder="Maliyet girin"
                          {...field}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                {/* Hybrid VAT Rate Input */}
                <FormField
                  control={form.control}
                  name="costVatRate"
                  render={({ field }) => (
                    <FormItem className="col-span-2">
                      <FormLabel>KDV Oranı</FormLabel>
                      <div className="flex flex-wrap items-center gap-2">
                        {COMMON_VAT_RATES.map((rate) => (
                          <Button
                            key={rate}
                            type="button"
                            variant={!isCustomVat && field.value === rate ? "default" : "outline"}
                            size="sm"
                            onClick={() => handleVatSelect(rate)}
                            className="min-w-[50px]"
                          >
                            %{rate}
                          </Button>
                        ))}
                        <Button
                          type="button"
                          variant={isCustomVat ? "default" : "outline"}
                          size="sm"
                          onClick={() => handleVatSelect("custom")}
                        >
                          Özel
                        </Button>
                        {isCustomVat && (
                          <>
                            <FormControl>
                              <Input
                                type="number"
                                min="0"
                                max="100"
                                step="0.01"
                                placeholder="KDV"
                                className="w-20"
                                {...field}
                                onChange={(e) => field.onChange(parseFloat(e.target.value) || 0)}
                              />
                            </FormControl>
                            <span className="text-sm text-muted-foreground">%</span>
                          </>
                        )}
                      </div>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="stockDate"
                  render={({ field }) => (
                    <FormItem className="col-span-2">
                      <FormLabel>Stok Tarihi</FormLabel>
                      <FormControl>
                        <Input
                          type="date"
                          max={today}
                          disabled={!!editingEntry}
                          {...field}
                        />
                      </FormControl>
                      {editingEntry && (
                        <p className="text-xs text-muted-foreground">
                          Düzenleme modunda tarih değiştirilemez
                        </p>
                      )}
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>

              {/* Advanced Settings Collapsible */}
              <Collapsible open={showAdvanced} onOpenChange={setShowAdvanced}>
                <CollapsibleTrigger asChild>
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    className="w-full flex items-center justify-between text-muted-foreground hover:text-foreground"
                  >
                    <span className="flex items-center gap-2">
                      <Settings2 className="h-4 w-4" />
                      Gelişmiş Ayarlar (Döviz, ÖTV)
                    </span>
                    <span className="text-xs">
                      {showAdvanced ? "Gizle" : "Göster"}
                    </span>
                  </Button>
                </CollapsibleTrigger>
                <CollapsibleContent className="space-y-4 pt-4">
                  {/* Currency Section */}
                  <div className="p-4 border rounded-lg space-y-4">
                    <h5 className="text-sm font-medium flex items-center gap-2">
                      <Globe className="h-4 w-4" />
                      Döviz Kuru (İthalat Ürünleri)
                    </h5>

                    <FormField
                      control={form.control}
                      name="currency"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>Para Birimi</FormLabel>
                          <div className="flex items-center gap-2">
                            {CURRENCIES.map((curr) => (
                              <button
                                key={curr.value}
                                type="button"
                                onClick={() => field.onChange(field.value === curr.value ? "TRY" : curr.value)}
                                className={`px-3 py-1.5 text-sm font-medium rounded-full border transition-all ${
                                  field.value === curr.value
                                    ? "bg-primary text-primary-foreground border-primary"
                                    : "bg-background text-muted-foreground border-border hover:bg-accent hover:text-accent-foreground hover:border-accent-foreground/20"
                                }`}
                              >
                                {curr.symbol} {curr.label}
                              </button>
                            ))}
                          </div>
                          <FormMessage />
                        </FormItem>
                      )}
                    />

                    {currentCurrency && currentCurrency !== "TRY" && (
                      <div className="grid grid-cols-2 gap-4">
                        <FormField
                          control={form.control}
                          name="foreignCost"
                          render={({ field }) => (
                            <FormItem>
                              <FormLabel>Maliyet</FormLabel>
                              <FormControl>
                                <Input
                                  type="number"
                                  min="0"
                                  step="0.01"
                                  placeholder="örn: 10"
                                  value={field.value ?? ""}
                                  onChange={(e) => field.onChange(e.target.value ? parseFloat(e.target.value) : null)}
                                />
                              </FormControl>
                              <FormMessage />
                            </FormItem>
                          )}
                        />

                        <FormField
                          control={form.control}
                          name="exchangeRate"
                          render={({ field }) => (
                            <FormItem>
                              <FormLabel>Döviz Kuru (TL)</FormLabel>
                              <FormControl>
                                <Input
                                  type="number"
                                  min="0"
                                  step="0.0001"
                                  placeholder="örn: 44.50"
                                  value={field.value ?? ""}
                                  onChange={(e) => field.onChange(e.target.value ? parseFloat(e.target.value) : null)}
                                />
                              </FormControl>
                              <FormMessage />
                            </FormItem>
                          )}
                        />
                      </div>
                    )}

                    {currentCurrency && currentCurrency !== "TRY" && currentForeignCost && currentExchangeRate && (
                      <div className="p-2 bg-green-50 dark:bg-green-900/20 rounded text-sm text-green-700 dark:text-green-300">
                        <strong>Hesaplanan TL Maliyet:</strong>{" "}
                        {formatCurrency(currentForeignCost * currentExchangeRate)}
                        {" "}({currentForeignCost} {CURRENCIES.find(c => c.value === currentCurrency)?.symbol} × {currentExchangeRate} TL)
                      </div>
                    )}
                  </div>

                  {/* OTV Section */}
                  <div className="p-4 border rounded-lg space-y-4">
                    <h5 className="text-sm font-medium flex items-center gap-2">
                      <Percent className="h-4 w-4" />
                      ÖTV - Özel Tüketim Vergisi
                    </h5>
                    <p className="text-xs text-muted-foreground">
                      Kozmetik, parfüm, elektronik vb. ürünler için ÖTV oranı
                    </p>

                    <FormField
                      control={form.control}
                      name="otvRate"
                      render={({ field }) => (
                        <FormItem>
                          <div className="space-y-2">
                            <div className="flex flex-wrap gap-2">
                              <Button
                                type="button"
                                variant={field.value === null || field.value === undefined ? "default" : "outline"}
                                size="sm"
                                onClick={() => handleOtvSelect(null)}
                              >
                                Yok
                              </Button>
                              {COMMON_OTV_RATES.filter(r => r > 0).map((rate) => (
                                <Button
                                  key={rate}
                                  type="button"
                                  variant={!isCustomOtv && field.value === rate ? "default" : "outline"}
                                  size="sm"
                                  onClick={() => handleOtvSelect(rate)}
                                  className="min-w-[50px]"
                                >
                                  %{(rate * 100).toFixed(0)}
                                </Button>
                              ))}
                              <Button
                                type="button"
                                variant={isCustomOtv ? "default" : "outline"}
                                size="sm"
                                onClick={() => handleOtvSelect("custom")}
                              >
                                Özel
                              </Button>
                            </div>

                            {isCustomOtv && (
                              <div className="flex items-center gap-2">
                                <FormControl>
                                  <Input
                                    type="number"
                                    min="0"
                                    max="100"
                                    step="1"
                                    placeholder="ÖTV oranı"
                                    className="w-24"
                                    value={field.value !== null && field.value !== undefined ? (field.value * 100) : ""}
                                    onChange={(e) => field.onChange(e.target.value ? parseFloat(e.target.value) / 100 : null)}
                                  />
                                </FormControl>
                                <span className="text-sm text-muted-foreground">%</span>
                              </div>
                            )}
                          </div>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                  </div>
                </CollapsibleContent>
              </Collapsible>

              {/* Cost Preview */}
              {form.watch("quantity") > 0 && form.watch("unitCost") > 0 && (
                <div className="p-3 bg-blue-50 dark:bg-blue-900/20 rounded-lg space-y-1">
                  <p className="text-sm text-blue-800 dark:text-blue-200">
                    <span className="font-medium">Birim Maliyet (KDV Hariç):</span>{" "}
                    {formatCurrency(form.watch("unitCost"))}
                  </p>
                  {currentOtvRate && currentOtvRate > 0 && (
                    <p className="text-sm text-blue-800 dark:text-blue-200">
                      <span className="font-medium">+ ÖTV (%{(currentOtvRate * 100).toFixed(0)}):</span>{" "}
                      {formatCurrency(form.watch("unitCost") * currentOtvRate)}
                    </p>
                  )}
                  <p className="text-sm text-blue-800 dark:text-blue-200">
                    <span className="font-medium">+ KDV (%{form.watch("costVatRate")}):</span>{" "}
                    {formatCurrency(form.watch("unitCost") * (1 + (currentOtvRate || 0)) * (form.watch("costVatRate") / 100))}
                  </p>
                  <p className="text-sm text-blue-800 dark:text-blue-200 font-semibold border-t border-blue-200 dark:border-blue-700 pt-1 mt-1">
                    <span>Fatura Maliyeti (ÖTV+KDV Dahil):</span>{" "}
                    {formatCurrency(form.watch("unitCost") * (1 + (currentOtvRate || 0)) * (1 + form.watch("costVatRate") / 100))}
                    {" "}× {form.watch("quantity")} adet ={" "}
                    {formatCurrency(form.watch("quantity") * form.watch("unitCost") * (1 + (currentOtvRate || 0)) * (1 + form.watch("costVatRate") / 100))}
                  </p>
                </div>
              )}

              <div className="flex justify-end gap-2 pt-2">
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => {
                    if (editingEntry) {
                      handleCancelEdit();
                    } else {
                      onOpenChange(false);
                    }
                  }}
                  disabled={isPending}
                >
                  {editingEntry ? "Düzenlemeyi İptal Et" : "Kapat"}
                </Button>
                <Button type="submit" disabled={isPending}>
                  {isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                  {isPending
                    ? (editingEntry ? "Güncelleniyor..." : "Kaydediliyor...")
                    : (editingEntry ? "Güncelle" : "Kaydet")}
                </Button>
              </div>
            </form>
          </Form>
        </div>
      </DialogContent>
    </Dialog>
  );
}
