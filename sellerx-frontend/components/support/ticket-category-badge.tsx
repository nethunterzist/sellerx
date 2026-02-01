'use client';

import { Badge } from '@/components/ui/badge';
import type { TicketCategory } from '@/types/support';

interface TicketCategoryBadgeProps {
  category: TicketCategory;
}

const categoryConfig: Record<TicketCategory, string> = {
  TECHNICAL: 'Teknik',
  BILLING: 'Faturalama',
  ORDER: 'Sipariş',
  PRODUCT: 'Ürün',
  INTEGRATION: 'Entegrasyon',
  OTHER: 'Diğer',
};

export function TicketCategoryBadge({ category }: TicketCategoryBadgeProps) {
  const label = categoryConfig[category] || category;

  return (
    <Badge variant="outline">
      {label}
    </Badge>
  );
}
