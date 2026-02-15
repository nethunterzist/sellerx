"use client";

import { useState } from "react";
import { FlaskConical, Plus, Trash2, Package, Pencil } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Checkbox } from "@/components/ui/checkbox";
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
import { Skeleton } from "@/components/ui/skeleton";
import { Badge } from "@/components/ui/badge";
import { toast } from "sonner";
import {
  useSandboxProducts,
  useCreateSandboxProduct,
  useUpdateSandboxProduct,
  useDeleteSandboxProduct,
  type CreateSandboxProductRequest,
} from "@/hooks/queries/use-admin";
import { TrendyolProduct } from "@/types/product";

const defaultFormData: CreateSandboxProductRequest = {
  barcode: "",
  title: "",
  brand: "",
  categoryName: "",
  salePrice: 0,
  vatRate: 20,
  quantity: 10,
  approved: true,
  onSale: true,
  archived: false,
  rejected: false,
  blacklisted: false,
  hasActiveCampaign: false,
};

export default function SandboxProductsPage() {
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [isEditDialogOpen, setIsEditDialogOpen] = useState(false);
  const [editingProduct, setEditingProduct] = useState<TrendyolProduct | null>(null);
  const [formData, setFormData] = useState<CreateSandboxProductRequest>(defaultFormData);

  const { data: products, isLoading, error } = useSandboxProducts();
  const createProduct = useCreateSandboxProduct();
  const updateProduct = useUpdateSandboxProduct();
  const deleteProduct = useDeleteSandboxProduct();

  const handleInputChange = (field: keyof CreateSandboxProductRequest, value: string | number | boolean) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  const handleSubmit = async () => {
    if (!formData.barcode || !formData.title || !formData.brand || !formData.categoryName) {
      toast.error("Zorunlu alanları doldurun");
      return;
    }

    try {
      await createProduct.mutateAsync(formData);
      toast.success("Ürün başarıyla eklendi");
      setIsDialogOpen(false);
      setFormData(defaultFormData);
    } catch (err) {
      toast.error("Ürün eklenemedi");
    }
  };

  const handleEdit = (product: TrendyolProduct) => {
    setEditingProduct(product);
    setFormData({
      barcode: product.barcode,
      title: product.title,
      brand: product.brand || "",
      brandId: product.brandId,
      categoryName: product.categoryName || "",
      pimCategoryId: product.pimCategoryId,
      salePrice: product.salePrice || 0,
      vatRate: product.vatRate || 20,
      quantity: product.trendyolQuantity || 0,
      dimensionalWeight: product.dimensionalWeight,
      image: product.image,
      approved: product.approved ?? true,
      onSale: product.onSale ?? true,
      archived: product.archived ?? false,
      rejected: product.rejected ?? false,
      blacklisted: product.blacklisted ?? false,
      hasActiveCampaign: product.hasActiveCampaign ?? false,
    });
    setIsEditDialogOpen(true);
  };

  const handleUpdate = async () => {
    if (!editingProduct) return;
    if (!formData.barcode || !formData.title || !formData.brand || !formData.categoryName) {
      toast.error("Zorunlu alanları doldurun");
      return;
    }

    try {
      await updateProduct.mutateAsync({ id: editingProduct.id, data: formData });
      toast.success("Ürün başarıyla güncellendi");
      setIsEditDialogOpen(false);
      setEditingProduct(null);
      setFormData(defaultFormData);
    } catch (err) {
      toast.error("Ürün güncellenemedi");
    }
  };

  const handleDelete = async (productId: string) => {
    if (!confirm("Bu ürünü silmek istediğinizden emin misiniz?")) return;

    try {
      await deleteProduct.mutateAsync(productId);
      toast.success("Ürün silindi");
    } catch (err) {
      toast.error("Ürün silinemedi");
    }
  };

  const formatPrice = (price: number | undefined) => {
    if (price === undefined || price === null) return "-";
    return new Intl.NumberFormat("tr-TR", {
      style: "currency",
      currency: "TRY",
    }).format(price);
  };

  if (error) {
    return (
      <div className="p-6">
        <Card className="border-red-200 bg-red-50">
          <CardContent className="pt-6">
            <p className="text-red-600">Sandbox ürünleri yüklenirken hata oluştu.</p>
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
            <h1 className="text-2xl font-bold">Sandbox Ürün Yönetimi</h1>
            <p className="text-muted-foreground">
              Furkan Test Mağaza - Manuel ürün ekleme ve test
            </p>
          </div>
        </div>

        <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
          <DialogTrigger asChild>
            <Button className="gap-2">
              <Plus className="h-4 w-4" />
              Ürün Ekle
            </Button>
          </DialogTrigger>
          <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
            <DialogHeader>
              <DialogTitle>Yeni Sandbox Ürünü Ekle</DialogTitle>
            </DialogHeader>

            <div className="grid gap-6 py-4">
              {/* Temel Bilgiler */}
              <div className="space-y-4">
                <h3 className="font-semibold text-sm text-muted-foreground uppercase tracking-wider">
                  Temel Bilgiler
                </h3>
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="barcode">Barkod *</Label>
                    <Input
                      id="barcode"
                      value={formData.barcode}
                      onChange={(e) => handleInputChange("barcode", e.target.value)}
                      placeholder="8681234567890"
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="title">Ürün Adı *</Label>
                    <Input
                      id="title"
                      value={formData.title}
                      onChange={(e) => handleInputChange("title", e.target.value)}
                      placeholder="Test Ürünü"
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="brand">Marka *</Label>
                    <Input
                      id="brand"
                      value={formData.brand}
                      onChange={(e) => handleInputChange("brand", e.target.value)}
                      placeholder="Test Marka"
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="brandId">Marka ID</Label>
                    <Input
                      id="brandId"
                      type="number"
                      value={formData.brandId || ""}
                      onChange={(e) => handleInputChange("brandId", Number(e.target.value))}
                      placeholder="12345"
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="categoryName">Kategori *</Label>
                    <Input
                      id="categoryName"
                      value={formData.categoryName}
                      onChange={(e) => handleInputChange("categoryName", e.target.value)}
                      placeholder="Elektronik > Telefon"
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="pimCategoryId">Kategori ID</Label>
                    <Input
                      id="pimCategoryId"
                      type="number"
                      value={formData.pimCategoryId || ""}
                      onChange={(e) => handleInputChange("pimCategoryId", Number(e.target.value))}
                      placeholder="1234"
                    />
                  </div>
                </div>
              </div>

              {/* Fiyat & Stok */}
              <div className="space-y-4">
                <h3 className="font-semibold text-sm text-muted-foreground uppercase tracking-wider">
                  Fiyat & Stok
                </h3>
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="salePrice">Satış Fiyatı (TL) *</Label>
                    <Input
                      id="salePrice"
                      type="number"
                      step="0.01"
                      value={formData.salePrice}
                      onChange={(e) => handleInputChange("salePrice", Number(e.target.value))}
                      placeholder="199.99"
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="listPrice">Liste Fiyatı (TL)</Label>
                    <Input
                      id="listPrice"
                      type="number"
                      step="0.01"
                      value={formData.listPrice || ""}
                      onChange={(e) => handleInputChange("listPrice", Number(e.target.value))}
                      placeholder="249.99"
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="vatRate">KDV Oranı (%) *</Label>
                    <Input
                      id="vatRate"
                      type="number"
                      value={formData.vatRate}
                      onChange={(e) => handleInputChange("vatRate", Number(e.target.value))}
                      placeholder="20"
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="quantity">Stok Adedi *</Label>
                    <Input
                      id="quantity"
                      type="number"
                      value={formData.quantity}
                      onChange={(e) => handleInputChange("quantity", Number(e.target.value))}
                      placeholder="10"
                    />
                  </div>
                </div>
              </div>

              {/* Ürün Detayları */}
              <div className="space-y-4">
                <h3 className="font-semibold text-sm text-muted-foreground uppercase tracking-wider">
                  Ürün Detayları
                </h3>
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="productMainId">Model Kodu</Label>
                    <Input
                      id="productMainId"
                      value={formData.productMainId || ""}
                      onChange={(e) => handleInputChange("productMainId", e.target.value)}
                      placeholder="ABC-123"
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="dimensionalWeight">Boyutsal Ağırlık (Desi)</Label>
                    <Input
                      id="dimensionalWeight"
                      type="number"
                      step="0.01"
                      value={formData.dimensionalWeight || ""}
                      onChange={(e) => handleInputChange("dimensionalWeight", Number(e.target.value))}
                      placeholder="1.5"
                    />
                    <p className="text-xs text-muted-foreground">
                      Kargo maliyeti: Desi × 8 TL
                    </p>
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="color">Renk</Label>
                    <Input
                      id="color"
                      value={formData.color || ""}
                      onChange={(e) => handleInputChange("color", e.target.value)}
                      placeholder="Siyah"
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="size">Beden</Label>
                    <Input
                      id="size"
                      value={formData.size || ""}
                      onChange={(e) => handleInputChange("size", e.target.value)}
                      placeholder="M"
                    />
                  </div>
                  <div className="col-span-2 space-y-2">
                    <Label htmlFor="image">Resim URL</Label>
                    <Input
                      id="image"
                      value={formData.image || ""}
                      onChange={(e) => handleInputChange("image", e.target.value)}
                      placeholder="https://cdn.trendyol.com/..."
                    />
                  </div>
                </div>
              </div>

              {/* Durum Bilgileri */}
              <div className="space-y-4">
                <h3 className="font-semibold text-sm text-muted-foreground uppercase tracking-wider">
                  Durum Bilgileri
                </h3>
                <div className="grid grid-cols-3 gap-4">
                  <div className="flex items-center space-x-2">
                    <Checkbox
                      id="approved"
                      checked={formData.approved}
                      onCheckedChange={(checked) => handleInputChange("approved", !!checked)}
                    />
                    <Label htmlFor="approved">Onaylı</Label>
                  </div>
                  <div className="flex items-center space-x-2">
                    <Checkbox
                      id="onSale"
                      checked={formData.onSale}
                      onCheckedChange={(checked) => handleInputChange("onSale", !!checked)}
                    />
                    <Label htmlFor="onSale">Satışta</Label>
                  </div>
                  <div className="flex items-center space-x-2">
                    <Checkbox
                      id="hasActiveCampaign"
                      checked={formData.hasActiveCampaign}
                      onCheckedChange={(checked) => handleInputChange("hasActiveCampaign", !!checked)}
                    />
                    <Label htmlFor="hasActiveCampaign">Kampanyalı</Label>
                  </div>
                  <div className="flex items-center space-x-2">
                    <Checkbox
                      id="archived"
                      checked={formData.archived}
                      onCheckedChange={(checked) => handleInputChange("archived", !!checked)}
                    />
                    <Label htmlFor="archived">Arşivlenmiş</Label>
                  </div>
                  <div className="flex items-center space-x-2">
                    <Checkbox
                      id="rejected"
                      checked={formData.rejected}
                      onCheckedChange={(checked) => handleInputChange("rejected", !!checked)}
                    />
                    <Label htmlFor="rejected">Reddedilmiş</Label>
                  </div>
                  <div className="flex items-center space-x-2">
                    <Checkbox
                      id="blacklisted"
                      checked={formData.blacklisted}
                      onCheckedChange={(checked) => handleInputChange("blacklisted", !!checked)}
                    />
                    <Label htmlFor="blacklisted">Kara Liste</Label>
                  </div>
                </div>
              </div>
            </div>

            <DialogFooter>
              <Button variant="outline" onClick={() => setIsDialogOpen(false)}>
                İptal
              </Button>
              <Button onClick={handleSubmit} disabled={createProduct.isPending}>
                {createProduct.isPending ? "Ekleniyor..." : "Ürün Ekle"}
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>

        {/* Edit Dialog */}
        <Dialog open={isEditDialogOpen} onOpenChange={(open) => {
          setIsEditDialogOpen(open);
          if (!open) {
            setEditingProduct(null);
            setFormData(defaultFormData);
          }
        }}>
          <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto">
            <DialogHeader>
              <DialogTitle>Ürün Düzenle</DialogTitle>
            </DialogHeader>

            <div className="grid gap-6 py-4">
              {/* Temel Bilgiler */}
              <div className="space-y-4">
                <h3 className="font-semibold text-sm text-muted-foreground uppercase tracking-wider">
                  Temel Bilgiler
                </h3>
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="edit-barcode">Barkod *</Label>
                    <Input
                      id="edit-barcode"
                      value={formData.barcode}
                      onChange={(e) => handleInputChange("barcode", e.target.value)}
                      placeholder="8681234567890"
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="edit-title">Ürün Adı *</Label>
                    <Input
                      id="edit-title"
                      value={formData.title}
                      onChange={(e) => handleInputChange("title", e.target.value)}
                      placeholder="Test Ürünü"
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="edit-brand">Marka *</Label>
                    <Input
                      id="edit-brand"
                      value={formData.brand}
                      onChange={(e) => handleInputChange("brand", e.target.value)}
                      placeholder="Test Marka"
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="edit-brandId">Marka ID</Label>
                    <Input
                      id="edit-brandId"
                      type="number"
                      value={formData.brandId || ""}
                      onChange={(e) => handleInputChange("brandId", Number(e.target.value))}
                      placeholder="12345"
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="edit-categoryName">Kategori *</Label>
                    <Input
                      id="edit-categoryName"
                      value={formData.categoryName}
                      onChange={(e) => handleInputChange("categoryName", e.target.value)}
                      placeholder="Elektronik > Telefon"
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="edit-pimCategoryId">Kategori ID</Label>
                    <Input
                      id="edit-pimCategoryId"
                      type="number"
                      value={formData.pimCategoryId || ""}
                      onChange={(e) => handleInputChange("pimCategoryId", Number(e.target.value))}
                      placeholder="1234"
                    />
                  </div>
                </div>
              </div>

              {/* Fiyat & Stok */}
              <div className="space-y-4">
                <h3 className="font-semibold text-sm text-muted-foreground uppercase tracking-wider">
                  Fiyat & Stok
                </h3>
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="edit-salePrice">Satış Fiyatı (TL) *</Label>
                    <Input
                      id="edit-salePrice"
                      type="number"
                      step="0.01"
                      value={formData.salePrice}
                      onChange={(e) => handleInputChange("salePrice", Number(e.target.value))}
                      placeholder="199.99"
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="edit-listPrice">Liste Fiyatı (TL)</Label>
                    <Input
                      id="edit-listPrice"
                      type="number"
                      step="0.01"
                      value={formData.listPrice || ""}
                      onChange={(e) => handleInputChange("listPrice", Number(e.target.value))}
                      placeholder="249.99"
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="edit-vatRate">KDV Oranı (%) *</Label>
                    <Input
                      id="edit-vatRate"
                      type="number"
                      value={formData.vatRate}
                      onChange={(e) => handleInputChange("vatRate", Number(e.target.value))}
                      placeholder="20"
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="edit-quantity">Stok Adedi *</Label>
                    <Input
                      id="edit-quantity"
                      type="number"
                      value={formData.quantity}
                      onChange={(e) => handleInputChange("quantity", Number(e.target.value))}
                      placeholder="10"
                    />
                  </div>
                </div>
              </div>

              {/* Ürün Detayları */}
              <div className="space-y-4">
                <h3 className="font-semibold text-sm text-muted-foreground uppercase tracking-wider">
                  Ürün Detayları
                </h3>
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="edit-productMainId">Model Kodu</Label>
                    <Input
                      id="edit-productMainId"
                      value={formData.productMainId || ""}
                      onChange={(e) => handleInputChange("productMainId", e.target.value)}
                      placeholder="ABC-123"
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="edit-dimensionalWeight">Boyutsal Ağırlık (Desi)</Label>
                    <Input
                      id="edit-dimensionalWeight"
                      type="number"
                      step="0.01"
                      value={formData.dimensionalWeight || ""}
                      onChange={(e) => handleInputChange("dimensionalWeight", Number(e.target.value))}
                      placeholder="1.5"
                    />
                    <p className="text-xs text-muted-foreground">
                      Kargo maliyeti: Desi × 8 TL
                    </p>
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="edit-color">Renk</Label>
                    <Input
                      id="edit-color"
                      value={formData.color || ""}
                      onChange={(e) => handleInputChange("color", e.target.value)}
                      placeholder="Siyah"
                    />
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="edit-size">Beden</Label>
                    <Input
                      id="edit-size"
                      value={formData.size || ""}
                      onChange={(e) => handleInputChange("size", e.target.value)}
                      placeholder="M"
                    />
                  </div>
                  <div className="col-span-2 space-y-2">
                    <Label htmlFor="edit-image">Resim URL</Label>
                    <Input
                      id="edit-image"
                      value={formData.image || ""}
                      onChange={(e) => handleInputChange("image", e.target.value)}
                      placeholder="https://cdn.trendyol.com/..."
                    />
                  </div>
                </div>
              </div>

              {/* Durum Bilgileri */}
              <div className="space-y-4">
                <h3 className="font-semibold text-sm text-muted-foreground uppercase tracking-wider">
                  Durum Bilgileri
                </h3>
                <div className="grid grid-cols-3 gap-4">
                  <div className="flex items-center space-x-2">
                    <Checkbox
                      id="edit-approved"
                      checked={formData.approved}
                      onCheckedChange={(checked) => handleInputChange("approved", !!checked)}
                    />
                    <Label htmlFor="edit-approved">Onaylı</Label>
                  </div>
                  <div className="flex items-center space-x-2">
                    <Checkbox
                      id="edit-onSale"
                      checked={formData.onSale}
                      onCheckedChange={(checked) => handleInputChange("onSale", !!checked)}
                    />
                    <Label htmlFor="edit-onSale">Satışta</Label>
                  </div>
                  <div className="flex items-center space-x-2">
                    <Checkbox
                      id="edit-hasActiveCampaign"
                      checked={formData.hasActiveCampaign}
                      onCheckedChange={(checked) => handleInputChange("hasActiveCampaign", !!checked)}
                    />
                    <Label htmlFor="edit-hasActiveCampaign">Kampanyalı</Label>
                  </div>
                  <div className="flex items-center space-x-2">
                    <Checkbox
                      id="edit-archived"
                      checked={formData.archived}
                      onCheckedChange={(checked) => handleInputChange("archived", !!checked)}
                    />
                    <Label htmlFor="edit-archived">Arşivlenmiş</Label>
                  </div>
                  <div className="flex items-center space-x-2">
                    <Checkbox
                      id="edit-rejected"
                      checked={formData.rejected}
                      onCheckedChange={(checked) => handleInputChange("rejected", !!checked)}
                    />
                    <Label htmlFor="edit-rejected">Reddedilmiş</Label>
                  </div>
                  <div className="flex items-center space-x-2">
                    <Checkbox
                      id="edit-blacklisted"
                      checked={formData.blacklisted}
                      onCheckedChange={(checked) => handleInputChange("blacklisted", !!checked)}
                    />
                    <Label htmlFor="edit-blacklisted">Kara Liste</Label>
                  </div>
                </div>
              </div>
            </div>

            <DialogFooter>
              <Button variant="outline" onClick={() => {
                setIsEditDialogOpen(false);
                setEditingProduct(null);
                setFormData(defaultFormData);
              }}>
                İptal
              </Button>
              <Button onClick={handleUpdate} disabled={updateProduct.isPending}>
                {updateProduct.isPending ? "Güncelleniyor..." : "Güncelle"}
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      </div>

      {/* Products Table */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Package className="h-5 w-5" />
            Sandbox Ürünleri
            {products && (
              <Badge variant="secondary" className="ml-2">
                {products.length} ürün
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
          ) : products && products.length > 0 ? (
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Barkod</TableHead>
                  <TableHead>Ürün Adı</TableHead>
                  <TableHead>Marka</TableHead>
                  <TableHead>Kategori</TableHead>
                  <TableHead className="text-right">Fiyat</TableHead>
                  <TableHead className="text-center">Stok</TableHead>
                  <TableHead className="text-center">Desi</TableHead>
                  <TableHead className="text-right">Kargo</TableHead>
                  <TableHead className="text-center">Durum</TableHead>
                  <TableHead className="text-right">İşlem</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {products.map((product) => (
                  <TableRow key={product.id}>
                    <TableCell className="font-mono text-sm">
                      {product.barcode}
                    </TableCell>
                    <TableCell className="max-w-[200px] truncate">
                      {product.title}
                    </TableCell>
                    <TableCell>{product.brand}</TableCell>
                    <TableCell className="max-w-[150px] truncate">
                      {product.categoryName}
                    </TableCell>
                    <TableCell className="text-right">
                      {formatPrice(product.salePrice)}
                    </TableCell>
                    <TableCell className="text-center">
                      {product.trendyolQuantity ?? 0}
                    </TableCell>
                    <TableCell className="text-center">
                      {product.dimensionalWeight ? (
                        <span className="font-mono">{product.dimensionalWeight}</span>
                      ) : (
                        <span className="text-muted-foreground">-</span>
                      )}
                    </TableCell>
                    <TableCell className="text-right">
                      {product.dimensionalWeight ? (
                        <span className="text-muted-foreground">
                          {formatPrice(product.dimensionalWeight * 8)}
                        </span>
                      ) : (
                        <span className="text-muted-foreground">-</span>
                      )}
                    </TableCell>
                    <TableCell className="text-center">
                      {product.onSale ? (
                        <Badge variant="default" className="bg-green-500">
                          Satışta
                        </Badge>
                      ) : (
                        <Badge variant="secondary">Pasif</Badge>
                      )}
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="flex items-center justify-end gap-1">
                        <Button
                          variant="ghost"
                          size="icon"
                          className="text-blue-500 hover:text-blue-700 hover:bg-blue-50"
                          onClick={() => handleEdit(product)}
                        >
                          <Pencil className="h-4 w-4" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          className="text-red-500 hover:text-red-700 hover:bg-red-50"
                          onClick={() => handleDelete(product.id)}
                          disabled={deleteProduct.isPending}
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
              <Package className="h-12 w-12 mx-auto mb-4 opacity-50" />
              <p>Henüz sandbox ürünü yok.</p>
              <p className="text-sm">Yukarıdaki &quot;Ürün Ekle&quot; butonuyla test ürünü ekleyin.</p>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
