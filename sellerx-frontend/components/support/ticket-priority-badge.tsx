'use client';

import { Badge } from '@/components/ui/badge';
import type { TicketPriority } from '@/types/support';

interface TicketPriorityBadgeProps {
  priority: TicketPriority;
}

const priorityConfig: Record<TicketPriority, { label: string; className: string }> = {
  LOW: { label: 'Düşük', className: 'bg-gray-100 text-gray-800 hover:bg-gray-100' },
  MEDIUM: { label: 'Normal', className: 'bg-blue-100 text-blue-800 hover:bg-blue-100' },
  HIGH: { label: 'Yüksek', className: 'bg-orange-100 text-orange-800 hover:bg-orange-100' },
  URGENT: { label: 'Acil', className: 'bg-red-100 text-red-800 hover:bg-red-100' },
};

export function TicketPriorityBadge({ priority }: TicketPriorityBadgeProps) {
  const config = priorityConfig[priority] || { label: priority, className: '' };

  return (
    <Badge variant="outline" className={config.className}>
      {config.label}
    </Badge>
  );
}
