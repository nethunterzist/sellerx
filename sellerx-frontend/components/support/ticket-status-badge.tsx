'use client';

import { Badge } from '@/components/ui/badge';
import type { TicketStatus } from '@/types/support';

interface TicketStatusBadgeProps {
  status: TicketStatus;
}

const statusConfig: Record<TicketStatus, { label: string; variant: 'default' | 'secondary' | 'destructive' | 'outline' }> = {
  OPEN: { label: 'Açık', variant: 'destructive' },
  IN_PROGRESS: { label: 'İşlemde', variant: 'default' },
  WAITING_CUSTOMER: { label: 'Yanıt Bekleniyor', variant: 'secondary' },
  RESOLVED: { label: 'Çözüldü', variant: 'outline' },
  CLOSED: { label: 'Kapatıldı', variant: 'outline' },
};

export function TicketStatusBadge({ status }: TicketStatusBadgeProps) {
  const config = statusConfig[status] || { label: status, variant: 'outline' as const };

  return (
    <Badge variant={config.variant}>
      {config.label}
    </Badge>
  );
}
