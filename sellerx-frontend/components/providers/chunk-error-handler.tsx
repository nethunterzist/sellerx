"use client";

import { useEffect } from "react";

const RELOAD_KEY = "chunk_error_reload";
const MAX_RELOADS = 3;
const RELOAD_WINDOW_MS = 10000; // 10 saniye içinde max 3 reload

/**
 * Global handler for chunk loading errors.
 * When Next.js rebuilds, old chunks become unavailable.
 * This component detects chunk loading failures and auto-reloads the page.
 */
export function ChunkErrorHandler() {
  useEffect(() => {
    const handleError = (event: ErrorEvent) => {
      const message = event.message || "";
      const isChunkError =
        message.includes("Loading chunk") ||
        message.includes("Failed to fetch dynamically imported module") ||
        message.includes("ChunkLoadError");

      if (isChunkError) {
        handleChunkError();
      }
    };

    const handleRejection = (event: PromiseRejectionEvent) => {
      const reason = event.reason?.message || String(event.reason) || "";
      const isChunkError =
        reason.includes("Loading chunk") ||
        reason.includes("Failed to fetch dynamically imported module") ||
        reason.includes("ChunkLoadError");

      if (isChunkError) {
        handleChunkError();
      }
    };

    const handleChunkError = () => {
      const reloadData = sessionStorage.getItem(RELOAD_KEY);
      const now = Date.now();

      if (reloadData) {
        const { count, timestamp } = JSON.parse(reloadData);
        // Sonsuz döngüyü önle: 10 saniye içinde max 3 reload
        if (now - timestamp < RELOAD_WINDOW_MS && count >= MAX_RELOADS) {
          console.error(
            "Chunk error: Max reload attempts reached. Please clear your browser cache and refresh manually."
          );
          return;
        }
        if (now - timestamp >= RELOAD_WINDOW_MS) {
          // Zaman penceresi geçti, sayacı sıfırla
          sessionStorage.setItem(
            RELOAD_KEY,
            JSON.stringify({ count: 1, timestamp: now })
          );
        } else {
          // Aynı pencere içinde, sayacı artır
          sessionStorage.setItem(
            RELOAD_KEY,
            JSON.stringify({ count: count + 1, timestamp })
          );
        }
      } else {
        sessionStorage.setItem(
          RELOAD_KEY,
          JSON.stringify({ count: 1, timestamp: now })
        );
      }

      // Sayfayı yenile - yeni chunk'lar yüklenecek
      window.location.reload();
    };

    window.addEventListener("error", handleError);
    window.addEventListener("unhandledrejection", handleRejection);

    return () => {
      window.removeEventListener("error", handleError);
      window.removeEventListener("unhandledrejection", handleRejection);
    };
  }, []);

  return null;
}
