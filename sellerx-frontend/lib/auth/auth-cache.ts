// Server-side auth cache for middleware coordination
interface AuthCacheEntry {
  user: any;
  timestamp: number;
  valid: boolean;
}

class AuthCache {
  private cache = new Map<string, AuthCacheEntry>();
  private readonly CACHE_DURATION = 60 * 60 * 1000; // 1 hour

  set(token: string, user: any, valid: boolean = true) {
    this.cache.set(token, {
      user,
      timestamp: Date.now(),
      valid,
    });
  }

  get(token: string): AuthCacheEntry | null {
    const entry = this.cache.get(token);
    if (!entry) return null;

    // Cache expired check
    if (Date.now() - entry.timestamp > this.CACHE_DURATION) {
      this.cache.delete(token);
      return null;
    }

    return entry;
  }

  isValid(token: string): boolean | null {
    const entry = this.get(token);
    return entry ? entry.valid : null;
  }

  invalidate(token: string) {
    this.cache.delete(token);
  }

  clear() {
    this.cache.clear();
  }
}

// Singleton instance
export const authCache = new AuthCache();
