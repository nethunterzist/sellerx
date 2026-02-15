package com.ecommerce.sellerx.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Health indicator for Resilience4j components.
 * Reports UP if no critical circuit breakers are open.
 */
@Component
@RequiredArgsConstructor
public class ResilienceHealthIndicator implements HealthIndicator {

    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @Override
    public Health health() {
        Map<String, Object> details = new HashMap<>();

        // Check all circuit breakers
        Map<String, String> circuitBreakerStates = new HashMap<>();
        boolean hasOpenCircuit = false;

        for (CircuitBreaker cb : circuitBreakerRegistry.getAllCircuitBreakers()) {
            CircuitBreaker.State state = cb.getState();
            circuitBreakerStates.put(cb.getName(), state.name());

            if (state == CircuitBreaker.State.OPEN) {
                hasOpenCircuit = true;
            }
        }

        details.put("circuitBreakers", circuitBreakerStates);

        // Count open circuits
        long openCount = circuitBreakerRegistry.getAllCircuitBreakers().stream()
            .filter(cb -> cb.getState() == CircuitBreaker.State.OPEN)
            .count();
        details.put("openCircuitCount", openCount);

        // Determine health status
        if (hasOpenCircuit) {
            details.put("warning", "One or more circuit breakers are OPEN");
            return Health.down()
                .withDetails(details)
                .build();
        }

        return Health.up()
            .withDetails(details)
            .build();
    }
}
