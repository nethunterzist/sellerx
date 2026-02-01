"use client";

import { useParams, useRouter } from "next/navigation";
import { ArrowLeft, RefreshCw, Trash2, ExternalLink } from "lucide-react";
import Link from "next/link";
import {
  useBuyboxProductDetail,
  useCheckBuyboxNow,
  useRemoveProductFromTrack,
} from "@/hooks/queries/use-buybox";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  BuyboxCompetitorsTable,
  BuyboxHistoryTable,
  BuyboxAlertSettings,
} from "@/components/buybox";
import { getBuyboxStatusLabel, getBuyboxStatusColor } from "@/types/buybox";

export default function BuyboxProductDetailPage() {
  const params = useParams();
  const router = useRouter();
  const trackedProductId = params.id as string;

  const {
    data: productDetail,
    isLoading,
    refetch,
  } = useBuyboxProductDetail(trackedProductId);

  const checkNow = useCheckBuyboxNow(trackedProductId);
  const removeProduct = useRemoveProductFromTrack(productDetail?.storeId);

  const handleCheckNow = async () => {
    try {
      await checkNow.mutateAsync();
      refetch();
    } catch (error) {
      // Error handled by mutation
    }
  };

  const handleRemove = async () => {
    if (!confirm("Bu ürünü takipten çıkarmak istediğinize emin misiniz?")) {
      return;
    }
    try {
      await removeProduct.mutateAsync(trackedProductId);
      router.push("/buybox");
    } catch (error) {
      // Error handled by mutation
    }
  };

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

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary" />
      </div>
    );
  }

  if (!productDetail) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[400px] text-center">
        <h2 className="text-xl font-semibold mb-2">Ürün Bulunamadı</h2>
        <p className="text-muted-foreground mb-4">
          Aradığınız ürün takip listesinde bulunamadı.
        </p>
        <Link href="/buybox">
          <Button variant="outline">
            <ArrowLeft className="h-4 w-4 mr-2" />
            Listeye Dön
          </Button>
        </Link>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-start justify-between">
        <div className="flex items-start gap-4">
          <Link href="/buybox">
            <Button variant="ghost" size="icon">
              <ArrowLeft className="h-5 w-5" />
            </Button>
          </Link>
          <div className="flex items-start gap-4">
            {productDetail.productImageUrl ? (
              <img
                src={productDetail.productImageUrl}
                alt={productDetail.productTitle}
                className="w-20 h-20 object-cover rounded-lg"
              />
            ) : (
              <div className="w-20 h-20 bg-muted rounded-lg flex items-center justify-center">
                <span className="text-muted-foreground text-xs">N/A</span>
              </div>
            )}
            <div>
              <h1 className="text-xl font-bold line-clamp-2">
                {productDetail.productTitle}
              </h1>
              <div className="flex items-center gap-2 mt-1">
                <span className="text-sm text-muted-foreground">
                  {productDetail.productBarcode}
                </span>
                {productDetail.trendyolUrl && (
                  <a
                    href={productDetail.trendyolUrl}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="text-blue-600 hover:text-blue-700"
                  >
                    <ExternalLink className="h-4 w-4" />
                  </a>
                )}
              </div>
              <div className="flex items-center gap-2 mt-2">
                <Badge variant={getStatusBadgeVariant(productDetail.currentStatus)}>
                  {getBuyboxStatusLabel(productDetail.currentStatus)}
                </Badge>
                {!productDetail.isActive && (
                  <Badge variant="outline">Pasif</Badge>
                )}
              </div>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={handleCheckNow}
            disabled={checkNow.isPending}
          >
            <RefreshCw
              className={`h-4 w-4 mr-2 ${checkNow.isPending ? "animate-spin" : ""}`}
            />
            {checkNow.isPending ? "Kontrol Ediliyor..." : "Şimdi Kontrol Et"}
          </Button>
          <Button
            variant="destructive"
            size="sm"
            onClick={handleRemove}
            disabled={removeProduct.isPending}
          >
            <Trash2 className="h-4 w-4 mr-2" />
            Takipten Çıkar
          </Button>
        </div>
      </div>

      {/* Current Status Summary */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              Sizin Fiyatınız
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-2xl font-bold">
              {formatPrice(productDetail.myPrice)}
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              Kazanan Fiyat
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-2xl font-bold">
              {formatPrice(productDetail.winnerPrice)}
            </p>
            <p className="text-xs text-muted-foreground mt-1">
              {productDetail.winnerName || "-"}
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              Sıranız
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p className="text-2xl font-bold">
              {productDetail.myPosition
                ? `${productDetail.myPosition}/${productDetail.totalSellers}`
                : "-"}
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="pb-2">
            <CardTitle className="text-sm font-medium text-muted-foreground">
              Fiyat Farkı
            </CardTitle>
          </CardHeader>
          <CardContent>
            <p
              className={`text-2xl font-bold ${
                (productDetail.priceDifference || 0) > 0
                  ? "text-red-600"
                  : (productDetail.priceDifference || 0) < 0
                    ? "text-green-600"
                    : ""
              }`}
            >
              {productDetail.priceDifference !== undefined
                ? `${productDetail.priceDifference > 0 ? "+" : ""}${formatPrice(productDetail.priceDifference)}`
                : "-"}
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Tabs */}
      <Tabs defaultValue="competitors" className="space-y-4">
        <TabsList>
          <TabsTrigger value="competitors">
            Rakipler ({productDetail.competitors?.length || 0})
          </TabsTrigger>
          <TabsTrigger value="history">
            Geçmiş ({productDetail.history?.length || 0})
          </TabsTrigger>
          <TabsTrigger value="settings">Ayarlar</TabsTrigger>
        </TabsList>

        <TabsContent value="competitors">
          <BuyboxCompetitorsTable
            competitors={productDetail.competitors || []}
            myMerchantId={productDetail.winnerMerchantId}
          />
        </TabsContent>

        <TabsContent value="history">
          <BuyboxHistoryTable history={productDetail.history || []} />
        </TabsContent>

        <TabsContent value="settings">
          <BuyboxAlertSettings product={productDetail} />
        </TabsContent>
      </Tabs>
    </div>
  );
}
