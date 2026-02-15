"use client";

import { useState } from "react";
import { FlaskConical, Plus, Trash2, Receipt, Calendar, Pencil, CheckCircle2, Clock } from "lucide-react";
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
  useSandboxOrders,
  useCreateSandboxOrder,
  useUpdateSandboxOrder,
  useDeleteSandboxOrder,
  useSandboxProducts,
  useSettleSandboxOrder,
  useSettleAllSandboxOrders,
  type CreateSandboxOrderRequest,
} from "@/hooks/queries/use-admin";
import { TrendyolOrder } from "@/types/order";

const defaultFormData: CreateSandboxOrderRequest = {
  barcode: "",
  quantity: 1,
  unitPrice: 0,
  orderDate: new Date().toISOString().split("T")[0],
  status: "Delivered",
  customerName: "",
  city: "",
};

export default function SandboxOrdersPage() {
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [isEditDialogOpen, setIsEditDialogOpen] = useState(false);
  const [editingOrder, setEditingOrder] = useState<TrendyolOrder | null>(null);
  const [formData, setFormData] = useState<CreateSandboxOrderRequest>(defaultFormData);

  const { data: orders, isLoading, error } = useSandboxOrders();
  const { data: products } = useSandboxProducts();
  const createOrder = useCreateSandboxOrder();
  const updateOrder = useUpdateSandboxOrder();
  const deleteOrder = useDeleteSandboxOrder();
  const settleOrder = useSettleSandboxOrder();
  const settleAllOrders = useSettleAllSandboxOrders();

  // Count unsettled orders (NONE hariç - veri yoksa settle edilecek bir şey yok)
  const unsettledCount = orders?.filter(o => o.isCommissionEstimated && o.commissionSource !== "NONE").length || 0;

  const handleInputChange = (
    field: keyof CreateSandboxOrderRequest,
    value: string | number
  ) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  const handleProductSelect = (barcode: string) => {
    const product = products?.find((p) => p.barcode === barcode);
    if (product) {
      setFormData((prev) => ({
        ...prev,
        barcode,
        unitPrice: product.salePrice || 0,
      }));
    }
  };

  const handleSubmit = async () => {
    if (!formData.barcode) {
      toast.error("Ürün seçmeniz gerekiyor");
      return;
    }
    if (formData.quantity < 1) {
      toast.error("Adet en az 1 olmalı");
      return;
    }

    try {
      await createOrder.mutateAsync(formData);
      toast.success("Sipariş başarıyla eklendi");
      setIsDialogOpen(false);
      setFormData(defaultFormData);
    } catch (err) {
      toast.error("Sipariş eklenemedi");
    }
  };

  const handleEdit = (order: TrendyolOrder) => {
    const orderItem = order.orderItems?.[0];
    setEditingOrder(order);
    setFormData({
      barcode: orderItem?.barcode || "",
      quantity: orderItem?.quantity || 1,
      unitPrice: orderItem?.unitPriceOrder || order.grossAmount || 0,
      orderDate: order.orderDate?.split("T")[0] || new Date().toISOString().split("T")[0],
      status: order.status || "Delivered",
      customerName: "",
      city: "",
    });
    setIsEditDialogOpen(true);
  };

  const handleUpdate = async () => {
    if (!editingOrder) return;
    if (!formData.barcode) {
      toast.error("Ürün seçmeniz gerekiyor");
      return;
    }
    if (formData.quantity < 1) {
      toast.error("Adet en az 1 olmalı");
      return;
    }

    try {
      await updateOrder.mutateAsync({ id: editingOrder.id, data: formData });
      toast.success("Sipariş başarıyla güncellendi");
      setIsEditDialogOpen(false);
      setEditingOrder(null);
      setFormData(defaultFormData);
    } catch (err) {
      toast.error("Sipariş güncellenemedi");
    }
  };

  const handleDelete = async (orderId: string) => {
    if (!confirm("Bu siparişi silmek istediğinizden emin misiniz?")) return;

    try {
      await deleteOrder.mutateAsync(orderId);
      toast.success("Sipariş silindi");
    } catch (err) {
      toast.error("Sipariş silinemedi");
    }
  };

  const handleSettle = async (orderId: string) => {
    try {
      await settleOrder.mutateAsync(orderId);
      toast.success("Sipariş settle edildi - Komisyon artık gerçek!");
    } catch (err) {
      toast.error("Settle işlemi başarısız");
    }
  };

  const handleSettleAll = async () => {
    if (!confirm(`${unsettledCount} siparişi settle etmek istediğinizden emin misiniz?`)) return;

    try {
      const result = await settleAllOrders.mutateAsync();
      toast.success(`${result.settledCount} sipariş settle edildi`);
    } catch (err) {
      toast.error("Settle işlemi başarısız");
    }
  };

  const formatPrice = (price: number | undefined) => {
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

  if (error) {
    return (
      <div className="p-6">
        <Card className="border-red-200 bg-red-50">
          <CardContent className="pt-6">
            <p className="text-red-600">
              Sandbox siparişleri yüklenirken hata oluştu.
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
            <h1 className="text-2xl font-bold">Sandbox Sipariş Yönetimi</h1>
            <p className="text-muted-foreground">
              Furkan Test Mağaza - Manuel sipariş ekleme ve test
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          {unsettledCount > 0 && (
            <Button
              variant="outline"
              className="gap-2"
              onClick={handleSettleAll}
              disabled={settleAllOrders.isPending}
            >
              <CheckCircle2 className="h-4 w-4" />
              {settleAllOrders.isPending ? "Settle ediliyor..." : `Tümünü Settle Et (${unsettledCount})`}
            </Button>
          )}
          <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
            <DialogTrigger asChild>
              <Button className="gap-2">
                <Plus className="h-4 w-4" />
                Sipariş Ekle
              </Button>
            </DialogTrigger>
          <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
            <DialogHeader>
              <DialogTitle>Yeni Sandbox Siparişi Ekle</DialogTitle>
            </DialogHeader>

            <div className="grid gap-6 py-4">
              {/* Ürün Seçimi */}
              <div className="space-y-4">
                <h3 className="font-semibold text-sm text-muted-foreground uppercase tracking-wider">
                  Ürün Seçimi
                </h3>
                <div className="space-y-2">
                  <Label htmlFor="product">Ürün *</Label>
                  <Select
                    value={formData.barcode}
                    onValueChange={handleProductSelect}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="Sandbox ürünü seçin..." />
                    </SelectTrigger>
                    <SelectContent>
                      {products?.map((product) => (
                        <SelectItem key={product.id} value={product.barcode}>
                          {product.title} - {product.barcode}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  {products?.length === 0 && (
                    <p className="text-sm text-muted-foreground">
                      Önce Ürün Yönetimi sayfasından ürün eklemeniz gerekiyor.
                    </p>
                  )}
                </div>
              </div>

              {/* Satış Bilgileri */}
              <div className="space-y-4">
                <h3 className="font-semibold text-sm text-muted-foreground uppercase tracking-wider">
                  Satış Bilgileri
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
                    <Label htmlFor="unitPrice">Birim Fiyat (TL) *</Label>
                    <Input
                      id="unitPrice"
                      type="number"
                      step="0.01"
                      value={formData.unitPrice}
                      onChange={(e) =>
                        handleInputChange("unitPrice", Number(e.target.value))
                      }
                    />
                  </div>
                </div>
                {/* Komisyon ve kargo bilgisi */}
                <div className="bg-blue-50 border border-blue-200 rounded-lg p-3 text-sm text-blue-700">
                  <p className="font-medium mb-1">Komisyon ve Kargo Otomatik Hesaplanır</p>
                  <p className="text-xs text-blue-600">
                    Komisyon: Ürünün son fatura oranı → Kategori referansı → 0<br/>
                    Kargo: Ürünün son kargo maliyeti → 0<br/>
                    Fatura ekleyerek bu değerleri güncelleyebilirsiniz.
                  </p>
                </div>
              </div>

              {/* Sipariş Detayları */}
              <div className="space-y-4">
                <h3 className="font-semibold text-sm text-muted-foreground uppercase tracking-wider">
                  Sipariş Detayları
                </h3>
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="orderDate">Sipariş Tarihi</Label>
                    <Input
                      id="orderDate"
                      type="date"
                      value={formData.orderDate}
                      onChange={(e) =>
                        handleInputChange("orderDate", e.target.value)
                      }
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="status">Durum</Label>
                    <Select
                      value={formData.status}
                      onValueChange={(v) => handleInputChange("status", v)}
                    >
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="Delivered">Teslim Edildi</SelectItem>
                        <SelectItem value="Shipped">Kargoda</SelectItem>
                        <SelectItem value="Created">Oluşturuldu</SelectItem>
                        <SelectItem value="Cancelled">İptal</SelectItem>
                        <SelectItem value="Returned">İade</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="customerName">Müşteri Adı</Label>
                    <Input
                      id="customerName"
                      value={formData.customerName}
                      onChange={(e) =>
                        handleInputChange("customerName", e.target.value)
                      }
                      placeholder="Test Müşteri"
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="city">Şehir</Label>
                    <Input
                      id="city"
                      value={formData.city}
                      onChange={(e) => handleInputChange("city", e.target.value)}
                      placeholder="İstanbul"
                    />
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
                disabled={createOrder.isPending || !formData.barcode}
              >
                {createOrder.isPending ? "Ekleniyor..." : "Sipariş Ekle"}
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
        </div>

        {/* Edit Dialog */}
        <Dialog open={isEditDialogOpen} onOpenChange={(open) => {
          setIsEditDialogOpen(open);
          if (!open) {
            setEditingOrder(null);
            setFormData(defaultFormData);
          }
        }}>
          <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
            <DialogHeader>
              <DialogTitle>Sipariş Düzenle</DialogTitle>
            </DialogHeader>

            <div className="grid gap-6 py-4">
              {/* Ürün Seçimi */}
              <div className="space-y-4">
                <h3 className="font-semibold text-sm text-muted-foreground uppercase tracking-wider">
                  Ürün Seçimi
                </h3>
                <div className="space-y-2">
                  <Label htmlFor="edit-product">Ürün *</Label>
                  <Select
                    value={formData.barcode}
                    onValueChange={handleProductSelect}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="Sandbox ürünü seçin..." />
                    </SelectTrigger>
                    <SelectContent>
                      {products?.map((product) => (
                        <SelectItem key={product.id} value={product.barcode}>
                          {product.title} - {product.barcode}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              </div>

              {/* Satış Bilgileri */}
              <div className="space-y-4">
                <h3 className="font-semibold text-sm text-muted-foreground uppercase tracking-wider">
                  Satış Bilgileri
                </h3>
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="edit-quantity">Adet *</Label>
                    <Input
                      id="edit-quantity"
                      type="number"
                      min={1}
                      value={formData.quantity}
                      onChange={(e) =>
                        handleInputChange("quantity", Number(e.target.value))
                      }
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="edit-unitPrice">Birim Fiyat (TL) *</Label>
                    <Input
                      id="edit-unitPrice"
                      type="number"
                      step="0.01"
                      value={formData.unitPrice}
                      onChange={(e) =>
                        handleInputChange("unitPrice", Number(e.target.value))
                      }
                    />
                  </div>
                </div>
                {/* Komisyon ve kargo bilgisi */}
                <div className="bg-blue-50 border border-blue-200 rounded-lg p-3 text-sm text-blue-700">
                  <p className="font-medium mb-1">Komisyon ve Kargo Otomatik Hesaplanır</p>
                  <p className="text-xs text-blue-600">
                    Komisyon: Ürünün son fatura oranı → Kategori referansı → 0<br/>
                    Kargo: Ürünün son kargo maliyeti → 0<br/>
                    Fatura ekleyerek bu değerleri güncelleyebilirsiniz.
                  </p>
                </div>
              </div>

              {/* Sipariş Detayları */}
              <div className="space-y-4">
                <h3 className="font-semibold text-sm text-muted-foreground uppercase tracking-wider">
                  Sipariş Detayları
                </h3>
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="edit-orderDate">Sipariş Tarihi</Label>
                    <Input
                      id="edit-orderDate"
                      type="date"
                      value={formData.orderDate}
                      onChange={(e) =>
                        handleInputChange("orderDate", e.target.value)
                      }
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="edit-status">Durum</Label>
                    <Select
                      value={formData.status}
                      onValueChange={(v) => handleInputChange("status", v)}
                    >
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="Delivered">Teslim Edildi</SelectItem>
                        <SelectItem value="Shipped">Kargoda</SelectItem>
                        <SelectItem value="Created">Oluşturuldu</SelectItem>
                        <SelectItem value="Cancelled">İptal</SelectItem>
                        <SelectItem value="Returned">İade</SelectItem>
                      </SelectContent>
                    </Select>
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="edit-customerName">Müşteri Adı</Label>
                    <Input
                      id="edit-customerName"
                      value={formData.customerName}
                      onChange={(e) =>
                        handleInputChange("customerName", e.target.value)
                      }
                      placeholder="Test Müşteri"
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="edit-city">Şehir</Label>
                    <Input
                      id="edit-city"
                      value={formData.city}
                      onChange={(e) => handleInputChange("city", e.target.value)}
                      placeholder="İstanbul"
                    />
                  </div>
                </div>
              </div>

            </div>

            <DialogFooter>
              <Button variant="outline" onClick={() => {
                setIsEditDialogOpen(false);
                setEditingOrder(null);
                setFormData(defaultFormData);
              }}>
                İptal
              </Button>
              <Button
                onClick={handleUpdate}
                disabled={updateOrder.isPending || !formData.barcode}
              >
                {updateOrder.isPending ? "Güncelleniyor..." : "Güncelle"}
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      </div>

      {/* Orders Table */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Receipt className="h-5 w-5" />
            Sandbox Siparişleri
            {orders && (
              <Badge variant="secondary" className="ml-2">
                {orders.length} sipariş
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
          ) : orders && orders.length > 0 ? (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Sipariş No</TableHead>
                  <TableHead>Tarih</TableHead>
                  <TableHead>Ürün</TableHead>
                  <TableHead className="text-center">Adet</TableHead>
                  <TableHead className="text-right">Brüt</TableHead>
                  <TableHead className="text-right">Komisyon</TableHead>
                  <TableHead className="text-right">Stopaj</TableHead>
                  <TableHead className="text-right">Kargo</TableHead>
                  <TableHead className="text-center">Kaynak</TableHead>
                  <TableHead className="text-center">Durum</TableHead>
                  <TableHead className="text-center">Settlement</TableHead>
                  <TableHead className="text-right">İşlem</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {orders.map((order) => (
                  <TableRow key={order.id}>
                    <TableCell className="font-mono text-sm">
                      {order.tyOrderNumber}
                    </TableCell>
                    <TableCell>
                      <div className="flex items-center gap-1 text-sm text-muted-foreground">
                        <Calendar className="h-3 w-3" />
                        {formatDate(order.orderDate)}
                      </div>
                    </TableCell>
                    <TableCell className="max-w-[200px] truncate">
                      {order.orderItems?.[0]?.productName || "-"}
                    </TableCell>
                    <TableCell className="text-center">
                      {order.orderItems?.reduce((sum, item) => sum + item.quantity, 0) || 0}
                    </TableCell>
                    <TableCell className="text-right">
                      {formatPrice(order.grossAmount)}
                    </TableCell>
                    <TableCell className="text-right text-red-600">
                      -{formatPrice(order.estimatedCommission)}
                    </TableCell>
                    <TableCell className="text-right text-orange-600">
                      -{formatPrice(order.stoppage || (order.grossAmount ? order.grossAmount * 0.01 : 0))}
                    </TableCell>
                    <TableCell className="text-right text-muted-foreground">
                      {formatPrice(order.estimatedShippingCost || 0)}
                    </TableCell>
                    <TableCell className="text-center">
                      {order.commissionSource === "INVOICE" ? (
                        <Badge variant="default" className="gap-1 bg-green-500 text-white">
                          Fatura
                        </Badge>
                      ) : order.commissionSource === "REFERENCE" ? (
                        <Badge variant="outline" className="gap-1 text-yellow-600 border-yellow-300 bg-yellow-50">
                          Referans
                        </Badge>
                      ) : (
                        <Badge variant="destructive" className="gap-1">
                          Veri Yok
                        </Badge>
                      )}
                    </TableCell>
                    <TableCell className="text-center">
                      {order.status === "Delivered" ? (
                        <Badge variant="default" className="bg-green-500">
                          Teslim
                        </Badge>
                      ) : order.status === "Shipped" ? (
                        <Badge variant="default" className="bg-blue-500">
                          Kargoda
                        </Badge>
                      ) : order.status === "Cancelled" ? (
                        <Badge variant="destructive">İptal</Badge>
                      ) : order.status === "Returned" ? (
                        <Badge variant="secondary">İade</Badge>
                      ) : (
                        <Badge variant="outline">{order.status}</Badge>
                      )}
                    </TableCell>
                    <TableCell className="text-center">
                      <div className="flex items-center justify-center gap-2">
                        {/* NONE: Veri yok - gösterilecek bir şey yok */}
                        {order.commissionSource === "NONE" ? (
                          <span className="text-muted-foreground">-</span>
                        ) : order.isCommissionEstimated ? (
                          /* REFERENCE/INVOICE + tahmini: Settle bekliyor */
                          <>
                            <Badge variant="outline" className="gap-1 text-yellow-600 border-yellow-300 bg-yellow-50">
                              <Clock className="h-3 w-3" />
                              Tahmini
                            </Badge>
                            <Button
                              variant="ghost"
                              size="sm"
                              className="h-6 px-2 text-xs text-green-600 hover:text-green-700 hover:bg-green-50"
                              onClick={() => handleSettle(order.id)}
                              disabled={settleOrder.isPending}
                            >
                              <CheckCircle2 className="h-3 w-3 mr-1" />
                              Settle
                            </Button>
                          </>
                        ) : (
                          /* Settle edilmiş: Gerçek */
                          <Badge variant="default" className="gap-1 bg-green-500">
                            <CheckCircle2 className="h-3 w-3" />
                            Gerçek
                          </Badge>
                        )}
                      </div>
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="flex items-center justify-end gap-1">
                        <Button
                          variant="ghost"
                          size="icon"
                          className="text-blue-500 hover:text-blue-700 hover:bg-blue-50"
                          onClick={() => handleEdit(order)}
                        >
                          <Pencil className="h-4 w-4" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          className="text-red-500 hover:text-red-700 hover:bg-red-50"
                          onClick={() => handleDelete(order.id)}
                          disabled={deleteOrder.isPending}
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          ) : (
            <div className="text-center py-12 text-muted-foreground">
              <Receipt className="h-12 w-12 mx-auto mb-4 opacity-50" />
              <p>Henüz sandbox siparişi yok.</p>
              <p className="text-sm">
                Yukarıdaki &quot;Sipariş Ekle&quot; butonuyla test siparişi
                ekleyin.
              </p>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
