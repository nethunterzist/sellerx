"use client";

import { useState } from "react";
import {
  useAdminReferrals,
  useAdminReferralStats,
} from "@/hooks/queries/use-admin-referrals";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
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
  ChevronLeft,
  ChevronRight,
  Users,
  UserCheck,
  Clock,
  Gift,
  Trophy,
} from "lucide-react";

export default function AdminReferralsPage() {
  const [page, setPage] = useState(0);

  const { data: stats, isLoading: statsLoading } = useAdminReferralStats();
  const { data: referralsData, isLoading: referralsLoading } =
    useAdminReferrals(page);

  const referrals = referralsData?.content || referralsData?.referrals || [];
  const topReferrers = stats?.topReferrers || [];
  const totalPages = referralsData?.totalPages || 1;

  const getStatusBadge = (status: string) => {
    switch (status?.toLowerCase()) {
      case "completed":
        return (
          <Badge className="bg-green-900/30 text-green-400 border-green-700/50 hover:bg-green-900/30">
            Tamamlandi
          </Badge>
        );
      case "pending":
        return (
          <Badge className="bg-yellow-900/30 text-yellow-400 border-yellow-700/50 hover:bg-yellow-900/30">
            Bekliyor
          </Badge>
        );
      case "expired":
        return (
          <Badge className="bg-slate-800 text-slate-400 border-slate-700/50 hover:bg-slate-800">
            Suresi Doldu
          </Badge>
        );
      case "cancelled":
        return (
          <Badge className="bg-red-900/30 text-red-400 border-red-700/50 hover:bg-red-900/30">
            Iptal Edildi
          </Badge>
        );
      default:
        return (
          <Badge className="bg-slate-800 text-slate-300 border-slate-700/50 hover:bg-slate-800">
            {status || "Bilinmiyor"}
          </Badge>
        );
    }
  };

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div>
        <h1 className="text-2xl font-bold text-white">Referanslar</h1>
        <p className="text-sm text-slate-400 mt-1">
          Referans sistemi istatistikleri ve kayitlari
        </p>
      </div>

      {/* Stat Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card className="bg-slate-900 border-slate-700/50">
          <CardContent className="pt-6">
            {statsLoading ? (
              <Skeleton className="h-16 w-full bg-slate-800" />
            ) : (
              <div className="flex items-center gap-4">
                <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-blue-900/30 text-blue-400">
                  <Users className="h-6 w-6" />
                </div>
                <div>
                  <p className="text-sm text-slate-400">Toplam Referans</p>
                  <p className="text-2xl font-bold text-white">
                    {stats?.totalReferrals ?? 0}
                  </p>
                </div>
              </div>
            )}
          </CardContent>
        </Card>

        <Card className="bg-slate-900 border-slate-700/50">
          <CardContent className="pt-6">
            {statsLoading ? (
              <Skeleton className="h-16 w-full bg-slate-800" />
            ) : (
              <div className="flex items-center gap-4">
                <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-green-900/30 text-green-400">
                  <UserCheck className="h-6 w-6" />
                </div>
                <div>
                  <p className="text-sm text-slate-400">Tamamlanan</p>
                  <p className="text-2xl font-bold text-white">
                    {stats?.completedReferrals ?? 0}
                  </p>
                </div>
              </div>
            )}
          </CardContent>
        </Card>

        <Card className="bg-slate-900 border-slate-700/50">
          <CardContent className="pt-6">
            {statsLoading ? (
              <Skeleton className="h-16 w-full bg-slate-800" />
            ) : (
              <div className="flex items-center gap-4">
                <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-yellow-900/30 text-yellow-400">
                  <Clock className="h-6 w-6" />
                </div>
                <div>
                  <p className="text-sm text-slate-400">Bekleyen</p>
                  <p className="text-2xl font-bold text-white">
                    {stats?.pendingReferrals ?? 0}
                  </p>
                </div>
              </div>
            )}
          </CardContent>
        </Card>

        <Card className="bg-slate-900 border-slate-700/50">
          <CardContent className="pt-6">
            {statsLoading ? (
              <Skeleton className="h-16 w-full bg-slate-800" />
            ) : (
              <div className="flex items-center gap-4">
                <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-purple-900/30 text-purple-400">
                  <Gift className="h-6 w-6" />
                </div>
                <div>
                  <p className="text-sm text-slate-400">Toplam Odul Gunu</p>
                  <p className="text-2xl font-bold text-white">
                    {stats?.totalRewardDays ?? 0}
                  </p>
                </div>
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Top Referrers */}
      {topReferrers.length > 0 && (
        <Card className="bg-slate-900 border-slate-700/50">
          <CardHeader>
            <CardTitle className="flex items-center gap-2 text-lg text-white">
              <Trophy className="h-5 w-5 text-yellow-400" />
              En Cok Referans Yapanlar
            </CardTitle>
          </CardHeader>
          <CardContent>
            {statsLoading ? (
              <div className="space-y-4">
                {[...Array(5)].map((_, i) => (
                  <Skeleton key={i} className="h-12 w-full bg-slate-800" />
                ))}
              </div>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow className="border-slate-700/50 hover:bg-transparent">
                    <TableHead className="text-slate-400">Email</TableHead>
                    <TableHead className="text-slate-400 text-right">
                      Referans Sayisi
                    </TableHead>
                    <TableHead className="text-slate-400 text-right">
                      Toplam Odul Gunu
                    </TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {topReferrers.map((referrer: any, index: number) => (
                    <TableRow
                      key={index}
                      className="border-slate-700/50 hover:bg-slate-800/50"
                    >
                      <TableCell className="text-white font-medium">
                        {referrer.email}
                      </TableCell>
                      <TableCell className="text-right text-slate-300">
                        {referrer.referralCount}
                      </TableCell>
                      <TableCell className="text-right text-purple-400 font-semibold">
                        {referrer.totalRewardDays}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </CardContent>
        </Card>
      )}

      {/* All Referrals */}
      <Card className="bg-slate-900 border-slate-700/50">
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-lg text-white">
            <Users className="h-5 w-5" />
            Tum Referanslar
          </CardTitle>
        </CardHeader>
        <CardContent>
          {referralsLoading ? (
            <div className="space-y-4">
              {[...Array(8)].map((_, i) => (
                <Skeleton key={i} className="h-14 w-full bg-slate-800" />
              ))}
            </div>
          ) : referrals.length === 0 ? (
            <div className="text-center py-12 text-slate-400">
              <Users className="h-12 w-12 mx-auto mb-4 opacity-50" />
              <p>Referans kaydı bulunamadı</p>
            </div>
          ) : (
            <>
              <Table>
                <TableHeader>
                  <TableRow className="border-slate-700/50 hover:bg-transparent">
                    <TableHead className="text-slate-400">
                      Referans Veren
                    </TableHead>
                    <TableHead className="text-slate-400">
                      Referans Alan
                    </TableHead>
                    <TableHead className="text-slate-400">Kod</TableHead>
                    <TableHead className="text-slate-400">Durum</TableHead>
                    <TableHead className="text-slate-400 text-right">
                      Odul Gunu
                    </TableHead>
                    <TableHead className="text-slate-400">Tarih</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {referrals.map((ref: any) => (
                    <TableRow
                      key={ref.id}
                      className="border-slate-700/50 hover:bg-slate-800/50"
                    >
                      <TableCell className="text-white font-medium">
                        {ref.referrerEmail}
                      </TableCell>
                      <TableCell className="text-slate-300">
                        {ref.referredEmail}
                      </TableCell>
                      <TableCell className="text-slate-300 font-mono text-sm">
                        {ref.referralCode || ref.code}
                      </TableCell>
                      <TableCell>{getStatusBadge(ref.status)}</TableCell>
                      <TableCell className="text-right text-purple-400 font-semibold">
                        {ref.rewardDays ?? 0}
                      </TableCell>
                      <TableCell className="text-slate-400 text-sm whitespace-nowrap">
                        {ref.createdAt
                          ? new Date(ref.createdAt).toLocaleString("tr-TR")
                          : "-"}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>

              {/* Pagination */}
              <div className="flex items-center justify-between mt-4">
                <p className="text-sm text-slate-400">
                  Sayfa {page + 1} / {totalPages}
                </p>
                <div className="flex items-center gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setPage(Math.max(0, page - 1))}
                    disabled={page === 0}
                    className="border-slate-700 text-slate-300 hover:bg-slate-800"
                  >
                    <ChevronLeft className="h-4 w-4" />
                  </Button>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
                    disabled={page >= totalPages - 1}
                    className="border-slate-700 text-slate-300 hover:bg-slate-800"
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
