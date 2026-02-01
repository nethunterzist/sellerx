"use client";

import { useMemo } from "react";
import Link from "next/link";
import { Ticket, AlertCircle, Clock, User, ArrowRight } from "lucide-react";
import { Skeleton } from "@/components/ui/skeleton";
import { useAdminTicketStats, useAdminActiveTickets } from "@/hooks/queries/use-support";
import type { Ticket as TicketType, TicketStatus, TicketPriority } from "@/types/support";

// Priority color configuration
const priorityConfig: Record<TicketPriority, { label: string; color: string; bgColor: string }> = {
  LOW: {
    label: "Düşük",
    color: "text-gray-600 dark:text-gray-400",
    bgColor: "bg-gray-100 dark:bg-gray-800",
  },
  MEDIUM: {
    label: "Orta",
    color: "text-blue-600 dark:text-blue-400",
    bgColor: "bg-blue-100 dark:bg-blue-900/40",
  },
  HIGH: {
    label: "Yüksek",
    color: "text-amber-600 dark:text-amber-400",
    bgColor: "bg-amber-100 dark:bg-amber-900/40",
  },
  URGENT: {
    label: "Acil",
    color: "text-red-600 dark:text-red-400",
    bgColor: "bg-red-100 dark:bg-red-900/40",
  },
};

// Status color configuration
const statusConfig: Record<TicketStatus, { label: string; color: string; bgColor: string }> = {
  OPEN: {
    label: "Açık",
    color: "text-blue-600 dark:text-blue-400",
    bgColor: "bg-blue-100 dark:bg-blue-900/40",
  },
  IN_PROGRESS: {
    label: "İşlemde",
    color: "text-yellow-600 dark:text-yellow-400",
    bgColor: "bg-yellow-100 dark:bg-yellow-900/40",
  },
  WAITING_CUSTOMER: {
    label: "Yanıt Bekliyor",
    color: "text-purple-600 dark:text-purple-400",
    bgColor: "bg-purple-100 dark:bg-purple-900/40",
  },
  RESOLVED: {
    label: "Çözüldü",
    color: "text-green-600 dark:text-green-400",
    bgColor: "bg-green-100 dark:bg-green-900/40",
  },
  CLOSED: {
    label: "Kapalı",
    color: "text-gray-600 dark:text-gray-400",
    bgColor: "bg-gray-100 dark:bg-gray-800",
  },
};

function formatRelativeTime(dateString: string): string {
  const date = new Date(dateString);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffSecs = Math.floor(diffMs / 1000);
  const diffMins = Math.floor(diffSecs / 60);
  const diffHours = Math.floor(diffMins / 60);
  const diffDays = Math.floor(diffHours / 24);

  if (diffSecs < 60) return "Az önce";
  if (diffMins < 60) return `${diffMins} dk önce`;
  if (diffHours < 24) return `${diffHours} saat önce`;
  if (diffDays < 7) return `${diffDays} gün önce`;

  return date.toLocaleDateString("tr-TR", {
    day: "numeric",
    month: "short",
  });
}

