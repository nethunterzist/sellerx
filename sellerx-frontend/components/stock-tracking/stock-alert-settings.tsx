"use client";

import { useState, useEffect } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { Settings, Loader2 } from "lucide-react";
import type { TrackedProduct, UpdateAlertSettingsRequest } from "@/types/stock-tracking";
import { useUpdateAlertSettings } from "@/hooks/queries/use-stock-tracking";
import { useTranslations } from "next-intl";
import { toast } from "sonner";

interface StockAlertSettingsProps {
  product: TrackedProduct | undefined;
  storeId: string | undefined;
  isLoading: boolean;
}

export function StockAlertSettings({
  product,
  storeId,
  isLoading,
}: StockAlertSettingsProps) {
  const t = useTranslations("stockTracking");

  const [alertOnOutOfStock, setAlertOnOutOfStock] = useState(true);
  const [alertOnLowStock, setAlertOnLowStock] = useState(true);
  const [lowStockThreshold, setLowStockThreshold] = useState(10);
  const [alertOnStockIncrease, setAlertOnStockIncrease] = useState(false);
  const [alertOnBackInStock, setAlertOnBackInStock] = useState(true);
  const [isActive, setIsActive] = useState(true);
  const [hasChanges, setHasChanges] = useState(false);

  const updateSettings = useUpdateAlertSettings(product?.id, storeId);

  // Initialize form when product loads
  useEffect(() => {
    if (product) {
      setAlertOnOutOfStock(product.alertOnOutOfStock);
      setAlertOnLowStock(product.alertOnLowStock);
      setLowStockThreshold(product.lowStockThreshold);
      setAlertOnStockIncrease(product.alertOnStockIncrease);
      setAlertOnBackInStock(product.alertOnBackInStock);
      setIsActive(product.isActive);
      setHasChanges(false);
    }
  }, [product]);

  // Track changes
  useEffect(() => {
    if (!product) return;

    const changed =
      alertOnOutOfStock !== product.alertOnOutOfStock ||
      alertOnLowStock !== product.alertOnLowStock ||
      lowStockThreshold !== product.lowStockThreshold ||
      alertOnStockIncrease !== product.alertOnStockIncrease ||
      alertOnBackInStock !== product.alertOnBackInStock ||
      isActive !== product.isActive;

    setHasChanges(changed);
  }, [
    product,
    alertOnOutOfStock,
    alertOnLowStock,
    lowStockThreshold,
    alertOnStockIncrease,
    alertOnBackInStock,
    isActive,
  ]);

  const handleSave = async () => {
    const settings: UpdateAlertSettingsRequest = {
      alertOnOutOfStock,
      alertOnLowStock,
      lowStockThreshold,
      alertOnStockIncrease,
      alertOnBackInStock,
      isActive,
    };

    try {
      await updateSettings.mutateAsync(settings);
      toast.success(t("settings.saveSuccess"));
      setHasChanges(false);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : t("settings.saveError"));
    }
  };

  if (isLoading) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Settings className="h-5 w-5" />
            {t("settings.title")}
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            {[1, 2, 3, 4, 5].map((i) => (
              <div key={i} className="animate-pulse flex items-center justify-between">
                <div className="space-y-1">
                  <div className="h-4 w-32 bg-gray-200 rounded" />
                  <div className="h-3 w-48 bg-gray-200 rounded" />
                </div>
                <div className="h-6 w-10 bg-gray-200 rounded-full" />
              </div>
            ))}
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Settings className="h-5 w-5" />
          {t("settings.title")}
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-6">
        <div className="flex items-center justify-between">
          <div className="space-y-0.5">
            <Label htmlFor="isActive">{t("settings.isActive")}</Label>
            <p className="text-xs text-muted-foreground">
              {t("settings.isActiveDesc")}
            </p>
          </div>
          <Switch
            id="isActive"
            checked={isActive}
            onCheckedChange={setIsActive}
            disabled={updateSettings.isPending}
          />
        </div>

        <hr />

        <div className="flex items-center justify-between">
          <div className="space-y-0.5">
            <Label htmlFor="alertOutOfStock">{t("settings.alertOnOutOfStock")}</Label>
            <p className="text-xs text-muted-foreground">
              {t("settings.alertOnOutOfStockDesc")}
            </p>
          </div>
          <Switch
            id="alertOutOfStock"
            checked={alertOnOutOfStock}
            onCheckedChange={setAlertOnOutOfStock}
            disabled={updateSettings.isPending}
          />
        </div>

        <div className="flex items-center justify-between">
          <div className="space-y-0.5">
            <Label htmlFor="alertLowStock">{t("settings.alertOnLowStock")}</Label>
            <p className="text-xs text-muted-foreground">
              {t("settings.alertOnLowStockDesc")}
            </p>
          </div>
          <Switch
            id="alertLowStock"
            checked={alertOnLowStock}
            onCheckedChange={setAlertOnLowStock}
            disabled={updateSettings.isPending}
          />
        </div>

        {alertOnLowStock && (
          <div className="space-y-2 pl-4 border-l-2 border-muted">
            <Label htmlFor="threshold">{t("settings.lowStockThreshold")}</Label>
            <div className="flex items-center gap-2">
              <Input
                id="threshold"
                type="number"
                min={1}
                max={1000}
                value={lowStockThreshold}
                onChange={(e) => setLowStockThreshold(parseInt(e.target.value) || 10)}
                disabled={updateSettings.isPending}
                className="w-24"
              />
              <span className="text-sm text-muted-foreground">{t("table.unit")}</span>
            </div>
            <p className="text-xs text-muted-foreground">
              {t("settings.lowStockThresholdDesc")}
            </p>
          </div>
        )}

        <div className="flex items-center justify-between">
          <div className="space-y-0.5">
            <Label htmlFor="alertBackInStock">{t("settings.alertOnBackInStock")}</Label>
            <p className="text-xs text-muted-foreground">
              {t("settings.alertOnBackInStockDesc")}
            </p>
          </div>
          <Switch
            id="alertBackInStock"
            checked={alertOnBackInStock}
            onCheckedChange={setAlertOnBackInStock}
            disabled={updateSettings.isPending}
          />
        </div>

        <div className="flex items-center justify-between">
          <div className="space-y-0.5">
            <Label htmlFor="alertStockIncrease">{t("settings.alertOnStockIncrease")}</Label>
            <p className="text-xs text-muted-foreground">
              {t("settings.alertOnStockIncreaseDesc")}
            </p>
          </div>
          <Switch
            id="alertStockIncrease"
            checked={alertOnStockIncrease}
            onCheckedChange={setAlertOnStockIncrease}
            disabled={updateSettings.isPending}
          />
        </div>

        {hasChanges && (
          <div className="flex justify-end pt-4 border-t">
            <Button onClick={handleSave} disabled={updateSettings.isPending}>
              {updateSettings.isPending && (
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              )}
              {t("settings.save")}
            </Button>
          </div>
        )}
      </CardContent>
    </Card>
  );
}
