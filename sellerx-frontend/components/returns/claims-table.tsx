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
  History,
} from "lucide-react";
import { cn } from "@/lib/utils";
import type { TrendyolClaim, ClaimStatus, ClaimItem } from "@/types/returns";
import { useCurrency } from "@/lib/contexts/currency-context";
import { useClaimItemAudit } from "@/hooks/queries/use-returns";

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

interface ClaimItemAuditTimelineProps {
  storeId: string;
  itemId: string;
}

function ClaimItemAuditTimeline({ storeId, itemId }: ClaimItemAuditTimelineProps) {
  const { data: audit, isLoading, error } = useClaimItemAudit(storeId, itemId, true);

  if (isLoading) {
    return (
      <div className="mt-2 pl-4 text-xs text-muted-foreground">
        <RefreshCw className="h-3 w-3 animate-spin inline mr-1" />
        Gecmis yukleniyor...
      </div>
    );
  }

  if (error) {
    return (
      <div className="mt-2 pl-4 text-xs text-red-500">
        Gecmis yuklenemedi
      </div>
    );
  }

  if (!audit || audit.length === 0) {
    return (
      <div className="mt-2 pl-4 text-xs text-muted-foreground">
        Gecmis bulunamadi
      </div>
    );
  }

  return (
    <div className="mt-2 pl-4 space-y-1.5">
      {audit.map((entry, idx) => (
        <div key={idx} className="flex items-start gap-2 text-xs">
          <div className="mt-1.5 h-1.5 w-1.5 rounded-full bg-blue-500 shrink-0" />
          <div>
            <span className="font-medium text-foreground">
              {entry.previousStatus}
            </span>
            <span className="text-muted-foreground mx-1">&rarr;</span>
            <span className="font-medium text-foreground">
              {entry.newStatus}
            </span>
            <span className="text-muted-foreground ml-2">
              ({formatDate(entry.date)}
              {entry.executorApp && `, ${entry.executorApp}`}
              {entry.executorUser && ` - ${entry.executorUser}`})
            </span>
          </div>
        </div>
      ))}
    </div>
  );
}

interface ClaimItemsRowProps {
  items: ClaimItem[];
  storeId?: string;
}

function ClaimItemsRow({ items, storeId }: ClaimItemsRowProps) {
  const { formatCurrency } = useCurrency();
  const [auditItemId, setAuditItemId] = useState<string | null>(null);

  const toggleAudit = (itemId: string) => {
    setAuditItemId((prev) => (prev === itemId ? null : itemId));
  };

  return (
    <div className="bg-muted p-4 rounded-lg mt-2 space-y-2">
      <p className="text-xs font-medium text-muted-foreground uppercase">
        Iade Kalemleri
      </p>
      {items.map((item, idx) => (
        <div key={idx}>
          <div className="flex items-center justify-between text-sm border-b border-border pb-2 last:border-0">
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
                    &quot;{item.customerNote}&quot;
                  </p>
                )}
              </div>
            </div>
            <div className="flex items-center gap-2">
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
              {storeId && (
                <Button
                  variant="ghost"
                  size="sm"
                  className={cn(
                    "h-7 px-2 text-xs gap-1",
                    auditItemId === item.claimItemId
                      ? "text-blue-600 bg-blue-50 dark:bg-blue-900/20"
                      : "text-muted-foreground"
                  )}
                  onClick={() => toggleAudit(item.claimItemId)}
                  title="Durum Gecmisi"
                >
                  <History className="h-3.5 w-3.5" />
                  Gecmis
                </Button>
              )}
            </div>
          </div>
          {auditItemId === item.claimItemId && storeId && (
            <ClaimItemAuditTimeline storeId={storeId} itemId={item.claimItemId} />
          )}
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
  storeId?: string;
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
  storeId,
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
                          <ClaimItemsRow items={claim.items} storeId={storeId} />
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
