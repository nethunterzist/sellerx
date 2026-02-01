"use client";

const isDev = process.env.NODE_ENV === "development";

import { useState, useEffect } from "react";
import { useUserPreferences, useUpdatePreferences } from "@/hooks/queries/use-settings";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { SettingsSection } from "./settings-section";
import { SyncLogPanel } from "./sync-log-panel";
import { Loader2, Save, Sun, Moon, Monitor, CheckCircle2, RefreshCw } from "lucide-react";
import type { SyncInterval } from "@/types/user";
import { cn } from "@/lib/utils";
import { useTheme } from "next-themes";

export function AppearanceSettings() {
  const { data: preferences } = useUserPreferences();
  const updatePreferencesMutation = useUpdatePreferences();
  const { theme: currentTheme, setTheme: setNextTheme } = useTheme();

  const [theme, setTheme] = useState<"light" | "dark" | "system">(
    (currentTheme as "light" | "dark" | "system") ?? "light"
  );
  const [currency, setCurrency] = useState<"TRY" | "USD" | "EUR">(
    preferences?.currency ?? "TRY"
  );
  const [syncInterval, setSyncInterval] = useState<SyncInterval>(
    preferences?.syncInterval ?? 60
  );
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    if (currentTheme) setTheme(currentTheme as "light" | "dark" | "system");
    if (preferences?.currency) setCurrency(preferences.currency);
    if (preferences?.syncInterval !== undefined) setSyncInterval(preferences.syncInterval);
  }, [currentTheme, preferences]);

  const handleAppearanceSave = async () => {
    try {
      // Apply theme to next-themes
      setNextTheme(theme);

      await updatePreferencesMutation.mutateAsync({
        theme,
        currency,
        syncInterval,
      });
      setSaved(true);
      setTimeout(() => setSaved(false), 3000);
    } catch (error) {
      if (isDev) console.error("Görünüm tercihleri kaydedilemedi:", error);
    }
  };

  const themeOptions = [
    {
      value: "light",
      label: "Açık Tema",
      icon: Sun,
      description: "Klasik aydınlık arayüz",
    },
    {
      value: "dark",
      label: "Koyu Tema",
      icon: Moon,
      description: "Gözleri yormayan karanlık mod",
    },
    {
      value: "system",
      label: "Sistem",
      icon: Monitor,
      description: "Cihazınızın temasını takip eder",
    },
  ];

  const syncIntervalOptions: { value: SyncInterval; label: string }[] = [
    { value: 0, label: "Kapalı" },
    { value: 30, label: "30 saniye" },
    { value: 60, label: "1 dakika" },
    { value: 120, label: "2 dakika" },
    { value: 300, label: "5 dakika" },
  ];

  return (
    <>
      <SettingsSection
        title="Görünüm Ayarları"
        description="Uygulama görünümünü ve bölgesel tercihlerinizi özelleştirin"
      >
      <div className="space-y-6">
        {/* Theme Selection */}
        <div className="space-y-3">
          <Label>Tema</Label>
          <div className="grid gap-3 sm:grid-cols-3">
            {themeOptions.map((option) => {
              const Icon = option.icon;
              const isSelected = theme === option.value;

              return (
                <button
                  key={option.value}
                  onClick={() => setTheme(option.value as typeof theme)}
                  className={cn(
                    "relative flex flex-col items-center gap-2 p-4 rounded-xl border-2 transition-all",
                    isSelected
                      ? "border-primary bg-primary/10"
                      : "border-border hover:border-muted-foreground/30"
                  )}
                >
                  <div
                    className={cn(
                      "h-10 w-10 rounded-lg flex items-center justify-center",
                      isSelected ? "bg-primary text-primary-foreground" : "bg-muted text-muted-foreground"
                    )}
                  >
                    <Icon className="h-5 w-5" />
                  </div>
                  <div className="text-center">
                    <p className={cn("font-medium", isSelected ? "text-primary" : "text-foreground")}>
                      {option.label}
                    </p>
                    <p className="text-xs text-muted-foreground mt-0.5">{option.description}</p>
                  </div>
                  {isSelected && (
                    <CheckCircle2 className="absolute top-2 right-2 h-5 w-5 text-primary" />
                  )}
                </button>
              );
            })}
          </div>
        </div>

        {/* Currency Selection */}
        <div className="space-y-2 max-w-xs">
          <Label>Para Birimi</Label>
          <Select
            value={currency}
            onValueChange={(v) => setCurrency(v as typeof currency)}
          >
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="TRY">
                <span className="flex items-center gap-2">
                  <span className="text-lg">₺</span>
                  TRY - Türk Lirası
                </span>
              </SelectItem>
              <SelectItem value="USD">
                <span className="flex items-center gap-2">
                  <span className="text-lg">$</span>
                  USD - Amerikan Doları
                </span>
              </SelectItem>
              <SelectItem value="EUR">
                <span className="flex items-center gap-2">
                  <span className="text-lg">€</span>
                  EUR - Euro
                </span>
              </SelectItem>
            </SelectContent>
          </Select>
          <p className="text-xs text-muted-foreground">
            Dashboard ve raporlarda gösterilecek para birimi
          </p>
        </div>

        {/* Sync Interval Selection */}
        <div className="space-y-2 max-w-xs">
          <Label className="flex items-center gap-2">
            <RefreshCw className="h-4 w-4" />
            Otomatik Yenileme
          </Label>
          <Select
            value={syncInterval.toString()}
            onValueChange={(v) => setSyncInterval(Number(v) as SyncInterval)}
          >
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              {syncIntervalOptions.map((option) => (
                <SelectItem key={option.value} value={option.value.toString()}>
                  {option.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
          <p className="text-xs text-muted-foreground">
            Siparişler, ürünler ve diğer verilerin ne sıklıkla güncelleneceğini belirler.
            Daha sık güncelleme daha fazla veri kullanır.
          </p>
        </div>
      </div>

      <div className="flex justify-end pt-6 mt-6 border-t border-border">
        <Button
          onClick={handleAppearanceSave}
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
              Ayarları Kaydet
            </>
          )}
        </Button>
      </div>
      </SettingsSection>

      {/* Sync Log Panel - only visible in development mode */}
      <div className="mt-8">
        <SyncLogPanel />
      </div>
    </>
  );
}
