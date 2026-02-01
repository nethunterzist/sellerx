package com.ecommerce.sellerx.billing;

import com.ecommerce.sellerx.billing.dto.AddPaymentMethodRequest;
import com.ecommerce.sellerx.billing.dto.PaymentMethodDto;
import com.ecommerce.sellerx.billing.service.PaymentMethodService;
import com.ecommerce.sellerx.users.User;
import com.ecommerce.sellerx.users.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Payment method management controller.
 * Handles CRUD operations for user payment methods (credit cards).
 */
@RestController
@RequestMapping("/api/billing/payment-methods")
@RequiredArgsConstructor
@Slf4j
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;
    private final UserService userService;

    /**
     * Get current authenticated user ID from SecurityContext
     */
    private Long getCurrentUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("User not authenticated");
        }
        return Long.valueOf(authentication.getPrincipal().toString());
    }

    /**
     * Get all payment methods for current user
     */
    @GetMapping
    public ResponseEntity<List<PaymentMethodDto>> getPaymentMethods() {
        Long userId = getCurrentUserId();
        List<PaymentMethodDto> methods = paymentMethodService.getPaymentMethods(userId);
        return ResponseEntity.ok(methods);
    }

    /**
     * Add a new payment method
     */
    @PostMapping
    public ResponseEntity<PaymentMethodDto> addPaymentMethod(
            @RequestBody AddPaymentMethodRequest request) {

        Long userId = getCurrentUserId();
        log.info("Adding payment method for user: {}", userId);

        PaymentMethodDto result = paymentMethodService.addPaymentMethod(userId, request);
        return ResponseEntity.ok(result);
    }

    /**
     * Delete a payment method
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePaymentMethod(@PathVariable UUID id) {
        Long userId = getCurrentUserId();
        log.info("Deleting payment method: {} for user: {}", id, userId);

        paymentMethodService.deletePaymentMethod(userId, id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Set a payment method as default
     */
    @PostMapping("/{id}/default")
    public ResponseEntity<PaymentMethodDto> setAsDefault(@PathVariable UUID id) {
        Long userId = getCurrentUserId();
        log.info("Setting payment method {} as default for user: {}", id, userId);

        PaymentMethodDto result = paymentMethodService.setAsDefault(userId, id);
        return ResponseEntity.ok(result);
    }
}
