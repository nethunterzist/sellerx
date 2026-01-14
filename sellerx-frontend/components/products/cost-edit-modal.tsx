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
import { Loader2, Package, TrendingUp } from "lucide-react";

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

function formatCurrency(value: number): string {
  return new Intl.NumberFormat("tr-TR", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value);
}

function formatDate(dateString: string): string {
  return new Date(dateString).toLocaleDateString("tr-TR");
}

export function CostEditModal({ product, open, onOpenChange }: CostEditModalProps) {
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

  // Calculate totals from cost history
  const costHistory = product.costAndStockInfo || [];
  const totalQuantity = costHistory.reduce((sum, item) => sum + item.quantity, 0);
  const totalUsed = costHistory.reduce((sum, item) => sum + (item.usedQuantity || 0), 0);
  const remainingStock = totalQuantity - totalUsed;

  // Get latest cost entry
  const latestCost = costHistory.length > 0
    ? costHistory.sort((a, b) => new Date(b.stockDate).getTime() - new Date(a.stockDate).getTime())[0]
    : null;

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
        <div className="grid grid-cols-3 gap-4 p-4 bg-gray-50 rounded-lg">
          <div className="text-center">
            <p className="text-xs text-gray-500">Satış Fiyatı</p>
            <p className="font-semibold text-lg">{formatCurrency(product.salePrice)} TL</p>
          </div>
          <div className="text-center">
            <p className="text-xs text-gray-500">Trendyol Stok</p>
            <p className="font-semibold text-lg">{product.trendyolQuantity}</p>
          </div>
          <div className="text-center">
            <p className="text-xs text-gray-500">Komisyon</p>
            <p className="font-semibold text-lg">%{product.commissionRate}</p>
          </div>
        </div>

        {/* Cost History */}
        {costHistory.length > 0 && (
          <div className="space-y-3">
            <h4 className="font-medium text-sm flex items-center gap-2">
              <TrendingUp className="h-4 w-4" />
              Maliyet Geçmişi
            </h4>
            <div className="border rounded-lg overflow-hidden">
              <table className="w-full text-sm">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="text-left p-2 font-medium">Tarih</th>
                    <th className="text-right p-2 font-medium">Adet</th>
                    <th className="text-right p-2 font-medium">Kalan</th>
                    <th className="text-right p-2 font-medium">Birim Maliyet</th>
                    <th className="text-right p-2 font-medium">KDV</th>
                  </tr>
                </thead>
                <tbody>
                  {costHistory
                    .sort((a, b) => new Date(b.stockDate).getTime() - new Date(a.stockDate).getTime())
                    .map((entry, index) => (
                      <tr key={index} className="border-t">
                        <td className="p-2">{formatDate(entry.stockDate)}</td>
                        <td className="p-2 text-right">{entry.quantity}</td>
                        <td className="p-2 text-right">
                          {entry.quantity - (entry.usedQuantity || 0)}
                        </td>
                        <td className="p-2 text-right">{formatCurrency(entry.unitCost)} TL</td>
                        <td className="p-2 text-right">%{entry.costVatRate}</td>
                      </tr>
                    ))}
                </tbody>
                <tfoot className="bg-gray-50 font-medium">
                  <tr className="border-t">
                    <td className="p-2">Toplam</td>
                    <td className="p-2 text-right">{totalQuantity}</td>
                    <td className="p-2 text-right">{remainingStock}</td>
                    <td className="p-2 text-right">-</td>
                    <td className="p-2 text-right">-</td>
                  </tr>
                </tfoot>
              </table>
            </div>
          </div>
        )}

        {/* Add New Cost Form */}
        <div className="space-y-4 pt-4 border-t">
          <h4 className="font-medium text-sm">Yeni Maliyet Ekle</h4>

          {error && (
            <div className="bg-red-50 border border-red-200 rounded-lg p-3">
              <p className="text-red-800 text-sm">{error}</p>
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
                <div className="p-3 bg-blue-50 rounded-lg">
                  <p className="text-sm text-blue-800">
                    <span className="font-medium">Toplam Maliyet:</span>{" "}
                    {formatCurrency(form.watch("quantity") * form.watch("unitCost"))} TL
                    {" "}({form.watch("quantity")} adet × {formatCurrency(form.watch("unitCost"))} TL)
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
