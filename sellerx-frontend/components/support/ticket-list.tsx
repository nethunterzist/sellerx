'use client';

import { useRouter } from 'next/navigation';
import { formatDistanceToNow } from 'date-fns';
import { tr } from 'date-fns/locale';
import { MessageSquare } from 'lucide-react';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import { TicketStatusBadge } from './ticket-status-badge';
import { TicketPriorityBadge } from './ticket-priority-badge';
import { TicketCategoryBadge } from './ticket-category-badge';
import type { Ticket } from '@/types/support';

interface TicketListProps {
  tickets: Ticket[];
  isAdmin?: boolean;
}

export function TicketList({ tickets, isAdmin = false }: TicketListProps) {
  const router = useRouter();

  const handleRowClick = (ticketId: number) => {
    if (isAdmin) {
      router.push(`/admin/support/${ticketId}`);
    } else {
      router.push(`/support/${ticketId}`);
    }
  };

  if (tickets.length === 0) {
    return (
      <div className="text-center py-12 text-muted-foreground">
        Henüz destek talebi bulunmuyor.
      </div>
    );
  }

  return (
    <Table>
      <TableHeader>
        <TableRow>
          <TableHead>Talep No</TableHead>
          <TableHead>Konu</TableHead>
          {isAdmin && <TableHead>Kullanıcı</TableHead>}
          <TableHead>Kategori</TableHead>
          <TableHead>Öncelik</TableHead>
          <TableHead>Durum</TableHead>
          <TableHead>Tarih</TableHead>
          <TableHead className="text-center">Mesaj</TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        {tickets.map((ticket) => (
          <TableRow
            key={ticket.id}
            className="cursor-pointer hover:bg-muted/50"
            onClick={() => handleRowClick(ticket.id)}
          >
            <TableCell className="font-mono text-sm">
              {ticket.ticketNumber}
            </TableCell>
            <TableCell className="max-w-[200px] truncate">
              {ticket.subject}
            </TableCell>
            {isAdmin && (
              <TableCell>
                <div className="text-sm">
                  <div>{ticket.userName}</div>
                  <div className="text-muted-foreground text-xs">{ticket.userEmail}</div>
                </div>
              </TableCell>
            )}
            <TableCell>
              <TicketCategoryBadge category={ticket.category} />
            </TableCell>
            <TableCell>
              <TicketPriorityBadge priority={ticket.priority} />
            </TableCell>
            <TableCell>
              <TicketStatusBadge status={ticket.status} />
            </TableCell>
            <TableCell className="text-sm text-muted-foreground">
              {formatDistanceToNow(new Date(ticket.createdAt), {
                addSuffix: true,
                locale: tr,
              })}
            </TableCell>
            <TableCell className="text-center">
              <div className="flex items-center justify-center gap-1">
                <MessageSquare className="h-4 w-4" />
                <span>{ticket.messageCount}</span>
              </div>
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}
