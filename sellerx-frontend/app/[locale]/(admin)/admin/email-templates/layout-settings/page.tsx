"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import {
  useBaseLayout,
  useUpdateBaseLayout,
} from "@/hooks/queries/use-admin-email-templates";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import {
  ArrowLeft,
  Settings,
  Save,
  Eye,
  Loader2,
  Palette,
  Image,
  Code,
} from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import { toast } from "sonner";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";

export default function AdminEmailLayoutSettingsPage() {
  const { data: layout, isLoading } = useBaseLayout();
  const updateMutation = useUpdateBaseLayout();

  const [headerHtml, setHeaderHtml] = useState("");
  const [footerHtml, setFooterHtml] = useState("");
  const [styles, setStyles] = useState("");
  const [logoUrl, setLogoUrl] = useState("");
  const [primaryColor, setPrimaryColor] = useState("#2563eb");
  const [showPreview, setShowPreview] = useState(false);

  // Initialize form when layout loads
  useEffect(() => {
    if (layout) {
      setHeaderHtml(layout.headerHtml || "");
      setFooterHtml(layout.footerHtml || "");
      setStyles(layout.styles || "");
      setLogoUrl(layout.logoUrl || "");
      setPrimaryColor(layout.primaryColor || "#2563eb");
    }
  }, [layout]);

  const handleSave = async () => {
    try {
      await updateMutation.mutateAsync({
        headerHtml,
        footerHtml,
        styles,
        logoUrl,
        primaryColor,
      });
      toast.success("Ayarlar başarıyla kaydedildi");
    } catch {
      toast.error("Ayarlar kaydedilemedi");
    }
  };

  const generatePreviewHtml = () => {
    const processedStyles = styles?.replace(/\{\{primaryColor\}\}/g, primaryColor) || "";
    const processedHeader = headerHtml?.replace(/\{\{logoUrl\}\}/g, logoUrl) || "";

    return `
      <!DOCTYPE html>
      <html lang="tr">
      <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <style>
          ${processedStyles}
        </style>
      </head>
      <body>
        <div class="container">
          ${processedHeader}
          <div class="content">
            <h2>Örnek Email İçeriği</h2>
            <p>Merhaba Ahmet Yılmaz,</p>
            <p>Bu bir örnek email içeriğidir. Gerçek email gönderiminde bu kısım şablon içeriğiyle değiştirilir.</p>
            <p style="text-align: center;">
              <a href="#" class="button">Örnek Buton</a>
            </p>
            <p>İyi günler dileriz,<br/>SellerX Ekibi</p>
          </div>
          ${footerHtml}
        </div>
      </body>
      </html>
    `;
  };

  if (isLoading) {
    return (
      <div className="space-y-6">
        <Skeleton className="h-8 w-64" />
        <Skeleton className="h-96 w-full" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Page Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <Button variant="ghost" size="icon" asChild>
            <Link href="/admin/email-templates">
              <ArrowLeft className="h-5 w-5" />
            </Link>
          </Button>
          <div>
            <h1 className="text-2xl font-bold text-slate-900 dark:text-white">
              Genel Email Ayarları
            </h1>
            <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
              Tüm emailler için geçerli base layout ayarları
            </p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <Button variant="outline" onClick={() => setShowPreview(true)}>
            <Eye className="h-4 w-4 mr-2" />
            Önizle
          </Button>
          <Button onClick={handleSave} disabled={updateMutation.isPending}>
            {updateMutation.isPending ? (
              <Loader2 className="h-4 w-4 mr-2 animate-spin" />
            ) : (
              <Save className="h-4 w-4 mr-2" />
            )}
            Kaydet
          </Button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Left Column - Settings */}
        <div className="space-y-6">
          {/* Brand Settings */}
          <Card>
            <CardHeader>
              <CardTitle className="text-base flex items-center gap-2">
                <Palette className="h-4 w-4" />
                Marka Ayarları
              </CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="logoUrl" className="flex items-center gap-2">
                  <Image className="h-4 w-4" />
                  Logo URL
                </Label>
                <Input
                  id="logoUrl"
                  value={logoUrl}
                  onChange={(e) => setLogoUrl(e.target.value)}
                  placeholder="https://example.com/logo.png"
                />
                <p className="text-xs text-slate-500">
                  Header'da gösterilecek logo resmi
                </p>
              </div>
              <div className="space-y-2">
                <Label htmlFor="primaryColor">Ana Renk</Label>
                <div className="flex gap-2">
                  <Input
                    id="primaryColor"
                    type="color"
                    value={primaryColor}
                    onChange={(e) => setPrimaryColor(e.target.value)}
                    className="w-16 h-10 p-1 cursor-pointer"
                  />
                  <Input
                    value={primaryColor}
                    onChange={(e) => setPrimaryColor(e.target.value)}
                    placeholder="#2563eb"
                    className="font-mono"
                  />
                </div>
                <p className="text-xs text-slate-500">
                  Linkler ve butonlar için kullanılır
                </p>
              </div>
            </CardContent>
          </Card>

          {/* Header HTML */}
          <Card>
            <CardHeader>
              <CardTitle className="text-base flex items-center gap-2">
                <Code className="h-4 w-4" />
                Header HTML
              </CardTitle>
              <CardDescription>
                Email'in üst kısmında gösterilecek HTML. {`{{logoUrl}}`} kullanılabilir.
              </CardDescription>
            </CardHeader>
            <CardContent>
              <Textarea
                value={headerHtml}
                onChange={(e) => setHeaderHtml(e.target.value)}
                placeholder='<div style="text-align: center;">...'
                className="min-h-[200px] font-mono text-sm"
              />
            </CardContent>
          </Card>

          {/* Footer HTML */}
          <Card>
            <CardHeader>
              <CardTitle className="text-base flex items-center gap-2">
                <Code className="h-4 w-4" />
                Footer HTML
              </CardTitle>
              <CardDescription>
                Email'in alt kısmında gösterilecek HTML (copyright, linkler vb.)
              </CardDescription>
            </CardHeader>
            <CardContent>
              <Textarea
                value={footerHtml}
                onChange={(e) => setFooterHtml(e.target.value)}
                placeholder='<div style="text-align: center;">...'
                className="min-h-[200px] font-mono text-sm"
              />
            </CardContent>
          </Card>
        </div>

        {/* Right Column - Styles */}
        <div className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle className="text-base flex items-center gap-2">
                <Settings className="h-4 w-4" />
                CSS Stilleri
              </CardTitle>
              <CardDescription>
                Tüm emaillere uygulanacak CSS stilleri. {`{{primaryColor}}`} kullanılabilir.
              </CardDescription>
            </CardHeader>
            <CardContent>
              <Textarea
                value={styles}
                onChange={(e) => setStyles(e.target.value)}
                placeholder="body { font-family: sans-serif; }..."
                className="min-h-[600px] font-mono text-sm"
              />
            </CardContent>
          </Card>
        </div>
      </div>

      {/* Preview Modal */}
      <Dialog open={showPreview} onOpenChange={setShowPreview}>
        <DialogContent className="max-w-4xl max-h-[90vh] overflow-hidden flex flex-col">
          <DialogHeader>
            <DialogTitle>Email Önizleme</DialogTitle>
            <DialogDescription>
              Base layout ile örnek email görünümü
            </DialogDescription>
          </DialogHeader>
          <div className="flex-1 overflow-auto border rounded-lg">
            <iframe
              srcDoc={generatePreviewHtml()}
              className="w-full h-[600px] bg-white"
              title="Email Preview"
            />
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}
