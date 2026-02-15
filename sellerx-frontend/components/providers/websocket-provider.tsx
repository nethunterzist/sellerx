"use client";

/**
 * WebSocket Provider - Manages global WebSocket connection state.
 * Provides real-time alert notifications and sync progress updates.
 *
 * Faz 5: Replaces polling with WebSocket for alerts.
 */

import {
  createContext,
  useContext,
  useEffect,
  useState,
  useCallback,
  useRef,
  type ReactNode,
} from "react";
import { Client, IMessage, StompSubscription } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { useQueryClient } from "@tanstack/react-query";
import { alertKeys } from "@/hooks/queries/use-alerts";
import { toast } from "sonner";

// WebSocket server URL - goes through Next.js API proxy
const getWsUrl = () => {
  if (typeof window === "undefined") return "";
  const protocol = window.location.protocol === "https:" ? "https:" : "http:";
  return `${protocol}//${window.location.host}/api/ws`;
};

export type WebSocketStatus =
  | "connecting"
  | "connected"
  | "disconnected"
  | "error";

export interface AlertMessage {
  id: string;
  type: string;
  severity: "LOW" | "MEDIUM" | "HIGH" | "CRITICAL";
  title: string;
  message: string;
  storeId: string;
  storeName: string;
  productBarcode?: string;
  productName?: string;
  data?: Record<string, unknown>;
  createdAt: string;
}

export interface SyncProgressMessage {
  storeId: string;
  taskId: string;
  type: "PRODUCTS" | "ORDERS" | "FINANCIAL" | "ALL";
  status: "STARTED" | "IN_PROGRESS" | "COMPLETED" | "FAILED";
  progress: number;
  totalItems?: number;
  processedItems?: number;
  message?: string;
}

interface WebSocketContextValue {
  status: WebSocketStatus;
  isConnected: boolean;
  unreadCount: number;
  latestAlert: AlertMessage | null;
  syncProgress: SyncProgressMessage | null;
  reconnect: () => void;
}

const WebSocketContext = createContext<WebSocketContextValue | null>(null);

interface WebSocketProviderProps {
  children: ReactNode;
  /**
   * Enable automatic reconnection on disconnect
   * @default true
   */
  autoReconnect?: boolean;
  /**
   * Reconnect delay in milliseconds
   * @default 5000
   */
  reconnectDelay?: number;
  /**
   * Show toast notifications for new alerts
   * @default true
   */
  showToasts?: boolean;
}

