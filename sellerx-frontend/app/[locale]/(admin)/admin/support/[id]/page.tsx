'use client';

import { use, useState } from 'react';
import { useRouter } from 'next/navigation';
import { format } from 'date-fns';
import { tr } from 'date-fns/locale';
import {
  ArrowLeft,
  Clock,
  User,
  Store,
  Tag,
  Send,
  UserCheck,
} from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Separator } from '@/components/ui/separator';
import { Textarea } from '@/components/ui/textarea';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import {
  TicketStatusBadge,
  TicketPriorityBadge,
  TicketCategoryBadge,
  TicketMessages,
} from '@/components/support';
import {
  useAdminTicket,
  useAdminReply,
  useUpdateTicketStatus,
} from '@/hooks/queries/use-support';
import type { TicketStatus } from '@/types/support';

interface AdminTicketDetailPageProps {
  params: Promise<{ id: string }>;
}

export default function AdminTicketDetailPage({ params }: AdminTicketDetailPageProps) {
  const { id } = use(params);
  const ticketId = parseInt(id, 10);
  const router = useRouter();

  const [replyMessage, setReplyMessage] = useState('');
  const [newStatus, setNewStatus] = useState<TicketStatus | ''>('');

  const { data: ticket, isLoading, error } = useAdminTicket(ticketId);
  const adminReply = useAdminReply(ticketId);
  const updateStatus = useUpdateTicketStatus(ticketId);

  const handleSendReply = async () => {
    if (!replyMessage.trim()) return;

    await adminReply.mutateAsync({ message: replyMessage });
    setReplyMessage('');
  };

  const handleStatusChange = async (status: TicketStatus) => {
    setNewStatus(status);
    await updateStatus.mutateAsync({ status });
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && (e.ctrlKey || e.metaKey)) {
      handleSendReply();
    }
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
        <Button variant="outline" onClick={() => router.push('/admin/support')}>
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
        <Button variant="ghost" size="icon" onClick={() => router.push('/admin/support')}>
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

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Main Content */}
        <div className="lg:col-span-2 space-y-6">
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
                  <div className="space-y-3">
                    <Textarea
                      value={replyMessage}
                      onChange={(e) => setReplyMessage(e.target.value)}
                      onKeyDown={handleKeyDown}
                      placeholder="Yanıtınızı yazın... (Ctrl+Enter ile gönder)"
                      disabled={adminReply.isPending}
                      rows={4}
                      className="resize-none"
                    />
                    <div className="flex justify-end">
                      <Button
                        onClick={handleSendReply}
                        disabled={!replyMessage.trim() || adminReply.isPending}
                      >
                        <Send className="h-4 w-4 mr-2" />
                        {adminReply.isPending ? 'Gönderiliyor...' : 'Yanıtla'}
                      </Button>
                    </div>
                  </div>
                </>
              )}

              {isClosed && (
                <div className="mt-6 p-4 bg-muted rounded-lg text-center text-muted-foreground">
                  Bu talep kapatılmış.
                </div>
              )}
            </CardContent>
          </Card>
        </div>

        {/* Sidebar */}
        <div className="space-y-6">
          {/* Ticket Info */}
          <Card>
            <CardHeader>
              <CardTitle className="text-base">Talep Bilgileri</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div className="flex items-center gap-3">
                <User className="h-4 w-4 text-muted-foreground" />
                <div>
                  <p className="text-xs text-muted-foreground">Kullanıcı</p>
                  <p className="text-sm font-medium">{ticket.userName}</p>
                  <p className="text-xs text-muted-foreground">{ticket.userEmail}</p>
                </div>
              </div>

              {ticket.storeName && (
                <div className="flex items-center gap-3">
                  <Store className="h-4 w-4 text-muted-foreground" />
                  <div>
                    <p className="text-xs text-muted-foreground">Mağaza</p>
                    <p className="text-sm font-medium">{ticket.storeName}</p>
                  </div>
                </div>
              )}

              <Separator />

              <div className="flex items-center gap-3">
                <Tag className="h-4 w-4 text-muted-foreground" />
                <div>
                  <p className="text-xs text-muted-foreground">Kategori</p>
                  <TicketCategoryBadge category={ticket.category} />
                </div>
              </div>

              <div className="flex items-center gap-3">
                <Clock className="h-4 w-4 text-muted-foreground" />
                <div>
                  <p className="text-xs text-muted-foreground">Öncelik</p>
                  <TicketPriorityBadge priority={ticket.priority} />
                </div>
              </div>

              <Separator />

              <div>
                <p className="text-xs text-muted-foreground mb-1">Oluşturulma</p>
                <p className="text-sm">
                  {format(new Date(ticket.createdAt), 'dd MMM yyyy HH:mm', { locale: tr })}
                </p>
              </div>

              {ticket.updatedAt && (
                <div>
                  <p className="text-xs text-muted-foreground mb-1">Son Güncelleme</p>
                  <p className="text-sm">
                    {format(new Date(ticket.updatedAt), 'dd MMM yyyy HH:mm', { locale: tr })}
                  </p>
                </div>
              )}

              {ticket.assignedToName && (
                <div className="flex items-center gap-3">
                  <UserCheck className="h-4 w-4 text-muted-foreground" />
                  <div>
                    <p className="text-xs text-muted-foreground">Atanan</p>
                    <p className="text-sm font-medium">{ticket.assignedToName}</p>
                  </div>
                </div>
              )}
            </CardContent>
          </Card>

          {/* Actions */}
          <Card>
            <CardHeader>
              <CardTitle className="text-base">İşlemler</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4">
              <div>
                <p className="text-xs text-muted-foreground mb-2">Durum Değiştir</p>
                <Select
                  value={newStatus || ticket.status}
                  onValueChange={(v) => handleStatusChange(v as TicketStatus)}
                  disabled={updateStatus.isPending}
                >
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="OPEN">Açık</SelectItem>
                    <SelectItem value="IN_PROGRESS">İşlemde</SelectItem>
                    <SelectItem value="WAITING_CUSTOMER">Müşteri Bekleniyor</SelectItem>
                    <SelectItem value="RESOLVED">Çözümlendi</SelectItem>
                    <SelectItem value="CLOSED">Kapatıldı</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              {updateStatus.isPending && (
                <p className="text-xs text-muted-foreground text-center">
                  Durum güncelleniyor...
                </p>
              )}
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
