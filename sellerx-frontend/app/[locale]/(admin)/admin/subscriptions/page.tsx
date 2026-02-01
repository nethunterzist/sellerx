"use client";

import { useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Skeleton } from "@/components/ui/skeleton";
import {
  useAdminSubscriptions,
  useAdminRevenueStats,
} from "@/hooks/queries/use-admin-billing";
import {
  ChevronLeft,
  ChevronRight,
  CreditCard,
  Users,
  Clock,
  AlertTriangle,
  XCircle,
  Info,
  RefreshCw,
} from "lucide-react";
import { format } from "date-fns";
import { tr } from "date-fns/locale";

const statusFilters = [
  { label: "Tumu", value: "" },
  { label: "Aktif", value: "ACTIVE" },
  { label: "Deneme", value: "TRIALING" },
  { label: "Gecikme", value: "PAST_DUE" },
  { label: "Iptal", value: "CANCELLED" },
];

function getStatusBadge(status: string) {
  switch (status) {
    case "ACTIVE":
      return (
        <Badge className="bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400 hover:bg-emerald-100">
          Aktif
        </Badge>
      );
    case "TRIALING":
      return (
        <Badge className="bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400 hover:bg-blue-100">
          Deneme
        </Badge>
      );
    case "PAST_DUE":
      return (
        <Badge className="bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400 hover:bg-amber-100">
          Gecikme
        </Badge>
      );
    case "CANCELLED":
      return (
        <Badge className="bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400 hover:bg-red-100">
          Iptal
        </Badge>
      );
    default:
      return <Badge variant="secondary">{status}</Badge>;
  }
}

function formatDate(date: string | null) {
  if (!date) return "-";
  try {
    return format(new Date(date), "dd MMM yyyy, HH:mm", { locale: tr });
  } catch {
    return "-";
  }
}

