package com.ecommerce.sellerx.alerts;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for managing alert rules.
 */
@Slf4j
@RestController
@RequestMapping("/api/alert-rules")
@RequiredArgsConstructor
public class AlertRuleController {

    private final AlertRuleService alertRuleService;

    /**
     * Get all alert rules for the current user.
     */
    @GetMapping
    public ResponseEntity<List<AlertRuleDto>> getRules() {
        log.debug("GET /api/alert-rules");
        List<AlertRuleDto> rules = alertRuleService.getRulesForCurrentUser();
        return ResponseEntity.ok(rules);
    }

    /**
     * Get paginated alert rules for the current user.
     */
    @GetMapping("/paginated")
    public ResponseEntity<Page<AlertRuleDto>> getRulesPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.debug("GET /api/alert-rules/paginated?page={}&size={}", page, size);
        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size);
        Page<AlertRuleDto> rules = alertRuleService.getRulesForCurrentUser(pageable);
        return ResponseEntity.ok(rules);
    }

    /**
     * Get a specific alert rule by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<AlertRuleDto> getRule(@PathVariable UUID id) {
        log.debug("GET /api/alert-rules/{}", id);
        AlertRuleDto rule = alertRuleService.getRuleById(id);
        return ResponseEntity.ok(rule);
    }

    /**
     * Create a new alert rule.
     */
    @PostMapping
    public ResponseEntity<AlertRuleDto> createRule(@Valid @RequestBody CreateAlertRuleRequest request) {
        log.debug("POST /api/alert-rules - name: {}", request.getName());
        AlertRuleDto created = alertRuleService.createRule(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update an existing alert rule.
     */
    @PutMapping("/{id}")
    public ResponseEntity<AlertRuleDto> updateRule(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAlertRuleRequest request) {
        log.debug("PUT /api/alert-rules/{}", id);
        AlertRuleDto updated = alertRuleService.updateRule(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Toggle active status of an alert rule.
     */
    @PutMapping("/{id}/toggle")
    public ResponseEntity<AlertRuleDto> toggleRule(@PathVariable UUID id) {
        log.debug("PUT /api/alert-rules/{}/toggle", id);
        AlertRuleDto toggled = alertRuleService.toggleRuleActive(id);
        return ResponseEntity.ok(toggled);
    }

    /**
     * Delete an alert rule.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable UUID id) {
        log.debug("DELETE /api/alert-rules/{}", id);
        alertRuleService.deleteRule(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get rule counts for the current user.
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> getRuleCounts() {
        log.debug("GET /api/alert-rules/count");
        long total = alertRuleService.getRuleCount();
        long active = alertRuleService.getActiveRuleCount();
        return ResponseEntity.ok(Map.of(
                "total", total,
                "active", active,
                "inactive", total - active
        ));
    }
}
