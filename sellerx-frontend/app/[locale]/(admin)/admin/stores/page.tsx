"use client";

import { useState } from "react";
import Link from "next/link";
import { useAdminStores, useAdminStoreSearch } from "@/hooks/queries/use-admin";
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
  Store,
  User,
  Package,
  ShoppingCart,
  Eye,
  CheckCircle,
  AlertTriangle,
  RefreshCw,
  Clock,
} from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import { format } from "date-fns";
import { tr } from "date-fns/locale";

export default function AdminStoresPage() {
  const [page, setPage] = useState(0);
  const [searchQuery, setSearchQuery] = useState("");
  const pageSize = 20;

  const { data: storesPage, isLoading } = useAdminStores(page, pageSize);
  const { data: searchResults, isLoading: isSearching } = useAdminStoreSearch(searchQuery);

  const stores = searchQuery.length >= 2 ? searchResults : storesPage?.content;
  const totalPages = storesPage?.totalPages || 1;

  const formatDate = (date: string | null) => {
    if (!date) return "-";
    try {
      return format(new Date(date), "dd MMM yyyy, HH:mm", { locale: tr });
    } catch {
      return "-";
    }
  };

  const getSyncStatusBadge = (status: string, initialSyncCompleted: boolean) => {
    if (status === "COMPLETED" && initialSyncCompleted) {
      return (
        <Badge className="bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400">
          <CheckCircle className="h-3 w-3 mr-1" />
          Tamamlandı
        </Badge>
      );
    }
    if (status === "FAILED") {
      return (
        <Badge className="bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400">
          <AlertTriangle className="h-3 w-3 mr-1" />
          Hata
        </Badge>
      );
    }
    if (status?.startsWith("SYNCING")) {
      return (
        <Badge className="bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400">
          <RefreshCw className="h-3 w-3 mr-1 animate-spin" />
          Sync
        </Badge>
      );
    }
    if (status === "pending") {
      return (
        <Badge className="bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400">
          <Clock className="h-3 w-3 mr-1" />
          Bekliyor
        </Badge>
      );
    }
    return <Badge variant="secondary">{status || "-"}</Badge>;
  };

  const getWebhookStatusBadge = (status: string | null) => {
    if (!status) return <Badge variant="secondary">-</Badge>;
    if (status === "active") {
      return (
        <Badge className="bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400">
          Aktif
        </Badge>
      );
    }
    if (status === "failed") {
      return (
        <Badge className="bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400">
          Hata
        </Badge>
      );
    }
    return <Badge variant="secondary">{status}</Badge>;
  };

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-white">Mağazalar</h1>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
            Sistemdeki tüm mağazaları yönetin
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Store className="h-5 w-5 text-purple-500" />
          <span className="text-lg font-semibold text-slate-900 dark:text-white">
            {storesPage?.totalElements || 0}
          </span>
        </div>
      </div>

      {/* Search */}
      <Card>
        <CardContent className="pt-6">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
            <Input
              placeholder="Mağaza adı veya kullanıcı email ile ara..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-10"
            />
          </div>
        </CardContent>
      </Card>

      {/* Stores Table */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-lg">
            <Store className="h-5 w-5" />
            Mağaza Listesi
          </CardTitle>
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
                    <TableHead>Mağaza</TableHead>
                    <TableHead>Sahip</TableHead>
                    <TableHead>Platform</TableHead>
                    <TableHead>Sync Durumu</TableHead>
                    <TableHead>Webhook</TableHead>
                    <TableHead>Ürün</TableHead>
                    <TableHead>Sipariş</TableHead>
                    <TableHead>Oluşturulma</TableHead>
                    <TableHead className="text-right">İşlem</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {stores?.map((store) => (
                    <TableRow key={store.id}>
                      <TableCell>
                        <div className="flex items-center gap-3">
                          <div className="flex h-8 w-8 items-center justify-center rounded-full bg-purple-100 dark:bg-purple-900/30">
                            <Store className="h-4 w-4 text-purple-600 dark:text-purple-400" />
                          </div>
                          <span className="font-medium text-slate-900 dark:text-white">
                            {store.storeName}
                          </span>
                        </div>
                      </TableCell>
                      <TableCell>
                        <div className="flex items-center gap-2">
                          <User className="h-4 w-4 text-slate-400" />
                          <div>
                            <p className="text-sm text-slate-500 truncate max-w-32">
                              {store.userEmail}
                            </p>
                          </div>
                        </div>
                      </TableCell>
                      <TableCell>
                        <Badge variant="outline">{store.marketplace}</Badge>
                      </TableCell>
                      <TableCell>
                        {getSyncStatusBadge(store.syncStatus, store.initialSyncCompleted)}
                      </TableCell>
                      <TableCell>
                        {getWebhookStatusBadge(store.webhookStatus)}
                      </TableCell>
                      <TableCell>
                        <div className="flex items-center gap-1.5">
                          <Package className="h-4 w-4 text-slate-400" />
                          <span>{store.productCount}</span>
                        </div>
                      </TableCell>
                      <TableCell>
                        <div className="flex items-center gap-1.5">
                          <ShoppingCart className="h-4 w-4 text-slate-400" />
                          <span>{store.orderCount}</span>
                        </div>
                      </TableCell>
                      <TableCell className="text-sm text-slate-500">
                        {formatDate(store.createdAt)}
                      </TableCell>
                      <TableCell className="text-right">
                        <Button variant="ghost" size="sm" asChild>
                          <Link href={`/admin/stores/${store.id}`}>
                            <Eye className="h-4 w-4 mr-1" />
                            Detay
                          </Link>
                        </Button>
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
