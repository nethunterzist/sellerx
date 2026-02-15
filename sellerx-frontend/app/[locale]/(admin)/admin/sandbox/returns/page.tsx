"use client";

import { useState } from "react";
import { FlaskConical, Plus, Trash2, RotateCcw, Calendar, AlertTriangle } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
  DialogFooter,
} from "@/components/ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Skeleton } from "@/components/ui/skeleton";
import { Badge } from "@/components/ui/badge";
import { toast } from "sonner";
import {
  useSandboxReturns,
  useSandboxOrders,
  useCreateSandboxReturn,
  useDeleteSandboxReturn,
  type CreateSandboxReturnRequest,
} from "@/hooks/queries/use-admin";

const defaultFormData: CreateSandboxReturnRequest = {
  orderNumber: "",
  barcode: "",
  productName: "",
  quantity: 1,
  productCost: 0,
  shippingCostOut: 25,
  shippingCostReturn: 25,
  commissionLoss: 0,
  packagingCost: 5,
  returnReason: "",
};

export default function SandboxReturnsPage() {
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [formData, setFormData] = useState<CreateSandboxReturnRequest>(defaultFormData);

  const { data: returns, isLoading, error } = useSandboxReturns();
  const { data: orders } = useSandboxOrders();
  const createReturn = useCreateSandboxReturn();
  const deleteReturn = useDeleteSandboxReturn();

  // Delivered orders only
  const deliveredOrders = orders?.filter(o => o.status === "Delivered") || [];

  const handleInputChange = (
    field: keyof CreateSandboxReturnRequest,
    value: string | number
  ) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  const handleOrderSelect = (orderNumber: string) => {
    const order = orders?.find((o) => o.tyOrderNumber === orderNumber);
    if (order) {
      const orderItem = order.orderItems?.[0];
      setFormData((prev) => ({
        ...prev,
        orderNumber,
        barcode: orderItem?.barcode || "",
        productName: orderItem?.productName || "",
        commissionLoss: order.estimatedCommission || 0,
      }));
    }
  };

  const handleSubmit = async () => {
    if (!formData.orderNumber || !formData.barcode) {
      toast.error("Sipariş ve ürün seçmeniz gerekiyor");
      return;
    }
    if (formData.quantity < 1) {
      toast.error("Adet en az 1 olmalı");
      return;
    }

    try {
      await createReturn.mutateAsync(formData);
      toast.success("İade başarıyla eklendi");
      setIsDialogOpen(false);
      setFormData(defaultFormData);
    } catch (err) {
      toast.error("İade eklenemedi");
    }
  };

  const handleDelete = async (returnId: string) => {
    if (!confirm("Bu iadeyi silmek istediğinizden emin misiniz?")) return;

    try {
      await deleteReturn.mutateAsync(returnId);
      toast.success("İade silindi");
    } catch (err) {
      toast.error("İade silinemedi");
    }
  };

  const formatPrice = (price: number | undefined | null) => {
    if (price === undefined || price === null) return "-";
    return new Intl.NumberFormat("tr-TR", {
      style: "currency",
      currency: "TRY",
    }).format(price);
  };

  const formatDate = (dateStr: string | undefined) => {
    if (!dateStr) return "-";
    try {
      return new Date(dateStr).toLocaleDateString("tr-TR", {
        day: "2-digit",
        month: "2-digit",
        year: "numeric",
      });
    } catch {
      return "-";
    }
  };

  // Calculate estimated total loss
  const totalLoss =
    (formData.productCost || 0) +
    (formData.shippingCostOut || 0) +
    (formData.shippingCostReturn || 0) +
    (formData.commissionLoss || 0) +
    (formData.packagingCost || 0);

  // Calculate all returns total loss
  const allReturnsLoss = returns?.reduce((sum, r) => sum + (r.totalLoss || 0), 0) || 0;

  if (error) {
    return (
      <div className="p-6">
        <Card className="border-red-200 bg-red-50">
          <CardContent className="pt-6">
            <p className="text-red-600">
              Sandbox iadeleri yüklenirken hata oluştu.
            </p>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <FlaskConical className="h-8 w-8 text-purple-500" />
          <div>
            <h1 className="text-2xl font-bold">Sandbox İade Yönetimi</h1>
            <p className="text-muted-foreground">
              Furkan Test Mağaza - Manuel iade ekleme ve maliyet hesaplama
            </p>
          </div>
        </div>

        <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
          <DialogTrigger asChild>
            <Button className="gap-2">
              <Plus className="h-4 w-4" />
              İade Ekle
            </Button>
          </DialogTrigger>
          <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
            <DialogHeader>
              <DialogTitle>Yeni Sandbox İadesi Ekle</DialogTitle>
            </DialogHeader>

            <div className="grid gap-6 py-4">
              {/* Sipariş Seçimi */}
              <div className="space-y-4">
                <h3 className="font-semibold text-sm text-muted-foreground uppercase tracking-wider">
                  Sipariş Seçimi
                </h3>
                <div className="space-y-2">
                  <Label htmlFor="order">Sipariş *</Label>
                  <Select
                    value={formData.orderNumber}
                    onValueChange={handleOrderSelect}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="Teslim edilmiş sipariş seçin..." />
                    </SelectTrigger>
                    <SelectContent>
                      {deliveredOrders.map((order) => (
                        <SelectItem key={order.id} value={order.tyOrderNumber || ""}>
                          {order.tyOrderNumber} - {order.orderItems?.[0]?.productName || "Ürün"}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  {deliveredOrders.length === 0 && (
                    <p className="text-sm text-muted-foreground">
                      Teslim edilmiş sipariş bulunamadı. Önce sipariş ekleyin.
                    </p>
                  )}
                </div>
                {formData.barcode && (
                  <div className="bg-muted/50 rounded-lg p-3 text-sm">
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Barkod:</span>
                      <span className="font-mono">{formData.barcode}</span>
                    </div>
                    <div className="flex justify-between">
                      <span className="text-muted-foreground">Ürün:</span>
                      <span>{formData.productName}</span>
                    </div>
                  </div>
                )}
              </div>

              {/* İade Bilgileri */}
              <div className="space-y-4">
                <h3 className="font-semibold text-sm text-muted-foreground uppercase tracking-wider">
                  İade Bilgileri
                </h3>
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="quantity">Adet *</Label>
                    <Input
                      id="quantity"
                      type="number"
                      min={1}
                      value={formData.quantity}
                      onChange={(e) =>
                        handleInputChange("quantity", Number(e.target.value))
                      }
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="returnReason">İade Sebebi</Label>
                    <Input
                      id="returnReason"
                      value={formData.returnReason}
                      onChange={(e) =>
                        handleInputChange("returnReason", e.target.value)
                      }
                      placeholder="Müşteri beğenmedi"
                    />
                  </div>
                </div>
              </div>

              {/* Maliyet Bileşenleri */}
              <div className="space-y-4">
                <h3 className="font-semibold text-sm text-muted-foreground uppercase tracking-wider">
                  Maliyet Bileşenleri
                </h3>
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="productCost">Ürün Maliyeti (TL)</Label>
                    <Input
                      id="productCost"
                      type="number"
                      step="0.01"
                      value={formData.productCost}
                      onChange={(e) =>
                        handleInputChange("productCost", Number(e.target.value))
                      }
                      placeholder="0"
                    />
                    <p className="text-xs text-muted-foreground">
                      Ürün hasarlı ise maliyet kaybı
                    </p>
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="commissionLoss">Komisyon Kaybı (TL)</Label>
                    <Input
                      id="commissionLoss"
                      type="number"
                      step="0.01"
                      value={formData.commissionLoss}
                      onChange={(e) =>
                        handleInputChange("commissionLoss", Number(e.target.value))
                      }
                    />
                    <p className="text-xs text-muted-foreground">
                      Siparişten otomatik alınır
                    </p>
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="shippingCostOut">Gidiş Kargosu (TL)</Label>
                    <Input
                      id="shippingCostOut"
                      type="number"
                      step="0.01"
                      value={formData.shippingCostOut}
                      onChange={(e) =>
                        handleInputChange("shippingCostOut", Number(e.target.value))
                      }
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="shippingCostReturn">Dönüş Kargosu (TL)</Label>
                    <Input
                      id="shippingCostReturn"
                      type="number"
                      step="0.01"
                      value={formData.shippingCostReturn}
                      onChange={(e) =>
                        handleInputChange("shippingCostReturn", Number(e.target.value))
                      }
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="packagingCost">Paketleme Maliyeti (TL)</Label>
                    <Input
                      id="packagingCost"
                      type="number"
                      step="0.01"
                      value={formData.packagingCost}
                      onChange={(e) =>
                        handleInputChange("packagingCost", Number(e.target.value))
                      }
                    />
                  </div>
                </div>
              </div>

              {/* Toplam Zarar Preview */}
              <div className="space-y-4">
                <h3 className="font-semibold text-sm text-muted-foreground uppercase tracking-wider">
                  Toplam Zarar Hesabı
                </h3>
                <div className="bg-red-50 border border-red-200 rounded-lg p-4 space-y-2">
                  <div className="flex justify-between text-sm">
                    <span>Ürün Maliyeti:</span>
                    <span>{formatPrice(formData.productCost)}</span>
                  </div>
                  <div className="flex justify-between text-sm">
                    <span>Gidiş Kargosu:</span>
                    <span>{formatPrice(formData.shippingCostOut)}</span>
                  </div>
                  <div className="flex justify-between text-sm">
                    <span>Dönüş Kargosu:</span>
                    <span>{formatPrice(formData.shippingCostReturn)}</span>
                  </div>
                  <div className="flex justify-between text-sm">
                    <span>Komisyon Kaybı:</span>
                    <span>{formatPrice(formData.commissionLoss)}</span>
                  </div>
                  <div className="flex justify-between text-sm">
                    <span>Paketleme:</span>
                    <span>{formatPrice(formData.packagingCost)}</span>
                  </div>
                  <div className="border-t border-red-300 pt-2 flex justify-between">
                    <span className="font-semibold text-red-700">TOPLAM ZARAR:</span>
                    <span className="font-bold text-red-700 text-lg">
                      {formatPrice(totalLoss)}
                    </span>
                  </div>
                </div>
              </div>
            </div>

            <DialogFooter>
              <Button variant="outline" onClick={() => setIsDialogOpen(false)}>
                İptal
              </Button>
              <Button
                onClick={handleSubmit}
                disabled={createReturn.isPending || !formData.orderNumber}
                className="bg-red-600 hover:bg-red-700"
              >
                {createReturn.isPending ? "Ekleniyor..." : "İade Ekle"}
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      </div>

      {/* Summary Card */}
      {returns && returns.length > 0 && (
        <Card className="border-red-200 bg-red-50">
          <CardContent className="pt-6">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <AlertTriangle className="h-8 w-8 text-red-500" />
                <div>
                  <p className="text-sm text-red-600">Toplam İade Zararı</p>
                  <p className="text-2xl font-bold text-red-700">
                    {formatPrice(allReturnsLoss)}
                  </p>
                </div>
              </div>
              <Badge variant="destructive" className="text-lg px-4 py-1">
                {returns.length} İade
              </Badge>
            </div>
          </CardContent>
        </Card>
      )}

      {/* Returns Table */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <RotateCcw className="h-5 w-5" />
            Sandbox İadeleri
            {returns && (
              <Badge variant="secondary" className="ml-2">
                {returns.length} iade
              </Badge>
            )}
          </CardTitle>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="space-y-3">
              {[...Array(5)].map((_, i) => (
                <Skeleton key={i} className="h-12 w-full" />
              ))}
            </div>
          ) : returns && returns.length > 0 ? (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Sipariş No</TableHead>
                  <TableHead>Tarih</TableHead>
                  <TableHead>Ürün</TableHead>
                  <TableHead className="text-center">Adet</TableHead>
                  <TableHead className="text-right">Ürün</TableHead>
                  <TableHead className="text-right">Kargo</TableHead>
                  <TableHead className="text-right">Komisyon</TableHead>
                  <TableHead className="text-right text-red-600">Toplam Zarar</TableHead>
                  <TableHead className="text-center">Sebep</TableHead>
                  <TableHead className="text-right">İşlem</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {returns.map((returnItem) => (
                  <TableRow key={returnItem.id}>
                    <TableCell className="font-mono text-sm">
                      {returnItem.order?.tyOrderNumber || "-"}
                    </TableCell>
                    <TableCell>
                      <div className="flex items-center gap-1 text-sm text-muted-foreground">
                        <Calendar className="h-3 w-3" />
                        {formatDate(returnItem.returnDate)}
                      </div>
                    </TableCell>
                    <TableCell className="max-w-[200px] truncate">
                      {returnItem.productName || "-"}
                    </TableCell>
                    <TableCell className="text-center">
                      {returnItem.quantity || 1}
                    </TableCell>
                    <TableCell className="text-right text-muted-foreground">
                      {formatPrice(returnItem.productCost)}
                    </TableCell>
                    <TableCell className="text-right text-muted-foreground">
                      {formatPrice(
                        (returnItem.shippingCostOut || 0) +
                          (returnItem.shippingCostReturn || 0)
                      )}
                    </TableCell>
                    <TableCell className="text-right text-muted-foreground">
                      {formatPrice(returnItem.commissionLoss)}
                    </TableCell>
                    <TableCell className="text-right">
                      <span className="font-semibold text-red-600">
                        {formatPrice(returnItem.totalLoss)}
                      </span>
                    </TableCell>
                    <TableCell className="text-center max-w-[120px] truncate">
                      {returnItem.returnReason ? (
                        <Badge variant="outline" className="text-xs">
                          {returnItem.returnReason}
                        </Badge>
                      ) : (
                        <span className="text-muted-foreground">-</span>
                      )}
                    </TableCell>
                    <TableCell className="text-right">
                      <Button
                        variant="ghost"
                        size="icon"
                        className="text-red-500 hover:text-red-700 hover:bg-red-50"
                        onClick={() => handleDelete(returnItem.id)}
                        disabled={deleteReturn.isPending}
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          ) : (
            <div className="text-center py-12 text-muted-foreground">
              <RotateCcw className="h-12 w-12 mx-auto mb-4 opacity-50" />
              <p>Henüz sandbox iadesi yok.</p>
              <p className="text-sm">
                Yukarıdaki &quot;İade Ekle&quot; butonuyla test iadesi ekleyin.
              </p>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
