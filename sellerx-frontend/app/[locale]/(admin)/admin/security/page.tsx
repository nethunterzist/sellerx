"use client";

import { useAdminSecuritySummary } from "@/hooks/queries/use-admin-activity";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
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
  ShieldAlert,
  AlertTriangle,
  Shield,
  LogIn,
  Globe,
  UserX,
} from "lucide-react";

export default function AdminSecurityPage() {
  const { data, isLoading } = useAdminSecuritySummary();

  const failedLogins24h = data?.failedLogins24h ?? 0;
  const failedLogins7d = data?.failedLogins7d ?? 0;
  const totalLoginsToday = data?.totalLoginsToday ?? 0;
  const suspiciousIps = data?.suspiciousIps || [];
  const suspiciousAccounts = data?.suspiciousAccounts || [];

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div>
        <h1 className="text-2xl font-bold text-white">Guvenlik Ozeti</h1>
        <p className="text-sm text-slate-400 mt-1">
          Basarisiz giris denemeleri ve suphe yaratan aktiviteler
        </p>
      </div>

      {/* Alert Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {/* Failed Logins 24h */}
        <Card
          className={`border-slate-700/50 ${
            failedLogins24h > 10
              ? "bg-red-950/50 border-red-700/50"
              : "bg-slate-900"
          }`}
        >
          <CardContent className="pt-6">
            {isLoading ? (
              <Skeleton className="h-20 w-full bg-slate-800" />
            ) : (
              <div className="flex items-center gap-4">
                <div
                  className={`flex h-12 w-12 items-center justify-center rounded-xl ${
                    failedLogins24h > 10
                      ? "bg-red-900/50 text-red-400"
                      : "bg-orange-900/30 text-orange-400"
                  }`}
                >
                  <AlertTriangle className="h-6 w-6" />
                </div>
                <div>
                  <p className="text-sm text-slate-400">
                    Basarisiz Giris (24s)
                  </p>
                  <p
                    className={`text-3xl font-bold ${
                      failedLogins24h > 10 ? "text-red-400" : "text-white"
                    }`}
                  >
                    {failedLogins24h}
                  </p>
                  {failedLogins24h > 10 && (
                    <p className="text-xs text-red-400 mt-1">
                      Yuksek basarisiz giris tespit edildi!
                    </p>
                  )}
                </div>
              </div>
            )}
          </CardContent>
        </Card>

        {/* Failed Logins 7d */}
        <Card className="bg-slate-900 border-slate-700/50">
          <CardContent className="pt-6">
            {isLoading ? (
              <Skeleton className="h-20 w-full bg-slate-800" />
            ) : (
              <div className="flex items-center gap-4">
                <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-yellow-900/30 text-yellow-400">
                  <ShieldAlert className="h-6 w-6" />
                </div>
                <div>
                  <p className="text-sm text-slate-400">
                    Basarisiz Giris (7 Gun)
                  </p>
                  <p className="text-3xl font-bold text-white">
                    {failedLogins7d}
                  </p>
                </div>
              </div>
            )}
          </CardContent>
        </Card>

        {/* Total Logins Today */}
        <Card className="bg-slate-900 border-slate-700/50">
          <CardContent className="pt-6">
            {isLoading ? (
              <Skeleton className="h-20 w-full bg-slate-800" />
            ) : (
              <div className="flex items-center gap-4">
                <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-green-900/30 text-green-400">
                  <LogIn className="h-6 w-6" />
                </div>
                <div>
                  <p className="text-sm text-slate-400">
                    Bugunku Toplam Giris
                  </p>
                  <p className="text-3xl font-bold text-white">
                    {totalLoginsToday}
                  </p>
                </div>
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Suspicious IPs */}
      <Card className="bg-slate-900 border-slate-700/50">
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-lg text-white">
            <Globe className="h-5 w-5 text-red-400" />
            Supheli IP Adresleri
          </CardTitle>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="space-y-4">
              {[...Array(5)].map((_, i) => (
                <Skeleton key={i} className="h-12 w-full bg-slate-800" />
              ))}
            </div>
          ) : suspiciousIps.length === 0 ? (
            <div className="text-center py-8 text-slate-400">
              <Shield className="h-12 w-12 mx-auto mb-4 text-green-500 opacity-50" />
              <p>Supheli IP adresi bulunamadi</p>
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow className="border-slate-700/50 hover:bg-transparent">
                  <TableHead className="text-slate-400">IP Adresi</TableHead>
                  <TableHead className="text-slate-400 text-right">
                    Basarisiz Deneme
                  </TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {suspiciousIps.map((item: any, index: number) => (
                  <TableRow
                    key={index}
                    className="border-slate-700/50 hover:bg-slate-800/50"
                  >
                    <TableCell className="text-white font-mono">
                      {item.ipAddress || item.ip}
                    </TableCell>
                    <TableCell className="text-right">
                      <span
                        className={`font-bold ${
                          (item.failedAttempts || item.count) > 5
                            ? "text-red-400"
                            : "text-yellow-400"
                        }`}
                      >
                        {item.failedAttempts || item.count}
                      </span>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>

      {/* Suspicious Accounts */}
      <Card className="bg-slate-900 border-slate-700/50">
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-lg text-white">
            <UserX className="h-5 w-5 text-orange-400" />
            Supheli Hesaplar
          </CardTitle>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="space-y-4">
              {[...Array(5)].map((_, i) => (
                <Skeleton key={i} className="h-12 w-full bg-slate-800" />
              ))}
            </div>
          ) : suspiciousAccounts.length === 0 ? (
            <div className="text-center py-8 text-slate-400">
              <Shield className="h-12 w-12 mx-auto mb-4 text-green-500 opacity-50" />
              <p>Supheli hesap bulunamadi</p>
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow className="border-slate-700/50 hover:bg-transparent">
                  <TableHead className="text-slate-400">Email</TableHead>
                  <TableHead className="text-slate-400 text-right">
                    Basarisiz Deneme
                  </TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {suspiciousAccounts.map((item: any, index: number) => (
                  <TableRow
                    key={index}
                    className="border-slate-700/50 hover:bg-slate-800/50"
                  >
                    <TableCell className="text-white font-medium">
                      {item.email}
                    </TableCell>
                    <TableCell className="text-right">
                      <span
                        className={`font-bold ${
                          (item.failedAttempts || item.count) > 5
                            ? "text-red-400"
                            : "text-yellow-400"
                        }`}
                      >
                        {item.failedAttempts || item.count}
                      </span>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
