package com.ecommerce.sellerx.billing;

import com.ecommerce.sellerx.billing.service.PaymentService;
import com.ecommerce.sellerx.billing.service.SubscriptionService;
import com.ecommerce.sellerx.users.User;
import com.ecommerce.sellerx.users.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Checkout controller for payment processing.
 * Handles payment initiation and 3DS completion.
 */
@RestController
@RequestMapping("/api/billing/checkout")
@RequiredArgsConstructor
@Slf4j
public class CheckoutController {

    private final PaymentService paymentService;
    private final SubscriptionService subscriptionService;
    private final UserService userService;
    private final InvoiceRepository invoiceRepository;
    private final PaymentMethodRepository paymentMethodRepository;

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
     * Start checkout process
     */
    @PostMapping("/start")
    public ResponseEntity<CheckoutResponse> startCheckout(
            @RequestBody CheckoutRequest request,
            HttpServletRequest httpRequest) {

        Long userId = getCurrentUserId();

        log.info("Starting checkout for user: {}, invoice: {}", userId, request.invoiceId());

        // Get invoice
        Invoice invoice = invoiceRepository.findById(request.invoiceId())
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

        // Validate invoice belongs to user
        if (!invoice.getUser().getId().equals(userId)) {
            return ResponseEntity.status(403).build();
        }

        String buyerIp = getClientIp(httpRequest);

        PaymentService.PaymentResult result;

        if (request.paymentMethodId() != null) {
            // Use existing payment method
            PaymentMethod paymentMethod = paymentMethodRepository.findById(request.paymentMethodId())
                    .orElseThrow(() -> new IllegalArgumentException("Payment method not found"));

            if (!paymentMethod.getUser().getId().equals(userId)) {
                return ResponseEntity.status(403).build();
            }

            result = paymentService.processPayment(invoice, paymentMethod, buyerIp);
        } else if (request.cardDetails() != null) {
            // Use new card
            PaymentService.CardDetails cardDetails = new PaymentService.CardDetails(
                    request.cardDetails().cardHolderName(),
                    request.cardDetails().cardNumber(),
                    request.cardDetails().expireMonth(),
                    request.cardDetails().expireYear(),
                    request.cardDetails().cvc()
            );
            result = paymentService.processPaymentWithNewCard(
                    invoice, cardDetails, buyerIp, request.saveCard()
            );
        } else {
            return ResponseEntity.badRequest().body(
                    new CheckoutResponse(false, null, "INVALID_REQUEST", "Payment method or card details required", null)
            );
        }

        if (result.success()) {
            return ResponseEntity.ok(new CheckoutResponse(
                    true,
                    result.transactionId(),
                    null,
                    null,
                    null
            ));
        } else {
            return ResponseEntity.ok(new CheckoutResponse(
                    false,
                    result.transactionId(),
                    result.errorCode(),
                    result.errorMessage(),
                    null
            ));
        }
    }

    /**
     * Complete 3DS verification (callback from iyzico)
     */
    @PostMapping("/complete-3ds")
    public ResponseEntity<CheckoutResponse> complete3ds(
            @RequestBody ThreeDsCompleteRequest request) {

        log.warn("3DS completion attempted but not yet implemented: {}", request.conversationId());

        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(new CheckoutResponse(false, null, "NOT_IMPLEMENTED", "3DS verification is not yet available", null));
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Checkout request
     */
    public record CheckoutRequest(
            UUID invoiceId,
            UUID paymentMethodId,
            CardDetailsDto cardDetails,
            boolean saveCard
    ) {}

    /**
     * Card details for new card payment
     */
    public record CardDetailsDto(
            String cardHolderName,
            String cardNumber,
            String expireMonth,
            String expireYear,
            String cvc
    ) {}

    /**
     * 3DS completion request
     */
    public record ThreeDsCompleteRequest(
            String conversationId,
            String paymentId
    ) {}

    /**
     * Checkout response
     */
    public record CheckoutResponse(
            boolean success,
            UUID transactionId,
            String errorCode,
            String errorMessage,
            String threeDsHtmlContent
    ) {}
}