export function SupportTicketsCard() {
  const { data: stats, isLoading: statsLoading } = useAdminTicketStats();
  const { data: ticketsData, isLoading: ticketsLoading } = useAdminActiveTickets(0, 5);

  const isLoading = statsLoading || ticketsLoading;
  const tickets = useMemo(() => ticketsData?.content || [], [ticketsData]);

  // Count of open + in_progress (active tickets needing attention)
  const activeCount = (stats?.openTickets || 0) + (stats?.inProgressTickets || 0);

  if (isLoading) {
    return (
      <div className="bg-card rounded-xl border border-border p-6">
        <div className="flex items-center justify-between mb-4">
          <Skeleton className="h-5 w-36" />
          <Skeleton className="h-5 w-16" />
        </div>
        {/* Stats bar skeleton */}
        <div className="flex gap-2 mb-4">
          <Skeleton className="h-6 w-20" />
          <Skeleton className="h-6 w-20" />
          <Skeleton className="h-6 w-24" />
        </div>
        {/* Ticket list skeleton */}
        <div className="space-y-3">
          {Array.from({ length: 5 }).map((_, i) => (
            <div key={i} className="flex items-start gap-3 p-3 rounded-lg border border-border/50">
              <Skeleton className="h-8 w-8 rounded-full flex-shrink-0" />
              <div className="flex-1 space-y-2">
                <Skeleton className="h-4 w-3/4" />
                <Skeleton className="h-3 w-1/2" />
              </div>
            </div>
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="bg-card rounded-xl border border-border p-6">
      {/* Header */}
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2">
          <Ticket className="h-5 w-5 text-primary" />
          <h3 className="font-semibold text-foreground">Destek Talepleri</h3>
          {activeCount > 0 && (
            <span className="px-2 py-0.5 text-xs font-medium rounded-full bg-red-100 dark:bg-red-900/40 text-red-600 dark:text-red-400">
              {activeCount}
            </span>
          )}
        </div>
        <Link
          href="/admin/support"
          className="text-xs text-primary hover:underline flex items-center gap-1"
        >
          Tümünü Gör
          <ArrowRight className="h-3 w-3" />
        </Link>
      </div>

      {/* Stats Bar */}
      {stats && (
        <div className="flex flex-wrap gap-2 mb-4">
          <div className="flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-blue-100 dark:bg-blue-900/40">
            <AlertCircle className="h-3.5 w-3.5 text-blue-600 dark:text-blue-400" />
            <span className="text-xs font-medium text-blue-600 dark:text-blue-400">
              Açık: {stats.openTickets}
            </span>
          </div>
          <div className="flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-yellow-100 dark:bg-yellow-900/40">
            <Clock className="h-3.5 w-3.5 text-yellow-600 dark:text-yellow-400" />
            <span className="text-xs font-medium text-yellow-600 dark:text-yellow-400">
              İşlemde: {stats.inProgressTickets}
            </span>
          </div>
          <div className="flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-purple-100 dark:bg-purple-900/40">
            <User className="h-3.5 w-3.5 text-purple-600 dark:text-purple-400" />
            <span className="text-xs font-medium text-purple-600 dark:text-purple-400">
              Bekleyen: {stats.waitingCustomerTickets}
            </span>
          </div>
        </div>
      )}

      {/* Ticket List */}
      {tickets.length === 0 ? (
        <div className="flex flex-col items-center justify-center h-[200px] text-muted-foreground">
          <Ticket className="h-10 w-10 mb-2 opacity-40" />
          <span className="text-sm">Açık destek talebi yok</span>
        </div>
      ) : (
        <div className="space-y-2 max-h-[280px] overflow-y-auto pr-1">
          {tickets.map((ticket: TicketType) => {
            const priority = priorityConfig[ticket.priority];
            const status = statusConfig[ticket.status];

            return (
              <Link
                key={ticket.id}
                href={`/admin/support/${ticket.id}`}
                className="block p-3 rounded-lg border border-border/50 hover:border-border hover:bg-muted/30 transition-colors group"
              >
                <div className="flex items-start justify-between gap-2">
                  <div className="flex-1 min-w-0">
                    {/* Ticket number and subject */}
                    <div className="flex items-center gap-2 mb-1">
                      <span className="text-xs font-mono text-muted-foreground">
                        #{ticket.ticketNumber}
                      </span>
                      {ticket.priority === "HIGH" || ticket.priority === "URGENT" ? (
                        <span className={`px-1.5 py-0.5 text-[10px] font-medium rounded ${priority.bgColor} ${priority.color}`}>
                          {priority.label}
                        </span>
                      ) : null}
                    </div>
                    <p className="text-sm font-medium text-foreground truncate group-hover:text-primary transition-colors">
                      {ticket.subject}
                    </p>
                    {/* User info and time */}
                    <div className="flex items-center gap-2 mt-1">
                      <span className="text-xs text-muted-foreground truncate max-w-[120px]">
                        {ticket.userName}
                      </span>
                      <span className="text-xs text-muted-foreground">•</span>
                      <span className="text-xs text-muted-foreground">
                        {formatRelativeTime(ticket.createdAt)}
                      </span>
                    </div>
                  </div>
                  {/* Status badge */}
                  <span className={`flex-shrink-0 px-2 py-0.5 text-[10px] font-medium rounded ${status.bgColor} ${status.color}`}>
                    {status.label}
                  </span>
                </div>
              </Link>
            );
          })}
        </div>
      )}
    </div>
  );
}
