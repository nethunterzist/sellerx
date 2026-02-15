'use client';

import { useEffect, useRef, useState, useCallback } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { alertKeys } from './queries/use-alerts';
import type { AlertHistory } from '@/types/alert';

interface WebSocketMessage {
  type: 'alert' | 'count' | 'sync_progress' | 'sync_complete' | 'system';
  payload: unknown;
}

interface SyncProgress {
  storeId: string;
  syncType: string;
  progress: number;
  status: string;
  timestamp: string;
}

interface UnreadCount {
  unreadCount: number;
  timestamp: string;
}

interface WebSocketHookOptions {
  /** Enable WebSocket connection (default: true) */
  enabled?: boolean;
  /** Callback when a new alert is received */
  onAlert?: (alert: AlertHistory) => void;
  /** Callback when unread count changes */
  onUnreadCountChange?: (count: number) => void;
  /** Callback when sync progress updates */
  onSyncProgress?: (progress: SyncProgress) => void;
  /** Callback when sync completes */
  onSyncComplete?: (data: SyncProgress) => void;
}

interface WebSocketState {
  connected: boolean;
  connecting: boolean;
  error: string | null;
}

/**
 * WebSocket hook for real-time alert notifications.
 * Replaces polling-based alert retrieval with push notifications.
 *
 * Usage:
 * ```tsx
 * const { connected, newAlerts } = useAlertWebSocket({
 *   onAlert: (alert) => toast.info(alert.title),
 *   onUnreadCountChange: (count) => setBadgeCount(count),
 * });
 * ```
 */
