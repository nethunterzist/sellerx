"use client";

import { AlertRulesSettings } from "@/components/settings/alert-rules-settings";

export default function AlertsPage() {
  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div>
        <h1 className="text-2xl font-semibold text-foreground">Uyarılar</h1>
        <p className="text-muted-foreground mt-1">
          Stok, kar ve sipariş değişikliklerinde otomatik bildirim almak için kurallar oluşturun
        </p>
      </div>

      {/* Alert Rules Content */}
      <AlertRulesSettings />
    </div>
  );
}
