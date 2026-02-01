"use client";

import React, { useState } from "react";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import {
  RefreshCw,
  Package,
  ChevronDown,
  ChevronUp,
  Check,
  X,
  Eye,
  ExternalLink,
} from "lucide-react";
import { cn } from "@/lib/utils";
import type { TrendyolClaim, ClaimStatus, ClaimItem } from "@/types/returns";
import { useCurrency } from "@/lib/contexts/currency-context";

function formatDate(dateString: string | null): string {
  if (!dateString) return "-";
  return new Date(dateString).toLocaleDateString("tr-TR", {
    year: "numeric",
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

interface ClaimItemsRowProps {
  items: ClaimItem[];
}

function ClaimItemsRow({ items }: ClaimItemsRowProps) {
  const { formatCurrency } = useCurrency();
  return (
    <div className="bg-muted p-4 rounded-lg mt-2 space-y-2">
      <p className="text-xs font-medium text-muted-foreground uppercase">
        Iade Kalemleri
      </p>
      {items.map((item, idx) => (
        <div
          key={idx}
          className="flex items-center justify-between text-sm border-b border-border pb-2 last:border-0"
        >
          <div className="flex items-center gap-3 flex-1">
            {item.imageUrl && (
              <img
                src={item.imageUrl}
                alt={item.productName}
                className="w-10 h-10 object-cover rounded"
              />
            )}
            <div>
              <p className="font-medium text-foreground line-clamp-1">
                {item.productName}
              </p>
              <p className="text-xs text-muted-foreground">
                Barkod: {item.barcode}
                {item.productSize && ` | Beden: ${item.productSize}`}
                {item.productColor && ` | Renk: ${item.productColor}`}
              </p>
              {item.reasonName && (
                <p className="text-xs text-orange-600 dark:text-orange-400 mt-1">
                  Neden: {item.reasonName}
                </p>
              )}
              {item.customerNote && (
                <p className="text-xs text-muted-foreground mt-1 italic">
                  "{item.customerNote}"
                </p>
              )}
            </div>
          </div>
          <div className="text-right">
            <p className="font-medium">
              {item.quantity} x {formatCurrency(item.price)}
            </p>
            <div className="flex items-center gap-2 justify-end mt-1">
              {item.autoAccepted && (
                <span className="text-xs bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300 px-2 py-0.5 rounded">
                  Otomatik Onay
                </span>
              )}
              {item.acceptedBySeller && (
                <span className="text-xs bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-300 px-2 py-0.5 rounded">
                  Satici Onayli
                </span>
              )}
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}

interface ClaimsTableProps {
  claims: TrendyolClaim[];
  isLoading: boolean;
  selectedClaims: Set<string>;
  onSelectClaim: (claimId: string, selected: boolean) => void;
  onSelectAll: (selected: boolean) => void;
  selectableCount: number;
  onApprove: (claim: TrendyolClaim) => void;
  onReject: (claim: TrendyolClaim) => void;
  onViewDetail: (claim: TrendyolClaim) => void;
  getStatusColor: (status: ClaimStatus) => string;
  getStatusLabel: (status: ClaimStatus) => string;
  approvePending: boolean;
}

export function ClaimsTable({
  claims,
  isLoading,
  selectedClaims,
  onSelectClaim,
  onSelectAll,
  selectableCount,
  onApprove,
  onReject,
  onViewDetail,
  getStatusColor,
  getStatusLabel,
  approvePending,
}: ClaimsTableProps) {
  const { formatCurrency } = useCurrency();
  const [expandedClaims, setExpandedClaims] = useState<Set<string>>(new Set());

  const toggleClaimExpand = (claimId: string) => {
    setExpandedClaims((prev) => {
      const next = new Set(prev);
      if (next.has(claimId)) {
        next.delete(claimId);
      } else {
        next.add(claimId);
      }
      return next;
    });
  };

  const allSelectableSelected =
    selectableCount > 0 && selectedClaims.size === selectableCount;
  const someSelected = selectedClaims.size > 0 && !allSelectableSelected;

  return (
    <div className="bg-card rounded-lg border border-border">
      <div className="overflow-x-auto">
        <Table>
          <TableHeader>
            <TableRow className="hover:bg-transparent">
              <TableHead className="w-[50px]">
                <Checkbox
                  checked={allSelectableSelected}
                  // @ts-expect-error - indeterminate is valid
                  indeterminate={someSelected}
                  onCheckedChange={(checked) => onSelectAll(checked as boolean)}
                  disabled={selectableCount === 0}
                />
              </TableHead>
              <TableHead className="w-[50px]"></TableHead>
              <TableHead>Siparis No</TableHead>
              <TableHead>Musteri</TableHead>
              <TableHead>Iade Tarihi</TableHead>
              <TableHead className="text-center">Durum</TableHead>
              <TableHead className="text-center">Kalem</TableHead>
              <TableHead className="text-right">Islemler</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading ? (
              <TableRow>
                <TableCell colSpan={8} className="h-24 text-center">
                  <RefreshCw className="h-5 w-5 animate-spin mx-auto mb-2" />
                  Iadeler yukleniyor...
                </TableCell>
              </TableRow>
            ) : claims.length === 0 ? (
              <TableRow>
                <TableCell
                  colSpan={8}
                  className="h-24 text-center text-muted-foreground"
                >
                  <Package className="h-8 w-8 mx-auto mb-2 opacity-50" />
                  Iade talebi bulunamadi
                </TableCell>
              </TableRow>
            ) : (
              claims.map((claim) => (
                <React.Fragment key={claim.id}>
                  <TableRow className="hover:bg-muted">
                    <TableCell>
                      <Checkbox
                        checked={selectedClaims.has(claim.id)}
                        onCheckedChange={(checked) =>
                          onSelectClaim(claim.id, checked as boolean)
                        }
                        disabled={claim.status !== "WaitingInAction"}
                      />
                    </TableCell>
                    <TableCell>
                      <Button
                        variant="ghost"
                        size="sm"
                        className="h-8 w-8 p-0"
                        onClick={() => toggleClaimExpand(claim.id)}
                      >
                        {expandedClaims.has(claim.id) ? (
                          <ChevronUp className="h-4 w-4" />
                        ) : (
                          <ChevronDown className="h-4 w-4" />
                        )}
                      </Button>
                    </TableCell>
                    <TableCell>
                      <div>
                        <p className="font-medium text-sm">
                          {claim.orderNumber || claim.claimId}
                        </p>
                        {claim.cargoTrackingNumber && (
                          <div className="flex items-center gap-1 mt-1">
                            <span className="text-xs text-muted-foreground">
                              Kargo: {claim.cargoTrackingNumber}
                            </span>
                            {claim.cargoTrackingLink && (
                              <a
                                href={claim.cargoTrackingLink}
                                target="_blank"
                                rel="noopener noreferrer"
                                className="text-blue-500 hover:text-blue-600"
                                onClick={(e) => e.stopPropagation()}
                              >
                                <ExternalLink className="h-3 w-3" />
                              </a>
                            )}
                          </div>
                        )}
                      </div>
                    </TableCell>
                    <TableCell>
                      <p className="text-sm">{claim.customerFullName}</p>
                    </TableCell>
                    <TableCell className="text-sm">
                      {formatDate(claim.claimDate)}
                    </TableCell>
                    <TableCell className="text-center">
                      <span
                        className={cn(
                          "inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium",
                          getStatusColor(claim.status)
                        )}
                      >
                        {getStatusLabel(claim.status)}
                      </span>
                    </TableCell>
                    <TableCell className="text-center">
                      <span className="inline-flex items-center justify-center w-6 h-6 rounded-full bg-muted text-xs font-medium">
                        {claim.totalItemCount}
                      </span>
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="flex items-center justify-end gap-1">
                        <Button
                          variant="ghost"
                          size="sm"
                          className="h-8 w-8 p-0"
                          onClick={() => onViewDetail(claim)}
                          title="Detay"
                        >
                          <Eye className="h-4 w-4" />
                        </Button>
                        {claim.status === "WaitingInAction" && (
                          <>
                            <Button
                              variant="ghost"
                              size="sm"
                              className="h-8 w-8 p-0 text-green-600 hover:text-green-700 hover:bg-green-50 dark:hover:bg-green-900/20"
                              onClick={() => onApprove(claim)}
                              disabled={approvePending}
                              title="Onayla"
                            >
                              <Check className="h-4 w-4" />
                            </Button>
                            <Button
                              variant="ghost"
                              size="sm"
                              className="h-8 w-8 p-0 text-red-600 hover:text-red-700 hover:bg-red-50 dark:hover:bg-red-900/20"
                              onClick={() => onReject(claim)}
                              title="Reddet"
                            >
                              <X className="h-4 w-4" />
                            </Button>
                          </>
                        )}
                      </div>
                    </TableCell>
                  </TableRow>
                  {expandedClaims.has(claim.id) && (
                    <TableRow>
                      <TableCell colSpan={8} className="p-0">
                        <div className="px-4 pb-4">
                          <ClaimItemsRow items={claim.items} />
                        </div>
                      </TableCell>
                    </TableRow>
                  )}
                </React.Fragment>
              ))
            )}
          </TableBody>
        </Table>
      </div>
    </div>
  );
}
