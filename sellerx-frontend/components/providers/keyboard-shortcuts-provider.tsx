"use client";

import { useEffect, useCallback, useState, createContext, useContext, ReactNode } from "react";
import { useRouter, usePathname } from "next/navigation";

interface KeyboardShortcutsContextType {
  isDialogOpen: boolean;
  openDialog: () => void;
  closeDialog: () => void;
}

const KeyboardShortcutsContext = createContext<KeyboardShortcutsContextType | null>(null);

export function useKeyboardShortcuts() {
  const context = useContext(KeyboardShortcutsContext);
  if (!context) {
    throw new Error("useKeyboardShortcuts must be used within KeyboardShortcutsProvider");
  }
  return context;
}

interface KeyboardShortcutsProviderProps {
  children: ReactNode;
}

export function KeyboardShortcutsProvider({ children }: KeyboardShortcutsProviderProps) {
  const router = useRouter();
  const pathname = usePathname();
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [pendingKey, setPendingKey] = useState<string | null>(null);

  // Get locale from pathname
  const locale = pathname?.split("/")[1] || "tr";

  const handleKeyDown = useCallback(
    (event: KeyboardEvent) => {
      // Skip if user is typing in an input, textarea, or contenteditable
      const target = event.target as HTMLElement;
      if (
        target.tagName === "INPUT" ||
        target.tagName === "TEXTAREA" ||
        target.isContentEditable
      ) {
        return;
      }

      const key = event.key.toLowerCase();
      const isCtrlOrCmd = event.ctrlKey || event.metaKey;

      // Ctrl/Cmd + K: Quick search (placeholder - can be connected to search dialog)
      if (isCtrlOrCmd && key === "k") {
        event.preventDefault();
        // TODO: Open search dialog when implemented
        return;
      }

      // Ctrl/Cmd + /: Show shortcuts dialog
      if (isCtrlOrCmd && event.key === "/") {
        event.preventDefault();
        // Navigate to settings page with activity section
        router.push(`/${locale}/settings?section=activity`);
        return;
      }

      // Escape: Close dialogs/modals
      if (key === "escape") {
        setIsDialogOpen(false);
        setPendingKey(null);
        return;
      }

      // G + key navigation (two-key combo)
      if (key === "g" && !pendingKey && !isCtrlOrCmd) {
        setPendingKey("g");
        // Clear pending key after 1.5 seconds
        setTimeout(() => setPendingKey(null), 1500);
        return;
      }

      // If we have a pending "g" key, check for second key
      if (pendingKey === "g" && !isCtrlOrCmd) {
        setPendingKey(null);

        switch (key) {
          case "d":
            // G + D: Go to Dashboard
            event.preventDefault();
            router.push(`/${locale}/dashboard`);
            return;
          case "p":
            // G + P: Go to Products
            event.preventDefault();
            router.push(`/${locale}/products`);
            return;
          case "o":
            // G + O: Go to Orders
            event.preventDefault();
            router.push(`/${locale}/orders`);
            return;
          case "s":
            // G + S: Go to Settings
            event.preventDefault();
            router.push(`/${locale}/settings`);
            return;
        }
      }
    },
    [router, locale, pendingKey]
  );

  useEffect(() => {
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [handleKeyDown]);

  const value: KeyboardShortcutsContextType = {
    isDialogOpen,
    openDialog: () => setIsDialogOpen(true),
    closeDialog: () => setIsDialogOpen(false),
  };

  return (
    <KeyboardShortcutsContext.Provider value={value}>
      {children}
    </KeyboardShortcutsContext.Provider>
  );
}
