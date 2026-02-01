"use client";

import { useState, useEffect } from "react";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import { Switch } from "@/components/ui/switch";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useUpdateAlertSettings } from "@/hooks/queries/use-buybox";
import { BuyboxProductDetail } from "@/types/buybox";
import { Save } from "lucide-react";

interface BuyboxAlertSettingsProps {
  product: BuyboxProductDetail;
}

export function BuyboxAlertSettings({ product }: BuyboxAlertSettingsProps) {
  const updateSettings = useUpdateAlertSettings(product.id);

  const [alertOnLoss, setAlertOnLoss] = useState(product.alertOnLoss);
  const [alertOnNewCompetitor, setAlertOnNewCompetitor] = useState(
    product.alertOnNewCompetitor
  );
  const [alertPriceThreshold, setAlertPriceThreshold] = useState(
    product.alertPriceThreshold.toString()
  );
  const [isActive, setIsActive] = useState(product.isActive);

  const [hasChanges, setHasChanges] = useState(false);

  useEffect(() => {
    const changed =
      alertOnLoss !== product.alertOnLoss ||
      alertOnNewCompetitor !== product.alertOnNewCompetitor ||
      parseFloat(alertPriceThreshold) !== product.alertPriceThreshold ||
      isActive !== product.isActive;
    setHasChanges(changed);
  }, [
    alertOnLoss,
    alertOnNewCompetitor,
    alertPriceThreshold,
    isActive,
    product,
  ]);

  const handleSave = async () => {
    try {
      await updateSettings.mutateAsync({
        alertOnLoss,
        alertOnNewCompetitor,
        alertPriceThreshold: parseFloat(alertPriceThreshold),
        isActive,
      });
    } catch (error) {
      // Error handled by mutation
    }
  };

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-lg">Uyarı Ayarları</CardTitle>
      </CardHeader>
      <CardContent className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <Label htmlFor="active">Takip Aktif</Label>
            <p className="text-sm text-muted-foreground">
              Bu ürünün buybox takibi yapılsın
            </p>
          </div>
          <Switch
            id="active"
            checked={isActive}
            onCheckedChange={setIsActive}
          />
        </div>

        <div className="flex items-center justify-between">
          <div>
            <Label htmlFor="alertOnLoss">Buybox Kaybı Uyarısı</Label>
            <p className="text-sm text-muted-foreground">
              Buybox kaybedildiğinde bildirim al
            </p>
          </div>
          <Switch
            id="alertOnLoss"
            checked={alertOnLoss}
            onCheckedChange={setAlertOnLoss}
          />
        </div>

        <div className="flex items-center justify-between">
          <div>
            <Label htmlFor="alertOnNewCompetitor">Yeni Rakip Uyarısı</Label>
            <p className="text-sm text-muted-foreground">
              Yeni satıcı eklendiğinde bildirim al
            </p>
          </div>
          <Switch
            id="alertOnNewCompetitor"
            checked={alertOnNewCompetitor}
            onCheckedChange={setAlertOnNewCompetitor}
          />
        </div>

        <div>
          <Label htmlFor="priceThreshold">Fiyat Risk Eşiği (TL)</Label>
          <p className="text-sm text-muted-foreground mb-2">
            Rakiple fiyat farkı bu değerin altına düştüğünde uyarı al
          </p>
          <Input
            id="priceThreshold"
            type="number"
            step="0.01"
            min="0.01"
            value={alertPriceThreshold}
            onChange={(e) => setAlertPriceThreshold(e.target.value)}
            className="w-32"
          />
        </div>

        <Button
          onClick={handleSave}
          disabled={!hasChanges || updateSettings.isPending}
        >
          <Save className="h-4 w-4 mr-2" />
          {updateSettings.isPending ? "Kaydediliyor..." : "Kaydet"}
        </Button>
      </CardContent>
    </Card>
  );
}
