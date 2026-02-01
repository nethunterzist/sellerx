"use client";

import { Bell, X } from "lucide-react";
import { Alert, AlertDescription } from "@/components/ui/alert";
import { Button } from "@/components/ui/button";
import { BuyboxAlert, getAlertTypeLabel } from "@/types/buybox";
import { useMarkAlertsRead } from "@/hooks/queries/use-buybox";

interface BuyboxAlertBannerProps {
  alerts: BuyboxAlert[];
  storeId: string;
}

export function BuyboxAlertBanner({ alerts, storeId }: BuyboxAlertBannerProps) {
  const markAsRead = useMarkAlertsRead(storeId);

  if (!alerts || alerts.length === 0) {
    return null;
  }

  const handleDismiss = () => {
    markAsRead.mutate();
  };

  return (
    <Alert variant="destructive" className="bg-red-50 border-red-200">
      <Bell className="h-4 w-4" />
      <AlertDescription className="flex items-center justify-between w-full">
        <div className="flex items-center gap-2">
          <span className="font-medium">{alerts.length} yeni uyarı:</span>
          <span className="text-sm">
            {alerts.slice(0, 2).map((alert, i) => (
              <span key={alert.id}>
                {i > 0 && ", "}
                {getAlertTypeLabel(alert.alertType)} - {alert.productTitle}
              </span>
            ))}
            {alerts.length > 2 && ` ve ${alerts.length - 2} daha...`}
          </span>
        </div>
        <Button
          variant="ghost"
          size="sm"
          onClick={handleDismiss}
          disabled={markAsRead.isPending}
        >
          <X className="h-4 w-4" />
        </Button>
      </AlertDescription>
    </Alert>
  );
}