export default function AdminSubscriptionsPage() {
  const [page, setPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState("");

  const { data: revenueStats, isLoading: isLoadingStats } =
    useAdminRevenueStats();
  const { data: subscriptionsPage, isLoading: isLoadingSubs, refetch, isFetching } =
    useAdminSubscriptions({
      status: statusFilter || undefined,
      page,
      size: 20,
    });

  const totalPages = subscriptionsPage?.totalPages || 1;
  const subscriptions = subscriptionsPage?.content || [];

  const statCards = [
    {
      title: "Aktif Abonelik",
      value: revenueStats?.activeSubscriptions,
      icon: Users,
      color: "text-emerald-500",
      bg: "bg-emerald-500/10",
    },
    {
      title: "Deneme Suresi",
      value: revenueStats?.trialSubscriptions,
      icon: Clock,
      color: "text-blue-500",
      bg: "bg-blue-500/10",
    },
    {
      title: "Odeme Gecikmesi",
      value: revenueStats?.pastDueSubscriptions,
      icon: AlertTriangle,
      color: "text-amber-500",
      bg: "bg-amber-500/10",
    },
    {
      title: "Iptal Edilen",
      value: revenueStats?.cancelledSubscriptions,
      icon: XCircle,
      color: "text-red-500",
      bg: "bg-red-500/10",
    },
  ];

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-white">
            Abonelikler
          </h1>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
            Tum kullanici aboneliklerini yonetin
          </p>
        </div>
        <Button
          onClick={() => refetch()}
          variant="outline"
          size="sm"
          disabled={isFetching}
        >
          <RefreshCw
            className={`h-4 w-4 mr-2 ${isFetching ? "animate-spin" : ""}`}
          />
          Yenile
        </Button>
      </div>

      {/* Billing Disabled Warning */}
      {revenueStats && !revenueStats.billingEnabled && (
        <div className="flex items-center gap-3 rounded-lg border border-amber-200 bg-amber-50 p-4 dark:border-amber-900/50 dark:bg-amber-950/30">
          <Info className="h-5 w-5 text-amber-500 shrink-0" />
          <div>
            <p className="text-sm font-medium text-amber-800 dark:text-amber-300">
              Faturalandirma sistemi aktif degil
            </p>
            <p className="text-sm text-amber-600 dark:text-amber-400">
              Abonelik ve odeme islemleri henuz etkinlestirilmemistir. Veriler
              ornek olarak gosterilmektedir.
            </p>
          </div>
        </div>
      )}

      {/* Stat Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {statCards.map((card) => (
          <Card key={card.title}>
            <CardContent className="pt-6">
              <div className="flex items-center gap-4">
                <div className={`p-3 rounded-lg ${card.bg}`}>
                  <card.icon className={`h-5 w-5 ${card.color}`} />
                </div>
                <div>
                  <p className="text-sm text-slate-500 dark:text-slate-400">
                    {card.title}
                  </p>
                  {isLoadingStats ? (
                    <Skeleton className="h-7 w-16 mt-1" />
                  ) : (
                    <p className="text-2xl font-bold text-slate-900 dark:text-white">
                      {card.value ?? 0}
                    </p>
                  )}
                </div>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Table */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle className="flex items-center gap-2 text-lg">
              <CreditCard className="h-5 w-5" />
              Abonelik Listesi
            </CardTitle>
            <div className="flex items-center gap-2">
              {statusFilters.map((f) => (
                <Button
                  key={f.value}
                  variant={statusFilter === f.value ? "default" : "outline"}
                  size="sm"
                  onClick={() => {
                    setStatusFilter(f.value);
                    setPage(0);
                  }}
                >
                  {f.label}
                </Button>
              ))}
            </div>
          </div>
        </CardHeader>
        <CardContent>
          {isLoadingSubs ? (
            <div className="space-y-4">
              {[...Array(5)].map((_, i) => (
                <Skeleton key={i} className="h-16 w-full" />
              ))}
            </div>
          ) : subscriptions.length === 0 ? (
            <div className="text-center py-12 text-slate-500 dark:text-slate-400">
              <CreditCard className="h-12 w-12 mx-auto mb-3 opacity-30" />
              <p>Abonelik bulunamadi</p>
            </div>
          ) : (
            <>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Kullanici</TableHead>
                    <TableHead>Plan</TableHead>
                    <TableHead>Durum</TableHead>
                    <TableHead>Periyot</TableHead>
                    <TableHead>Bitis Tarihi</TableHead>
                    <TableHead>Olusturulma</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {subscriptions.map((sub) => (
                    <TableRow key={sub.id}>
                      <TableCell>
                        <div>
                          <p className="font-medium text-slate-900 dark:text-white">
                            {sub.userName || "-"}
                          </p>
                          <p className="text-sm text-slate-500">
                            {sub.userEmail}
                          </p>
                        </div>
                      </TableCell>
                      <TableCell>
                        <Badge variant="secondary">{sub.planName}</Badge>
                      </TableCell>
                      <TableCell>{getStatusBadge(sub.status)}</TableCell>
                      <TableCell className="text-sm text-slate-500 dark:text-slate-400">
                        {sub.billingCycle === "MONTHLY"
                          ? "Aylik"
                          : sub.billingCycle === "YEARLY"
                          ? "Yillik"
                          : sub.billingCycle}
                      </TableCell>
                      <TableCell className="text-sm text-slate-500 dark:text-slate-400">
                        {formatDate(sub.currentPeriodEnd)}
                      </TableCell>
                      <TableCell className="text-sm text-slate-500 dark:text-slate-400">
                        {formatDate(sub.createdAt)}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>

              {/* Pagination */}
              <div className="flex items-center justify-between mt-4">
                <p className="text-sm text-slate-500">
                  Sayfa {page + 1} / {totalPages} (
                  {subscriptionsPage?.totalElements || 0} kayit)
                </p>
                <div className="flex items-center gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setPage(Math.max(0, page - 1))}
                    disabled={page === 0}
                  >
                    <ChevronLeft className="h-4 w-4" />
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() =>
                      setPage(Math.min(totalPages - 1, page + 1))
                    }
                    disabled={page >= totalPages - 1}
                  >
                    <ChevronRight className="h-4 w-4" />
                  </Button>
                </div>
              </div>
            </>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
