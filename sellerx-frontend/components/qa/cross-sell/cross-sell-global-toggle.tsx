"use client";

import { useTranslations } from "next-intl";
import { Card, CardContent } from "@/components/ui/card";
import { Switch } from "@/components/ui/switch";
import { Badge } from "@/components/ui/badge";
import {
  useCrossSellSettings,
  useUpdateCrossSellSettings,
} from "@/hooks/queries/use-cross-sell";
import { ShoppingBag, Loader2 } from "lucide-react";
import { toast } from "sonner";

interface CrossSellGlobalToggleProps {
  storeId: string;
}

export function CrossSellGlobalToggle({
  storeId,
}: CrossSellGlobalToggleProps) {
  const t = useTranslations("qa.crossSell.globalToggle");
  const { data: settings, isLoading } = useCrossSellSettings(storeId);
  const updateMutation = useUpdateCrossSellSettings();

  const handleToggle = async (enabled: boolean) => {
    try {
      await updateMutation.mutateAsync({
        storeId,
        data: { enabled },
      });
      toast.success(enabled ? t("enabled") : t("disabled"));
    } catch {
      toast.error(t("error"));
    }
  };

  const isEnabled = settings?.enabled ?? false;

  return (
    <Card className="border-dashed">
      <CardContent className="flex items-center justify-between p-4">
        <div className="flex items-center gap-3">
          <div className="flex items-center justify-center h-9 w-9 rounded-lg bg-amber-100 dark:bg-amber-900/30">
            <ShoppingBag className="h-5 w-5 text-amber-600 dark:text-amber-400" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h3 className="text-sm font-medium">{t("title")}</h3>
              <Badge
                variant="outline"
                className={
                  isEnabled
                    ? "bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400"
                    : "bg-gray-100 text-gray-600 dark:bg-gray-900/30 dark:text-gray-400"
                }
              >
                {isEnabled ? t("active") : t("inactive")}
              </Badge>
            </div>
            <p className="text-xs text-muted-foreground mt-0.5">
              {t("description")}
            </p>
          </div>
        </div>

        {isLoading ? (
          <Loader2 className="h-4 w-4 animate-spin text-muted-foreground" />
        ) : (
          <Switch
            checked={isEnabled}
            onCheckedChange={handleToggle}
            disabled={updateMutation.isPending}
          />
        )}
      </CardContent>
    </Card>
  );
}
