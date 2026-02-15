"use client";

import React, { useState, useCallback } from "react";
import { useSelectedStore } from "@/hooks/queries/use-stores";
import {
  useClaims,
  useClaimsStats,
  useApproveClaim,
  useRejectClaim,
  useBulkApproveClaims,
} from "@/hooks/queries/use-returns";
import { ClaimsTable } from "./claims-table";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import {
  Inbox,
  CheckCircle,
  XCircle,
  AlertTriangle,
  Clock,
} from "lucide-react";
import { cn } from "@/lib/utils";
import type { TrendyolClaim, ClaimStatus } from "@/types/returns";

type StatusFilter = "ALL" | "WaitingInAction" | "Accepted" | "Rejected" | "Unresolved";

const STATUS_FILTERS: { value: StatusFilter; label: string }[] = [
  { value: "ALL", label: "Tumu" },
  { value: "WaitingInAction", label: "Bekleyen" },
  { value: "Accepted", label: "Onaylanan" },
  { value: "Rejected", label: "Reddedilen" },
  { value: "Unresolved", label: "Cozumsuz" },
];

function getStatusColor(status: ClaimStatus): string {
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

function getStatusLabel(status: ClaimStatus): string {
  switch (status) {
    case "Created":
      return "Olusturuldu";
    case "WaitingInAction":
      return "Bekliyor";
    case "Accepted":
      return "Onaylandi";
    case "Rejected":
      return "Reddedildi";
    case "Unresolved":
      return "Cozumsuz";
    case "Cancelled":
      return "Iptal";
    case "InAnalysis":
      return "Inceleniyor";
    default:
      return status;
  }
}

export function ClaimsManagement() {
  const { data: selectedStore } = useSelectedStore();
  const storeId = selectedStore?.selectedStoreId;

  const [statusFilter, setStatusFilter] = useState<StatusFilter>("ALL");
  const [page, setPage] = useState(0);
  const [selectedClaims, setSelectedClaims] = useState<Set<string>>(new Set());

  // API filter param
  const filterParam = statusFilter === "ALL" ? undefined : statusFilter;

  const { data: stats, isLoading: statsLoading } = useClaimsStats(storeId);
  const { data: claimsPage, isLoading: claimsLoading } = useClaims(storeId, filterParam, page);
  const approveClaim = useApproveClaim();
  const rejectClaim = useRejectClaim();
  const bulkApproveClaims = useBulkApproveClaims();

  const claims = claimsPage?.content || [];
  const totalPages = claimsPage?.totalPages || 0;
  const totalElements = claimsPage?.totalElements || 0;

  // Selectable claims = WaitingInAction
  const selectableClaims = claims.filter((c) => c.status === "WaitingInAction");

  const handleSelectClaim = useCallback((claimId: string, selected: boolean) => {
    setSelectedClaims((prev) => {
      const next = new Set(prev);
      if (selected) {
        next.add(claimId);
      } else {
        next.delete(claimId);
      }
      return next;
    });
  }, []);

  const handleSelectAll = useCallback(
    (selected: boolean) => {
      if (selected) {
        setSelectedClaims(new Set(selectableClaims.map((c) => c.id)));
      } else {
        setSelectedClaims(new Set());
      }
    },
    [selectableClaims]
  );

  const handleApprove = useCallback(
    (claim: TrendyolClaim) => {
      if (!storeId) return;
      const itemIds = claim.items.map((item) => item.claimItemId);
      approveClaim.mutate({
        storeId,
        claimId: claim.claimId,
        claimLineItemIds: itemIds,
      });
    },
    [storeId, approveClaim]
  );

  const handleReject = useCallback(
    (claim: TrendyolClaim) => {
      if (!storeId) return;
      const itemIds = claim.items.map((item) => item.claimItemId);
      rejectClaim.mutate({
        storeId,
        claimId: claim.claimId,
        reasonId: 1,
        claimItemIds: itemIds,
      });
    },
    [storeId, rejectClaim]
  );

  const handleBulkApprove = useCallback(() => {
    if (!storeId || selectedClaims.size === 0) return;
    const claimsToApprove = claims
      .filter((c) => selectedClaims.has(c.id) && c.status === "WaitingInAction")
      .map((c) => ({
        claimId: c.claimId,
        claimLineItemIds: c.items.map((item) => item.claimItemId),
      }));
    bulkApproveClaims.mutate(
      { storeId, claims: claimsToApprove },
      { onSuccess: () => setSelectedClaims(new Set()) }
    );
  }, [storeId, selectedClaims, claims, bulkApproveClaims]);

  const handleViewDetail = useCallback((_claim: TrendyolClaim) => {
    // Could open a detail panel/modal in the future
  }, []);

  const statCards = [
    {
      label: "Toplam",
      value: stats?.totalClaims ?? 0,
      icon: Inbox,
      color: "text-foreground",
    },
    {
      label: "Bekleyen",
      value: stats?.pendingClaims ?? 0,
      icon: Clock,
      color: "text-amber-600",
    },
    {
      label: "Onaylanan",
      value: stats?.acceptedClaims ?? 0,
      icon: CheckCircle,
      color: "text-green-600",
    },
    {
      label: "Reddedilen",
      value: stats?.rejectedClaims ?? 0,
      icon: XCircle,
      color: "text-red-600",
    },
    {
      label: "Cozumsuz",
      value: stats?.unresolvedClaims ?? 0,
      icon: AlertTriangle,
      color: "text-purple-600",
    },
  ];

  return (
    <div className="space-y-4">
      {/* Stats Bar */}
      <div className="grid grid-cols-2 sm:grid-cols-5 gap-3">
        {statCards.map((card) =>
          statsLoading ? (
            <div key={card.label} className="bg-card rounded-lg border border-border p-3">
              <Skeleton className="h-4 w-16 mb-1" />
              <Skeleton className="h-6 w-10" />
            </div>
          ) : (
            <div
              key={card.label}
              className="bg-card rounded-lg border border-border p-3 flex items-center gap-2"
            >
              <card.icon className={cn("h-4 w-4", card.color)} />
              <div>
                <p className="text-xs text-muted-foreground">{card.label}</p>
                <p className={cn("text-lg font-bold", card.color)}>{card.value}</p>
              </div>
            </div>
          )
        )}
      </div>

      {/* Actions Bar */}
      <div className="flex items-center justify-between gap-2 flex-wrap">
        <div className="flex items-center gap-1.5 flex-wrap">
          {STATUS_FILTERS.map((filter) => (
            <Button
              key={filter.value}
              variant={statusFilter === filter.value ? "default" : "outline"}
              size="sm"
              onClick={() => {
                setStatusFilter(filter.value);
                setPage(0);
                setSelectedClaims(new Set());
              }}
              className="h-8 text-xs"
            >
              {filter.label}
            </Button>
          ))}
        </div>
        <div className="flex items-center gap-2">
          {selectedClaims.size > 0 && (
            <Button
              size="sm"
              onClick={handleBulkApprove}
              disabled={bulkApproveClaims.isPending}
              className="h-8 text-xs bg-green-600 hover:bg-green-700 text-white"
            >
              <CheckCircle className="h-3.5 w-3.5 mr-1" />
              {selectedClaims.size} Talep Onayla
            </Button>
          )}
        </div>
      </div>

      {/* Claims Table */}
      <ClaimsTable
        claims={claims}
        isLoading={claimsLoading}
        selectedClaims={selectedClaims}
        onSelectClaim={handleSelectClaim}
        onSelectAll={handleSelectAll}
        selectableCount={selectableClaims.length}
        onApprove={handleApprove}
        onReject={handleReject}
        onViewDetail={handleViewDetail}
        getStatusColor={getStatusColor}
        getStatusLabel={getStatusLabel}
        approvePending={approveClaim.isPending}
        storeId={storeId}
      />

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-between text-sm">
          <p className="text-muted-foreground">
            Toplam {totalElements} talep, Sayfa {page + 1} / {totalPages}
          </p>
          <div className="flex items-center gap-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0}
              className="h-8"
            >
              Onceki
            </Button>
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
        </div>
      )}
    </div>
  );
}
