import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import type {
  Ticket,
  PagedTickets,
  TicketStats,
  CreateTicketRequest,
  AddMessageRequest,
  UpdateTicketStatusRequest,
  AssignTicketRequest,
  TicketFilters,
  TicketMessage,
} from '@/types/support';

// === Query Keys ===
export const supportKeys = {
  all: ['support'] as const,
  tickets: () => [...supportKeys.all, 'tickets'] as const,
  ticket: (id: number) => [...supportKeys.tickets(), id] as const,
  admin: ['admin-support'] as const,
  adminTickets: () => [...supportKeys.admin, 'tickets'] as const,
  adminTicket: (id: number) => [...supportKeys.adminTickets(), id] as const,
  adminStats: () => [...supportKeys.admin, 'stats'] as const,
  adminActive: () => [...supportKeys.admin, 'active'] as const,
};

// === User Hooks ===

/**
 * Fetch user's tickets with pagination
 */
export function useMyTickets(page = 0, size = 10) {
  return useQuery<PagedTickets>({
    queryKey: [...supportKeys.tickets(), { page, size }],
    queryFn: async () => {
      const res = await fetch(`/api/support/tickets?page=${page}&size=${size}`);
      if (!res.ok) throw new Error('Destek talepleri yüklenemedi');
      return res.json();
    },
  });
}

/**
 * Fetch single ticket detail (user)
 */
export function useTicket(id: number) {
  return useQuery<Ticket>({
    queryKey: supportKeys.ticket(id),
    queryFn: async () => {
      const res = await fetch(`/api/support/tickets/${id}`);
      if (!res.ok) throw new Error('Destek talebi bulunamadı');
      return res.json();
    },
    enabled: !!id,
  });
}

/**
 * Create new ticket mutation
 */
export function useCreateTicket() {
  const queryClient = useQueryClient();

  return useMutation<Ticket, Error, CreateTicketRequest>({
    mutationFn: async (data) => {
      const res = await fetch('/api/support/tickets', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data),
      });
      if (!res.ok) {
        const error = await res.json().catch(() => ({}));
        throw new Error(error.message || 'Destek talebi oluşturulamadı');
      }
      return res.json();
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: supportKeys.tickets() });
    },
  });
}

/**
 * Add message to ticket mutation (user)
 */
export function useAddMessage(ticketId: number) {
  const queryClient = useQueryClient();

  return useMutation<TicketMessage, Error, AddMessageRequest>({
    mutationFn: async (data) => {
      const res = await fetch(`/api/support/tickets/${ticketId}/messages`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data),
      });
      if (!res.ok) {
        const error = await res.json().catch(() => ({}));
        throw new Error(error.message || 'Mesaj gönderilemedi');
      }
      return res.json();
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: supportKeys.ticket(ticketId) });
    },
  });
}

// === Admin Hooks ===

/**
 * Fetch all tickets (admin) with filters
 */
export function useAdminTickets(filters: TicketFilters = {}) {
  const { page = 0, size = 20, status, priority, category } = filters;

  return useQuery<PagedTickets>({
    queryKey: [...supportKeys.adminTickets(), filters],
    queryFn: async () => {
      const params = new URLSearchParams();
      params.set('page', String(page));
      params.set('size', String(size));
      if (status) params.set('status', status);
      if (priority) params.set('priority', priority);
      if (category) params.set('category', category);

      const res = await fetch(`/api/admin/support/tickets?${params}`);
      if (!res.ok) throw new Error('Destek talepleri yüklenemedi');
      return res.json();
    },
  });
}

/**
 * Fetch active tickets (admin)
 */
export function useAdminActiveTickets(page = 0, size = 20) {
  return useQuery<PagedTickets>({
    queryKey: [...supportKeys.adminActive(), { page, size }],
    queryFn: async () => {
      const res = await fetch(`/api/admin/support/tickets/active?page=${page}&size=${size}`);
      if (!res.ok) throw new Error('Aktif talepler yüklenemedi');
      return res.json();
    },
  });
}

/**
 * Fetch ticket statistics (admin)
 */
export function useAdminTicketStats() {
  return useQuery<TicketStats>({
    queryKey: supportKeys.adminStats(),
    queryFn: async () => {
      const res = await fetch('/api/admin/support/tickets/stats');
      if (!res.ok) throw new Error('İstatistikler yüklenemedi');
      return res.json();
    },
  });
}

/**
 * Search tickets (admin)
 */
export function useAdminSearchTickets(query: string, page = 0, size = 20) {
  return useQuery<PagedTickets>({
    queryKey: [...supportKeys.adminTickets(), 'search', query, { page, size }],
    queryFn: async () => {
      const res = await fetch(`/api/admin/support/tickets/search?q=${encodeURIComponent(query)}&page=${page}&size=${size}`);
      if (!res.ok) throw new Error('Arama başarısız');
      return res.json();
    },
    enabled: query.length > 0,
  });
}

/**
 * Fetch single ticket detail (admin)
 */
export function useAdminTicket(id: number) {
  return useQuery<Ticket>({
    queryKey: supportKeys.adminTicket(id),
    queryFn: async () => {
      const res = await fetch(`/api/admin/support/tickets/${id}`);
      if (!res.ok) throw new Error('Destek talebi bulunamadı');
      return res.json();
    },
    enabled: !!id,
  });
}

/**
 * Admin reply to ticket mutation
 */
export function useAdminReply(ticketId: number) {
  const queryClient = useQueryClient();

  return useMutation<TicketMessage, Error, AddMessageRequest>({
    mutationFn: async (data) => {
      const res = await fetch(`/api/admin/support/tickets/${ticketId}/reply`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data),
      });
      if (!res.ok) {
        const error = await res.json().catch(() => ({}));
        throw new Error(error.message || 'Yanıt gönderilemedi');
      }
      return res.json();
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: supportKeys.adminTicket(ticketId) });
      queryClient.invalidateQueries({ queryKey: supportKeys.adminTickets() });
    },
  });
}

/**
 * Update ticket status mutation (admin)
 */
export function useUpdateTicketStatus(ticketId: number) {
  const queryClient = useQueryClient();

  return useMutation<Ticket, Error, UpdateTicketStatusRequest>({
    mutationFn: async (data) => {
      const res = await fetch(`/api/admin/support/tickets/${ticketId}/status`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data),
      });
      if (!res.ok) {
        const error = await res.json().catch(() => ({}));
        throw new Error(error.message || 'Durum güncellenemedi');
      }
      return res.json();
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: supportKeys.adminTicket(ticketId) });
      queryClient.invalidateQueries({ queryKey: supportKeys.adminTickets() });
      queryClient.invalidateQueries({ queryKey: supportKeys.adminStats() });
    },
  });
}

/**
 * Assign ticket mutation (admin)
 */
export function useAssignTicket(ticketId: number) {
  const queryClient = useQueryClient();

  return useMutation<Ticket, Error, AssignTicketRequest>({
    mutationFn: async (data) => {
      const res = await fetch(`/api/admin/support/tickets/${ticketId}/assign`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data),
      });
      if (!res.ok) {
        const error = await res.json().catch(() => ({}));
        throw new Error(error.message || 'Atama yapılamadı');
      }
      return res.json();
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: supportKeys.adminTicket(ticketId) });
      queryClient.invalidateQueries({ queryKey: supportKeys.adminTickets() });
    },
  });
}
