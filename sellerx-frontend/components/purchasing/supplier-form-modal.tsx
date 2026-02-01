"use client";

import { useEffect } from "react";
import { useForm } from "react-hook-form";
import type {
  Supplier,
  CreateSupplierRequest,
  UpdateSupplierRequest,
} from "@/types/supplier";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Loader2 } from "lucide-react";

const CURRENCIES = [
  { value: "TRY", label: "TRY (₺)" },
  { value: "USD", label: "USD ($)" },
  { value: "EUR", label: "EUR (€)" },
  { value: "GBP", label: "GBP (£)" },
  { value: "CNY", label: "CNY (¥)" },
];

interface SupplierFormModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSubmit: (data: CreateSupplierRequest | UpdateSupplierRequest) => void;
  isSubmitting: boolean;
  supplier?: Supplier | null;
}

export function SupplierFormModal({
  open,
  onOpenChange,
  onSubmit,
  isSubmitting,
  supplier,
}: SupplierFormModalProps) {
  const isEditing = !!supplier;

  const { register, handleSubmit, reset, setValue, watch } =
    useForm<CreateSupplierRequest>({
      defaultValues: {
        name: "",
        contactPerson: "",
        email: "",
        phone: "",
        address: "",
        country: "",
        currency: "TRY",
        paymentTermsDays: undefined,
        notes: "",
      },
    });

  const watchedCurrency = watch("currency");

  useEffect(() => {
    if (supplier) {
      reset({
        name: supplier.name,
        contactPerson: supplier.contactPerson || "",
        email: supplier.email || "",
        phone: supplier.phone || "",
        address: supplier.address || "",
        country: supplier.country || "",
        currency: supplier.currency || "TRY",
        paymentTermsDays: supplier.paymentTermsDays,
        notes: supplier.notes || "",
      });
    } else {
      reset({
        name: "",
        contactPerson: "",
        email: "",
        phone: "",
        address: "",
        country: "",
        currency: "TRY",
        paymentTermsDays: undefined,
        notes: "",
      });
    }
  }, [supplier, reset]);

  const handleFormSubmit = (data: CreateSupplierRequest) => {
    onSubmit(data);
  };

  const handleOpenChange = (isOpen: boolean) => {
    if (!isOpen) {
      reset();
    }
    onOpenChange(isOpen);
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle>
            {isEditing ? "Tedarikçi Düzenle" : "Yeni Tedarikçi"}
          </DialogTitle>
        </DialogHeader>

        <form
          onSubmit={handleSubmit(handleFormSubmit)}
          className="space-y-4"
        >
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-2 col-span-2">
              <Label htmlFor="name">Tedarikçi Adı *</Label>
              <Input
                id="name"
                placeholder="Tedarikçi adı"
                {...register("name", { required: true })}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="contactPerson">İletişim Kişisi</Label>
              <Input
                id="contactPerson"
                placeholder="Ad soyad"
                {...register("contactPerson")}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="email">E-posta</Label>
              <Input
                id="email"
                type="email"
                placeholder="ornek@firma.com"
                {...register("email")}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="phone">Telefon</Label>
              <Input
                id="phone"
                placeholder="+90 555 123 4567"
                {...register("phone")}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="country">Ülke</Label>
              <Input
                id="country"
                placeholder="Türkiye"
                {...register("country")}
              />
            </div>

            <div className="space-y-2">
              <Label>Para Birimi</Label>
              <Select
                value={watchedCurrency || "TRY"}
                onValueChange={(val) => setValue("currency", val)}
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

            <div className="space-y-2">
              <Label htmlFor="paymentTermsDays">Ödeme Vadesi (gün)</Label>
              <Input
                id="paymentTermsDays"
                type="number"
                min="0"
                placeholder="30"
                {...register("paymentTermsDays", { valueAsNumber: true })}
              />
            </div>

            <div className="space-y-2 col-span-2">
              <Label htmlFor="address">Adres</Label>
              <Input
                id="address"
                placeholder="Adres"
                {...register("address")}
              />
            </div>

            <div className="space-y-2 col-span-2">
              <Label htmlFor="notes">Notlar</Label>
              <Textarea
                id="notes"
                placeholder="Ek notlar"
                {...register("notes")}
                rows={2}
              />
            </div>
          </div>

          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => handleOpenChange(false)}
            >
              İptal
            </Button>
            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting ? (
                <Loader2 className="h-4 w-4 animate-spin mr-2" />
              ) : null}
              {isEditing ? "Güncelle" : "Oluştur"}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
