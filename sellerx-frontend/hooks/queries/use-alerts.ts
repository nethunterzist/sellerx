import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import type {
  AlertRule,
  AlertHistory,
  AlertStats,
  AlertRuleCounts,
  CreateAlertRuleRequest,
  UpdateAlertRuleRequest,
  PaginatedAlerts,
  AlertType,
  AlertSeverity,
} from '@/types/alert';

// Query keys
export const alertKeys = {
  all: ['alerts'] as const,
  rules: () => [...alertKeys.all, 'rules'] as const,
  rule: (id: string) => [...alertKeys.rules(), id] as const,
  ruleCounts: () => [...alertKeys.rules(), 'counts'] as const,
  history: () => [...alertKeys.all, 'history'] as const,
  historyFiltered: (filters: { page?: number; type?: AlertType; severity?: AlertSeverity }) =>
    [...alertKeys.history(), filters] as const,
  unread: () => [...alertKeys.all, 'unread'] as const,
  unreadCount: () => [...alertKeys.all, 'unread-count'] as const,
  recent: () => [...alertKeys.all, 'recent'] as const,
  stats: () => [...alertKeys.all, 'stats'] as const,
};

// ============ Alert Rules Queries ============

/**
 * Get all alert rules for the current user
 */
export function useAlertRules() {
  return useQuery({
    queryKey: alertKeys.rules(),
    queryFn: async (): Promise<AlertRule[]> => {
      const response = await fetch('/api/alert-rules');
      if (!response.ok) {
        throw new Error('Failed to fetch alert rules');
      }
      return response.json();
    },
    retry: false, // Don't retry on 401 errors
    staleTime: 30000, // 30 seconds
  });
}

/**
 * Get a specific alert rule by ID
 */
export function useAlertRule(id: string) {
  return useQuery({
    queryKey: alertKeys.rule(id),
    queryFn: async (): Promise<AlertRule> => {
      const response = await fetch(`/api/alert-rules/${id}`);
      if (!response.ok) {
        throw new Error('Failed to fetch alert rule');
      }
      return response.json();
    },
    enabled: !!id,
  });
}

/**
 * Get rule counts (total, active, inactive)
 */
export function useAlertRuleCounts() {
  return useQuery({
    queryKey: alertKeys.ruleCounts(),
    queryFn: async (): Promise<AlertRuleCounts> => {
      const response = await fetch('/api/alert-rules/count');
      if (!response.ok) {
        throw new Error('Failed to fetch rule counts');
      }
      return response.json();
    },
    retry: false, // Don't retry on 401 errors
    staleTime: 30000, // 30 seconds
  });
}

// ============ Alert Rules Mutations ============

/**
 * Create a new alert rule
 */
export function useCreateAlertRule() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: CreateAlertRuleRequest): Promise<AlertRule> => {
      const response = await fetch('/api/alert-rules', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data),
      });
      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || 'Failed to create alert rule');
      }
      return response.json();
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: alertKeys.rules() });
      queryClient.invalidateQueries({ queryKey: alertKeys.ruleCounts() });
    },
  });
}

/**
 * Update an existing alert rule
 */
export function useUpdateAlertRule() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({
      id,
      data,
    }: {
      id: string;
      data: UpdateAlertRuleRequest;
    }): Promise<AlertRule> => {
      const response = await fetch(`/api/alert-rules/${id}`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data),
      });
      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || 'Failed to update alert rule');
      }
      return response.json();
    },
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: alertKeys.rules() });
      queryClient.invalidateQueries({ queryKey: alertKeys.rule(data.id) });
      queryClient.invalidateQueries({ queryKey: alertKeys.ruleCounts() });
    },
  });
}

/**
 * Toggle alert rule active status
 */
export function useToggleAlertRule() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id: string): Promise<AlertRule> => {
      const response = await fetch(`/api/alert-rules/${id}/toggle`, {
        method: 'PUT',
      });
      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || 'Failed to toggle alert rule');
      }
      return response.json();
    },
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: alertKeys.rules() });
      queryClient.invalidateQueries({ queryKey: alertKeys.rule(data.id) });
      queryClient.invalidateQueries({ queryKey: alertKeys.ruleCounts() });
    },
  });
}

