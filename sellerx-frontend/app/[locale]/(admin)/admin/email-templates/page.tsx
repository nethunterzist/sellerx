"use client";

import { useState } from "react";
import Link from "next/link";
import { useEmailTemplates } from "@/hooks/queries/use-admin-email-templates";
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
  Mail,
  Search,
  Edit,
  Settings,
  CheckCircle2,
  XCircle,
} from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import { format } from "date-fns";
import { tr } from "date-fns/locale";

// Template type labels in Turkish
const templateTypeLabels: Record<string, string> = {
  WELCOME: "Hoş Geldiniz",
  PASSWORD_RESET: "Şifre Sıfırlama",
  EMAIL_VERIFICATION: "Email Doğrulama",
  SUBSCRIPTION_CONFIRMED: "Abonelik Onayı",
  SUBSCRIPTION_REMINDER_7: "Abonelik Hatırlatma (7 gün)",
  SUBSCRIPTION_REMINDER_1: "Abonelik Hatırlatma (1 gün)",
  SUBSCRIPTION_RENEWED: "Abonelik Yenilendi",
  PAYMENT_FAILED: "Ödeme Başarısız",
  SUBSCRIPTION_CANCELLED: "Abonelik İptal",
  ALERT_NOTIFICATION: "Alarm Bildirimi",
  DAILY_DIGEST: "Günlük Özet",
  WEEKLY_REPORT: "Haftalık Rapor",
  ADMIN_BROADCAST: "Admin Duyurusu",
};

// Template category for grouping
const templateCategories: Record<string, string[]> = {
  "Kullanıcı": ["WELCOME", "PASSWORD_RESET", "EMAIL_VERIFICATION"],
  "Abonelik": ["SUBSCRIPTION_CONFIRMED", "SUBSCRIPTION_REMINDER_7", "SUBSCRIPTION_REMINDER_1", "SUBSCRIPTION_RENEWED", "PAYMENT_FAILED", "SUBSCRIPTION_CANCELLED"],
  "Bildirimler": ["ALERT_NOTIFICATION", "DAILY_DIGEST", "WEEKLY_REPORT", "ADMIN_BROADCAST"],
};

export default function AdminEmailTemplatesPage() {
  const [searchQuery, setSearchQuery] = useState("");
  const { data: templates, isLoading } = useEmailTemplates();

  const filteredTemplates = templates?.filter((template) => {
    const searchLower = searchQuery.toLowerCase();
    return (
      template.name.toLowerCase().includes(searchLower) ||
      template.emailType.toLowerCase().includes(searchLower) ||
      template.description?.toLowerCase().includes(searchLower)
    );
  });

  const formatDate = (date: string | null) => {
    if (!date) return "-";
    try {
      return format(new Date(date), "dd MMM yyyy, HH:mm", { locale: tr });
    } catch {
      return "-";
    }
  };

  const getCategoryForType = (emailType: string): string => {
    for (const [category, types] of Object.entries(templateCategories)) {
      if (types.includes(emailType)) return category;
    }
    return "Diğer";
  };

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-slate-900 dark:text-white">Email Şablonları</h1>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
            Sistem tarafından gönderilen email şablonlarını yönetin
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="outline" asChild>
            <Link href="/admin/email-templates/layout-settings">
              <Settings className="h-4 w-4 mr-2" />
              Genel Ayarlar
            </Link>
          </Button>
        </div>
      </div>

      {/* Search */}
      <Card>
        <CardContent className="pt-6">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
            <Input
              placeholder="Şablon adı veya tip ile ara..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-10"
            />
          </div>
        </CardContent>
      </Card>

      {/* Templates Table */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-lg">
            <Mail className="h-5 w-5" />
            Email Şablonları
            {templates && (
              <Badge variant="secondary" className="ml-2">
                {templates.length} şablon
              </Badge>
            )}
          </CardTitle>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="space-y-4">
              {[...Array(5)].map((_, i) => (
                <Skeleton key={i} className="h-16 w-full" />
              ))}
            </div>
          ) : (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Şablon</TableHead>
                  <TableHead>Kategori</TableHead>
                  <TableHead>Konu</TableHead>
                  <TableHead>Durum</TableHead>
                  <TableHead>Son Güncelleme</TableHead>
                  <TableHead className="text-right">İşlem</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredTemplates?.map((template) => (
                  <TableRow key={template.id}>
                    <TableCell>
                      <div className="flex items-center gap-3">
                        <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-blue-50 dark:bg-blue-900/30">
                          <Mail className="h-5 w-5 text-blue-600 dark:text-blue-400" />
                        </div>
                        <div>
                          <p className="font-medium text-slate-900 dark:text-white">
                            {template.name}
                          </p>
                          <p className="text-xs text-slate-500 font-mono">
                            {template.emailType}
                          </p>
                        </div>
                      </div>
                    </TableCell>
                    <TableCell>
                      <Badge variant="outline">
                        {getCategoryForType(template.emailType)}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      <p className="text-sm text-slate-600 dark:text-slate-300 truncate max-w-[200px]">
                        {template.subjectTemplate}
                      </p>
                    </TableCell>
                    <TableCell>
                      {template.isActive ? (
                        <Badge className="bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400 hover:bg-green-100">
                          <CheckCircle2 className="h-3 w-3 mr-1" />
                          Aktif
                        </Badge>
                      ) : (
                        <Badge variant="secondary" className="text-slate-500">
                          <XCircle className="h-3 w-3 mr-1" />
                          Pasif
                        </Badge>
                      )}
                    </TableCell>
                    <TableCell className="text-sm text-slate-500">
                      {formatDate(template.updatedAt)}
                    </TableCell>
                    <TableCell className="text-right">
                      <Button variant="ghost" size="sm" asChild>
                        <Link href={`/admin/email-templates/${template.emailType}`}>
                          <Edit className="h-4 w-4 mr-1" />
                          Düzenle
                        </Link>
                      </Button>
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
