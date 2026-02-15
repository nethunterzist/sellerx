'use client';

// Simple toast hook - minimal implementation to fix build error
import { useState, useCallback } from 'react';

type ToastProps = {
  title?: string;
  description?: string;
  variant?: 'default' | 'destructive';
};

export function useToast() {
  const [, setToasts] = useState<ToastProps[]>([]);

  const toast = useCallback(({ title, description, variant = 'default' }: ToastProps) => {
    // For now, just console log - can be enhanced later with actual toast UI
    console.log(`[Toast ${variant}]`, title, description);
    setToasts((prev) => [...prev, { title, description, variant }]);
  }, []);

  return { toast };
}
