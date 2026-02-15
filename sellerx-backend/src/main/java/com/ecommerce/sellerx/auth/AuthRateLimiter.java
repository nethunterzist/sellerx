package com.ecommerce.sellerx.auth;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate limiter for authentication endpoints to prevent brute force attacks.
 * Limits attempts per IP address with automatic cleanup of expired entries.
 *
 * Rate limits:
 * - Login: 5 attempts per minute per IP
 * - Password reset: 3 attempts per hour per IP
 * - Email verification resend: 3 attempts per hour per IP
 * - Registration: 5 attempts per hour per IP
 * - Token refresh: 10 attempts per minute per IP
 */
@Component
public class AuthRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(AuthRateLimiter.class);

    // Login rate limiting: 5 attempts per minute
    private static final int LOGIN_MAX_ATTEMPTS = 5;
    private static final Duration LOGIN_WINDOW = Duration.ofMinutes(1);

    // Password reset rate limiting: 3 attempts per hour
    private static final int PASSWORD_RESET_MAX_ATTEMPTS = 3;
    private static final Duration PASSWORD_RESET_WINDOW = Duration.ofHours(1);

    // Email verification resend rate limiting: 3 attempts per hour
    private static final int EMAIL_RESEND_MAX_ATTEMPTS = 3;
    private static final Duration EMAIL_RESEND_WINDOW = Duration.ofHours(1);

    // Registration rate limiting: 5 attempts per hour
    private static final int REGISTRATION_MAX_ATTEMPTS = 5;
    private static final Duration REGISTRATION_WINDOW = Duration.ofHours(1);

    // Refresh token rate limiting: 10 attempts per minute
    private static final int REFRESH_MAX_ATTEMPTS = 10;
    private static final Duration REFRESH_WINDOW = Duration.ofMinutes(1);

    // Cache entries expire after 2 hours of inactivity to prevent memory leaks
    private static final Duration CACHE_EXPIRY = Duration.ofHours(2);

    private final Cache<String, AttemptTracker> loginAttempts;
    private final Cache<String, AttemptTracker> passwordResetAttempts;
    private final Cache<String, AttemptTracker> emailResendAttempts;
    private final Cache<String, AttemptTracker> registrationAttempts;
    private final Cache<String, AttemptTracker> refreshAttempts;

    public AuthRateLimiter() {
        this.loginAttempts = CacheBuilder.newBuilder()
                .expireAfterAccess(CACHE_EXPIRY.toMinutes(), TimeUnit.MINUTES)
                .maximumSize(10000)
                .build();

        this.passwordResetAttempts = CacheBuilder.newBuilder()
                .expireAfterAccess(CACHE_EXPIRY.toMinutes(), TimeUnit.MINUTES)
                .maximumSize(10000)
                .build();

        this.emailResendAttempts = CacheBuilder.newBuilder()
                .expireAfterAccess(CACHE_EXPIRY.toMinutes(), TimeUnit.MINUTES)
                .maximumSize(10000)
                .build();

        this.registrationAttempts = CacheBuilder.newBuilder()
                .expireAfterAccess(CACHE_EXPIRY.toMinutes(), TimeUnit.MINUTES)
                .maximumSize(10000)
                .build();

        this.refreshAttempts = CacheBuilder.newBuilder()
                .expireAfterAccess(CACHE_EXPIRY.toMinutes(), TimeUnit.MINUTES)
                .maximumSize(10000)
                .build();
    }

    /**
     * Check if login attempt is allowed for the given IP.
     * @param ipAddress the client IP address
     * @return true if allowed, false if rate limited
     */
    public boolean isLoginAllowed(String ipAddress) {
        return isAllowed(loginAttempts, ipAddress, LOGIN_MAX_ATTEMPTS, LOGIN_WINDOW);
    }

    /**
     * Record a login attempt for the given IP.
     * @param ipAddress the client IP address
     */
    public void recordLoginAttempt(String ipAddress) {
        recordAttempt(loginAttempts, ipAddress, LOGIN_WINDOW);
        log.debug("Login attempt recorded for IP: {}", maskIp(ipAddress));
    }

    /**
     * Check if password reset request is allowed for the given IP.
     * @param ipAddress the client IP address
     * @return true if allowed, false if rate limited
     */
    public boolean isPasswordResetAllowed(String ipAddress) {
        return isAllowed(passwordResetAttempts, ipAddress, PASSWORD_RESET_MAX_ATTEMPTS, PASSWORD_RESET_WINDOW);
    }

    /**
     * Record a password reset attempt for the given IP.
     * @param ipAddress the client IP address
     */
    public void recordPasswordResetAttempt(String ipAddress) {
        recordAttempt(passwordResetAttempts, ipAddress, PASSWORD_RESET_WINDOW);
        log.debug("Password reset attempt recorded for IP: {}", maskIp(ipAddress));
    }

    /**
     * Check if email resend request is allowed for the given IP.
     * @param ipAddress the client IP address
     * @return true if allowed, false if rate limited
     */
    public boolean isEmailResendAllowed(String ipAddress) {
        return isAllowed(emailResendAttempts, ipAddress, EMAIL_RESEND_MAX_ATTEMPTS, EMAIL_RESEND_WINDOW);
    }

    /**
     * Record an email resend attempt for the given IP.
     * @param ipAddress the client IP address
     */
    public void recordEmailResendAttempt(String ipAddress) {
        recordAttempt(emailResendAttempts, ipAddress, EMAIL_RESEND_WINDOW);
        log.debug("Email resend attempt recorded for IP: {}", maskIp(ipAddress));
    }

    /**
     * Check if registration attempt is allowed for the given IP.
     * @param ipAddress the client IP address
     * @return true if allowed, false if rate limited
     */
    public boolean isRegistrationAllowed(String ipAddress) {
        return isAllowed(registrationAttempts, ipAddress, REGISTRATION_MAX_ATTEMPTS, REGISTRATION_WINDOW);
    }

    /**
     * Record a registration attempt for the given IP.
     * @param ipAddress the client IP address
     */
    public void recordRegistrationAttempt(String ipAddress) {
        recordAttempt(registrationAttempts, ipAddress, REGISTRATION_WINDOW);
        log.debug("Registration attempt recorded for IP: {}", maskIp(ipAddress));
    }

    /**
     * Get time until registration rate limit resets (in seconds).
     * @param ipAddress the client IP address
     * @return seconds until reset, or 0 if not rate limited
     */
    public long getRegistrationResetTimeSeconds(String ipAddress) {
        return getResetTimeSeconds(registrationAttempts, ipAddress, REGISTRATION_WINDOW);
    }

    /**
     * Check if token refresh is allowed for the given IP.
     * @param ipAddress the client IP address
     * @return true if allowed, false if rate limited
     */
    public boolean isRefreshAllowed(String ipAddress) {
        return isAllowed(refreshAttempts, ipAddress, REFRESH_MAX_ATTEMPTS, REFRESH_WINDOW);
    }

    /**
     * Record a token refresh attempt for the given IP.
     * @param ipAddress the client IP address
     */
    public void recordRefreshAttempt(String ipAddress) {
        recordAttempt(refreshAttempts, ipAddress, REFRESH_WINDOW);
        log.debug("Refresh attempt recorded for IP: {}", maskIp(ipAddress));
    }

    /**
     * Get remaining attempts for login.
     * @param ipAddress the client IP address
     * @return number of remaining attempts
     */
    public int getRemainingLoginAttempts(String ipAddress) {
        return getRemainingAttempts(loginAttempts, ipAddress, LOGIN_MAX_ATTEMPTS, LOGIN_WINDOW);
    }

    /**
     * Get time until rate limit resets for login (in seconds).
     * @param ipAddress the client IP address
     * @return seconds until reset, or 0 if not rate limited
     */
    public long getLoginResetTimeSeconds(String ipAddress) {
        return getResetTimeSeconds(loginAttempts, ipAddress, LOGIN_WINDOW);
    }

    private boolean isAllowed(Cache<String, AttemptTracker> cache, String ipAddress, int maxAttempts, Duration window) {
        AttemptTracker tracker = cache.getIfPresent(ipAddress);
        if (tracker == null) {
            return true;
        }

        // Clean up old attempts
        tracker.cleanupOldAttempts(window);

        return tracker.getAttemptCount() < maxAttempts;
    }

    private void recordAttempt(Cache<String, AttemptTracker> cache, String ipAddress, Duration window) {
        AttemptTracker tracker = cache.getIfPresent(ipAddress);
        if (tracker == null) {
            tracker = new AttemptTracker();
            cache.put(ipAddress, tracker);
        }
        tracker.cleanupOldAttempts(window);
        tracker.recordAttempt();
    }

    private int getRemainingAttempts(Cache<String, AttemptTracker> cache, String ipAddress, int maxAttempts, Duration window) {
        AttemptTracker tracker = cache.getIfPresent(ipAddress);
        if (tracker == null) {
            return maxAttempts;
        }
        tracker.cleanupOldAttempts(window);
        return Math.max(0, maxAttempts - tracker.getAttemptCount());
    }

    private long getResetTimeSeconds(Cache<String, AttemptTracker> cache, String ipAddress, Duration window) {
        AttemptTracker tracker = cache.getIfPresent(ipAddress);
        if (tracker == null) {
            return 0;
        }
        Long oldestAttempt = tracker.getOldestAttemptTime();
        if (oldestAttempt == null) {
            return 0;
        }
        long resetTime = oldestAttempt + window.toMillis();
        long remaining = resetTime - System.currentTimeMillis();
        return Math.max(0, remaining / 1000);
    }

    /**
     * Mask IP address for logging (privacy).
     */
    private String maskIp(String ipAddress) {
        if (ipAddress == null) {
            return "unknown";
        }
        if (ipAddress.contains(".")) {
            // IPv4: mask last octet
            int lastDot = ipAddress.lastIndexOf('.');
            return ipAddress.substring(0, lastDot) + ".***";
        }
        // IPv6: mask last segment
        int lastColon = ipAddress.lastIndexOf(':');
        return lastColon > 0 ? ipAddress.substring(0, lastColon) + ":****" : ipAddress;
    }

    /**
     * Tracks attempts for a single IP address with timestamps.
     */
    private static class AttemptTracker {
        private final ConcurrentHashMap<Long, Boolean> attempts = new ConcurrentHashMap<>();
        private final AtomicInteger count = new AtomicInteger(0);

        void recordAttempt() {
            long now = System.currentTimeMillis();
            attempts.put(now, Boolean.TRUE);
            count.incrementAndGet();
        }

        void cleanupOldAttempts(Duration window) {
            long cutoff = System.currentTimeMillis() - window.toMillis();
            attempts.entrySet().removeIf(entry -> {
                if (entry.getKey() < cutoff) {
                    count.decrementAndGet();
                    return true;
                }
                return false;
            });
        }

        int getAttemptCount() {
            return count.get();
        }

        Long getOldestAttemptTime() {
            return attempts.keySet().stream().min(Long::compare).orElse(null);
        }
    }
}
