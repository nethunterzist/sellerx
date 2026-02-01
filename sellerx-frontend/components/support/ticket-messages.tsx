'use client';

import { format } from 'date-fns';
import { tr } from 'date-fns/locale';
import { User, Shield } from 'lucide-react';
import { cn } from '@/lib/utils';
import type { TicketMessage } from '@/types/support';

interface TicketMessagesProps {
  messages: TicketMessage[];
}

export function TicketMessages({ messages }: TicketMessagesProps) {
  if (messages.length === 0) {
    return (
      <div className="text-center py-8 text-muted-foreground">
        Henüz mesaj yok.
      </div>
    );
  }

  return (
    <div className="space-y-4">
      {messages.map((message) => (
        <div
          key={message.id}
          className={cn(
            'flex gap-3 p-4 rounded-lg',
            message.isAdminReply
              ? 'bg-primary/5 border border-primary/20'
              : 'bg-muted/50'
          )}
        >
          <div
            className={cn(
              'flex-shrink-0 w-10 h-10 rounded-full flex items-center justify-center',
              message.isAdminReply
                ? 'bg-primary text-primary-foreground'
                : 'bg-secondary'
            )}
          >
            {message.isAdminReply ? (
              <Shield className="h-5 w-5" />
            ) : (
              <User className="h-5 w-5" />
            )}
          </div>

          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2 mb-1">
              <span className="font-medium text-sm">
                {message.senderName}
              </span>
              {message.isAdminReply && (
                <span className="text-xs bg-primary/10 text-primary px-2 py-0.5 rounded">
                  Destek Ekibi
                </span>
              )}
              <span className="text-xs text-muted-foreground">
                {format(new Date(message.createdAt), 'dd MMM yyyy HH:mm', { locale: tr })}
              </span>
            </div>
            <p className="text-sm whitespace-pre-wrap">{message.message}</p>
          </div>
        </div>
      ))}
    </div>
  );
}
