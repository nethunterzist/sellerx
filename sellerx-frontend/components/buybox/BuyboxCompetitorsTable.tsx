"use client";

import { Trophy, Star, Truck } from "lucide-react";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { MerchantInfo } from "@/types/buybox";

interface BuyboxCompetitorsTableProps {
  competitors: MerchantInfo[];
  myMerchantId?: number;
}

export function BuyboxCompetitorsTable({
  competitors,
  myMerchantId,
}: BuyboxCompetitorsTableProps) {
  const formatPrice = (price: number | undefined) => {
    if (price === undefined || price === null) return "-";
    return new Intl.NumberFormat("tr-TR", {
      style: "currency",
      currency: "TRY",
    }).format(price);
  };

  if (!competitors || competitors.length === 0) {
    return (
      <div className="text-center py-8 text-muted-foreground">
        Rakip satıcı bilgisi bulunamadı
      </div>
    );
  }

  return (
    <div className="border rounded-lg">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead className="w-[50px]">#</TableHead>
            <TableHead>Satıcı</TableHead>
            <TableHead className="text-right">Fiyat</TableHead>
            <TableHead className="text-center">Puan</TableHead>
            <TableHead className="text-center">Kargo</TableHead>
            <TableHead>Teslimat</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {competitors.map((merchant, index) => {
            const isMe = myMerchantId && merchant.merchantId === myMerchantId;
            return (
              <TableRow
                key={merchant.merchantId}
                className={isMe ? "bg-blue-50" : ""}
              >
                <TableCell>
                  {merchant.isWinner ? (
                    <Trophy className="h-4 w-4 text-yellow-500" />
                  ) : (
                    <span className="text-muted-foreground">{index + 1}</span>
                  )}
                </TableCell>
                <TableCell>
                  <div className="flex items-center gap-2">
                    <span className={isMe ? "font-bold text-blue-600" : ""}>
                      {merchant.merchantName}
                    </span>
                    {isMe && (
                      <Badge variant="outline" className="text-xs">
                        Siz
                      </Badge>
                    )}
                    {merchant.isWinner && (
                      <Badge className="bg-yellow-100 text-yellow-800 text-xs">
                        Buybox
                      </Badge>
                    )}
                  </div>
                </TableCell>
                <TableCell className="text-right font-medium">
                  {formatPrice(merchant.price)}
                </TableCell>
                <TableCell className="text-center">
                  {merchant.sellerScore ? (
                    <div className="flex items-center justify-center gap-1">
                      <Star className="h-3 w-3 text-yellow-500 fill-yellow-500" />
                      <span className="text-sm">{merchant.sellerScore.toFixed(1)}</span>
                    </div>
                  ) : (
                    "-"
                  )}
                </TableCell>
                <TableCell className="text-center">
                  {merchant.isFreeCargo ? (
                    <Truck className="h-4 w-4 text-green-500 mx-auto" />
                  ) : (
                    <span className="text-muted-foreground text-sm">Ücretli</span>
                  )}
                </TableCell>
                <TableCell>
                  <span className="text-sm text-muted-foreground">
                    {merchant.deliveryDate || "-"}
                  </span>
                </TableCell>
              </TableRow>
            );
          })}
        </TableBody>
      </Table>
    </div>
  );
}
