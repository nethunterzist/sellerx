'use client';

import { use } from 'react';
import { useRouter } from 'next/navigation';
import { format } from 'date-fns';
import { tr } from 'date-fns/locale';
import { ArrowLeft, Clock, User, Store, Tag } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Separator } from '@/components/ui/separator';
import {
  TicketStatusBadge,
  TicketPriorityBadge,
  TicketCategoryBadge,
  TicketMessages,
  MessageInput,
} from '@/components/support';
import { useTicket, useAddMessage } from '@/hooks/queries/use-support';

interface TicketDetailPageProps {
  params: Promise<{ id: string }>;
}

export default function TicketDetailPage({ params }: TicketDetailPageProps) {
  const { id } = use(params);
  const ticketId = parseInt(id, 10);
  const router = useRouter();

  const { data: ticket, isLoading, error } = useTicket(ticketId);
  const addMessage = useAddMessage(ticketId);

  const handleSendMessage = async (message: string) => {
    await addMessage.mutateAsync({ message });
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-12">
        <p className="text-muted-foreground">Yükleniyor...</p>
      </div>
    );
  }

  if (error || !ticket) {
    return (
      <div className="flex flex-col items-center justify-center py-12">
        <p className="text-destructive mb-4">Destek talebi bulunamadı</p>
        <Button variant="outline" onClick={() => router.push('/support')}>
          <ArrowLeft className="h-4 w-4 mr-2" />
          Geri Dön
        </Button>
      </div>
    );
  }

  const isClosed = ticket.status === 'CLOSED' || ticket.status === 'RESOLVED';

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" onClick={() => router.push('/support')}>
          <ArrowLeft className="h-5 w-5" />
        </Button>
        <div className="flex-1">
          <div className="flex items-center gap-3">
            <h1 className="text-xl font-bold">{ticket.subject}</h1>
            <TicketStatusBadge status={ticket.status} />
          </div>
          <p className="text-sm text-muted-foreground font-mono">{ticket.ticketNumber}</p>
        </div>
      </div>

      {/* Info Card */}
      <Card>
        <CardContent className="pt-6">
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <div className="flex items-center gap-2">
              <Tag className="h-4 w-4 text-muted-foreground" />
              <div>
                <p className="text-xs text-muted-foreground">Kategori</p>
                <TicketCategoryBadge category={ticket.category} />
              </div>
            </div>
            <div className="flex items-center gap-2">
              <Clock className="h-4 w-4 text-muted-foreground" />
              <div>
                <p className="text-xs text-muted-foreground">Öncelik</p>
                <TicketPriorityBadge priority={ticket.priority} />
              </div>
            </div>
            <div className="flex items-center gap-2">
              <User className="h-4 w-4 text-muted-foreground" />
              <div>
                <p className="text-xs text-muted-foreground">Oluşturulma</p>
                <p className="text-sm">
                  {format(new Date(ticket.createdAt), 'dd MMM yyyy HH:mm', { locale: tr })}
                </p>
              </div>
            </div>
            {ticket.storeName && (
              <div className="flex items-center gap-2">
                <Store className="h-4 w-4 text-muted-foreground" />
                <div>
                  <p className="text-xs text-muted-foreground">Mağaza</p>
                  <p className="text-sm">{ticket.storeName}</p>
                </div>
              </div>
            )}
          </div>
        </CardContent>
      </Card>

      {/* Messages */}
      <Card>
        <CardHeader>
          <CardTitle>Mesajlar</CardTitle>
        </CardHeader>
        <CardContent>
          <TicketMessages messages={ticket.messages || []} />

          {!isClosed && (
            <>
              <Separator className="my-6" />
              <MessageInput
                onSend={handleSendMessage}
                disabled={addMessage.isPending}
                placeholder="Yanıtınızı yazın... (Ctrl+Enter ile gönder)"
              />
            </>
          )}

          {isClosed && (
            <div className="mt-6 p-4 bg-muted rounded-lg text-center text-muted-foreground">
              Bu talep kapatılmış. Yeni bir sorunuz varsa yeni talep oluşturabilirsiniz.
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
