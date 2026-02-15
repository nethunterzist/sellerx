/**
 * WebSocket hook for real-time communication with Spring Boot backend.
 * Uses STOMP over SockJS for cross-browser compatibility.
 *
 * Faz 5: Replaces polling with WebSocket for alerts and sync notifications.
 */
import { useEffect, useRef, useState, useCallback } from "react";
import { Client, IMessage, StompSubscription } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import { useQueryClient } from "@tanstack/react-query";
import { alertKeys } from "./queries/use-alerts";

// WebSocket server URL - goes through Next.js API proxy
const WS_URL =
  typeof window !== "undefined"
    ? `${window.location.protocol === "https:" ? "wss:" : "ws:"}//${window.location.host}/api/ws`
    : "";

export type WebSocketStatus =
  | "connecting"
  | "connected"
  | "disconnected"
  | "error";

interface AlertMessage {
  id: string;
  type: string;
  severity: string;
  title: string;
  message: string;
  storeId: string;
  storeName: string;
  productBarcode?: string;
  productName?: string;
  data?: Record<string, unknown>;
  createdAt: string;
}

interface SyncProgressMessage {
  storeId: string;
  taskId: string;
  type: "PRODUCTS" | "ORDERS" | "FINANCIAL" | "ALL";
  status: "STARTED" | "IN_PROGRESS" | "COMPLETED" | "FAILED";
  progress: number;
  totalItems?: number;
  processedItems?: number;
  message?: string;
}

interface UseWebSocketOptions {
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
   * Called when a new alert is received
   */
  onAlert?: (alert: AlertMessage) => void;
  /**
   * Called when sync progress is updated
   */
  onSyncProgress?: (progress: SyncProgressMessage) => void;
  /**
   * Enable debug logging
   * @default false
   */
  debug?: boolean;
}

interface UseWebSocketReturn {
  /**
   * Current WebSocket connection status
   */
  status: WebSocketStatus;
  /**
   * Whether WebSocket is connected
   */
  isConnected: boolean;
  /**
   * Number of unread alerts (updated in real-time)
   */
  unreadCount: number;
  /**
   * Latest received alert
   */
  latestAlert: AlertMessage | null;
  /**
   * Latest sync progress
   */
  syncProgress: SyncProgressMessage | null;
  /**
   * Manually reconnect the WebSocket
   */
  reconnect: () => void;
  /**
   * Manually disconnect the WebSocket
   */
  disconnect: () => void;
}

/**
 * Hook for WebSocket connection to receive real-time updates.
 *
 * @example
 * ```tsx
 * const { status, unreadCount, latestAlert } = useWebSocket({
 *   onAlert: (alert) => toast.info(alert.message),
 * });
 * ```
 */
