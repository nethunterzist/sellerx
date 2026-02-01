"use client";

import { Loader2 } from "lucide-react";
import { SettingsSection } from "./settings-section";
import { InvoiceList } from "@/components/billing/invoice-list";
import { useInvoices } from "@/hooks/queries/use-billing";
import { toast } from "sonner";

export function InvoicesSettings() {
  const { data: invoicesData, isLoading } = useInvoices(0, 20);

  const handleDownload = async (invoiceId: string) => {
    try {
      // Open invoice PDF in new tab
      window.open(`/api/billing/invoices/${invoiceId}/pdf`, "_blank");
    } catch (error) {
      toast.error("Fatura indirilemedi");
    }
  };

  return (
    <SettingsSection
      title="Faturalar"
      description="Geçmiş faturalarınızı görüntüleyin ve indirin"
      noPadding
    >
      {isLoading ? (
        <div className="flex items-center justify-center py-8">
          <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
        </div>
      ) : (
        <div className="p-6">
          <InvoiceList
            invoices={invoicesData?.content || []}
            isLoading={isLoading}
            onDownload={handleDownload}
          />
        </div>
      )}
    </SettingsSection>
  );
}
