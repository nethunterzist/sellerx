"use client";

import { format } from "date-fns";
import { tr } from "date-fns/locale";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { BuyboxSnapshot, getBuyboxStatusLabel, getBuyboxStatusColor } from "@/types/buybox";

interface BuyboxHistoryTableProps {
  history: BuyboxSnapshot[];
}

export function BuyboxHistoryTable({ history }: BuyboxHistoryTableProps) {
  const formatPrice = (price: number | undefined) => {
    if (price === undefined || price === null) return "-";
    return new Intl.NumberFormat("tr-TR", {
      style: "currency",
      currency: "TRY",
    }).format(price);
  };

  const getStatusBadgeVariant = (status: string) => {
    const colorMap: Record<string, "default" | "secondary" | "destructive" | "outline"> = {
      success: "default",
      destructive: "destructive",
      warning: "secondary",
      secondary: "outline",
    };
    return colorMap[getBuyboxStatusColor(status as any)] || "secondary";
  };

  if (!history || history.length === 0) {
    return (
      <div className="text-center py-8 text-muted-foreground">
        Henüz geçmiş verisi yok
      </div>
    );
  }

  return (
    <div className="border rounded-lg">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Tarih</TableHead>
            <TableHead>Durum</TableHead>
            <TableHead>Kazanan</TableHead>
            <TableHead className="text-right">Kazanan Fiyat</TableHead>
            <TableHead className="text-right">Sizin Fiyat</TableHead>
            <TableHead className="text-center">Sıranız</TableHead>
            <TableHead className="text-center">Satıcı Sayısı</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {history.map((snapshot) => (
            <TableRow key={snapshot.id}>
              <TableCell>
                <span className="text-sm">
                  {format(new Date(snapshot.checkedAt), "dd MMM yyyy HH:mm", {
                    locale: tr,
                  })}
                </span>
              </TableCell>
              <TableCell>
                <Badge variant={getStatusBadgeVariant(snapshot.buyboxStatus)}>
                  {getBuyboxStatusLabel(snapshot.buyboxStatus)}
                </Badge>
              </TableCell>
              <TableCell>
                <span className="text-sm">
                  {snapshot.winnerMerchantName || "-"}
                </span>
              </TableCell>
              <TableCell className="text-right">
                {formatPrice(snapshot.winnerPrice)}
              </TableCell>
              <TableCell className="text-right">
                {formatPrice(snapshot.myPrice)}
              </TableCell>
              <TableCell className="text-center">
                {snapshot.myPosition || "-"}
              </TableCell>
              <TableCell className="text-center">
                {snapshot.totalSellers || "-"}
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}
