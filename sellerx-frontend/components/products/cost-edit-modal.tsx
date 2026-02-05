"use client";

import { useState } from "react";
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
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { useUpdateProductCostAndStock } from "@/hooks/queries/use-products";
import type { TrendyolProduct, CostAndStockInfo } from "@/types/product";
import { AlertTriangle, Loader2, Package, TrendingUp } from "lucide-react";
import { CostHistoryTimeline } from "./cost-history-timeline";
import { useCurrency } from "@/lib/contexts/currency-context";

const formSchema = z.object({
  quantity: z.coerce.number().min(1, "Miktar en az 1 olmalıdır"),
  unitCost: z.coerce.number().min(0, "Maliyet 0'dan küçük olamaz"),
  costVatRate: z.coerce.number(),
  stockDate: z.string().min(1, "Tarih gereklidir"),
});

type FormValues = z.infer<typeof formSchema>;

interface CostEditModalProps {
  product: TrendyolProduct;
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

function formatDate(dateString: string): string {
  return new Date(dateString).toLocaleDateString("tr-TR");
}

export function CostEditModal({ product, open, onOpenChange }: CostEditModalProps) {
  const { formatCurrency } = useCurrency();
  const [error, setError] = useState<string | null>(null);
  const { mutate, isPending } = useUpdateProductCostAndStock();

  const today = new Date().toISOString().split("T")[0];

  const form = useForm<FormValues>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      quantity: 1,
      unitCost: 0,
      costVatRate: 20,
      stockDate: today,
    },
  });

  const onSubmit = (values: FormValues) => {
    setError(null);
    mutate(
      {
        productId: product.id,
        data: {
          quantity: values.quantity,
          unitCost: values.unitCost,
          costVatRate: values.costVatRate,
          stockDate: values.stockDate,
        },
      },
      {
        onSuccess: () => {
          form.reset({
            quantity: 1,
            unitCost: 0,
            costVatRate: 20,
            stockDate: today,
          });
          onOpenChange(false);
        },
        onError: (err: Error) => {
          setError(err.message || "Maliyet güncellenirken bir hata oluştu");
        },
      }
    );
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
          />
        </div>

        {/* Add New Cost Form */}
        <div className="space-y-4 pt-4 border-t">
          <h4 className="font-medium text-sm">Yeni Maliyet Ekle</h4>

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

                <FormField
                  control={form.control}
                  name="costVatRate"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>KDV Oranı</FormLabel>
                      <Select
                        onValueChange={field.onChange}
                        defaultValue={String(field.value)}
                      >
                        <FormControl>
                          <SelectTrigger>
                            <SelectValue placeholder="KDV seçin" />
                          </SelectTrigger>
                        </FormControl>
                        <SelectContent>
                          <SelectItem value="0">%0</SelectItem>
                          <SelectItem value="1">%1</SelectItem>
                          <SelectItem value="10">%10</SelectItem>
                          <SelectItem value="20">%20</SelectItem>
                        </SelectContent>
                      </Select>
                      <FormMessage />
                    </FormItem>
                  )}
                />

                <FormField
                  control={form.control}
                  name="stockDate"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel>Stok Tarihi</FormLabel>
                      <FormControl>
                        <Input
                          type="date"
                          max={today}
                          {...field}
                        />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>

              {/* Cost Preview */}
              {form.watch("quantity") > 0 && form.watch("unitCost") > 0 && (
                <div className="p-3 bg-blue-50 dark:bg-blue-900/20 rounded-lg">
                  <p className="text-sm text-blue-800 dark:text-blue-200">
                    <span className="font-medium">Toplam Maliyet:</span>{" "}
                    {formatCurrency(form.watch("quantity") * form.watch("unitCost"))}
                    {" "}({form.watch("quantity")} adet × {formatCurrency(form.watch("unitCost"))})
                  </p>
                </div>
              )}

              <div className="flex justify-end gap-2 pt-2">
                <Button
                  type="button"
                  variant="outline"
                  onClick={() => onOpenChange(false)}
                  disabled={isPending}
                >
                  İptal
                </Button>
                <Button type="submit" disabled={isPending}>
                  {isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                  {isPending ? "Kaydediliyor..." : "Kaydet"}
                </Button>
              </div>
            </form>
          </Form>
        </div>
      </DialogContent>
    </Dialog>
  );
}
