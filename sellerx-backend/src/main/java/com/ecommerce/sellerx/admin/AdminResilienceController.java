package com.ecommerce.sellerx.admin;

import com.ecommerce.sellerx.config.ResilienceMetricsService;
import com.ecommerce.sellerx.config.ResilienceMetricsService.ResilienceStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin endpoints for monitoring and managing Resilience4j components.
 */
@RestController
@RequestMapping("/api/admin/resilience")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminResilienceController {

    private final ResilienceMetricsService resilienceMetricsService;

    /**
     * Get full resilience status.
     */
    @GetMapping("/status")
    public ResponseEntity<ResilienceStatus> getStatus() {
        return ResponseEntity.ok(resilienceMetricsService.getFullStatus());
    }

    /**
     * Get circuit breaker status.
     */
    @GetMapping("/circuit-breakers")
    public ResponseEntity<Map<String, ResilienceMetricsService.CircuitBreakerStatus>> getCircuitBreakers() {
        return ResponseEntity.ok(resilienceMetricsService.getCircuitBreakerStatus());
    }

    /**
     * Get rate limiter status.
     */
    @GetMapping("/rate-limiters")
    public ResponseEntity<Map<String, ResilienceMetricsService.RateLimiterStatus>> getRateLimiters() {
        return ResponseEntity.ok(resilienceMetricsService.getRateLimiterStatus());
    }

    /**
     * Get bulkhead status.
     */
    @GetMapping("/bulkheads")
    public ResponseEntity<Map<String, ResilienceMetricsService.BulkheadStatus>> getBulkheads() {
        return ResponseEntity.ok(resilienceMetricsService.getBulkheadStatus());
    }

    /**
     * Reset a circuit breaker.
     */
    @PostMapping("/circuit-breakers/{name}/reset")
    public ResponseEntity<String> resetCircuitBreaker(@PathVariable String name) {
        resilienceMetricsService.resetCircuitBreaker(name);
        return ResponseEntity.ok("Circuit breaker '" + name + "' has been reset");
    }

    /**
     * Force close a circuit breaker.
     */
    @PostMapping("/circuit-breakers/{name}/close")
    public ResponseEntity<String> closeCircuitBreaker(@PathVariable String name) {
        resilienceMetricsService.closeCircuitBreaker(name);
        return ResponseEntity.ok("Circuit breaker '" + name + "' transitioned to CLOSED");
    }
}
