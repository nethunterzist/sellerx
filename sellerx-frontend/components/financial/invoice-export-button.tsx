"use client";

import { Button } from "@/components/ui/button";
import { Download } from "lucide-react";
import { useState } from "react";
import type { InvoiceDetail } from "@/types/invoice";
import { format } from "date-fns";
import { tr } from "date-fns/locale";
import ExcelJS from "exceljs";
import { saveAs } from "file-saver";

interface InvoiceExportButtonProps {
  invoices: InvoiceDetail[];
  filename?: string;
  isLoading?: boolean;
}

export function InvoiceExportButton({
  invoices,
  filename = "faturalar",
  isLoading = false,
}: InvoiceExportButtonProps) {
  const [isExporting, setIsExporting] = useState(false);

  const handleExport = async () => {
    if (invoices.length === 0) return;

    setIsExporting(true);

    try {
      // Prepare data for Excel
      const excelData = invoices.map((inv) => ({
        "Fatura No": inv.invoiceNumber || inv.invoiceTypeCode || "-",
        "Tarih": inv.invoiceDate
          ? format(new Date(inv.invoiceDate), "dd.MM.yyyy", { locale: tr })
          : "-",
        "Kategori": inv.invoiceCategory || "-",
        "Tip": inv.invoiceType || inv.invoiceTypeCode || "-",
        "Tutar": inv.amount || 0,
        "KDV": inv.vatAmount || 0,
        "Siparis No": inv.orderNumber || "-",
        "Aciklama": inv.description || "",
      }));

      const workbook = new ExcelJS.Workbook();
      const worksheet = workbook.addWorksheet("Faturalar");

      // Add header row
      const headers = Object.keys(excelData[0]);
      worksheet.addRow(headers);

      // Add data rows
      excelData.forEach((row) => {
        worksheet.addRow(Object.values(row));
      });

      // Set column widths
      const colWidths = [20, 12, 15, 25, 12, 10, 15, 40];
      worksheet.columns.forEach((col, i) => {
        col.width = colWidths[i] || 15;
      });

      // Generate filename with date
      const dateStr = new Date().toISOString().split("T")[0];

      // Write to buffer and save
      const buffer = await workbook.xlsx.writeBuffer();
      const blob = new Blob([buffer], { type: "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" });
      saveAs(blob, `${filename}-${dateStr}.xlsx`);
    } finally {
      setIsExporting(false);
    }
  };

  return (
    <Button
      variant="outline"
      size="sm"
      onClick={handleExport}
      disabled={isExporting || invoices.length === 0 || isLoading}
      className="gap-2"
    >
      <Download className="h-4 w-4" />
      Excel Indir
    </Button>
  );
}