export function useWebSocket(
  options: UseWebSocketOptions = {}
): UseWebSocketReturn {
  const {
    autoReconnect = true,
    reconnectDelay = 5000,
    onAlert,
    onSyncProgress,
    debug = false,
  } = options;

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

  const log = useCallback(
    (message: string, ...args: unknown[]) => {
      if (debug) {
        console.log(`[WebSocket] ${message}`, ...args);
      }
    },
    [debug]
  );

  const handleAlertMessage = useCallback(
    (message: IMessage) => {
      try {
        const alert: AlertMessage = JSON.parse(message.body);
        log("Alert received:", alert);

        setLatestAlert(alert);
        setUnreadCount((prev) => prev + 1);

        // Invalidate alert queries to refresh UI
        queryClient.invalidateQueries({ queryKey: alertKeys.unread() });
        queryClient.invalidateQueries({ queryKey: alertKeys.unreadCount() });
        queryClient.invalidateQueries({ queryKey: alertKeys.history() });
        queryClient.invalidateQueries({ queryKey: alertKeys.stats() });

        onAlert?.(alert);
      } catch (error) {
        console.error("[WebSocket] Failed to parse alert message:", error);
      }
    },
    [log, onAlert, queryClient]
  );

  const handleSyncProgressMessage = useCallback(
    (message: IMessage) => {
      try {
        const progress: SyncProgressMessage = JSON.parse(message.body);
        log("Sync progress:", progress);

        setSyncProgress(progress);
        onSyncProgress?.(progress);
      } catch (error) {
        console.error(
          "[WebSocket] Failed to parse sync progress message:",
          error
        );
      }
    },
    [log, onSyncProgress]
  );

  const connect = useCallback(() => {
    if (clientRef.current?.active) {
      log("Already connected");
      return;
    }

    // Clear any existing reconnect timeout
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current);
      reconnectTimeoutRef.current = null;
    }

    setStatus("connecting");
    log("Connecting to WebSocket...");

    const client = new Client({
      // Use SockJS for cross-browser compatibility
      webSocketFactory: () => new SockJS(WS_URL),

      // Connection timeout
      connectionTimeout: 10000,

      // Reconnect settings (handled by STOMP client)
      reconnectDelay: autoReconnect ? reconnectDelay : 0,

      // Debug logging
      debug: debug
        ? (msg) => {
            console.log("[STOMP]", msg);
          }
        : () => {},

      // Connection success handler
      onConnect: () => {
        log("Connected successfully");
        setStatus("connected");

        // Subscribe to user-specific alert channel
        const alertSub = client.subscribe(
          "/user/queue/alerts",
          handleAlertMessage
        );
        subscriptionsRef.current.push(alertSub);
        log("Subscribed to /user/queue/alerts");

        // Subscribe to user-specific sync progress channel
        const syncSub = client.subscribe(
          "/user/queue/sync-progress",
          handleSyncProgressMessage
        );
        subscriptionsRef.current.push(syncSub);
        log("Subscribed to /user/queue/sync-progress");
      },

      // Error handler
      onStompError: (frame) => {
        console.error("[WebSocket] STOMP error:", frame.headers.message);
        setStatus("error");
      },

      // Disconnect handler
      onDisconnect: () => {
        log("Disconnected");
        setStatus("disconnected");

        // Clear subscriptions
        subscriptionsRef.current = [];

        // Auto-reconnect if enabled
        if (autoReconnect) {
          log(`Will reconnect in ${reconnectDelay}ms`);
          reconnectTimeoutRef.current = setTimeout(() => {
            connect();
          }, reconnectDelay);
        }
      },

      // WebSocket close handler
      onWebSocketClose: () => {
        log("WebSocket closed");
        if (status !== "error") {
          setStatus("disconnected");
        }
      },

      // WebSocket error handler
      onWebSocketError: (event) => {
        console.error("[WebSocket] WebSocket error:", event);
        setStatus("error");
      },
    });

    client.activate();
    clientRef.current = client;
  }, [
    autoReconnect,
    reconnectDelay,
    debug,
    log,
    handleAlertMessage,
    handleSyncProgressMessage,
    status,
  ]);

  const disconnect = useCallback(() => {
    log("Disconnecting...");

    // Clear reconnect timeout
    if (reconnectTimeoutRef.current) {
      clearTimeout(reconnectTimeoutRef.current);
      reconnectTimeoutRef.current = null;
    }

    // Unsubscribe from all channels
    subscriptionsRef.current.forEach((sub) => {
      try {
        sub.unsubscribe();
      } catch {
        // Ignore errors
      }
    });
    subscriptionsRef.current = [];

    // Deactivate client
    if (clientRef.current) {
      clientRef.current.deactivate();
      clientRef.current = null;
    }

    setStatus("disconnected");
  }, [log]);

  const reconnect = useCallback(() => {
    disconnect();
    // Small delay before reconnecting
    setTimeout(connect, 100);
  }, [disconnect, connect]);

  // Connect on mount, disconnect on unmount
  useEffect(() => {
    connect();

    return () => {
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
      } catch (error) {
        console.error("[WebSocket] Failed to fetch initial unread count:", error);
      }
    }

    fetchInitialUnreadCount();
  }, []);

  return {
    status,
    isConnected: status === "connected",
    unreadCount,
    latestAlert,
    syncProgress,
    reconnect,
    disconnect,
  };
}

/**
 * Hook specifically for alert notifications.
 * Provides a simpler interface when only alerts are needed.
 */
export function useAlertWebSocket(
  onAlert?: (alert: AlertMessage) => void
): Pick<
  UseWebSocketReturn,
  "status" | "isConnected" | "unreadCount" | "latestAlert"
> {
  const result = useWebSocket({
    onAlert,
    autoReconnect: true,
    reconnectDelay: 5000,
  });

  return {
    status: result.status,
    isConnected: result.isConnected,
    unreadCount: result.unreadCount,
    latestAlert: result.latestAlert,
  };
}

/**
 * Hook specifically for sync progress tracking.
 */
export function useSyncProgressWebSocket(
  onProgress?: (progress: SyncProgressMessage) => void
): Pick<UseWebSocketReturn, "status" | "isConnected" | "syncProgress"> {
  const result = useWebSocket({
    onSyncProgress: onProgress,
    autoReconnect: true,
    reconnectDelay: 5000,
  });

  return {
    status: result.status,
    isConnected: result.isConnected,
    syncProgress: result.syncProgress,
  };
}

export type { AlertMessage, SyncProgressMessage };
