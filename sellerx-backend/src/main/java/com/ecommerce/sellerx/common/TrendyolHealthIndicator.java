package com.ecommerce.sellerx.common;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom health indicator for the Trendyol API integration.
 * Reports the health of the rate limiter which controls all Trendyol API calls.
 */
@Component("trendyol")
@RequiredArgsConstructor
public class TrendyolHealthIndicator implements HealthIndicator {

    private final TrendyolRateLimiter rateLimiter;

    @Override
    public Health health() {
        try {
            double currentRate = rateLimiter.getRate();
            boolean canAcquire = rateLimiter.tryAcquire();

            if (currentRate > 0 && canAcquire) {
                return Health.up()
                        .withDetail("rateLimit", currentRate + " req/sec")
                        .withDetail("status", "Rate limiter operational")
                        .build();
            } else if (currentRate > 0) {
                // Rate limiter exists but is currently saturated
                return Health.up()
                        .withDetail("rateLimit", currentRate + " req/sec")
                        .withDetail("status", "Rate limiter saturated - requests are being throttled")
                        .build();
            } else {
                return Health.down()
                        .withDetail("rateLimit", currentRate)
                        .withDetail("status", "Rate limiter not configured properly")
                        .build();
            }
        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}
