package com.ecommerce.sellerx.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthRateLimiterTest {

    private AuthRateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new AuthRateLimiter();
    }

    @Test
    void shouldAllowLoginWithinLimit() {
        String ip = "192.168.1.1";

        // First 5 attempts should be allowed
        for (int i = 0; i < 5; i++) {
            assertThat(rateLimiter.isLoginAllowed(ip)).isTrue();
            rateLimiter.recordLoginAttempt(ip);
        }
    }

    @Test
    void shouldBlockLoginAfterLimit() {
        String ip = "192.168.1.2";

        // Make 5 attempts
        for (int i = 0; i < 5; i++) {
            rateLimiter.recordLoginAttempt(ip);
        }

        // 6th attempt should be blocked
        assertThat(rateLimiter.isLoginAllowed(ip)).isFalse();
    }

    @Test
    void shouldTrackRemainingAttempts() {
        String ip = "192.168.1.3";

        assertThat(rateLimiter.getRemainingLoginAttempts(ip)).isEqualTo(5);

        rateLimiter.recordLoginAttempt(ip);
        assertThat(rateLimiter.getRemainingLoginAttempts(ip)).isEqualTo(4);

        rateLimiter.recordLoginAttempt(ip);
        assertThat(rateLimiter.getRemainingLoginAttempts(ip)).isEqualTo(3);
    }

    @Test
    void shouldIsolateAttemptsByIp() {
        String ip1 = "192.168.1.4";
        String ip2 = "192.168.1.5";

        // Make 5 attempts from ip1
        for (int i = 0; i < 5; i++) {
            rateLimiter.recordLoginAttempt(ip1);
        }

        // ip1 should be blocked, ip2 should be allowed
        assertThat(rateLimiter.isLoginAllowed(ip1)).isFalse();
        assertThat(rateLimiter.isLoginAllowed(ip2)).isTrue();
    }

    @Test
    void shouldAllowPasswordResetWithinLimit() {
        String ip = "192.168.1.6";

        // First 3 attempts should be allowed
        for (int i = 0; i < 3; i++) {
            assertThat(rateLimiter.isPasswordResetAllowed(ip)).isTrue();
            rateLimiter.recordPasswordResetAttempt(ip);
        }
    }

    @Test
    void shouldBlockPasswordResetAfterLimit() {
        String ip = "192.168.1.7";

        // Make 3 attempts
        for (int i = 0; i < 3; i++) {
            rateLimiter.recordPasswordResetAttempt(ip);
        }

        // 4th attempt should be blocked
        assertThat(rateLimiter.isPasswordResetAllowed(ip)).isFalse();
    }

    @Test
    void shouldAllowEmailResendWithinLimit() {
        String ip = "192.168.1.8";

        // First 3 attempts should be allowed
        for (int i = 0; i < 3; i++) {
            assertThat(rateLimiter.isEmailResendAllowed(ip)).isTrue();
            rateLimiter.recordEmailResendAttempt(ip);
        }
    }

    @Test
    void shouldBlockEmailResendAfterLimit() {
        String ip = "192.168.1.9";

        // Make 3 attempts
        for (int i = 0; i < 3; i++) {
            rateLimiter.recordEmailResendAttempt(ip);
        }

        // 4th attempt should be blocked
        assertThat(rateLimiter.isEmailResendAllowed(ip)).isFalse();
    }

    @Test
    void shouldReturnResetTimeWhenRateLimited() {
        String ip = "192.168.1.10";

        // Make 5 login attempts to trigger rate limit
        for (int i = 0; i < 5; i++) {
            rateLimiter.recordLoginAttempt(ip);
        }

        // Should have positive reset time
        long resetTime = rateLimiter.getLoginResetTimeSeconds(ip);
        assertThat(resetTime).isGreaterThan(0);
        assertThat(resetTime).isLessThanOrEqualTo(60); // Within 1 minute window
    }

    @Test
    void shouldAllowRegistrationWithinLimit() {
        String ip = "192.168.1.20";
        for (int i = 0; i < 5; i++) {
            assertThat(rateLimiter.isRegistrationAllowed(ip)).isTrue();
            rateLimiter.recordRegistrationAttempt(ip);
        }
    }

    @Test
    void shouldBlockRegistrationAfterLimit() {
        String ip = "192.168.1.21";
        for (int i = 0; i < 5; i++) {
            rateLimiter.recordRegistrationAttempt(ip);
        }
        assertThat(rateLimiter.isRegistrationAllowed(ip)).isFalse();
    }

    @Test
    void shouldReturnRegistrationResetTime() {
        String ip = "192.168.1.22";
        for (int i = 0; i < 5; i++) {
            rateLimiter.recordRegistrationAttempt(ip);
        }
        long resetTime = rateLimiter.getRegistrationResetTimeSeconds(ip);
        assertThat(resetTime).isGreaterThan(0);
        assertThat(resetTime).isLessThanOrEqualTo(3600); // Within 1 hour window
    }

    @Test
    void shouldAllowRefreshWithinLimit() {
        String ip = "192.168.1.30";
        for (int i = 0; i < 10; i++) {
            assertThat(rateLimiter.isRefreshAllowed(ip)).isTrue();
            rateLimiter.recordRefreshAttempt(ip);
        }
    }

    @Test
    void shouldBlockRefreshAfterLimit() {
        String ip = "192.168.1.31";
        for (int i = 0; i < 10; i++) {
            rateLimiter.recordRefreshAttempt(ip);
        }
        assertThat(rateLimiter.isRefreshAllowed(ip)).isFalse();
    }
}
