"use client";

import { useForm } from "react-hook-form";
import { useEffect } from "react";
import type { PurchaseOrder, UpdatePurchaseOrderRequest } from "@/types/purchasing";
import type { Supplier } from "@/types/supplier";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Save, Loader2 } from "lucide-react";

interface POFormProps {
  purchaseOrder: PurchaseOrder;
  onSave: (data: UpdatePurchaseOrderRequest) => void;
  isSaving: boolean;
  suppliers?: Supplier[];
}

const CURRENCIES = [
  { value: "TRY", label: "TRY (₺)" },
  { value: "USD", label: "USD ($)" },
  { value: "EUR", label: "EUR (€)" },
  { value: "GBP", label: "GBP (£)" },
  { value: "CNY", label: "CNY (¥)" },
];

export function POForm({ purchaseOrder, onSave, isSaving, suppliers }: POFormProps) {
  const { register, handleSubmit, reset, setValue, watch, formState: { isDirty } } = useForm<UpdatePurchaseOrderRequest>({
    defaultValues: {
      poDate: purchaseOrder.poDate,
      estimatedArrival: purchaseOrder.estimatedArrival || "",
      stockEntryDate: purchaseOrder.stockEntryDate || "",
      supplierName: purchaseOrder.supplierName || "",
      supplierId: purchaseOrder.supplierId,
      supplierCurrency: purchaseOrder.supplierCurrency || "TRY",
      exchangeRate: purchaseOrder.exchangeRate,
      carrier: purchaseOrder.carrier || "",
      trackingNumber: purchaseOrder.trackingNumber || "",
      comment: purchaseOrder.comment || "",
      transportationCost: purchaseOrder.transportationCost,
    },
  });

  const watchedCurrency = watch("supplierCurrency");
  const watchedSupplierId = watch("supplierId");

  useEffect(() => {
    reset({
      poDate: purchaseOrder.poDate,
      estimatedArrival: purchaseOrder.estimatedArrival || "",
      stockEntryDate: purchaseOrder.stockEntryDate || "",
      supplierName: purchaseOrder.supplierName || "",
      supplierId: purchaseOrder.supplierId,
      supplierCurrency: purchaseOrder.supplierCurrency || "TRY",
      exchangeRate: purchaseOrder.exchangeRate,
      carrier: purchaseOrder.carrier || "",
      trackingNumber: purchaseOrder.trackingNumber || "",
      comment: purchaseOrder.comment || "",
      transportationCost: purchaseOrder.transportationCost,
    });
  }, [purchaseOrder, reset]);

  const handleSupplierChange = (value: string) => {
    if (value === "none") {
      setValue("supplierId", undefined, { shouldDirty: true });
      setValue("supplierName", "", { shouldDirty: true });
      return;
    }
    const id = parseInt(value);
    const supplier = suppliers?.find((s) => s.id === id);
    if (supplier) {
      setValue("supplierId", id, { shouldDirty: true });
      setValue("supplierName", supplier.name, { shouldDirty: true });
      if (supplier.currency && supplier.currency !== "TRY") {
        setValue("supplierCurrency", supplier.currency, { shouldDirty: true });
      }
    }
  };

  const handleCurrencyChange = (value: string) => {
    setValue("supplierCurrency", value, { shouldDirty: true });
    if (value === "TRY") {
      setValue("exchangeRate", undefined, { shouldDirty: true });
    }
  };

  return (
    <form onSubmit={handleSubmit(onSave)} className="bg-card rounded-lg border border-border p-4">
      <div className="flex items-center justify-between mb-4">
        <h3 className="font-semibold text-foreground">Sipariş Bilgileri</h3>
        {isDirty && (
          <Button type="submit" size="sm" disabled={isSaving}>
            {isSaving ? (
              <Loader2 className="h-4 w-4 animate-spin mr-2" />
            ) : (
              <Save className="h-4 w-4 mr-2" />
            )}
            Kaydet
          </Button>
        )}
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="space-y-2">
          <Label htmlFor="poNumber">Sipariş No</Label>
          <Input
            id="poNumber"
            value={purchaseOrder.poNumber}
            disabled
            className="bg-muted"
          />
        </div>

        <div className="space-y-2">
          <Label htmlFor="poDate">Sipariş Tarihi</Label>
          <Input
            id="poDate"
            type="date"
            {...register("poDate")}
          />
        </div>

        <div className="space-y-2">
          <Label htmlFor="estimatedArrival">Tahmini Varış</Label>
          <Input
            id="estimatedArrival"
            type="date"
            {...register("estimatedArrival")}
          />
        </div>

        <div className="space-y-2">
          <Label htmlFor="stockEntryDate">Stok Giriş Tarihi</Label>
          <Input
            id="stockEntryDate"
            type="date"
            {...register("stockEntryDate")}
          />
          <p className="text-xs text-muted-foreground">
            Ürünlerin depoya girdiği tarih. Boş bırakılırsa sipariş tarihi kullanılır.
          </p>
        </div>

        {/* Supplier */}
        <div className="space-y-2">
          <Label>Tedarikçi</Label>
          <Select
            value={watchedSupplierId ? String(watchedSupplierId) : "none"}
            onValueChange={handleSupplierChange}
          >
            <SelectTrigger>
              <SelectValue placeholder="Tedarikçi seçin" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="none">Seçilmemiş</SelectItem>
              {suppliers?.map((s) => (
                <SelectItem key={s.id} value={String(s.id)}>
                  {s.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        {/* Currency */}
        <div className="space-y-2">
          <Label>Para Birimi</Label>
          <Select
            value={watchedCurrency || "TRY"}
            onValueChange={handleCurrencyChange}
          >
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {CURRENCIES.map((c) => (
                <SelectItem key={c.value} value={c.value}>
                  {c.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        {/* Exchange Rate - only shown for non-TRY */}
        {watchedCurrency && watchedCurrency !== "TRY" && (
          <div className="space-y-2">
            <Label htmlFor="exchangeRate">Kur (1 {watchedCurrency} = ? TRY)</Label>
            <Input
              id="exchangeRate"
              type="number"
              min="0"
              step="0.0001"
              placeholder="Otomatik"
              {...register("exchangeRate", { valueAsNumber: true })}
            />
          </div>
        )}

        {/* Carrier */}
        <div className="space-y-2">
          <Label htmlFor="carrier">Kargo Firması</Label>
          <Input
            id="carrier"
            placeholder="Kargo firması"
            {...register("carrier")}
          />
        </div>

        {/* Tracking Number */}
        <div className="space-y-2">
          <Label htmlFor="trackingNumber">Takip Numarası</Label>
          <Input
            id="trackingNumber"
            placeholder="Kargo takip no"
            {...register("trackingNumber")}
          />
        </div>

        {/* Transportation Cost */}
        <div className="space-y-2">
          <Label htmlFor="transportationCost">Toplam Nakliye Maliyeti (₺)</Label>
          <Input
            id="transportationCost"
            type="number"
            min="0"
            step="0.01"
            placeholder="0.00"
            {...register("transportationCost", { valueAsNumber: true })}
          />
        </div>

        <div className="space-y-2 md:col-span-3">
          <Label htmlFor="comment">Not</Label>
          <Textarea
            id="comment"
            placeholder="Sipariş hakkında notlar"
            {...register("comment")}
            rows={2}
          />
        </div>
      </div>
    </form>
  );
}
