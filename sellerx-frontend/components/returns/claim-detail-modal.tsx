"use client";

import React from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import {
  Package,
  User,
  Calendar,
  Truck,
  ExternalLink,
  Check,
  X,
} from "lucide-react";
import { cn } from "@/lib/utils";
import type { TrendyolClaim, ClaimStatus } from "@/types/returns";
import { useCurrency } from "@/lib/contexts/currency-context";

function formatDate(dateString: string | null): string {
  if (!dateString) return "-";
  return new Date(dateString).toLocaleDateString("tr-TR", {
    year: "numeric",
    month: "long",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

interface ClaimDetailModalProps {
  claim: TrendyolClaim;
  open: boolean;
  onClose: () => void;
  onApprove: () => void;
  onReject: () => void;
  getStatusColor: (status: ClaimStatus) => string;
  getStatusLabel: (status: ClaimStatus) => string;
}

export function ClaimDetailModal({
  claim,
  open,
  onClose,
  onApprove,
  onReject,
  getStatusColor,
  getStatusLabel,
}: ClaimDetailModalProps) {
  const { formatCurrency } = useCurrency();

  const canTakeAction = claim.status === "WaitingInAction";

  // Calculate total value
  const totalValue = claim.items.reduce(
    (sum, item) => sum + item.price * item.quantity,
    0
  );

  return (
    <Dialog open={open} onOpenChange={(isOpen) => !isOpen && onClose()}>
      <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-3">
            <Package className="h-5 w-5" />
            Iade Detayi
            <Badge className={cn("ml-2", getStatusColor(claim.status))}>
              {getStatusLabel(claim.status)}
            </Badge>
          </DialogTitle>
        </DialogHeader>

        <div className="space-y-6">
          {/* Order Info */}
          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-1">
              <p className="text-xs text-muted-foreground">Siparis No</p>
              <p className="font-medium">{claim.orderNumber || claim.claimId}</p>
            </div>
            <div className="space-y-1">
              <p className="text-xs text-muted-foreground">Claim ID</p>
              <p className="font-mono text-sm">{claim.claimId}</p>
            </div>
          </div>

          <Separator />

          {/* Customer Info */}
          <div className="space-y-3">
            <div className="flex items-center gap-2 text-sm font-medium">
              <User className="h-4 w-4" />
              Musteri Bilgileri
            </div>
            <div className="grid grid-cols-2 gap-4 pl-6">
              <div className="space-y-1">
                <p className="text-xs text-muted-foreground">Ad Soyad</p>
                <p className="font-medium">{claim.customerFullName}</p>
              </div>
            </div>
          </div>

          <Separator />

          {/* Date Info */}
          <div className="space-y-3">
            <div className="flex items-center gap-2 text-sm font-medium">
              <Calendar className="h-4 w-4" />
              Tarih Bilgileri
            </div>
            <div className="grid grid-cols-2 gap-4 pl-6">
              <div className="space-y-1">
                <p className="text-xs text-muted-foreground">Iade Tarihi</p>
                <p>{formatDate(claim.claimDate)}</p>
              </div>
              {claim.lastModifiedDate && (
                <div className="space-y-1">
                  <p className="text-xs text-muted-foreground">Son Guncelleme</p>
                  <p>{formatDate(claim.lastModifiedDate)}</p>
                </div>
              )}
            </div>
          </div>

          {/* Cargo Info */}
          {claim.cargoTrackingNumber && (
            <>
              <Separator />
              <div className="space-y-3">
                <div className="flex items-center gap-2 text-sm font-medium">
                  <Truck className="h-4 w-4" />
                  Kargo Bilgileri
                </div>
                <div className="grid grid-cols-2 gap-4 pl-6">
                  <div className="space-y-1">
                    <p className="text-xs text-muted-foreground">Kargo Firmasi</p>
                    <p>{claim.cargoProviderName || "-"}</p>
                  </div>
                  <div className="space-y-1">
                    <p className="text-xs text-muted-foreground">Takip No</p>
                    <div className="flex items-center gap-2">
                      <p className="font-mono">{claim.cargoTrackingNumber}</p>
                      {claim.cargoTrackingLink && (
                        <a
                          href={claim.cargoTrackingLink}
                          target="_blank"
                          rel="noopener noreferrer"
                          className="text-blue-500 hover:text-blue-600"
                        >
                          <ExternalLink className="h-4 w-4" />
                        </a>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            </>
          )}

          <Separator />

          {/* Items */}
          <div className="space-y-3">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2 text-sm font-medium">
                <Package className="h-4 w-4" />
                Iade Edilen Urunler ({claim.totalItemCount} adet)
              </div>
              <div className="text-sm font-medium">
                Toplam: {formatCurrency(totalValue)}
              </div>
            </div>

            <div className="space-y-3 pl-6">
              {claim.items.map((item, idx) => (
                <div
                  key={idx}
                  className="flex items-start gap-3 p-3 bg-muted rounded-lg"
                >
                  {item.imageUrl && (
                    <img
                      src={item.imageUrl}
                      alt={item.productName}
                      className="w-16 h-16 object-cover rounded"
                    />
                  )}
                  <div className="flex-1 min-w-0">
                    <p className="font-medium line-clamp-2">{item.productName}</p>
                    <p className="text-xs text-muted-foreground mt-1">
                      Barkod: {item.barcode}
                      {item.productSize && ` | Beden: ${item.productSize}`}
                      {item.productColor && ` | Renk: ${item.productColor}`}
                    </p>
                    <div className="flex items-center gap-4 mt-2">
                      <span className="text-sm">
                        {item.quantity} x {formatCurrency(item.price)}
                      </span>
                      {item.autoAccepted && (
                        <Badge variant="secondary" className="text-xs">
                          Otomatik Onay
                        </Badge>
                      )}
                      {item.acceptedBySeller && (
                        <Badge variant="secondary" className="text-xs bg-green-100 text-green-700">
                          Satici Onayli
                        </Badge>
                      )}
                    </div>
                    {item.reasonName && (
                      <p className="text-sm text-orange-600 dark:text-orange-400 mt-2">
                        Iade Nedeni: {item.reasonName}
                      </p>
                    )}
                    {item.customerNote && (
                      <p className="text-sm text-muted-foreground mt-1 italic">
                        Musteri Notu: "{item.customerNote}"
                      </p>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>

        <DialogFooter className="gap-2 sm:gap-0">
          <Button variant="outline" onClick={onClose}>
            Kapat
          </Button>
          {canTakeAction && (
            <>
              <Button
                variant="destructive"
                onClick={onReject}
                className="gap-2"
              >
                <X className="h-4 w-4" />
                Reddet
              </Button>
              <Button onClick={onApprove} className="gap-2">
                <Check className="h-4 w-4" />
                Onayla
              </Button>
            </>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}
