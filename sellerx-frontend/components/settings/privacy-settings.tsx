"use client";

import { useState } from "react";
import { Switch } from "@/components/ui/switch";
import { SettingsSection, SettingsRow } from "./settings-section";
import { Shield, Info } from "lucide-react";

export function PrivacySettings() {
  // These are frontend-only settings stored in local state
  // In a real app, these would be persisted to backend
  const [analyticsEnabled, setAnalyticsEnabled] = useState(true);
  const [marketingEnabled, setMarketingEnabled] = useState(false);
  const [thirdPartyEnabled, setThirdPartyEnabled] = useState(false);

  return (
    <div className="space-y-6">
      <SettingsSection
        title="Gizlilik Ayarları"
        description="Verilerinizin nasıl kullanıldığını kontrol edin"
        noPadding
      >
        <div className="divide-y divide-border">
          <div className="p-5">
            <SettingsRow
              label="Analitik Verisi"
              description="Uygulama kullanım verilerini anonim olarak paylaşarak geliştirmemize yardımcı olun"
              className="border-0 py-0"
            >
              <Switch
                checked={analyticsEnabled}
                onCheckedChange={setAnalyticsEnabled}
              />
            </SettingsRow>
          </div>

          <div className="p-5">
            <SettingsRow
              label="Pazarlama İletişimleri"
              description="Kampanyalar, özel teklifler ve ürün güncellemeleri hakkında e-posta alın"
              className="border-0 py-0"
            >
              <Switch
                checked={marketingEnabled}
                onCheckedChange={setMarketingEnabled}
              />
            </SettingsRow>
          </div>

          <div className="p-5">
            <SettingsRow
              label="Üçüncü Taraf Paylaşımı"
              description="İş ortaklarımızla sınırlı veri paylaşımına izin verin"
              className="border-0 py-0"
            >
              <Switch
                checked={thirdPartyEnabled}
                onCheckedChange={setThirdPartyEnabled}
              />
            </SettingsRow>
          </div>
        </div>
      </SettingsSection>

      {/* Data Retention Info */}
      <SettingsSection
        title="Veri Saklama"
        description="Verilerinizin ne kadar süre saklandığını öğrenin"
      >
        <div className="space-y-4">
          <div className="flex items-start gap-4 p-4 rounded-lg bg-muted">
            <Shield className="h-5 w-5 text-muted-foreground mt-0.5" />
            <div>
              <p className="font-medium text-foreground">Sipariş Verileri</p>
              <p className="text-sm text-muted-foreground">
                Sipariş geçmişiniz yasal gereklilikler nedeniyle 5 yıl süreyle saklanır
              </p>
            </div>
          </div>

          <div className="flex items-start gap-4 p-4 rounded-lg bg-muted">
            <Shield className="h-5 w-5 text-muted-foreground mt-0.5" />
            <div>
              <p className="font-medium text-foreground">Analitik Verileri</p>
              <p className="text-sm text-muted-foreground">
                Kullanım istatistikleri 2 yıl süreyle anonim olarak saklanır
              </p>
            </div>
          </div>

          <div className="flex items-start gap-4 p-4 rounded-lg bg-muted">
            <Shield className="h-5 w-5 text-muted-foreground mt-0.5" />
            <div>
              <p className="font-medium text-foreground">Hesap Verileri</p>
              <p className="text-sm text-muted-foreground">
                Hesabınızı sildiğinizde tüm kişisel verileriniz 30 gün içinde kalıcı olarak silinir
              </p>
            </div>
          </div>
        </div>

        <div className="mt-6 p-4 rounded-lg bg-blue-50 border border-blue-200">
          <div className="flex items-start gap-3">
            <Info className="h-5 w-5 text-blue-600 mt-0.5" />
            <div>
              <p className="font-medium text-blue-900">KVKK Uyumu</p>
              <p className="text-sm text-blue-700 mt-1">
                Verileriniz 6698 sayılı Kişisel Verilerin Korunması Kanunu kapsamında
                işlenmekte ve korunmaktadır. Detaylı bilgi için{" "}
                <a href="#" className="underline hover:no-underline">
                  Gizlilik Politikamızı
                </a>{" "}
                inceleyebilirsiniz.
              </p>
            </div>
          </div>
        </div>
      </SettingsSection>
    </div>
  );
}
