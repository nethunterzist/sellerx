"use client";

import React from "react";
import { logger } from "@/lib/logger";

interface ErrorBoundaryProps {
  children: React.ReactNode;
  fallback?: React.ReactNode;
}

interface ErrorBoundaryState {
  hasError: boolean;
  error: Error | null;
}

export class ErrorBoundary extends React.Component<ErrorBoundaryProps, ErrorBoundaryState> {
  constructor(props: ErrorBoundaryProps) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { hasError: true, error };
  }

  componentDidCatch(error: Error, errorInfo: React.ErrorInfo): void {
    logger.error("Uncaught error in component tree", {
      error,
      componentStack: errorInfo.componentStack ?? "unknown",
    });
  }

  handleReset = (): void => {
    this.setState({ hasError: false, error: null });
    window.location.reload();
  };

  render(): React.ReactNode {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return this.props.fallback;
      }

      return (
        <div className="flex min-h-[400px] flex-col items-center justify-center gap-4 p-8">
          <div className="rounded-lg border border-red-200 bg-red-50 p-8 text-center dark:border-red-900 dark:bg-red-950">
            <h2 className="mb-2 text-lg font-semibold text-red-800 dark:text-red-200">
              Bir hata olustu
            </h2>
            <p className="mb-4 text-sm text-red-600 dark:text-red-400">
              Beklenmeyen bir hata meydana geldi. Lutfen sayfayi yenileyin.
            </p>
            {this.state.error && (
              <p className="mb-4 max-w-md truncate text-xs text-red-500 dark:text-red-500">
                {this.state.error.message}
              </p>
            )}
            <button
              onClick={this.handleReset}
              className="rounded-md bg-red-600 px-4 py-2 text-sm font-medium text-white transition-colors hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-red-500 focus:ring-offset-2 dark:bg-red-700 dark:hover:bg-red-600"
            >
              Sayfayi yenile
            </button>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
