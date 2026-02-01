"use client";

const isDev = process.env.NODE_ENV === "development";

import { useState, useEffect } from "react";
import { useUserPreferences, useUpdatePreferences } from "@/hooks/queries/use-settings";
import { Button } from "@/components/ui/button";
import { Switch } from "@/components/ui/switch";
import { SettingsSection, SettingsRow } from "./settings-section";
import { Loader2, Save, CheckCircle2 } from "lucide-react";

export function NotificationSettings() {
  const { data: preferences } = useUserPreferences();
  const updatePreferencesMutation = useUpdatePreferences();

  const [emailNotifications, setEmailNotifications] = useState(
    preferences?.notifications?.email ?? true
  );
  const [pushNotifications, setPushNotifications] = useState(
    preferences?.notifications?.push ?? true
  );
  const [orderUpdates, setOrderUpdates] = useState(
    preferences?.notifications?.orderUpdates ?? true
  );
  const [stockAlerts, setStockAlerts] = useState(
    preferences?.notifications?.stockAlerts ?? true
  );
  const [weeklyReport, setWeeklyReport] = useState(
    preferences?.notifications?.weeklyReport ?? false
  );
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    if (preferences?.notifications) {
      setEmailNotifications(preferences.notifications.email ?? true);
      setPushNotifications(preferences.notifications.push ?? true);
      setOrderUpdates(preferences.notifications.orderUpdates ?? true);
      setStockAlerts(preferences.notifications.stockAlerts ?? true);
      setWeeklyReport(preferences.notifications.weeklyReport ?? false);
    }
  }, [preferences]);

  const handleNotificationsSave = async () => {
    try {
      await updatePreferencesMutation.mutateAsync({
        notifications: {
          email: emailNotifications,
          push: pushNotifications,
          orderUpdates,
          stockAlerts,
          weeklyReport,
        },
      });
      setSaved(true);
      setTimeout(() => setSaved(false), 3000);
    } catch (error) {
      if (isDev) console.error("Bildirim tercihleri kaydedilemedi:", error);
    }
  };

  return (
    <SettingsSection
      title="Bildirim Tercihleri"
      description="Hangi bildirimlerden haberdar olmak istediğinizi seçin"
      noPadding
    >
      <div className="divide-y divide-border">
        <div className="p-5">
          <SettingsRow
            label="E-posta Bildirimleri"
            description="Önemli güncellemeler ve duyurular için e-posta alın"
            className="border-0 py-0"
          >
            <Switch
              checked={emailNotifications}
              onCheckedChange={setEmailNotifications}
            />
          </SettingsRow>
        </div>

        <div className="p-5">
          <SettingsRow
            label="Push Bildirimleri"
            description="Tarayıcı üzerinden anlık bildirimler alın"
            className="border-0 py-0"
          >
            <Switch
              checked={pushNotifications}
              onCheckedChange={setPushNotifications}
            />
          </SettingsRow>
        </div>

        <div className="p-5">
          <SettingsRow
            label="Sipariş Güncellemeleri"
            description="Yeni sipariş ve sipariş durum değişiklikleri"
            className="border-0 py-0"
          >
            <Switch checked={orderUpdates} onCheckedChange={setOrderUpdates} />
          </SettingsRow>
        </div>

        <div className="p-5">
          <SettingsRow
            label="Stok Uyarıları"
            description="Düşük stok ve stok tükenmesi uyarıları"
            className="border-0 py-0"
          >
            <Switch checked={stockAlerts} onCheckedChange={setStockAlerts} />
          </SettingsRow>
        </div>

        <div className="p-5">
          <SettingsRow
            label="Haftalık Rapor"
            description="Her hafta satış ve performans özeti"
            className="border-0 py-0"
          >
            <Switch checked={weeklyReport} onCheckedChange={setWeeklyReport} />
          </SettingsRow>
        </div>
      </div>

      <div className="flex justify-end p-6 border-t border-border">
        <Button
          onClick={handleNotificationsSave}
          disabled={updatePreferencesMutation.isPending}
          className="bg-[#1D70F1] hover:bg-[#1560d1]"
        >
          {updatePreferencesMutation.isPending ? (
            <>
              <Loader2 className="h-4 w-4 mr-2 animate-spin" />
              Kaydediliyor...
            </>
          ) : saved ? (
            <>
              <CheckCircle2 className="h-4 w-4 mr-2" />
              Kaydedildi
            </>
          ) : (
            <>
              <Save className="h-4 w-4 mr-2" />
              Tercihleri Kaydet
            </>
          )}
        </Button>
      </div>
    </SettingsSection>
  );
}
