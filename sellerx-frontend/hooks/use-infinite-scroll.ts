"use client";

import { useEffect, useRef, useCallback } from "react";

interface UseInfiniteScrollOptions {
  hasNextPage: boolean | undefined;
  isFetchingNextPage: boolean;
  fetchNextPage: () => void;
  rootMargin?: string;
}

/**
 * Hook for infinite scroll using IntersectionObserver.
 * Returns a ref to attach to a sentinel element at the bottom of the list.
 * When the sentinel becomes visible, fetches the next page.
 */
export function useInfiniteScroll({
  hasNextPage,
  isFetchingNextPage,
  fetchNextPage,
  rootMargin = "100px",
}: UseInfiniteScrollOptions) {
  const sentinelRef = useRef<HTMLDivElement>(null);

  const handleObserver = useCallback(
    (entries: IntersectionObserverEntry[]) => {
      const target = entries[0];
      if (target.isIntersecting && hasNextPage && !isFetchingNextPage) {
        fetchNextPage();
      }
    },
    [hasNextPage, isFetchingNextPage, fetchNextPage]
  );

  useEffect(() => {
    const element = sentinelRef.current;
    if (!element) return undefined;

    const observer = new IntersectionObserver(handleObserver, {
      rootMargin,
    });

    observer.observe(element);
    return () => observer.disconnect();
  }, [handleObserver, rootMargin]);

  return sentinelRef;
}
