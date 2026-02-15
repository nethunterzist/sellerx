"use client";

import { useState } from "react";
import { FlaskConical, Plus, Trash2, FileText, Calendar, Pencil } from "lucide-react";
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
import { Textarea } from "@/components/ui/textarea";
import { toast } from "sonner";
import {
  useSandboxInvoices,
  useCreateSandboxInvoice,
  useUpdateSandboxInvoice,
  useDeleteSandboxInvoice,
  useSandboxProducts,
  type SandboxInvoice,
  type CreateSandboxInvoiceRequest,
} from "@/hooks/queries/use-admin";

// Fatura türleri - Trendyol'un gerçek fatura türleri
const INVOICE_TYPES = [
  // Komisyon
  { value: "Komisyon Faturası", label: "Komisyon Faturası", category: "KOMISYON" },

  // Kargo
  { value: "Kargo Fatura", label: "Kargo Faturası", category: "KARGO" },

  // Platform
  { value: "Platform Hizmet Bedeli", label: "Platform Hizmet Bedeli", category: "PLATFORM" },
  { value: "AZ-Platform Hizmet Bedeli", label: "AZ-Platform Hizmet Bedeli", category: "PLATFORM" },
  { value: "AZ-Uluslararası Hizmet Bedeli", label: "AZ-Uluslararası Hizmet Bedeli", category: "PLATFORM" },
  { value: "AZ-Yurtdışı Operasyon Bedeli", label: "AZ-Yurtdışı Operasyon Bedeli", category: "PLATFORM" },

  // Reklam
  { value: "Reklam Bedeli", label: "Reklam Bedeli", category: "REKLAM" },
  { value: "Komisyonlu Influencer Reklam Bedeli", label: "Komisyonlu Influencer Reklam", category: "REKLAM" },

  // Kampanya
  { value: "Kurumsal Kampanya Yansıtma Bedeli", label: "Kurumsal Kampanya Yansıtma", category: "KAMPANYA" },

  // Ceza
  { value: "Tedarik Edememe", label: "Tedarik Edememe Cezası", category: "CEZA" },
  { value: "Termin Gecikme Bedeli", label: "Termin Gecikme Bedeli", category: "CEZA" },
  { value: "Kusurlu Ürün Faturası", label: "Kusurlu Ürün Faturası", category: "CEZA" },
  { value: "Eksik Ürün Faturası", label: "Eksik Ürün Faturası", category: "CEZA" },
  { value: "Teslim Kontrol Faturası", label: "Teslim Kontrol Faturası", category: "CEZA" },
  { value: "Yanlış Ürün Faturası", label: "Yanlış Ürün Faturası", category: "CEZA" },

  // Diğer
  { value: "Erken Ödeme Kesinti Faturası", label: "Erken Ödeme Kesinti", category: "DIGER" },
  { value: "Fatura Kontör Satış Bedeli", label: "Fatura Kontör Satış", category: "DIGER" },
  { value: "TEX Tazmin-İşleme-%0", label: "TEX Tazmin-İşleme", category: "DIGER" },

  // İade
  { value: "Yurtdışı Operasyon Iade Bedeli", label: "İade (Alacak)", category: "IADE" },
];

const defaultFormData: CreateSandboxInvoiceRequest = {
  transactionType: "Komisyon Faturası",
  amount: 0,
  transactionDate: new Date().toISOString().split("T")[0],
  description: "",
  orderNumber: "",
  invoiceSerialNumber: "",
  // Ürün ilişkilendirme (Komisyon/Kargo faturaları için)
  barcode: "",
  commissionRate: undefined,
  shippingCostPerUnit: undefined,
};

