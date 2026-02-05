"use client";

import { useState } from "react";
import Link from "next/link";
import { useAdminUsers, useAdminUserSearch, useImpersonateUser } from "@/hooks/queries/use-admin";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
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
import {
  ChevronLeft,
  ChevronRight,
  Search,
  Users,
  Store,
  Shield,
  User,
  Eye,
  Download,
  LogIn,
} from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import { format } from "date-fns";
import { tr } from "date-fns/locale";
import ExcelJS from "exceljs";
import { saveAs } from "file-saver";
import { useTranslations } from "next-intl";
import type { AdminUserListItem } from "@/types/admin";
import type { ImpersonationMeta } from "@/hooks/use-impersonation";

export default function AdminUsersPage() {
  const [page, setPage] = useState(0);
  const [searchQuery, setSearchQuery] = useState("");
  const [isExporting, setIsExporting] = useState(false);
  const pageSize = 20;
  const t = useTranslations("impersonation");
  const impersonateMutation = useImpersonateUser();

  const { data: usersPage, isLoading } = useAdminUsers(page, pageSize);
  const { data: searchResults, isLoading: isSearching } = useAdminUserSearch(searchQuery);

  const users = searchQuery.length >= 2 ? searchResults : usersPage?.content;
  const totalPages = usersPage?.totalPages || 1;

  const formatDate = (date: string | null) => {
    if (!date) return "-";
    try {
      return format(new Date(date), "dd MMM yyyy, HH:mm", { locale: tr });
    } catch {
      return "-";
    }
  };

  const formatDateForExcel = (date: string | null) => {
    if (!date) return "-";
    try {
      return format(new Date(date), "dd.MM.yyyy HH:mm", { locale: tr });
    } catch {
      return "-";
    }
  };

  const handleExportExcel = async () => {
    setIsExporting(true);
    try {
      const response = await fetch("/api/admin/users/export");
      if (!response.ok) {
        throw new Error("Kullanıcı listesi alınamadı");
      }
      const allUsers: AdminUserListItem[] = await response.json();

      if (!allUsers || allUsers.length === 0) {
        alert("Dışa aktarılacak kullanıcı bulunamadı");
        setIsExporting(false);
        return;
      }

      const todayStr = new Date().toISOString().split("T")[0];
      const exportData = allUsers.map((user) => ({
        ID: user.id,
        İsim: user.name || "-",
        Email: user.email,
        Rol: user.role === "ADMIN" ? "Admin" : "Kullanıcı",
        "Mağaza Sayısı": user.storeCount,
        "Kayıt Tarihi": formatDateForExcel(user.createdAt),
        "Son Giriş": formatDateForExcel(user.lastLoginAt),
      }));

      const workbook = new ExcelJS.Workbook();
      const worksheet = workbook.addWorksheet("Kullanıcılar");

      // Add header row
      const headers = Object.keys(exportData[0]);
      worksheet.addRow(headers);

      // Add data rows
      exportData.forEach((row) => {
        worksheet.addRow(Object.values(row));
      });

      // Set column widths
      const colWidths = [10, 25, 30, 12, 15, 20, 20];
      worksheet.columns.forEach((col, i) => {
        col.width = colWidths[i] || 15;
      });

      // Write to buffer and save
      const buffer = await workbook.xlsx.writeBuffer();
      const blob = new Blob([buffer], { type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" });
      saveAs(blob, `kullanicilar-${todayStr}.xlsx`);
    } catch (error) {
      console.error("Excel export error:", error);
      alert("Excel dışa aktarma sırasında bir hata oluştu");
    } finally {
      setIsExporting(false);
    }
  };

  const handleImpersonate = async (user: AdminUserListItem) => {
    if (!confirm(t("loginAsConfirm"))) return;

    try {
      const result = await impersonateMutation.mutateAsync({
        id: user.id,
        name: user.name || "",
        email: user.email,
      });

      const meta: ImpersonationMeta = {
        targetUserId: user.id,
        targetUserName: user.name || "",
        targetUserEmail: user.email,
        adminUserId: 0, // Admin ID is embedded in the token
        startedAt: new Date().toISOString(),
      };

      const metaParam = encodeURIComponent(JSON.stringify(meta));
      const url = `/impersonate?token=${encodeURIComponent(result.token)}&meta=${metaParam}`;
      window.open(url, "_blank");
    } catch {
      alert(t("error"));
    }
  };

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-white">Kullanıcılar</h1>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
            Sistemdeki tüm kullanıcıları yönetin
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Users className="h-5 w-5 text-blue-500" />
          <span className="text-lg font-semibold text-slate-900 dark:text-white">
            {usersPage?.totalElements || 0}
          </span>
        </div>
      </div>

      {/* Search */}
      <Card>
        <CardContent className="pt-6">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
            <Input
              placeholder="Email veya isim ile ara..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-10"
            />
          </div>
        </CardContent>
      </Card>

      {/* Users Table */}
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <CardTitle className="flex items-center gap-2 text-lg">
              <Users className="h-5 w-5" />
              Kullanıcı Listesi
            </CardTitle>
            <Button
              variant="outline"
              size="sm"
              onClick={handleExportExcel}
              disabled={isExporting}
            >
              <Download className="h-4 w-4 mr-2" />
              {isExporting ? "Dışa Aktarılıyor..." : "Excel'e Aktar"}
            </Button>
          </div>
        </CardHeader>
        <CardContent>
          {isLoading || isSearching ? (
            <div className="space-y-4">
              {[...Array(5)].map((_, i) => (
                <Skeleton key={i} className="h-16 w-full" />
              ))}
            </div>
          ) : (
            <>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>ID</TableHead>
                    <TableHead>Kullanıcı</TableHead>
                    <TableHead>Rol</TableHead>
                    <TableHead>Mağazalar</TableHead>
                    <TableHead>Kayıt Tarihi</TableHead>
                    <TableHead>Son Giriş</TableHead>
                    <TableHead className="text-right">İşlem</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {users?.map((user) => (
                    <TableRow key={user.id}>
                      <TableCell className="font-mono text-sm">{user.id}</TableCell>
                      <TableCell>
                        <div className="flex items-center gap-3">
                          <div className="flex h-8 w-8 items-center justify-center rounded-full bg-slate-100 dark:bg-slate-800">
                            <User className="h-4 w-4 text-slate-600 dark:text-slate-400" />
                          </div>
                          <div>
                            <p className="font-medium text-slate-900 dark:text-white">
                              {user.name}
                            </p>
                            <p className="text-sm text-slate-500">{user.email}</p>
                          </div>
                        </div>
                      </TableCell>
                      <TableCell>
                        {user.role === "ADMIN" ? (
                          <Badge className="bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400 hover:bg-amber-100">
                            <Shield className="h-3 w-3 mr-1" />
                            Admin
                          </Badge>
                        ) : (
                          <Badge variant="secondary">
                            <User className="h-3 w-3 mr-1" />
                            Kullanıcı
                          </Badge>
                        )}
                      </TableCell>
                      <TableCell>
                        <div className="flex items-center gap-1.5">
                          <Store className="h-4 w-4 text-slate-400" />
                          <span>{user.storeCount}</span>
                        </div>
                      </TableCell>
                      <TableCell className="text-sm text-slate-500">
                        {formatDate(user.createdAt)}
                      </TableCell>
                      <TableCell className="text-sm text-slate-500">
                        {formatDate(user.lastLoginAt)}
                      </TableCell>
                      <TableCell className="text-right">
                        <div className="flex items-center justify-end gap-1">
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleImpersonate(user)}
                            disabled={impersonateMutation.isPending}
                            title={t("loginAs")}
                          >
                            <LogIn className="h-4 w-4 mr-1" />
                            {t("loginAs")}
                          </Button>
                          <Button variant="ghost" size="sm" asChild>
                            <Link href={`/admin/users/${user.id}`}>
                              <Eye className="h-4 w-4 mr-1" />
                              Detay
                            </Link>
                          </Button>
                        </div>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>

              {/* Pagination */}
              {!searchQuery && (
                <div className="flex items-center justify-between mt-4">
                  <p className="text-sm text-slate-500">
                    Sayfa {page + 1} / {totalPages}
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
                      onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
                      disabled={page >= totalPages - 1}
                    >
                      <ChevronRight className="h-4 w-4" />
                    </Button>
                  </div>
                </div>
              )}
            </>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