export function useAlertWebSocket(options: WebSocketHookOptions = {}) {
  const {
    enabled = true,
    onAlert,
    onUnreadCountChange,
    onSyncProgress,
    onSyncComplete,
  } = options;

  const queryClient = useQueryClient();
  const [state, setState] = useState<WebSocketState>({
    connected: false,
    connecting: false,
    error: null,
  });
  const [newAlerts, setNewAlerts] = useState<AlertHistory[]>([]);
  const [unreadCount, setUnreadCount] = useState<number | null>(null);

  const socketRef = useRef<WebSocket | null>(null);
  const stompClientRef = useRef<unknown>(null);
  const reconnectAttempts = useRef(0);
  const maxReconnectAttempts = 5;
  const reconnectDelay = useRef(1000);

  const connect = useCallback(async () => {
    if (!enabled || socketRef.current?.readyState === WebSocket.OPEN) {
      return;
    }

    setState(prev => ({ ...prev, connecting: true, error: null }));

    try {
      // Get the access token from cookies (handled by the backend)
      // For WebSocket, we'll pass token via query parameter or header
      const token = document.cookie
        .split('; ')
        .find(row => row.startsWith('access_token='))
        ?.split('=')[1];

      // Determine WebSocket URL
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
      const host = process.env.NEXT_PUBLIC_WS_URL || window.location.host;
      const wsUrl = `${protocol}//${host}/ws`;

      // Dynamic import SockJS and STOMP for client-side only
      const SockJS = (await import('sockjs-client')).default;
      const { Client } = await import('@stomp/stompjs');

      const client = new Client({
        webSocketFactory: () => new SockJS(wsUrl),
        connectHeaders: token ? { Authorization: `Bearer ${token}` } : {},
        debug: process.env.NODE_ENV === 'development' ? console.log : () => {},
        reconnectDelay: reconnectDelay.current,
        heartbeatIncoming: 10000,
        heartbeatOutgoing: 10000,
      });

      client.onConnect = () => {
        setState({ connected: true, connecting: false, error: null });
        reconnectAttempts.current = 0;
        reconnectDelay.current = 1000;

        // Subscribe to user-specific alert queue
        client.subscribe('/user/queue/alerts', (message) => {
          try {
            const alert = JSON.parse(message.body) as AlertHistory;
            setNewAlerts(prev => [alert, ...prev.slice(0, 49)]); // Keep last 50
            onAlert?.(alert);

            // Invalidate queries to refresh data
            queryClient.invalidateQueries({ queryKey: alertKeys.unread() });
            queryClient.invalidateQueries({ queryKey: alertKeys.history() });
          } catch (e) {
            console.error('[WS] Failed to parse alert message:', e);
          }
        });

        // Subscribe to unread count updates
        client.subscribe('/user/queue/alerts/count', (message) => {
          try {
            const data = JSON.parse(message.body) as UnreadCount;
            setUnreadCount(data.unreadCount);
            onUnreadCountChange?.(data.unreadCount);

            // Update React Query cache
            queryClient.setQueryData(alertKeys.unreadCount(), { count: data.unreadCount });
          } catch (e) {
            console.error('[WS] Failed to parse count message:', e);
          }
        });

        // Subscribe to sync progress updates
        client.subscribe('/user/queue/sync/progress', (message) => {
          try {
            const progress = JSON.parse(message.body) as SyncProgress;
            onSyncProgress?.(progress);
          } catch (e) {
            console.error('[WS] Failed to parse sync progress message:', e);
          }
        });

        // Subscribe to sync completion notifications
        client.subscribe('/user/queue/sync/complete', (message) => {
          try {
            const data = JSON.parse(message.body) as SyncProgress;
            onSyncComplete?.(data);
          } catch (e) {
            console.error('[WS] Failed to parse sync complete message:', e);
          }
        });

        // Subscribe to system-wide notifications
        client.subscribe('/topic/system', (message) => {
          try {
            const notification = JSON.parse(message.body);
            console.log('[WS] System notification:', notification);
          } catch (e) {
            console.error('[WS] Failed to parse system message:', e);
          }
        });
      };

      client.onStompError = (frame) => {
        console.error('[WS] STOMP error:', frame.headers['message']);
        setState(prev => ({
          ...prev,
          connected: false,
          error: frame.headers['message'] || 'Connection error',
        }));
      };

      client.onWebSocketClose = () => {
        setState(prev => ({ ...prev, connected: false }));

        // Attempt reconnection with exponential backoff
        if (reconnectAttempts.current < maxReconnectAttempts) {
          reconnectAttempts.current += 1;
          reconnectDelay.current = Math.min(reconnectDelay.current * 2, 30000);
          console.log(
            `[WS] Reconnecting in ${reconnectDelay.current}ms (attempt ${reconnectAttempts.current})`
          );
        }
      };

      client.activate();
      stompClientRef.current = client;
    } catch (error) {
      console.error('[WS] Connection failed:', error);
      setState({
        connected: false,
        connecting: false,
        error: error instanceof Error ? error.message : 'Connection failed',
      });
    }
  }, [enabled, onAlert, onUnreadCountChange, onSyncProgress, onSyncComplete, queryClient]);

  const disconnect = useCallback(() => {
    if (stompClientRef.current) {
      (stompClientRef.current as { deactivate: () => void }).deactivate();
      stompClientRef.current = null;
    }
    setState({ connected: false, connecting: false, error: null });
  }, []);

  // Connect on mount, disconnect on unmount
  useEffect(() => {
    if (enabled) {
      connect();
    }

    return () => {
      disconnect();
    };
  }, [enabled, connect, disconnect]);

  // Clear new alerts
  const clearNewAlerts = useCallback(() => {
    setNewAlerts([]);
  }, []);

  return {
    /** Whether WebSocket is connected */
    connected: state.connected,
    /** Whether WebSocket is connecting */
    connecting: state.connecting,
    /** Connection error message */
    error: state.error,
    /** New alerts received via WebSocket (most recent first) */
    newAlerts,
    /** Current unread count from WebSocket */
    unreadCount,
    /** Clear the new alerts array */
    clearNewAlerts,
    /** Manually disconnect */
    disconnect,
    /** Manually reconnect */
    reconnect: connect,
  };
}