export function WebSocketProvider({
  children,
  autoReconnect = true,
  reconnectDelay = 5000,
  showToasts = true,
}: WebSocketProviderProps) {
  const [status, setStatus] = useState<WebSocketStatus>("disconnected");
  const [unreadCount, setUnreadCount] = useState(0);
  const [latestAlert, setLatestAlert] = useState<AlertMessage | null>(null);
  const [syncProgress, setSyncProgress] = useState<SyncProgressMessage | null>(
    null
  );

  const clientRef = useRef<Client | null>(null);
  const subscriptionsRef = useRef<StompSubscription[]>([]);
  const reconnectTimeoutRef = useRef<NodeJS.Timeout | null>(null);
  const queryClient = useQueryClient();

  // Handle incoming alert messages
  const handleAlertMessage = useCallback(
    (message: IMessage) => {
      try {
        const alert: AlertMessage = JSON.parse(message.body);

        setLatestAlert(alert);
        setUnreadCount((prev) => prev + 1);

        // Invalidate alert queries to refresh UI
        queryClient.invalidateQueries({ queryKey: alertKeys.unread() });
        queryClient.invalidateQueries({ queryKey: alertKeys.unreadCount() });
        queryClient.invalidateQueries({ queryKey: alertKeys.history() });
        queryClient.invalidateQueries({ queryKey: alertKeys.stats() });

        // Show toast notification
        if (showToasts) {
          const toastFn =
            alert.severity === "CRITICAL" || alert.severity === "HIGH"
              ? toast.error
              : alert.severity === "MEDIUM"
                ? toast.warning
                : toast.info;

          toastFn(alert.title, {
            description: alert.message,
            duration: 5000,
          });
        }
      } catch (error) {
        console.error("[WebSocket] Failed to parse alert message:", error);
      }
    },
    [queryClient, showToasts]
  );

  // Handle sync progress messages
  const handleSyncProgressMessage = useCallback((message: IMessage) => {
    try {
      const progress: SyncProgressMessage = JSON.parse(message.body);
      setSyncProgress(progress);

      // Show completion toast
      if (progress.status === "COMPLETED") {
        toast.success(`${progress.type} senkronizasyonu tamamlandı`, {
          duration: 3000,
        });
      } else if (progress.status === "FAILED") {
        toast.error(`${progress.type} senkronizasyonu başarısız`, {
          duration: 5000,
        });
      }
    } catch (error) {
      console.error(
        "[WebSocket] Failed to parse sync progress message:",
        error
      );
    }
  }, []);

  // Connect to WebSocket
  const connect = useCallback(() => {
    if (clientRef.current?.active) {
      return;
    }

    // Clear any existing reconnect timeout
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current);
      reconnectTimeoutRef.current = null;
    }

    setStatus("connecting");

    const wsUrl = getWsUrl();
    if (!wsUrl) {
      return;
    }

    const client = new Client({
      // Use SockJS for cross-browser compatibility
      webSocketFactory: () => new SockJS(wsUrl),

      // Connection timeout
      connectionTimeout: 10000,

      // Reconnect settings
      reconnectDelay: autoReconnect ? reconnectDelay : 0,

      // Debug logging disabled in production
      debug: () => {},

      // Connection success handler
      onConnect: () => {
        setStatus("connected");

        // Subscribe to user-specific alert channel
        const alertSub = client.subscribe(
          "/user/queue/alerts",
          handleAlertMessage
        );
        subscriptionsRef.current.push(alertSub);

        // Subscribe to user-specific sync progress channel
        const syncSub = client.subscribe(
          "/user/queue/sync-progress",
          handleSyncProgressMessage
        );
        subscriptionsRef.current.push(syncSub);
      },

      // Error handler
      onStompError: (frame) => {
        console.error("[WebSocket] STOMP error:", frame.headers.message);
        setStatus("error");
      },

      // Disconnect handler
      onDisconnect: () => {
        setStatus("disconnected");
        subscriptionsRef.current = [];

        // Auto-reconnect if enabled
        if (autoReconnect) {
          reconnectTimeoutRef.current = setTimeout(() => {
            connect();
          }, reconnectDelay);
        }
      },

      // WebSocket close handler
      onWebSocketClose: () => {
        if (status !== "error") {
          setStatus("disconnected");
        }
      },

      // WebSocket error handler
      onWebSocketError: () => {
        setStatus("error");
      },
    });

    client.activate();
    clientRef.current = client;
  }, [
    autoReconnect,
    reconnectDelay,
    handleAlertMessage,
    handleSyncProgressMessage,
    status,
  ]);

  // Disconnect from WebSocket
  const disconnect = useCallback(() => {
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current);
      reconnectTimeoutRef.current = null;
    }

    subscriptionsRef.current.forEach((sub) => {
      try {
        sub.unsubscribe();
      } catch {
        // Ignore errors
      }
    });
    subscriptionsRef.current = [];

    if (clientRef.current) {
      clientRef.current.deactivate();
      clientRef.current = null;
    }

    setStatus("disconnected");
  }, []);

  // Reconnect manually
  const reconnect = useCallback(() => {
    disconnect();
    setTimeout(connect, 100);
  }, [disconnect, connect]);

  // Connect on mount, disconnect on unmount
  useEffect(() => {
    // Small delay to ensure auth cookies are available
    const timer = setTimeout(connect, 1000);

    return () => {
      clearTimeout(timer);
      disconnect();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Fetch initial unread count
  useEffect(() => {
    async function fetchInitialUnreadCount() {
      try {
        const response = await fetch("/api/alerts/unread-count");
        if (response.ok) {
          const data = await response.json();
          setUnreadCount(data.count || 0);
        }
      } catch {
        // Ignore errors - will be fetched via polling fallback
      }
    }

    fetchInitialUnreadCount();
  }, []);

  const value: WebSocketContextValue = {
    status,
    isConnected: status === "connected",
    unreadCount,
    latestAlert,
    syncProgress,
    reconnect,
  };

  return (
    <WebSocketContext.Provider value={value}>
      {children}
    </WebSocketContext.Provider>
  );
}

/**
 * Hook to access WebSocket state.
 * Returns null if WebSocket provider is not available (e.g., during SSR).
 */
export function useWebSocketContext(): WebSocketContextValue | null {
  return useContext(WebSocketContext);
}

/**
 * Hook to get WebSocket connection status.
 * Falls back to false if provider is not available.
 */
export function useWebSocketConnected(): boolean {
  const context = useContext(WebSocketContext);
  return context?.isConnected ?? false;
}

/**
 * Hook to get real-time unread alert count.
 * Falls back to 0 if provider is not available.
 */
export function useWebSocketUnreadCount(): number {
  const context = useContext(WebSocketContext);
  return context?.unreadCount ?? 0;
}
