"use client";

import { use } from "react";
import Link from "next/link";
import { useAdminUser, useChangeUserRole } from "@/hooks/queries/use-admin";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  ArrowLeft,
  User,
  Mail,
  Shield,
  Store,
  Calendar,
  Clock,
  Package,
  ShoppingCart,
  AlertTriangle,
  CheckCircle,
  Loader2,
} from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import { format } from "date-fns";
import { tr } from "date-fns/locale";
import { useState } from "react";
import { toast } from "sonner";

export default function AdminUserDetailPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  const { data: user, isLoading, error } = useAdminUser(id);
  const changeRole = useChangeUserRole();
  const [selectedRole, setSelectedRole] = useState<string | undefined>(undefined);

  const formatDate = (date: string | null) => {
    if (!date) return "-";
    try {
      return format(new Date(date), "dd MMMM yyyy, HH:mm", { locale: tr });
    } catch {
      return "-";
    }
  };

  const handleRoleChange = async (newRole: string) => {
    if (!user) return;

    try {
      await changeRole.mutateAsync({
        id: user.id,
        role: newRole as "USER" | "ADMIN",
      });
      toast.success("Kullanıcı rolü başarıyla güncellendi");
    } catch {
      toast.error("Rol güncellenirken bir hata oluştu");
    }
  };

  if (isLoading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-8 w-48" />
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <Skeleton className="h-64" />
          <Skeleton className="h-64" />
        </div>
      </div>
    );
  }

  if (error || !user) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-center">
          <AlertTriangle className="h-12 w-12 text-red-500 mx-auto mb-4" />
          <p className="text-lg text-slate-600 dark:text-slate-400">
            Kullanıcı bulunamadı
          </p>
          <Button variant="outline" asChild className="mt-4">
            <Link href="/admin/users">
              <ArrowLeft className="h-4 w-4 mr-2" />
              Listeye Dön
            </Link>
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Back Button */}
      <Button variant="ghost" asChild>
        <Link href="/admin/users">
          <ArrowLeft className="h-4 w-4 mr-2" />
          Kullanıcılara Dön
        </Link>
      </Button>

      {/* User Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <div className="flex h-16 w-16 items-center justify-center rounded-full bg-slate-100 dark:bg-slate-800">
            <User className="h-8 w-8 text-slate-600 dark:text-slate-400" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-slate-900 dark:text-white">
              {user.name}
            </h1>
            <p className="text-slate-500">{user.email}</p>
          </div>
        </div>
        <div>
          {user.role === "ADMIN" ? (
            <Badge className="bg-amber-100 text-amber-700 dark:bg-amber-900/30 dark:text-amber-400 text-base px-4 py-2">
              <Shield className="h-4 w-4 mr-2" />
              Admin
            </Badge>
          ) : (
            <Badge variant="secondary" className="text-base px-4 py-2">
              <User className="h-4 w-4 mr-2" />
              Kullanıcı
            </Badge>
          )}
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* User Info */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <User className="h-5 w-5" />
              Kullanıcı Bilgileri
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex items-center gap-3">
              <Mail className="h-5 w-5 text-slate-400" />
              <div>
                <p className="text-sm text-slate-500">Email</p>
                <p className="font-medium text-slate-900 dark:text-white">{user.email}</p>
              </div>
            </div>
            <div className="flex items-center gap-3">
              <Calendar className="h-5 w-5 text-slate-400" />
              <div>
                <p className="text-sm text-slate-500">Kayıt Tarihi</p>
                <p className="font-medium text-slate-900 dark:text-white">
                  {formatDate(user.createdAt)}
                </p>
              </div>
            </div>
            <div className="flex items-center gap-3">
              <Clock className="h-5 w-5 text-slate-400" />
              <div>
                <p className="text-sm text-slate-500">Son Giriş</p>
                <p className="font-medium text-slate-900 dark:text-white">
                  {formatDate(user.lastLoginAt)}
                </p>
              </div>
            </div>
            <div className="flex items-center gap-3">
              <Store className="h-5 w-5 text-slate-400" />
              <div>
                <p className="text-sm text-slate-500">Mağaza Sayısı</p>
                <p className="font-medium text-slate-900 dark:text-white">{user.storeCount}</p>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Role Management */}
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Shield className="h-5 w-5" />
              Rol Yönetimi
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div>
              <p className="text-sm text-slate-500 mb-2">Mevcut Rol</p>
              <p className="font-medium text-slate-900 dark:text-white">
                {user.role === "ADMIN" ? "Administrator" : "Normal Kullanıcı"}
              </p>
            </div>
            <div>
              <p className="text-sm text-slate-500 mb-2">Rol Değiştir</p>
              <div className="flex items-center gap-3">
                <Select
                  value={selectedRole || user.role}
                  onValueChange={setSelectedRole}
                >
                  <SelectTrigger className="w-48">
                    <SelectValue placeholder="Rol seçin" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="USER">Kullanıcı</SelectItem>
                    <SelectItem value="ADMIN">Admin</SelectItem>
                  </SelectContent>
                </Select>
                <Button
                  onClick={() => selectedRole && handleRoleChange(selectedRole)}
                  disabled={changeRole.isPending || !selectedRole || selectedRole === user.role}
                >
                  {changeRole.isPending ? (
                    <Loader2 className="h-4 w-4 animate-spin mr-2" />
                  ) : (
                    <CheckCircle className="h-4 w-4 mr-2" />
                  )}
                  Kaydet
                </Button>
              </div>
            </div>

            {/* Subscription Info */}
            <div className="pt-4 border-t">
              <p className="text-sm text-slate-500 mb-2">Abonelik Durumu</p>
              {user.activeSubscription ? (
                <Badge className="bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400">
                  <CheckCircle className="h-3 w-3 mr-1" />
                  Aktif - {user.subscriptionPlan}
                </Badge>
              ) : (
                <Badge variant="secondary">
                  Abonelik Yok
                </Badge>
              )}
            </div>
          </CardContent>
        </Card>
      </div>

      {/* User Stores */}
      {user.stores && user.stores.length > 0 && (
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <Store className="h-5 w-5" />
              Mağazalar ({user.stores.length})
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
              {user.stores.map((store) => (
                <Card key={store.id} className="bg-slate-50 dark:bg-slate-800/50">
                  <CardContent className="pt-4">
                    <div className="flex items-center justify-between mb-3">
                      <h4 className="font-semibold text-slate-900 dark:text-white">
                        {store.storeName}
                      </h4>
                      <Badge variant="outline">{store.marketplace}</Badge>
                    </div>
                    <div className="space-y-2 text-sm">
                      <div className="flex items-center justify-between">
                        <span className="text-slate-500 flex items-center gap-1">
                          <Package className="h-3.5 w-3.5" />
                          Ürünler
                        </span>
                        <span className="font-medium">{store.productCount}</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-slate-500 flex items-center gap-1">
                          <ShoppingCart className="h-3.5 w-3.5" />
                          Siparişler
                        </span>
                        <span className="font-medium">{store.orderCount}</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-slate-500">Sync Durumu</span>
                        <Badge
                          variant={store.syncStatus === "COMPLETED" ? "default" : "secondary"}
                          className={
                            store.syncStatus === "COMPLETED"
                              ? "bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400"
                              : ""
                          }
                        >
                          {store.syncStatus}
                        </Badge>
                      </div>
                    </div>
                    <Button variant="outline" size="sm" className="w-full mt-4" asChild>
                      <Link href={`/admin/stores/${store.id}`}>
                        Mağaza Detayı
                      </Link>
                    </Button>
                  </CardContent>
                </Card>
              ))}
            </div>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