/**
 * Delete an alert rule
 */
export function useDeleteAlertRule() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id: string): Promise<void> => {
      const response = await fetch(`/api/alert-rules/${id}`, {
        method: 'DELETE',
      });
      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || 'Failed to delete alert rule');
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: alertKeys.rules() });
      queryClient.invalidateQueries({ queryKey: alertKeys.ruleCounts() });
    },
  });
}

// ============ Alert History Queries ============

/**
 * Get paginated alert history
 */
export function useAlerts(options?: {
  page?: number;
  size?: number;
  type?: AlertType;
  severity?: AlertSeverity;
}) {
  const { page = 0, size = 20, type, severity } = options || {};

  return useQuery({
    queryKey: alertKeys.historyFiltered({ page, type, severity }),
    queryFn: async (): Promise<PaginatedAlerts> => {
      const params = new URLSearchParams({
        page: page.toString(),
        size: size.toString(),
      });
      if (type) params.append('type', type);
      if (severity) params.append('severity', severity);

      const response = await fetch(`/api/alerts?${params}`);
      if (!response.ok) {
        throw new Error('Failed to fetch alerts');
      }
      return response.json();
    },
  });
}

/**
 * Get unread alerts
 */
export function useUnreadAlerts() {
  return useQuery({
    queryKey: alertKeys.unread(),
    queryFn: async (): Promise<AlertHistory[]> => {
      const response = await fetch('/api/alerts/unread');
      if (!response.ok) {
        throw new Error('Failed to fetch unread alerts');
      }
      return response.json();
    },
    // Refetch every 30 seconds for real-time updates
    refetchInterval: 30000,
  });
}

/**
 * Get unread alert count
 */
export function useUnreadAlertCount() {
  return useQuery({
    queryKey: alertKeys.unreadCount(),
    queryFn: async (): Promise<{ count: number }> => {
      const response = await fetch('/api/alerts/unread-count');
      if (!response.ok) {
        throw new Error('Failed to fetch unread count');
      }
      return response.json();
    },
    // Refetch every 30 seconds for real-time badge updates
    refetchInterval: 30000,
  });
}

/**
 * Get recent alerts (last 24 hours)
 */
export function useRecentAlerts() {
  return useQuery({
    queryKey: alertKeys.recent(),
    queryFn: async (): Promise<AlertHistory[]> => {
      const response = await fetch('/api/alerts/recent');
      if (!response.ok) {
        throw new Error('Failed to fetch recent alerts');
      }
      return response.json();
    },
  });
}

/**
 * Get alert statistics
 */
export function useAlertStats() {
  return useQuery({
    queryKey: alertKeys.stats(),
    queryFn: async (): Promise<AlertStats> => {
      const response = await fetch('/api/alerts/stats');
      if (!response.ok) {
        throw new Error('Failed to fetch alert stats');
      }
      return response.json();
    },
  });
}

// ============ Alert History Mutations ============

/**
 * Mark an alert as read
 */
export function useMarkAlertAsRead() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (id: string): Promise<AlertHistory> => {
      const response = await fetch(`/api/alerts/${id}/read`, {
        method: 'PUT',
      });
      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || 'Failed to mark alert as read');
      }
      return response.json();
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: alertKeys.history() });
      queryClient.invalidateQueries({ queryKey: alertKeys.unread() });
      queryClient.invalidateQueries({ queryKey: alertKeys.unreadCount() });
      queryClient.invalidateQueries({ queryKey: alertKeys.stats() });
    },
  });
}

/**
 * Mark all alerts as read
 */
export function useMarkAllAlertsAsRead() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (): Promise<{ markedCount: number }> => {
      const response = await fetch('/api/alerts/read-all', {
        method: 'PUT',
      });
      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.error || 'Failed to mark all alerts as read');
      }
      return response.json();
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: alertKeys.history() });
      queryClient.invalidateQueries({ queryKey: alertKeys.unread() });
      queryClient.invalidateQueries({ queryKey: alertKeys.unreadCount() });
      queryClient.invalidateQueries({ queryKey: alertKeys.stats() });
    },
  });
}
