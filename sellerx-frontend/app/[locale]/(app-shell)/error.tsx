"use client";

import { useEffect } from "react";
import { logger } from "@/lib/logger";

export default function AppShellError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    logger.error("App shell error page triggered", {
      error,
      digest: error.digest,
    });
  }, [error]);

  return (
    <div className="flex min-h-[60vh] flex-col items-center justify-center gap-4 p-8">
      <div className="rounded-lg border border-red-200 bg-red-50 p-8 text-center dark:border-red-900 dark:bg-red-950">
        <h2 className="mb-2 text-lg font-semibold text-red-800 dark:text-red-200">
          Bir hata olustu
        </h2>
        <p className="mb-4 text-sm text-red-600 dark:text-red-400">
          Sayfa yuklenirken beklenmeyen bir hata meydana geldi.
        </p>
        {error.message && (
          <p className="mb-4 max-w-md truncate text-xs text-red-500 dark:text-red-500">
            {error.message}
          </p>
        )}
        <div className="flex items-center justify-center gap-3">
          <button
            onClick={reset}
            className="rounded-md bg-red-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-offset-2 dark:bg-red-700 dark:hover:bg-red-600"
          >
            Tekrar dene
          </button>
          <button
            onClick={() => window.location.reload()}
            className="rounded-md border border-red-300 bg-white px-4 py-2 text-sm font-medium text-red-700 transition-colors hover:bg-red-50 focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-offset-2 dark:border-red-700 dark:bg-red-900 dark:text-red-200 dark:hover:bg-red-800"
          >
            Sayfayi yenile
          </button>
        </div>
      </div>
    </div>
  );
}
