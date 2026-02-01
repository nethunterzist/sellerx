"use client";

import { SettingsSection } from "./settings-section";
import { ShortcutsDialog } from "./shortcuts-dialog";
import { useActivityLogs } from "@/hooks/queries/use-activity-logs";
import {
  Monitor,
  Smartphone,
  Globe,
  CheckCircle2,
  XCircle,
  Clock,
  Shield,
  Info,
  Loader2,
  AlertCircle,
} from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { cn } from "@/lib/utils";

type ActivityAction = "login" | "logout" | "failed_login";

const actionLabels: Record<ActivityAction, string> = {
  login: "Oturum Açıldı",
  logout: "Oturum Kapatıldı",
  failed_login: "Başarısız Giriş Denemesi",
};

function getDeviceIcon(device: string) {
  const deviceLower = device?.toLowerCase() || "";
  if (deviceLower === "mobile") return Smartphone;
  return Monitor;
}

function formatTimeAgo(dateStr: string): string {
  const date = new Date(dateStr);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMins = Math.floor(diffMs / (1000 * 60));
  const diffHours = Math.floor(diffMs / (1000 * 60 * 60));
  const diffDays = Math.floor(diffMs / (1000 * 60 * 60 * 24));

  if (diffMins < 1) {
    return "Az önce";
  } else if (diffMins < 60) {
    return `${diffMins} dakika önce`;
  } else if (diffHours < 24) {
    return `${diffHours} saat önce`;
  } else if (diffDays < 7) {
    return `${diffDays} gün önce`;
  } else {
    return date.toLocaleDateString("tr-TR", {
      day: "numeric",
      month: "long",
      year: "numeric",
    });
  }
}

export function ActivityLogSettings() {
  const { data: activityLogs, isLoading, error } = useActivityLogs(20);

  return (
    <div className="space-y-6">
      {/* Shortcuts Section */}
      <SettingsSection
        title="Klavye Kısayolları"
        description="Uygulamada hızlı gezinmek için kısayolları öğrenin"
      >
        <ShortcutsDialog />
      </SettingsSection>

      {/* Activity Log Section */}
      <SettingsSection
        title="Aktivite Geçmişi"
        description="Hesabınızdaki son oturum ve güvenlik etkinlikleri"
        noPadding
      >
        {isLoading ? (
          <div className="flex items-center justify-center py-12">
            <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
          </div>
        ) : error ? (
          <div className="flex items-center justify-center gap-2 py-12 text-red-600">
            <AlertCircle className="h-5 w-5" />
            <span>Aktivite geçmişi yüklenemedi</span>
          </div>
        ) : !activityLogs || activityLogs.length === 0 ? (
          <div className="flex flex-col items-center justify-center py-12 text-muted-foreground">
            <Clock className="h-12 w-12 mb-3 text-muted-foreground" />
            <p className="font-medium">Henüz aktivite kaydı yok</p>
            <p className="text-sm mt-1">Giriş ve çıkış işlemleriniz burada görüntülenecek</p>
          </div>
        ) : (
          <div className="divide-y divide-border">
            {activityLogs.map((entry) => {
              const DeviceIcon = getDeviceIcon(entry.device);
              const isFailedLogin = entry.action === "failed_login";
              const actionLabel = actionLabels[entry.action as ActivityAction] || entry.action;

              return (
                <div
                  key={entry.id}
                  className={cn(
                    "flex items-start gap-4 p-5",
                    isFailedLogin && "bg-red-50 dark:bg-red-900/20"
                  )}
                >
                  <div
                    className={cn(
                      "h-10 w-10 rounded-lg flex items-center justify-center",
                      entry.success
                        ? "bg-muted text-muted-foreground"
                        : "bg-red-100 dark:bg-red-900/30 text-red-600 dark:text-red-400"
                    )}
                  >
                    <DeviceIcon className="h-5 w-5" />
                  </div>

                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <span className="font-medium text-foreground">
                        {actionLabel}
                      </span>
                      {entry.success ? (
                        <CheckCircle2 className="h-4 w-4 text-green-500" />
                      ) : (
                        <XCircle className="h-4 w-4 text-red-500" />
                      )}
                      {isFailedLogin && (
                        <Badge variant="destructive" className="text-xs">
                          Dikkat
                        </Badge>
                      )}
                    </div>

                    <div className="flex flex-wrap items-center gap-x-4 gap-y-1 mt-1 text-sm text-muted-foreground">
                      <span className="flex items-center gap-1">
                        <Globe className="h-3.5 w-3.5" />
                        {entry.browser || "Bilinmiyor"}
                      </span>
                      <span className="flex items-center gap-1">
                        <Monitor className="h-3.5 w-3.5" />
                        {entry.device || "Bilinmiyor"}
                      </span>
                      <span className="text-muted-foreground">IP: {entry.ipAddress || "—"}</span>
                    </div>
                  </div>

                  <div className="flex items-center gap-1 text-sm text-muted-foreground whitespace-nowrap">
                    <Clock className="h-3.5 w-3.5" />
                    {formatTimeAgo(entry.createdAt)}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </SettingsSection>

      {/* Security Tip */}
      <div className="p-4 rounded-lg bg-blue-50 border border-blue-200">
        <div className="flex items-start gap-3">
          <Shield className="h-5 w-5 text-blue-600 mt-0.5" />
          <div>
            <p className="font-medium text-blue-900">Güvenlik Önerisi</p>
            <p className="text-sm text-blue-700 mt-1">
              Tanımadığınız bir cihaz veya konumdan giriş görürseniz, hemen şifrenizi değiştirin
              ve iki faktörlü kimlik doğrulamayı etkinleştirin.
            </p>
          </div>
        </div>
      </div>

      {/* Data Notice */}
      <div className="p-4 rounded-lg bg-muted border border-border">
        <div className="flex items-start gap-3">
          <Info className="h-5 w-5 text-muted-foreground mt-0.5" />
          <div>
            <p className="font-medium text-foreground">Veri Saklama</p>
            <p className="text-sm text-muted-foreground mt-1">
              Aktivite geçmişi güvenlik amacıyla 90 gün süreyle saklanmaktadır.
              Daha eski kayıtlar otomatik olarak silinir.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}
