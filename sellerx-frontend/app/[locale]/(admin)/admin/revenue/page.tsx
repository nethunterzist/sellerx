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
  useAdminRevenueStats,
  useAdminRevenueHistory,
  useAdminPayments,
} from "@/hooks/queries/use-admin-billing";
import {
  TrendingUp,
  DollarSign,
  BarChart3,
  ChevronLeft,
  ChevronRight,
  RefreshCw,
  Info,
} from "lucide-react";
import { format } from "date-fns";
import { tr } from "date-fns/locale";
import {
  ResponsiveContainer,
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
} from "recharts";

const formatCurrency = (value: number) =>
  new Intl.NumberFormat("tr-TR", {
    style: "currency",
    currency: "TRY",
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(value);

function formatDate(date: string | null) {
  if (!date) return "-";
  try {
    return format(new Date(date), "dd MMM yyyy, HH:mm", { locale: tr });
  } catch {
    return "-";
  }
}

function getPaymentStatusBadge(status: string) {
  switch (status) {
    case "COMPLETED":
    case "PAID":
      return (
        <Badge className="bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400 hover:bg-emerald-100">
          Odendi
        </Badge>
      );
    case "PENDING":
      return (
        <Badge className="bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400 hover:bg-amber-100">
          Bekliyor
        </Badge>
      );
    case "FAILED":
      return (
        <Badge className="bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400 hover:bg-red-100">
          Basarisiz
        </Badge>
      );
    case "REFUNDED":
      return (
        <Badge className="bg-slate-100 text-slate-700 dark:bg-slate-900/30 dark:text-slate-400 hover:bg-slate-100">
          Iade
        </Badge>
      );
    default:
      return <Badge variant="secondary">{status}</Badge>;
  }
}

export default function AdminRevenuePage() {
  const [paymentsPage, setPaymentsPage] = useState(0);

  const {
    data: revenueStats,
    isLoading: isLoadingStats,
    refetch: refetchStats,
    isFetching: isFetchingStats,
  } = useAdminRevenueStats();
  const { data: revenueHistory, isLoading: isLoadingHistory } =
    useAdminRevenueHistory();
  const { data: payments, isLoading: isLoadingPayments } =
    useAdminPayments(paymentsPage);

  const totalPaymentPages = payments?.totalPages || 1;

  const kpiCards = [
    {
      title: "Aylik Tekrarlayan Gelir (MRR)",
      value: revenueStats?.mrr,
      icon: DollarSign,
      color: "text-emerald-500",
      bg: "bg-emerald-500/10",
      format: formatCurrency,
    },
    {
      title: "Yillik Tekrarlayan Gelir (ARR)",
      value: revenueStats?.arr,
      icon: TrendingUp,
      color: "text-blue-500",
      bg: "bg-blue-500/10",
      format: formatCurrency,
    },
    {
      title: "Churn Orani",
      value: revenueStats?.churnRate,
      icon: BarChart3,
      color: "text-amber-500",
      bg: "bg-amber-500/10",
      format: (v: number) => `%${v.toFixed(1)}`,
    },
  ];

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-white">
            Gelir
          </h1>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
            Gelir metrikleri, grafik ve odeme gecmisi
          </p>
        </div>
        <Button
          onClick={() => refetchStats()}
          variant="outline"
          size="sm"
          disabled={isFetchingStats}
        >
          <RefreshCw
            className={`h-4 w-4 mr-2 ${isFetchingStats ? "animate-spin" : ""}`}
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
              Gelir verileri ornek olarak gosterilmektedir.
            </p>
          </div>
        </div>
      )}

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        {kpiCards.map((card) => (
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
                    <Skeleton className="h-7 w-24 mt-1" />
                  ) : (
                    <p className="text-2xl font-bold text-slate-900 dark:text-white">
                      {card.value !== undefined && card.value !== null
                        ? card.format(card.value)
                        : "-"}
                    </p>
                  )}
                </div>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>

      {/* Revenue Chart */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-lg">
            <BarChart3 className="h-5 w-5" />
            Aylik Gelir Grafigi
          </CardTitle>
        </CardHeader>
        <CardContent>
          {isLoadingHistory ? (
            <Skeleton className="h-[350px] w-full" />
          ) : revenueHistory && revenueHistory.length > 0 ? (
            <ResponsiveContainer width="100%" height={350}>
              <LineChart data={revenueHistory}>
                <CartesianGrid strokeDasharray="3 3" stroke="#334155" />
                <XAxis
                  dataKey="month"
                  stroke="#94a3b8"
                  tick={{ fill: "#94a3b8", fontSize: 12 }}
                />
                <YAxis
                  stroke="#94a3b8"
                  tick={{ fill: "#94a3b8", fontSize: 12 }}
                  tickFormatter={(v) =>
                    new Intl.NumberFormat("tr-TR", {
                      notation: "compact",
                    }).format(v)
                  }
                />
                <Tooltip
                  contentStyle={{
                    backgroundColor: "#1e293b",
                    border: "1px solid #334155",
                    borderRadius: "8px",
                    color: "#f1f5f9",
                  }}
                  formatter={(value: number) => [formatCurrency(value), "Gelir"]}
                />
                <Legend />
                <Line
                  type="monotone"
                  dataKey="revenue"
                  name="Gelir"
                  stroke="#10b981"
                  strokeWidth={2}
                  dot={{ fill: "#10b981", r: 4 }}
                  activeDot={{ r: 6 }}
                />
              </LineChart>
            </ResponsiveContainer>
          ) : (
            <div className="flex items-center justify-center h-[350px] text-slate-500 dark:text-slate-400">
              <p>Gelir verisi bulunamadi</p>
            </div>
          )}
        </CardContent>
      </Card>

      {/* Payments Table */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-lg">
            <DollarSign className="h-5 w-5" />
            Odeme Gecmisi
          </CardTitle>
        </CardHeader>
        <CardContent>
          {isLoadingPayments ? (
            <div className="space-y-4">
              {[...Array(5)].map((_, i) => (
                <Skeleton key={i} className="h-16 w-full" />
              ))}
            </div>
          ) : !payments?.content || payments.content.length === 0 ? (
            <div className="text-center py-12 text-slate-500 dark:text-slate-400">
              <DollarSign className="h-12 w-12 mx-auto mb-3 opacity-30" />
              <p>Odeme kaydi bulunamadi</p>
            </div>
          ) : (
            <>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Kullanici</TableHead>
                    <TableHead>Tutar</TableHead>
                    <TableHead>Durum</TableHead>
                    <TableHead>Aciklama</TableHead>
                    <TableHead>Tarih</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {payments.content.map((payment) => (
                    <TableRow key={payment.id}>
                      <TableCell className="font-medium text-slate-900 dark:text-white">
                        {payment.userEmail}
                      </TableCell>
                      <TableCell className="font-semibold text-slate-900 dark:text-white">
                        {formatCurrency(payment.amount)}
                      </TableCell>
                      <TableCell>
                        {getPaymentStatusBadge(payment.status)}
                      </TableCell>
                      <TableCell className="text-sm text-slate-500 dark:text-slate-400 max-w-[200px] truncate">
                        {payment.description || "-"}
                      </TableCell>
                      <TableCell className="text-sm text-slate-500 dark:text-slate-400">
                        {formatDate(payment.createdAt)}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>

              {/* Pagination */}
              <div className="flex items-center justify-between mt-4">
                <p className="text-sm text-slate-500">
                  Sayfa {paymentsPage + 1} / {totalPaymentPages} (
                  {payments.totalElements || 0} kayit)
                </p>
                <div className="flex items-center gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() =>
                      setPaymentsPage(Math.max(0, paymentsPage - 1))
                    }
                    disabled={paymentsPage === 0}
                  >
                    <ChevronLeft className="h-4 w-4" />
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() =>
                      setPaymentsPage(
                        Math.min(totalPaymentPages - 1, paymentsPage + 1)
                      )
                    }
                    disabled={paymentsPage >= totalPaymentPages - 1}
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
