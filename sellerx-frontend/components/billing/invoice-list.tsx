'use client';

import { format } from 'date-fns';
import { tr } from 'date-fns/locale';
import { Download, FileText } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import type { Invoice, InvoiceStatus } from '@/types/billing';
import { formatCurrency, getInvoiceStatusLabel } from '@/types/billing';

interface InvoiceListProps {
  invoices: Invoice[];
  isLoading?: boolean;
  onDownload?: (invoiceId: string) => void;
}

function getStatusBadgeVariant(
  status: InvoiceStatus
): 'default' | 'secondary' | 'destructive' | 'outline' {
  switch (status) {
    case 'PAID':
      return 'default';
    case 'PENDING':
      return 'secondary';
    case 'FAILED':
      return 'destructive';
    default:
      return 'outline';
  }
}

export function InvoiceList({ invoices, isLoading, onDownload }: InvoiceListProps) {
  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-8">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary" />
      </div>
    );
  }

  if (invoices.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-8 text-center">
        <FileText className="h-12 w-12 text-muted-foreground mb-4" />
        <h3 className="text-lg font-medium">Henüz fatura bulunmuyor</h3>
        <p className="text-sm text-muted-foreground">
          Abonelik ödemeleri yapıldığında faturalarınız burada görünecek.
        </p>
      </div>
    );
  }

  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Fatura No</TableHead>
          <TableHead>Tarih</TableHead>
          <TableHead>Dönem</TableHead>
          <TableHead>Tutar</TableHead>
          <TableHead>Durum</TableHead>
          <TableHead className="text-right">İşlem</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {invoices.map((invoice) => (
          <TableRow key={invoice.id}>
            <TableCell className="font-medium">{invoice.invoiceNumber}</TableCell>
            <TableCell>
              {format(new Date(invoice.createdAt), 'dd MMM yyyy', { locale: tr })}
            </TableCell>
            <TableCell>
              {format(new Date(invoice.billingPeriodStart), 'dd MMM', { locale: tr })} -{' '}
              {format(new Date(invoice.billingPeriodEnd), 'dd MMM yyyy', { locale: tr })}
            </TableCell>
            <TableCell>{formatCurrency(invoice.totalAmount, invoice.currency)}</TableCell>
            <TableCell>
              <Badge variant={getStatusBadgeVariant(invoice.status)}>
                {getInvoiceStatusLabel(invoice.status)}
              </Badge>
            </TableCell>
            <TableCell className="text-right">
              {invoice.status === 'PAID' && onDownload && (
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => onDownload(invoice.id)}
                >
                  <Download className="h-4 w-4" />
                </Button>
              )}
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}