export default function SandboxInvoicesPage() {
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [isEditDialogOpen, setIsEditDialogOpen] = useState(false);
  const [editingInvoice, setEditingInvoice] = useState<SandboxInvoice | null>(null);
  const [formData, setFormData] = useState<CreateSandboxInvoiceRequest>(defaultFormData);

  const { data: invoices, isLoading, error } = useSandboxInvoices();
  const { data: products } = useSandboxProducts();
  const createInvoice = useCreateSandboxInvoice();
  const updateInvoice = useUpdateSandboxInvoice();
  const deleteInvoice = useDeleteSandboxInvoice();

  // Komisyon veya Kargo faturası mı kontrol et
  const isCommissionInvoice = formData.transactionType === "Komisyon Faturası";
  const isCargoInvoice = formData.transactionType === "Kargo Fatura";

  const handleInputChange = (
    field: keyof CreateSandboxInvoiceRequest,
    value: string | number
  ) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  const handleSubmit = async () => {
    if (!formData.transactionType) {
      toast.error("Fatura türü seçmeniz gerekiyor");
      return;
    }
    if (formData.amount === 0) {
      toast.error("Tutar girmeniz gerekiyor");
      return;
    }

    // Komisyon faturası için ek validasyonlar
    if (isCommissionInvoice) {
      if (!formData.barcode) {
        toast.error("Komisyon faturası için ürün seçmeniz gerekiyor");
        return;
      }
      if (formData.commissionRate === undefined || formData.commissionRate === null) {
        toast.error("Komisyon oranı girmeniz gerekiyor");
        return;
      }
    }

    // Kargo faturası için ek validasyonlar
    if (isCargoInvoice) {
      if (!formData.barcode) {
        toast.error("Kargo faturası için ürün seçmeniz gerekiyor");
        return;
      }
      if (formData.shippingCostPerUnit === undefined || formData.shippingCostPerUnit === null) {
        toast.error("Birim kargo maliyeti girmeniz gerekiyor");
        return;
      }
    }

    try {
      await createInvoice.mutateAsync(formData);
      toast.success("Fatura başarıyla eklendi");
      setIsDialogOpen(false);
      setFormData(defaultFormData);
    } catch (err) {
      toast.error("Fatura eklenemedi");
    }
  };

  const handleEdit = (invoice: SandboxInvoice) => {
    setEditingInvoice(invoice);
    // Tutar: borç varsa pozitif, alacak varsa negatif
    const amount = invoice.debt > 0 ? invoice.debt : -invoice.credit;
    setFormData({
      transactionType: invoice.transactionType,
      amount: amount,
      transactionDate: invoice.transactionDate?.split("T")[0] || new Date().toISOString().split("T")[0],
      description: invoice.description || "",
      orderNumber: invoice.orderNumber || "",
      invoiceSerialNumber: invoice.invoiceSerialNumber || "",
    });
    setIsEditDialogOpen(true);
  };

  const handleUpdate = async () => {
    if (!editingInvoice) return;
    if (!formData.transactionType) {
      toast.error("Fatura türü seçmeniz gerekiyor");
      return;
    }
    if (formData.amount === 0) {
      toast.error("Tutar girmeniz gerekiyor");
      return;
    }

    // Komisyon faturası için ek validasyonlar
    if (isCommissionInvoice) {
      if (!formData.barcode) {
        toast.error("Komisyon faturası için ürün seçmeniz gerekiyor");
        return;
      }
      if (formData.commissionRate === undefined || formData.commissionRate === null) {
        toast.error("Komisyon oranı girmeniz gerekiyor");
        return;
      }
    }

    // Kargo faturası için ek validasyonlar
    if (isCargoInvoice) {
      if (!formData.barcode) {
        toast.error("Kargo faturası için ürün seçmeniz gerekiyor");
        return;
      }
      if (formData.shippingCostPerUnit === undefined || formData.shippingCostPerUnit === null) {
        toast.error("Birim kargo maliyeti girmeniz gerekiyor");
        return;
      }
    }

    try {
      await updateInvoice.mutateAsync({ id: editingInvoice.id, data: formData });
      toast.success("Fatura başarıyla güncellendi");
      setIsEditDialogOpen(false);
      setEditingInvoice(null);
      setFormData(defaultFormData);
    } catch (err) {
      toast.error("Fatura güncellenemedi");
    }
  };

  const handleDelete = async (invoiceId: string) => {
    if (!confirm("Bu faturayı silmek istediğinizden emin misiniz?")) return;

    try {
      await deleteInvoice.mutateAsync(invoiceId);
      toast.success("Fatura silindi");
    } catch (err) {
      toast.error("Fatura silinemedi");
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

  const getCategoryBadge = (type: string) => {
    const invoiceType = INVOICE_TYPES.find((t) => t.value === type);
    const category = invoiceType?.category || "DIGER";

    switch (category) {
      case "KOMISYON":
        return <Badge variant="default" className="bg-blue-500">Komisyon</Badge>;
      case "KARGO":
        return <Badge variant="default" className="bg-orange-500">Kargo</Badge>;
      case "PLATFORM":
        return <Badge variant="default" className="bg-purple-500">Platform</Badge>;
      case "REKLAM":
        return <Badge variant="default" className="bg-pink-500">Reklam</Badge>;
      case "KAMPANYA":
        return <Badge variant="default" className="bg-amber-500">Kampanya</Badge>;
      case "CEZA":
        return <Badge variant="destructive">Ceza</Badge>;
      case "IADE":
        return <Badge variant="default" className="bg-green-500">İade</Badge>;
      default:
        return <Badge variant="secondary">Diğer</Badge>;
    }
  };

  // Calculate totals
  const totalDebt = invoices?.reduce((sum, inv) => sum + (inv.debt || 0), 0) || 0;
  const totalCredit = invoices?.reduce((sum, inv) => sum + (inv.credit || 0), 0) || 0;

  if (error) {
    return (
      <div className="p-6">
        <Card className="border-red-200 bg-red-50">
          <CardContent className="pt-6">
            <p className="text-red-600">
              Sandbox faturaları yüklenirken hata oluştu.
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
            <h1 className="text-2xl font-bold">Sandbox Fatura Yönetimi</h1>
            <p className="text-muted-foreground">
              Furkan Test Mağaza - Manuel fatura ekleme ve test
            </p>
          </div>
        </div>

        <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
          <DialogTrigger asChild>
            <Button className="gap-2">
              <Plus className="h-4 w-4" />
              Fatura Ekle
            </Button>
          </DialogTrigger>
          <DialogContent className="max-w-lg">
            <DialogHeader>
              <DialogTitle>Yeni Sandbox Faturası Ekle</DialogTitle>
            </DialogHeader>

            <div className="grid gap-4 py-4">
              {/* Fatura Türü */}
              <div className="space-y-2">
                <Label htmlFor="transactionType">Fatura Türü *</Label>
                <Select
                  value={formData.transactionType}
                  onValueChange={(v) => handleInputChange("transactionType", v)}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Fatura türü seçin..." />
                  </SelectTrigger>
                  <SelectContent>
                    {INVOICE_TYPES.map((type) => (
                      <SelectItem key={type.value} value={type.value}>
                        {type.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              {/* Tutar */}
              <div className="space-y-2">
                <Label htmlFor="amount">
                  Tutar (TL) *
                  <span className="text-xs text-muted-foreground ml-2">
                    (Pozitif = Borç, Negatif = Alacak/İade)
                  </span>
                </Label>
                <Input
                  id="amount"
                  type="number"
                  step="0.01"
                  value={formData.amount}
                  onChange={(e) => handleInputChange("amount", Number(e.target.value))}
                  placeholder="Örn: 150.00 veya -50.00"
                />
              </div>

              {/* Tarih */}
              <div className="space-y-2">
                <Label htmlFor="transactionDate">Fatura Tarihi</Label>
                <Input
                  id="transactionDate"
                  type="date"
                  value={formData.transactionDate}
                  onChange={(e) => handleInputChange("transactionDate", e.target.value)}
                />
              </div>

              {/* Sipariş No */}
              <div className="space-y-2">
                <Label htmlFor="orderNumber">
                  İlişkili Sipariş No
                  <span className="text-xs text-muted-foreground ml-2">
                    (Komisyon ve Kargo faturaları için)
                  </span>
                </Label>
                <Input
                  id="orderNumber"
                  value={formData.orderNumber}
                  onChange={(e) => handleInputChange("orderNumber", e.target.value)}
                  placeholder="SANDBOX-..."
                />
              </div>

              {/* Ürün İlişkilendirme (Komisyon/Kargo Faturaları İçin) */}
              {(isCommissionInvoice || isCargoInvoice) && (
                <>
                  <div className="border-t pt-4 mt-4">
                    <div className="bg-blue-50 border border-blue-200 rounded-lg p-3 text-sm text-blue-700 mb-4">
                      <p className="font-medium mb-1">
                        {isCommissionInvoice ? "Komisyon Faturası" : "Kargo Faturası"} - Ürün Güncelleme
                      </p>
                      <p className="text-xs text-blue-600">
                        {isCommissionInvoice
                          ? "Bu fatura, seçilen ürünün lastCommissionRate değerini güncelleyecek. Yeni siparişler bu oranı referans alacak."
                          : "Bu fatura, seçilen ürünün lastShippingCostPerUnit değerini güncelleyecek. Yeni siparişler bu maliyeti referans alacak."}
                      </p>
                    </div>

                    {/* Ürün Seçimi */}
                    <div className="space-y-2">
                      <Label htmlFor="barcode">Ürün Seçimi *</Label>
                      <Select
                        value={formData.barcode || ""}
                        onValueChange={(v) => handleInputChange("barcode", v)}
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

                    {/* Komisyon Oranı (Komisyon Faturası için) */}
                    {isCommissionInvoice && (
                      <div className="space-y-2 mt-4">
                        <Label htmlFor="commissionRate">
                          Komisyon Oranı (%) *
                          <span className="text-xs text-muted-foreground ml-2">
                            (Bu oran ürünün lastCommissionRate değerini güncelleyecek)
                          </span>
                        </Label>
                        <Input
                          id="commissionRate"
                          type="number"
                          step="0.1"
                          min="0"
                          max="100"
                          value={formData.commissionRate ?? ""}
                          onChange={(e) => handleInputChange("commissionRate", Number(e.target.value))}
                          placeholder="Örn: 18.5"
                        />
                      </div>
                    )}

                    {/* Birim Kargo Maliyeti (Kargo Faturası için) */}
                    {isCargoInvoice && (
                      <div className="space-y-2 mt-4">
                        <Label htmlFor="shippingCostPerUnit">
                          Birim Kargo Maliyeti (TL/adet) *
                          <span className="text-xs text-muted-foreground ml-2">
                            (Bu değer ürünün lastShippingCostPerUnit değerini güncelleyecek)
                          </span>
                        </Label>
                        <Input
                          id="shippingCostPerUnit"
                          type="number"
                          step="0.01"
                          min="0"
                          value={formData.shippingCostPerUnit ?? ""}
                          onChange={(e) => handleInputChange("shippingCostPerUnit", Number(e.target.value))}
                          placeholder="Örn: 12.50"
                        />
                      </div>
                    )}
                  </div>
                </>
              )}

              {/* Açıklama */}
              <div className="space-y-2">
                <Label htmlFor="description">Açıklama</Label>
                <Textarea
                  id="description"
                  value={formData.description}
                  onChange={(e) => handleInputChange("description", e.target.value)}
                  placeholder="Fatura açıklaması..."
                  rows={2}
                />
              </div>

              {/* Önizleme */}
              <div className="bg-muted/50 rounded-lg p-4">
                <div className="flex justify-between items-center">
                  <span className="text-muted-foreground">Fatura Tutarı:</span>
                  <span
                    className={`font-bold ${
                      formData.amount >= 0 ? "text-red-600" : "text-green-600"
                    }`}
                  >
                    {formData.amount >= 0 ? "-" : "+"}
                    {formatPrice(Math.abs(formData.amount))}
                  </span>
                </div>
                <p className="text-xs text-muted-foreground mt-2">
                  {formData.amount >= 0
                    ? "Bu tutar hesabınızdan kesilecek (borç)"
                    : "Bu tutar hesabınıza eklenecek (iade/alacak)"}
                </p>
              </div>
            </div>

            <DialogFooter>
              <Button variant="outline" onClick={() => setIsDialogOpen(false)}>
                İptal
              </Button>
              <Button
                onClick={handleSubmit}
                disabled={createInvoice.isPending || !formData.transactionType}
              >
                {createInvoice.isPending ? "Ekleniyor..." : "Fatura Ekle"}
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>

        {/* Edit Dialog */}
        <Dialog open={isEditDialogOpen} onOpenChange={(open) => {
          setIsEditDialogOpen(open);
          if (!open) {
            setEditingInvoice(null);
            setFormData(defaultFormData);
          }
        }}>
          <DialogContent className="max-w-lg">
            <DialogHeader>
              <DialogTitle>Fatura Düzenle</DialogTitle>
            </DialogHeader>

            <div className="grid gap-4 py-4">
              {/* Fatura Türü */}
              <div className="space-y-2">
                <Label htmlFor="edit-transactionType">Fatura Türü *</Label>
                <Select
                  value={formData.transactionType}
                  onValueChange={(v) => handleInputChange("transactionType", v)}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Fatura türü seçin..." />
                  </SelectTrigger>
                  <SelectContent>
                    {INVOICE_TYPES.map((type) => (
                      <SelectItem key={type.value} value={type.value}>
                        {type.label}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              {/* Tutar */}
              <div className="space-y-2">
                <Label htmlFor="edit-amount">
                  Tutar (TL) *
                  <span className="text-xs text-muted-foreground ml-2">
                    (Pozitif = Borç, Negatif = Alacak/İade)
                  </span>
                </Label>
                <Input
                  id="edit-amount"
                  type="number"
                  step="0.01"
                  value={formData.amount}
                  onChange={(e) => handleInputChange("amount", Number(e.target.value))}
                />
              </div>

              {/* Tarih */}
              <div className="space-y-2">
                <Label htmlFor="edit-transactionDate">Fatura Tarihi</Label>
                <Input
                  id="edit-transactionDate"
                  type="date"
                  value={formData.transactionDate}
                  onChange={(e) => handleInputChange("transactionDate", e.target.value)}
                />
              </div>

              {/* Sipariş No */}
              <div className="space-y-2">
                <Label htmlFor="edit-orderNumber">İlişkili Sipariş No</Label>
                <Input
                  id="edit-orderNumber"
                  value={formData.orderNumber}
                  onChange={(e) => handleInputChange("orderNumber", e.target.value)}
                />
              </div>

              {/* Ürün İlişkilendirme (Komisyon/Kargo Faturaları İçin) */}
              {(isCommissionInvoice || isCargoInvoice) && (
                <>
                  <div className="border-t pt-4 mt-4">
                    <div className="bg-blue-50 border border-blue-200 rounded-lg p-3 text-sm text-blue-700 mb-4">
                      <p className="font-medium mb-1">
                        {isCommissionInvoice ? "Komisyon Faturası" : "Kargo Faturası"} - Ürün Güncelleme
                      </p>
                      <p className="text-xs text-blue-600">
                        {isCommissionInvoice
                          ? "Bu fatura, seçilen ürünün lastCommissionRate değerini güncelleyecek."
                          : "Bu fatura, seçilen ürünün lastShippingCostPerUnit değerini güncelleyecek."}
                      </p>
                    </div>

                    {/* Ürün Seçimi */}
                    <div className="space-y-2">
                      <Label htmlFor="edit-barcode">Ürün Seçimi *</Label>
                      <Select
                        value={formData.barcode || ""}
                        onValueChange={(v) => handleInputChange("barcode", v)}
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

                    {/* Komisyon Oranı */}
                    {isCommissionInvoice && (
                      <div className="space-y-2 mt-4">
                        <Label htmlFor="edit-commissionRate">Komisyon Oranı (%) *</Label>
                        <Input
                          id="edit-commissionRate"
                          type="number"
                          step="0.1"
                          min="0"
                          max="100"
                          value={formData.commissionRate ?? ""}
                          onChange={(e) => handleInputChange("commissionRate", Number(e.target.value))}
                        />
                      </div>
                    )}

                    {/* Birim Kargo Maliyeti */}
                    {isCargoInvoice && (
                      <div className="space-y-2 mt-4">
                        <Label htmlFor="edit-shippingCostPerUnit">Birim Kargo Maliyeti (TL/adet) *</Label>
                        <Input
                          id="edit-shippingCostPerUnit"
                          type="number"
                          step="0.01"
                          min="0"
                          value={formData.shippingCostPerUnit ?? ""}
                          onChange={(e) => handleInputChange("shippingCostPerUnit", Number(e.target.value))}
                        />
                      </div>
                    )}
                  </div>
                </>
              )}

              {/* Açıklama */}
              <div className="space-y-2">
                <Label htmlFor="edit-description">Açıklama</Label>
                <Textarea
                  id="edit-description"
                  value={formData.description}
                  onChange={(e) => handleInputChange("description", e.target.value)}
                  rows={2}
                />
              </div>
            </div>

            <DialogFooter>
              <Button variant="outline" onClick={() => {
                setIsEditDialogOpen(false);
                setEditingInvoice(null);
                setFormData(defaultFormData);
              }}>
                İptal
              </Button>
              <Button
                onClick={handleUpdate}
                disabled={updateInvoice.isPending || !formData.transactionType}
              >
                {updateInvoice.isPending ? "Güncelleniyor..." : "Güncelle"}
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <Card>
          <CardContent className="pt-6">
            <div className="text-center">
              <p className="text-sm text-muted-foreground">Toplam Kesinti (Borç)</p>
              <p className="text-2xl font-bold text-red-600">
                -{formatPrice(totalDebt)}
              </p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="text-center">
              <p className="text-sm text-muted-foreground">Toplam İade (Alacak)</p>
              <p className="text-2xl font-bold text-green-600">
                +{formatPrice(totalCredit)}
              </p>
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-6">
            <div className="text-center">
              <p className="text-sm text-muted-foreground">Net Etki</p>
              <p className={`text-2xl font-bold ${totalDebt - totalCredit >= 0 ? "text-red-600" : "text-green-600"}`}>
                {totalDebt - totalCredit >= 0 ? "-" : "+"}
                {formatPrice(Math.abs(totalDebt - totalCredit))}
              </p>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Invoices Table */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <FileText className="h-5 w-5" />
            Sandbox Faturaları
            {invoices && (
              <Badge variant="secondary" className="ml-2">
                {invoices.length} fatura
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
          ) : invoices && invoices.length > 0 ? (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Tarih</TableHead>
                  <TableHead>Tür</TableHead>
                  <TableHead>Kategori</TableHead>
                  <TableHead>Açıklama</TableHead>
                  <TableHead className="text-right">Borç</TableHead>
                  <TableHead className="text-right">Alacak</TableHead>
                  <TableHead className="text-right">İşlem</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {invoices.map((invoice) => (
                  <TableRow key={invoice.id}>
                    <TableCell>
                      <div className="flex items-center gap-1 text-sm text-muted-foreground">
                        <Calendar className="h-3 w-3" />
                        {formatDate(invoice.transactionDate)}
                      </div>
                    </TableCell>
                    <TableCell className="max-w-[200px]">
                      {invoice.transactionType}
                    </TableCell>
                    <TableCell>
                      {getCategoryBadge(invoice.transactionType)}
                    </TableCell>
                    <TableCell className="max-w-[200px] truncate">
                      {invoice.description || invoice.orderNumber || "-"}
                    </TableCell>
                    <TableCell className="text-right text-red-600">
                      {invoice.debt > 0 ? `-${formatPrice(invoice.debt)}` : "-"}
                    </TableCell>
                    <TableCell className="text-right text-green-600">
                      {invoice.credit > 0 ? `+${formatPrice(invoice.credit)}` : "-"}
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="flex items-center justify-end gap-1">
                        <Button
                          variant="ghost"
                          size="icon"
                          className="text-blue-500 hover:text-blue-700 hover:bg-blue-50"
                          onClick={() => handleEdit(invoice)}
                        >
                          <Pencil className="h-4 w-4" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          className="text-red-500 hover:text-red-700 hover:bg-red-50"
                          onClick={() => handleDelete(invoice.id)}
                          disabled={deleteInvoice.isPending}
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
              <FileText className="h-12 w-12 mx-auto mb-4 opacity-50" />
              <p>Henüz sandbox faturası yok.</p>
              <p className="text-sm">
                Yukarıdaki &quot;Fatura Ekle&quot; butonuyla test faturası
                ekleyin.
              </p>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
