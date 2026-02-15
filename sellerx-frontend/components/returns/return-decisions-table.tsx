"use client";

import React, { useMemo, useState, useCallback, useEffect } from "react";
import { useSelectedStore } from "@/hooks/queries/use-stores";
import {
  useReturnedOrders,
  useUpdateResalable,
  useClaims,
  useApproveClaim,
  useRejectClaim,
  useClaimItemAudit,
} from "@/hooks/queries/use-returns";
import { useCurrency } from "@/lib/contexts/currency-context";
import { format } from "date-fns";
import { tr } from "date-fns/locale";
import {
  Check,
  X,
  Loader2,
  Package,
  Truck,
  Search,
  ChevronDown,
  ChevronUp,
  ExternalLink,
  Inbox,
  CheckCircle,
  XCircle,
  AlertTriangle,
  Clock,
  History,
  RefreshCw,
} from "lucide-react";
import { cn } from "@/lib/utils";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Checkbox } from "@/components/ui/checkbox";
import {
  Tooltip,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "@/components/ui/tooltip";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { TableSkeleton } from "@/components/ui/skeleton-blocks";
import { ReturnExportButton } from "./return-export-button";
import type {
  TrendyolClaim,
  ClaimItem,
} from "@/types/returns";

// =====================================================
// Types
// =====================================================

type DecisionFilter = "ALL" | "RESALABLE" | "NOT_RESALABLE" | "PENDING";
type ClaimStatusFilter = "ALL" | "WaitingInAction" | "Accepted" | "Rejected" | "Unresolved";

const PAGE_SIZE = 20;

interface ReturnDecisionsTableProps {
  startDate: string;
  endDate: string;
}

// =====================================================
// Helpers
// =====================================================

function getClaimStatusColor(status: string): string {
  switch (status) {
    case "WaitingInAction":
      return "bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400";
    case "Accepted":
      return "bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400";
    case "Rejected":
      return "bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400";
    case "Unresolved":
      return "bg-purple-100 text-purple-700 dark:bg-purple-900/30 dark:text-purple-400";
    case "Cancelled":
      return "bg-gray-100 text-gray-700 dark:bg-gray-900/30 dark:text-gray-400";
    case "InAnalysis":
      return "bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400";
    default:
      return "bg-gray-100 text-gray-700 dark:bg-gray-900/30 dark:text-gray-400";
  }
}

function getClaimStatusLabel(status: string): string {
  switch (status) {
    case "Created": return "Olusturuldu";
    case "WaitingInAction": return "Bekliyor";
    case "Accepted": return "Onaylandi";
    case "Rejected": return "Reddedildi";
    case "Unresolved": return "Cozumsuz";
    case "Cancelled": return "Iptal";
    case "InAnalysis": return "Inceleniyor";
    default: return status;
  }
}

function formatClaimDate(dateString: string | null): string {
  if (!dateString) return "-";
  return new Date(dateString).toLocaleDateString("tr-TR", {
    year: "numeric",
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

// =====================================================
// Claim Item Audit Timeline
// =====================================================

function ClaimItemAuditTimeline({ storeId, itemId }: { storeId: string; itemId: string }) {
  const { data: audit, isLoading, error } = useClaimItemAudit(storeId, itemId, true);

  if (isLoading) {
    return (
      <div className="mt-2 pl-4 text-xs text-muted-foreground">
        <RefreshCw className="h-3 w-3 animate-spin inline mr-1" />
        Gecmis yukleniyor...
      </div>
    );
  }
  if (error) return <div className="mt-2 pl-4 text-xs text-red-500">Gecmis yuklenemedi</div>;
  if (!audit || audit.length === 0) return <div className="mt-2 pl-4 text-xs text-muted-foreground">Gecmis bulunamadi</div>;

  return (
    <div className="mt-2 pl-4 space-y-1.5">
      {audit.map((entry, idx) => (
        <div key={idx} className="flex items-start gap-2 text-xs">
          <div className="mt-1.5 h-1.5 w-1.5 rounded-full bg-blue-500 shrink-0" />
          <div>
            <span className="font-medium text-foreground">{entry.previousStatus}</span>
            <span className="text-muted-foreground mx-1">&rarr;</span>
            <span className="font-medium text-foreground">{entry.newStatus}</span>
            <span className="text-muted-foreground ml-2">
              ({formatClaimDate(entry.date)}
              {entry.executorApp && `, ${entry.executorApp}`}
              {entry.executorUser && ` - ${entry.executorUser}`})
            </span>
          </div>
        </div>
      ))}
    </div>
  );
}

// =====================================================
// Claim Items Expanded Row
// =====================================================

function ClaimItemsExpanded({ items, storeId }: { items: ClaimItem[]; storeId?: string }) {
  const { formatCurrency } = useCurrency();
  const [auditItemId, setAuditItemId] = useState<string | null>(null);

  return (
    <div className="bg-muted p-4 rounded-lg space-y-2">
      <p className="text-xs font-medium text-muted-foreground uppercase">Iade Kalemleri</p>
      {items.map((item, idx) => (
        <div key={idx}>
          <div className="flex items-center justify-between text-sm border-b border-border pb-2 last:border-0">
            <div className="flex items-center gap-3 flex-1">
              {item.imageUrl && (
                <img src={item.imageUrl} alt={item.productName} className="w-10 h-10 object-cover rounded" />
              )}
              <div>
                <p className="font-medium text-foreground line-clamp-1">{item.productName}</p>
                <p className="text-xs text-muted-foreground">
                  Barkod: {item.barcode}
                  {item.productSize && ` | Beden: ${item.productSize}`}
                  {item.productColor && ` | Renk: ${item.productColor}`}
                </p>
                {item.reasonName && (
                  <p className="text-xs text-orange-600 dark:text-orange-400 mt-1">Neden: {item.reasonName}</p>
                )}
                {item.customerNote && (
                  <p className="text-xs text-muted-foreground mt-1 italic">&quot;{item.customerNote}&quot;</p>
                )}
              </div>
            </div>
            <div className="flex items-center gap-2">
              <div className="text-right">
                <p className="font-medium">{item.quantity} x {formatCurrency(item.price)}</p>
                <div className="flex items-center gap-2 justify-end mt-1">
                  {item.autoAccepted && (
                    <span className="text-xs bg-blue-100 dark:bg-blue-900/30 text-blue-700 dark:text-blue-300 px-2 py-0.5 rounded">Otomatik Onay</span>
                  )}
                  {item.acceptedBySeller && (
                    <span className="text-xs bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-300 px-2 py-0.5 rounded">Satici Onayli</span>
                  )}
                </div>
              </div>
              {storeId && (
                <Button
                  variant="ghost"
                  size="sm"
                  className={cn(
                    "h-7 px-2 text-xs gap-1",
                    auditItemId === item.claimItemId ? "text-blue-600 bg-blue-50 dark:bg-blue-900/20" : "text-muted-foreground"
                  )}
                  onClick={() => setAuditItemId(prev => prev === item.claimItemId ? null : item.claimItemId)}
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

// =====================================================
// Main Component
// =====================================================

export function ReturnDecisionsTable({ startDate, endDate }: ReturnDecisionsTableProps) {
  const { data: selectedStore } = useSelectedStore();
  const storeId = selectedStore?.selectedStoreId;

  // Main data: returned orders with financial details
  const { data: orders, isLoading, error } = useReturnedOrders(storeId, startDate, endDate);
  const updateResalable = useUpdateResalable();
  const { formatCurrency } = useCurrency();

  // Claims data: fetch to match with orders
  const { data: claimsPage } = useClaims(storeId, undefined, 0, 500);
  const approveClaim = useApproveClaim();
  const rejectClaim = useRejectClaim();

  // Build claim lookup by orderNumber
  const claimMap = useMemo(() => {
    const map = new Map<string, TrendyolClaim>();
    if (claimsPage?.content) {
      for (const claim of claimsPage.content) {
        if (claim.orderNumber) {
          map.set(claim.orderNumber, claim);
        }
      }
    }
    return map;
  }, [claimsPage]);

  // Filter states
  const [searchTerm, setSearchTerm] = useState("");
  const [reasonFilter, setReasonFilter] = useState<string>("ALL");
  const [decisionFilter, setDecisionFilter] = useState<DecisionFilter>("ALL");
  const [claimStatusFilter, setClaimStatusFilter] = useState<ClaimStatusFilter>("ALL");
  const [page, setPage] = useState(0);

  // Expanded rows & selection
  const [expandedRows, setExpandedRows] = useState<Set<string>>(new Set());
  const [selectedOrders, setSelectedOrders] = useState<Set<string>>(new Set());

  // Unique return reasons for dropdown
  const uniqueReasons = useMemo(() => {
    if (!orders) return [];
    const reasons = new Set<string>();
    orders.forEach((o) => { if (o.returnReason) reasons.add(o.returnReason); });
    return Array.from(reasons).sort();
  }, [orders]);

  // Compute date-filtered claim stats from orders + claimMap
  const computedStats = useMemo(() => {
    if (!orders) return { total: 0, pending: 0, accepted: 0, rejected: 0, unresolved: 0 };
    let pending = 0, accepted = 0, rejected = 0, unresolved = 0;
    for (const order of orders) {
      const claim = claimMap.get(order.orderNumber);
      const status = claim?.status || order.claimStatus;
      if (status === "WaitingInAction" || status === "Created" || status === "InAnalysis") pending++;
      else if (status === "Accepted") accepted++;
      else if (status === "Rejected") rejected++;
      else if (status === "Unresolved") unresolved++;
    }
    return { total: orders.length, pending, accepted, rejected, unresolved };
  }, [orders, claimMap]);

  // Client-side filtering
  const filteredOrders = useMemo(() => {
    if (!orders) return [];
    return orders.filter((order) => {
      if (searchTerm) {
        const term = searchTerm.toLowerCase();
        const matchesOrderNo = order.orderNumber.toLowerCase().includes(term);
        const matchesBarcode = order.items.some((item) => item.barcode.toLowerCase().includes(term));
        const matchesProduct = order.items.some((item) => item.productName?.toLowerCase().includes(term));
        if (!matchesOrderNo && !matchesBarcode && !matchesProduct) return false;
      }
      if (reasonFilter !== "ALL" && order.returnReason !== reasonFilter) return false;
      if (decisionFilter !== "ALL") {
        if (decisionFilter === "RESALABLE" && order.isResalable !== true) return false;
        if (decisionFilter === "NOT_RESALABLE" && order.isResalable !== false) return false;
        if (decisionFilter === "PENDING" && order.isResalable !== null) return false;
      }
      if (claimStatusFilter !== "ALL") {
        const claim = claimMap.get(order.orderNumber);
        const status = claim?.status || order.claimStatus;
        if (status !== claimStatusFilter) return false;
      }
      return true;
    });
  }, [orders, searchTerm, reasonFilter, decisionFilter, claimStatusFilter, claimMap]);

  const hasActiveFilters = searchTerm !== "" || reasonFilter !== "ALL" || decisionFilter !== "ALL" || claimStatusFilter !== "ALL";

  // Pagination
  const totalPages = Math.ceil(filteredOrders.length / PAGE_SIZE);
  const paginatedOrders = useMemo(() => {
    const start = page * PAGE_SIZE;
    return filteredOrders.slice(start, start + PAGE_SIZE);
  }, [filteredOrders, page]);

  // Reset page when filters change
  useEffect(() => { setPage(0); }, [searchTerm, reasonFilter, decisionFilter, claimStatusFilter]);

  // Toggle expanded row
  const toggleExpand = useCallback((orderNumber: string) => {
    setExpandedRows(prev => {
      const next = new Set(prev);
      if (next.has(orderNumber)) next.delete(orderNumber);
      else next.add(orderNumber);
      return next;
    });
  }, []);

  // Resalable decision
  const handleDecision = (orderNumber: string, isResalable: boolean) => {
    if (!storeId) return;
    updateResalable.mutate({ storeId, orderNumber, isResalable });
  };

  // Selection handlers
  const handleSelectOrder = useCallback((orderNumber: string, selected: boolean) => {
    setSelectedOrders(prev => {
      const next = new Set(prev);
      if (selected) next.add(orderNumber); else next.delete(orderNumber);
      return next;
    });
  }, []);

  const handleSelectAll = useCallback((selected: boolean) => {
    setSelectedOrders(selected ? new Set(filteredOrders.map((o) => o.orderNumber)) : new Set());
  }, [filteredOrders]);

  const [bulkLoading, setBulkLoading] = useState(false);
  const handleBulkDecision = useCallback(async (isResalable: boolean) => {
    if (!storeId || selectedOrders.size === 0) return;
    setBulkLoading(true);
    try {
      const promises = Array.from(selectedOrders).map((orderNumber) =>
        updateResalable.mutateAsync({ storeId, orderNumber, isResalable })
      );
      await Promise.allSettled(promises);
      setSelectedOrders(new Set());
    } finally {
      setBulkLoading(false);
    }
  }, [storeId, selectedOrders, updateResalable]);

  // Claim action handlers
  const handleApproveClaim = useCallback((claim: TrendyolClaim) => {
    if (!storeId) return;
    approveClaim.mutate({
      storeId,
      claimId: claim.claimId,
      claimLineItemIds: claim.items.map((item) => item.claimItemId),
    });
  }, [storeId, approveClaim]);

  const handleRejectClaim = useCallback((claim: TrendyolClaim) => {
    if (!storeId) return;
    rejectClaim.mutate({
      storeId,
      claimId: claim.claimId,
      reasonId: 1,
      claimItemIds: claim.items.map((item) => item.claimItemId),
    });
  }, [storeId, rejectClaim]);

  const allSelected = filteredOrders.length > 0 && selectedOrders.size === filteredOrders.length;
  const someSelected = selectedOrders.size > 0 && !allSelected;

  if (isLoading) {
    return <TableSkeleton columns={9} rows={8} />;
  }

  if (error) {
    return (
      <div className="text-center py-8 text-muted-foreground">
        Veriler yuklenirken hata olustu.
      </div>
    );
  }

  if (!orders || orders.length === 0) {
    return (
      <div className="text-center py-12 text-muted-foreground">
        <Package className="h-12 w-12 mx-auto mb-3 opacity-30" />
        <p className="text-lg font-medium">Secilen tarih araliginda iade bulunamadi</p>
        <p className="text-sm mt-1">Farkli bir tarih araligi secmeyi deneyin.</p>
      </div>
    );
  }

  const statCards = [
    { label: "Toplam Iade", value: computedStats.total, icon: Inbox, color: "text-foreground" },
    { label: "Bekleyen", value: computedStats.pending, icon: Clock, color: "text-amber-600" },
    { label: "Onaylanan", value: computedStats.accepted, icon: CheckCircle, color: "text-green-600" },
    { label: "Reddedilen", value: computedStats.rejected, icon: XCircle, color: "text-red-600" },
    { label: "Cozumsuz", value: computedStats.unresolved, icon: AlertTriangle, color: "text-purple-600" },
  ];

  return (
    <div className="space-y-4">
      {/* Claim Stats Cards */}
      <div className="grid grid-cols-2 sm:grid-cols-5 gap-3">
        {statCards.map((card) => (
          <div key={card.label} className="bg-card rounded-lg border border-border p-3 flex items-center gap-2">
            <card.icon className={cn("h-4 w-4", card.color)} />
            <div>
              <p className="text-xs text-muted-foreground">{card.label}</p>
              <p className={cn("text-lg font-bold", card.color)}>{card.value}</p>
            </div>
          </div>
        ))}
      </div>

      {/* Filter Bar */}
      <div className="flex items-center gap-3 flex-wrap">
        <div className="relative flex-1 min-w-[200px] max-w-[300px]">
          <Search className="absolute left-2.5 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="Siparis no, barkod, urun ara..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="pl-9 h-8 text-sm"
          />
        </div>

        <Select value={reasonFilter} onValueChange={setReasonFilter}>
          <SelectTrigger className="w-[180px] h-8 text-xs">
            <SelectValue placeholder="Iade Sebebi" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">Tum Sebepler</SelectItem>
            {uniqueReasons.map((reason) => (
              <SelectItem key={reason} value={reason}>{reason}</SelectItem>
            ))}
          </SelectContent>
        </Select>

        <Select value={decisionFilter} onValueChange={(v) => setDecisionFilter(v as DecisionFilter)}>
          <SelectTrigger className="w-[150px] h-8 text-xs">
            <SelectValue placeholder="Karar" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">Tum Kararlar</SelectItem>
            <SelectItem value="RESALABLE">Satilabilir</SelectItem>
            <SelectItem value="NOT_RESALABLE">Satilamaz</SelectItem>
            <SelectItem value="PENDING">Bekleyen</SelectItem>
          </SelectContent>
        </Select>

        <Select value={claimStatusFilter} onValueChange={(v) => setClaimStatusFilter(v as ClaimStatusFilter)}>
          <SelectTrigger className="w-[150px] h-8 text-xs">
            <SelectValue placeholder="Talep Durumu" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ALL">Tum Durumlar</SelectItem>
            <SelectItem value="WaitingInAction">Bekleyen</SelectItem>
            <SelectItem value="Accepted">Onaylanan</SelectItem>
            <SelectItem value="Rejected">Reddedilen</SelectItem>
            <SelectItem value="Unresolved">Cozumsuz</SelectItem>
          </SelectContent>
        </Select>

        <ReturnExportButton orders={filteredOrders} formatCurrency={formatCurrency} />
      </div>

      {/* Clear Filters */}
      {hasActiveFilters && (
        <div className="flex justify-end">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => {
              setSearchTerm("");
              setReasonFilter("ALL");
              setDecisionFilter("ALL");
              setClaimStatusFilter("ALL");
            }}
            className="h-7 text-xs"
          >
            Filtreleri Temizle
          </Button>
        </div>
      )}

      {/* Bulk Action Bar */}
      {selectedOrders.size > 0 && (
        <div className="flex items-center gap-3 bg-muted/50 border rounded-lg p-2.5">
          <span className="text-sm font-medium">{selectedOrders.size} iade secildi</span>
          <div className="flex items-center gap-2 ml-auto">
            <Button
              size="sm"
              variant="outline"
              className="h-7 text-xs bg-emerald-50 border-emerald-200 text-emerald-700 hover:bg-emerald-100 dark:bg-emerald-900/20 dark:border-emerald-800 dark:text-emerald-400"
              onClick={() => handleBulkDecision(true)}
              disabled={bulkLoading}
            >
              {bulkLoading ? <Loader2 className="h-3 w-3 mr-1 animate-spin" /> : <Check className="h-3 w-3 mr-1" />}
              Hepsini Satilabilir
            </Button>
            <Button
              size="sm"
              variant="outline"
              className="h-7 text-xs bg-red-50 border-red-200 text-red-700 hover:bg-red-100 dark:bg-red-900/20 dark:border-red-800 dark:text-red-400"
              onClick={() => handleBulkDecision(false)}
              disabled={bulkLoading}
            >
              {bulkLoading ? <Loader2 className="h-3 w-3 mr-1 animate-spin" /> : <X className="h-3 w-3 mr-1" />}
              Hepsini Satilamaz
            </Button>
            <Button
              size="sm"
              variant="ghost"
              className="h-7 text-xs"
              onClick={() => setSelectedOrders(new Set())}
            >
              Secimi Kaldir
            </Button>
          </div>
        </div>
      )}

      {/* Result Count & Pagination Info */}
      <div className="flex items-center justify-between text-sm text-muted-foreground">
        <p>
          Toplam {filteredOrders.length} iade{hasActiveFilters ? ` (${orders.length} icinden)` : ""}
        </p>
        {totalPages > 1 && (
          <p>Sayfa {page + 1} / {totalPages}</p>
        )}
      </div>

      {/* Unified Table */}
      <div className="rounded-lg border bg-card">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead className="w-[40px]">
                <Checkbox
                  checked={allSelected}
                  // @ts-expect-error - indeterminate is valid
                  indeterminate={someSelected}
                  onCheckedChange={(checked) => handleSelectAll(checked as boolean)}
                  disabled={filteredOrders.length === 0}
                />
              </TableHead>
              <TableHead className="w-[40px]"></TableHead>
              <TableHead className="w-[140px]">Siparis No</TableHead>
              <TableHead>Musteri</TableHead>
              <TableHead className="w-[100px]">Tarih</TableHead>
              <TableHead>Urunler</TableHead>
              <TableHead className="w-[120px]">Iade Sebebi</TableHead>
              <TableHead className="w-[90px] text-center">Talep</TableHead>
              <TableHead className="text-right w-[90px]">Kargo</TableHead>
              <TableHead className="text-right w-[100px]">Toplam Zarar</TableHead>
              <TableHead className="text-center w-[220px]">Islemler</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {filteredOrders.length === 0 ? (
              <TableRow>
                <TableCell colSpan={11} className="h-24 text-center text-muted-foreground">
                  Filtre kriterlerine uygun iade bulunamadi
                </TableCell>
              </TableRow>
            ) : (
              paginatedOrders.map((order) => {
                const claim = claimMap.get(order.orderNumber);
                const isMutating = updateResalable.isPending && updateResalable.variables?.orderNumber === order.orderNumber;
                const shippingTotal = (order.shippingCostOut || 0) + (order.shippingCostReturn || 0);
                const isExpanded = expandedRows.has(order.orderNumber);
                const claimStatus = claim?.status || order.claimStatus;

                return (
                  <React.Fragment key={order.orderNumber}>
                    <TableRow>
                      <TableCell>
                        <Checkbox
                          checked={selectedOrders.has(order.orderNumber)}
                          onCheckedChange={(checked) => handleSelectOrder(order.orderNumber, checked as boolean)}
                        />
                      </TableCell>
                      <TableCell>
                        {claim && (
                          <Button variant="ghost" size="sm" className="h-8 w-8 p-0" onClick={() => toggleExpand(order.orderNumber)}>
                            {isExpanded ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
                          </Button>
                        )}
                      </TableCell>
                      <TableCell>
                        <div>
                          <p className="font-mono text-xs">{order.orderNumber}</p>
                          {claim?.cargoTrackingNumber && (
                            <div className="flex items-center gap-1 mt-1">
                              <span className="text-[10px] text-muted-foreground truncate max-w-[100px]">
                                {claim.cargoTrackingNumber}
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
                      <TableCell className="text-sm">{order.customerName || "-"}</TableCell>
                      <TableCell className="text-sm text-muted-foreground">
                        {format(new Date(order.orderDate), "dd MMM yyyy", { locale: tr })}
                      </TableCell>
                      <TableCell>
                        <TooltipProvider>
                          <Tooltip>
                            <TooltipTrigger asChild>
                              <div className="text-sm max-w-[180px] truncate">
                                {order.items.map((item) => (
                                  <span key={item.barcode} className="block truncate text-xs">
                                    {item.quantity}x {item.productName}
                                  </span>
                                )).slice(0, 2)}
                                {order.items.length > 2 && (
                                  <span className="text-xs text-muted-foreground">+{order.items.length - 2} urun daha</span>
                                )}
                              </div>
                            </TooltipTrigger>
                            <TooltipContent side="bottom" className="max-w-[300px]">
                              {order.items.map((item) => (
                                <div key={item.barcode} className="text-xs py-0.5">
                                  {item.quantity}x {item.productName} ({formatCurrency(item.unitCost)})
                                </div>
                              ))}
                            </TooltipContent>
                          </Tooltip>
                        </TooltipProvider>
                      </TableCell>
                      <TableCell className="text-xs">{order.returnReason || "-"}</TableCell>
                      <TableCell className="text-center">
                        {claimStatus && (
                          <span className={cn(
                            "inline-flex items-center px-2 py-0.5 rounded-full text-[10px] font-medium",
                            getClaimStatusColor(claimStatus)
                          )}>
                            {getClaimStatusLabel(claimStatus)}
                          </span>
                        )}
                      </TableCell>
                      <TableCell className="text-right text-sm">
                        <TooltipProvider>
                          <Tooltip>
                            <TooltipTrigger>
                              <div className="flex items-center justify-end gap-1">
                                <Truck className="h-3 w-3 text-muted-foreground" />
                                {formatCurrency(shippingTotal)}
                              </div>
                            </TooltipTrigger>
                            <TooltipContent>
                              <div className="text-xs space-y-0.5">
                                <div>Gonderi: {formatCurrency(order.shippingCostOut)}</div>
                                <div>Iade: {formatCurrency(order.shippingCostReturn)}</div>
                              </div>
                            </TooltipContent>
                          </Tooltip>
                        </TooltipProvider>
                      </TableCell>
                      <TableCell className="text-right font-medium text-sm">
                        {formatCurrency(order.totalLoss)}
                      </TableCell>
                      <TableCell>
                        <div className="flex items-center justify-center gap-1">
                          {isMutating ? (
                            <Loader2 className="h-4 w-4 animate-spin text-muted-foreground" />
                          ) : (
                            <>
                              <Button
                                size="sm"
                                variant={order.isResalable === true ? "default" : "outline"}
                                className={cn("h-7 px-2 text-[10px]", order.isResalable === true && "bg-emerald-600 hover:bg-emerald-700 text-white")}
                                onClick={() => handleDecision(order.orderNumber, true)}
                                disabled={updateResalable.isPending}
                              >
                                <Check className="h-3 w-3 mr-0.5" />
                                Satilabilir
                              </Button>
                              <Button
                                size="sm"
                                variant={order.isResalable === false ? "default" : "outline"}
                                className={cn("h-7 px-2 text-[10px]", order.isResalable === false && "bg-red-600 hover:bg-red-700 text-white")}
                                onClick={() => handleDecision(order.orderNumber, false)}
                                disabled={updateResalable.isPending}
                              >
                                <X className="h-3 w-3 mr-0.5" />
                                Satilamaz
                              </Button>
                              {claim?.status === "WaitingInAction" && (
                                <>
                                  <div className="w-px h-5 bg-border mx-0.5" />
                                  <Button
                                    variant="ghost"
                                    size="sm"
                                    className="h-7 w-7 p-0 text-green-600 hover:text-green-700 hover:bg-green-50 dark:hover:bg-green-900/20"
                                    onClick={() => handleApproveClaim(claim)}
                                    disabled={approveClaim.isPending}
                                    title="Talebi Onayla"
                                  >
                                    <CheckCircle className="h-3.5 w-3.5" />
                                  </Button>
                                  <Button
                                    variant="ghost"
                                    size="sm"
                                    className="h-7 w-7 p-0 text-red-600 hover:text-red-700 hover:bg-red-50 dark:hover:bg-red-900/20"
                                    onClick={() => handleRejectClaim(claim)}
                                    title="Talebi Reddet"
                                  >
                                    <XCircle className="h-3.5 w-3.5" />
                                  </Button>
                                </>
                              )}
                            </>
                          )}
                        </div>
                      </TableCell>
                    </TableRow>
                    {/* Expanded claim items */}
                    {isExpanded && claim && (
                      <TableRow>
                        <TableCell colSpan={11} className="p-0">
                          <div className="px-4 pb-4">
                            <ClaimItemsExpanded items={claim.items} storeId={storeId} />
                          </div>
                        </TableCell>
                      </TableRow>
                    )}
                  </React.Fragment>
                );
              })
            )}
          </TableBody>
        </Table>
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-end gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={page === 0}
            className="h-8"
          >
            Onceki
          </Button>
          {Array.from({ length: Math.min(totalPages, 7) }, (_, i) => {
            let pageNum: number;
            if (totalPages <= 7) {
              pageNum = i;
            } else if (page < 3) {
              pageNum = i;
            } else if (page > totalPages - 4) {
              pageNum = totalPages - 7 + i;
            } else {
              pageNum = page - 3 + i;
            }
            return (
              <Button
                key={pageNum}
                variant={page === pageNum ? "default" : "outline"}
                size="sm"
                onClick={() => setPage(pageNum)}
                className="h-8 w-8 p-0 text-xs"
              >
                {pageNum + 1}
              </Button>
            );
          })}
          <Button
            variant="outline"
            size="sm"
            onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
            disabled={page >= totalPages - 1}
            className="h-8"
          >
            Sonraki
          </Button>
        </div>
      )}
    </div>
  );
}
