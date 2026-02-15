"use client";

import { Button } from "@/components/ui/button";
import { Download } from "lucide-react";
import { useState } from "react";
import type { ReturnedOrderDecision } from "@/types/returns";
import { format } from "date-fns";
import { tr } from "date-fns/locale";

interface ReturnExportButtonProps {
  orders: ReturnedOrderDecision[];
  formatCurrency: (value: number) => string;
  isLoading?: boolean;
}

function getSourceLabel(source?: string): string {
  switch (source) {
    case "order_status":
      return "Siparis";
    case "claim":
      return "Talep";
    case "cargo_invoice":
      return "Kargo";
    default:
      return "-";
  }
}

function getDecisionLabel(isResalable: boolean | null): string {
  if (isResalable === true) return "Satilabilir";
  if (isResalable === false) return "Satilamaz";
  return "Karar bekleniyor";
}

export function ReturnExportButton({
  orders,
  formatCurrency,
  isLoading = false,
}: ReturnExportButtonProps) {
  const [isExporting, setIsExporting] = useState(false);

  const handleExport = async () => {
    if (orders.length === 0) return;

    setIsExporting(true);

    try {
      const { exportToXLSX } = await import("@/lib/utils/lazy-excel");

      const headers = [
        "Siparis No",
        "Musteri",
        "Tarih",
        "Urunler",
        "Iade Sebebi",
        "Kaynak",
        "Gonderi Kargo",
        "Iade Kargo",
        "Urun Maliyeti",
        "Toplam Zarar",
        "Karar",
      ];

      const rows = orders.map((order) => {
        const products = order.items
          .map((item) => `${item.quantity}x ${item.productName}`)
          .join(", ");
        return [
          order.orderNumber,
          order.customerName || "-",
          format(new Date(order.orderDate), "dd.MM.yyyy", { locale: tr }),
          products,
          order.returnReason || "-",
          getSourceLabel(order.returnSource),
          order.shippingCostOut || 0,
          order.shippingCostReturn || 0,
          order.productCost || 0,
          order.totalLoss || 0,
          getDecisionLabel(order.isResalable),
        ];
      });

      const dateStr = new Date().toISOString().split("T")[0];
      await exportToXLSX(
        { headers, rows },
        `iade-kararlari-${dateStr}.xlsx`,
        "Iade Kararlari",
        [18, 20, 12, 40, 25, 10, 14, 14, 14, 14, 16]
      );
    } finally {
      setIsExporting(false);
    }
  };

  return (
    <Button
      variant="outline"
      size="sm"
      onClick={handleExport}
      disabled={isExporting || orders.length === 0 || isLoading}
      className="gap-2"
    >
      <Download className="h-4 w-4" />
      Excel Indir
    </Button>
  );
}
