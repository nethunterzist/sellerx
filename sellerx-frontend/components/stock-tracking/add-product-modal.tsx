"use client";

import { useState, useEffect } from "react";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  useAddTrackedProduct,
  useProductPreview,
  isValidTrendyolProductUrl,
} from "@/hooks/queries/use-stock-tracking";
import { useTranslations } from "next-intl";
import {
  Loader2,
  CheckCircle2,
  AlertCircle,
  Package,
  ImageOff,
} from "lucide-react";
import { toast } from "sonner";
import Image from "next/image";

interface AddProductModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  storeId: string | undefined;
}

export function AddProductModal({
  open,
  onOpenChange,
  storeId,
}: AddProductModalProps) {
  const t = useTranslations("stockTracking");
  const [productUrl, setProductUrl] = useState("");
  const [debouncedUrl, setDebouncedUrl] = useState("");

  const addProduct = useAddTrackedProduct(storeId);

  // Debounce URL input (500ms)
  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedUrl(productUrl.trim());
    }, 500);

    return () => clearTimeout(timer);
  }, [productUrl]);

  // Preview query
  const previewQuery = useProductPreview(debouncedUrl);
  const isUrlValid = isValidTrendyolProductUrl(debouncedUrl);

  // Show error for any non-empty URL that isn't a valid Trendyol product page
  const showUrlError = debouncedUrl.length > 0 && !isUrlValid;

  // Can submit only if preview is valid
  const canSubmit =
    previewQuery.data?.isValid === true && !addProduct.isPending;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!previewQuery.data?.isValid) {
      toast.error(t("addProductForm.invalidProductUrl"));
      return;
    }

    try {
      await addProduct.mutateAsync({
        productUrl: productUrl.trim(),
      });

      toast.success(t("addProductForm.success"));
      onOpenChange(false);
      resetForm();
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : t("addProductForm.error")
      );
    }
  };

  const resetForm = () => {
    setProductUrl("");
    setDebouncedUrl("");
  };

  // Format price
  const formatPrice = (price: number | null) => {
    if (price === null) return "";
    return new Intl.NumberFormat("tr-TR", {
      style: "currency",
      currency: "TRY",
    }).format(price);
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle>{t("addProductForm.title")}</DialogTitle>
          <DialogDescription>
            {t("addProductForm.description")}
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit} className="space-y-4">
          {/* URL Input */}
          <div className="space-y-2">
            <Label htmlFor="productUrl">{t("addProductForm.urlLabel")}</Label>
            <div className="relative">
              <Input
                id="productUrl"
                type="url"
                placeholder="https://www.trendyol.com/marka/urun-p-123456789"
                value={productUrl}
                onChange={(e) => setProductUrl(e.target.value)}
                disabled={addProduct.isPending}
                className={
                  showUrlError
                    ? "border-red-500 focus-visible:ring-red-500"
                    : ""
                }
              />
              {/* Status indicator on the right side of input */}
              {debouncedUrl && (
                <div className="absolute right-3 top-1/2 -translate-y-1/2">
                  {previewQuery.isFetching ? (
                    <Loader2 className="h-4 w-4 animate-spin text-muted-foreground" />
                  ) : previewQuery.data?.isValid ? (
                    <CheckCircle2 className="h-4 w-4 text-green-500" />
                  ) : isUrlValid ? (
                    <AlertCircle className="h-4 w-4 text-red-500" />
                  ) : null}
                </div>
              )}
            </div>
            <p className="text-xs text-muted-foreground">
              {t("addProductForm.urlHint")}
            </p>

            {/* URL validation error */}
            {showUrlError && (
              <p className="text-xs text-red-500 flex items-center gap-1">
                <AlertCircle className="h-3 w-3" />
                {t("addProductForm.invalidProductUrl")}
              </p>
            )}
          </div>

          {/* Product Preview Card */}
          {previewQuery.isFetching && isUrlValid && (
            <div className="flex items-center justify-center p-4 bg-muted/50 rounded-lg">
              <Loader2 className="h-5 w-5 animate-spin mr-2" />
              <span className="text-sm text-muted-foreground">
                {t("addProductForm.validating")}
              </span>
            </div>
          )}

          {previewQuery.data && !previewQuery.isFetching && isUrlValid && (
            <div
              className={`rounded-lg border p-4 ${
                previewQuery.data.isValid
                  ? "border-green-200 bg-green-50 dark:border-green-900 dark:bg-green-950/50"
                  : "border-red-200 bg-red-50 dark:border-red-900 dark:bg-red-950/50"
              }`}
            >
              {previewQuery.data.isValid ? (
                <div className="flex gap-4">
                  {/* Product Image */}
                  <div className="flex-shrink-0 w-16 h-16 relative rounded-md overflow-hidden bg-white border">
                    {previewQuery.data.imageUrl ? (
                      <Image
                        src={previewQuery.data.imageUrl}
                        alt={previewQuery.data.productName || "Product"}
                        fill
                        className="object-contain"
                        sizes="64px"
                      />
                    ) : (
                      <div className="w-full h-full flex items-center justify-center bg-muted">
                        <ImageOff className="h-6 w-6 text-muted-foreground" />
                      </div>
                    )}
                  </div>

                  {/* Product Info */}
                  <div className="flex-1 min-w-0">
                    <h4 className="font-medium text-sm line-clamp-2">
                      {previewQuery.data.productName}
                    </h4>
                    {previewQuery.data.brandName && (
                      <p className="text-xs text-muted-foreground mt-0.5">
                        {previewQuery.data.brandName}
                      </p>
                    )}
                    <div className="flex items-center gap-2 mt-1.5">
                      {previewQuery.data.price && (
                        <span className="text-sm font-semibold text-primary">
                          {formatPrice(previewQuery.data.price)}
                        </span>
                      )}
                      <span className="text-xs">•</span>
                      {previewQuery.data.inStock ? (
                        <span className="text-xs text-green-600 dark:text-green-400 flex items-center gap-1">
                          <Package className="h-3 w-3" />
                          {t("addProductForm.inStock")} (
                          {previewQuery.data.quantity}{" "}
                          {t("addProductForm.pieces")})
                        </span>
                      ) : (
                        <span className="text-xs text-red-600 dark:text-red-400">
                          {t("addProductForm.outOfStock")}
                        </span>
                      )}
                    </div>
                  </div>
                </div>
              ) : (
                <div className="flex items-center gap-2 text-red-600 dark:text-red-400">
                  <AlertCircle className="h-4 w-4" />
                  <span className="text-sm">
                    {previewQuery.data.errorMessage ||
                      t("addProductForm.previewError")}
                  </span>
                </div>
              )}
            </div>
          )}

          <DialogFooter>
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
              disabled={addProduct.isPending}
            >
              {t("common.cancel")}
            </Button>
            <Button type="submit" disabled={!canSubmit}>
              {addProduct.isPending && (
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
              )}
              {t("addProductForm.submit")}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
