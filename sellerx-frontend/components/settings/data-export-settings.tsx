"use client";

import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { SettingsSection } from "./settings-section";
import {
  Download,
  FileJson,
  FileSpreadsheet,
  Package,
  ShoppingCart,
  BarChart3,
  Loader2,
  Info,
} from "lucide-react";
import { toast } from "sonner";

interface ExportOption {
  id: string;
  label: string;
  description: string;
  icon: typeof Package;
  formats: string[];
}

const exportOptions: ExportOption[] = [
  {
    id: "products",
    label: "Ürünler",
    description: "Tüm ürün bilgileri, maliyetler ve stok durumu",
    icon: Package,
    formats: ["CSV", "JSON"],
  },
  {
    id: "orders",
    label: "Siparişler",
    description: "Sipariş geçmişi ve detayları",
    icon: ShoppingCart,
    formats: ["CSV", "JSON"],
  },
  {
    id: "financial",
    label: "Finansal Veriler",
    description: "Satış, komisyon ve kâr/zarar raporları",
    icon: BarChart3,
    formats: ["CSV", "XLSX"],
  },
];

export function DataExportSettings() {
  const [selectedFormats, setSelectedFormats] = useState<Record<string, string>>({
    products: "CSV",
    orders: "CSV",
    financial: "CSV",
  });
  const [exporting, setExporting] = useState<string | null>(null);

  const handleExport = async (exportId: string) => {
    setExporting(exportId);

    // Simulate export - in real app, this would call an API
    await new Promise((resolve) => setTimeout(resolve, 1500));

    toast.info("Bu özellik yakında kullanılabilir olacak", {
      description: "Veri dışa aktarma özelliği geliştirme aşamasındadır.",
    });

    setExporting(null);
  };

  return (
    <SettingsSection
      title="Veri Dışa Aktarımı"
      description="Verilerinizi CSV veya JSON formatında indirin"
    >
      <div className="space-y-4">
        {exportOptions.map((option) => {
          const Icon = option.icon;
          const isExporting = exporting === option.id;

          return (
            <div
              key={option.id}
              className="flex items-center justify-between p-4 rounded-lg border border-border hover:border-border transition-colors"
            >
              <div className="flex items-center gap-4">
                <div className="h-12 w-12 rounded-xl bg-muted flex items-center justify-center">
                  <Icon className="h-6 w-6 text-muted-foreground" />
                </div>
                <div>
                  <div className="flex items-center gap-2">
                    <p className="font-medium text-foreground">{option.label}</p>
                    <Badge variant="outline" className="text-xs">
                      {option.formats.join(" / ")}
                    </Badge>
                  </div>
                  <p className="text-sm text-muted-foreground">{option.description}</p>
                </div>
              </div>

              <div className="flex items-center gap-3">
                <Select
                  value={selectedFormats[option.id]}
                  onValueChange={(value) =>
                    setSelectedFormats((prev) => ({ ...prev, [option.id]: value }))
                  }
                >
                  <SelectTrigger className="w-24">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {option.formats.map((format) => (
                      <SelectItem key={format} value={format}>
                        <span className="flex items-center gap-2">
                          {format === "JSON" ? (
                            <FileJson className="h-4 w-4" />
                          ) : (
                            <FileSpreadsheet className="h-4 w-4" />
                          )}
                          {format}
                        </span>
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>

                <Button
                  variant="outline"
                  onClick={() => handleExport(option.id)}
                  disabled={isExporting}
                >
                  {isExporting ? (
                    <Loader2 className="h-4 w-4 animate-spin" />
                  ) : (
                    <Download className="h-4 w-4" />
                  )}
                </Button>
              </div>
            </div>
          );
        })}
      </div>

      <div className="mt-6 p-4 rounded-lg bg-amber-50 border border-amber-200">
        <div className="flex items-start gap-3">
          <Info className="h-5 w-5 text-amber-600 mt-0.5" />
          <div>
            <p className="font-medium text-amber-900">Veri aktarımı hakkında</p>
            <p className="text-sm text-amber-700 mt-1">
              Dışa aktarılan veriler seçili mağazanıza aittir. Büyük veri setleri için
              dışa aktarma işlemi birkaç dakika sürebilir. İşlem tamamlandığında
              e-posta ile bilgilendirileceksiniz.
            </p>
          </div>
        </div>
      </div>

      <div className="mt-4 p-4 rounded-lg bg-muted border border-border">
        <div className="flex items-center justify-between">
          <div>
            <p className="font-medium text-foreground">Tüm Verileri İndir</p>
            <p className="text-sm text-muted-foreground">
              Hesabınızdaki tüm verileri tek seferde indirin (GDPR uyumlu)
            </p>
          </div>
          <Button variant="outline" disabled>
            <Download className="h-4 w-4 mr-2" />
            Yakında
          </Button>
        </div>
      </div>
    </SettingsSection>
  );
}
