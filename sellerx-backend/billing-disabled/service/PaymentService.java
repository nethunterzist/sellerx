package com.ecommerce.sellerx.billing.service;

import com.ecommerce.sellerx.billing.*;
import com.ecommerce.sellerx.billing.config.SubscriptionConfig;
import com.ecommerce.sellerx.billing.iyzico.*;
import com.ecommerce.sellerx.users.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Payment processing service
 * Handles payments via iyzico payment gateway
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final IyzicoApiClient iyzicoClient;
    private final PaymentMethodRepository paymentMethodRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final InvoiceService invoiceService;
    private final SubscriptionService subscriptionService;
    private final SubscriptionConfig config;

    /**
     * Process payment for an invoice using stored card
     */
    @Transactional
    public PaymentResult processPayment(Invoice invoice, PaymentMethod paymentMethod, String buyerIp) {
        log.info("Processing payment: invoice={}, paymentMethod={}",
                invoice.getInvoiceNumber(), paymentMethod.getId());

        User user = invoice.getUser();
        Subscription subscription = invoice.getSubscription();
        String conversationId = UUID.randomUUID().toString();

        // Create payment transaction record
        PaymentTransaction transaction = PaymentTransaction.builder()
                .invoice(invoice)
                .paymentMethod(paymentMethod)
                .amount(invoice.getTotalAmount())
                .currency(invoice.getCurrency())
                .status(PaymentStatus.PROCESSING)
                .iyzicoConversationId(conversationId)
                .build();
        transaction = transactionRepository.save(transaction);

        // Build iyzico payment request
        IyzicoPaymentRequest request = IyzicoPaymentRequest.builder()
                .conversationId(conversationId)
                .price(invoice.getSubtotal())
                .paidPrice(invoice.getTotalAmount())
                .cardUserKey(paymentMethod.getIyzicoCardUserKey())
                .cardToken(paymentMethod.getIyzicoCardToken())
                .buyerId(user.getId().toString())
                .buyerName(user.getFirstName())
                .buyerSurname(user.getLastName())
                .buyerEmail(user.getEmail())
                .buyerIdentityNumber("11111111111") // TODO: Get from billing address
                .buyerAddress("Istanbul")
                .buyerCity("Istanbul")
                .buyerIp(buyerIp)
                .itemId(subscription.getPlan().getId().toString())
                .itemName(subscription.getPlan().getName() + " Abonelik")
                .build();

        // Process payment via iyzico
        IyzicoPaymentResult result = iyzicoClient.createPayment(request);

        // Update transaction with result
        if (result.isSuccess()) {
            transaction.setStatus(PaymentStatus.SUCCESS);
            transaction.setIyzicoPaymentId(result.getPaymentId());
            transaction.setProviderResponse(result.getRawResponse());
            transactionRepository.save(transaction);

            // Mark invoice as paid
            invoiceService.markAsPaid(invoice.getId());

            // Activate or renew subscription
            if (subscription.getStatus() == SubscriptionStatus.PENDING_PAYMENT ||
                    subscription.getStatus() == SubscriptionStatus.TRIAL) {
                subscriptionService.activateSubscription(subscription.getId());
            } else if (subscription.getStatus() == SubscriptionStatus.PAST_DUE) {
                subscriptionService.activateSubscription(subscription.getId());
            }

            log.info("Payment successful: invoice={}, paymentId={}",
                    invoice.getInvoiceNumber(), result.getPaymentId());

            return PaymentResult.success(transaction.getId(), result.getPaymentId());
        } else {
            transaction.setStatus(PaymentStatus.FAILED);
            transaction.setFailureCode(result.getErrorCode());
            transaction.setFailureMessage(result.getErrorMessage());
            transaction.setProviderResponse(result.getRawResponse());

            // Schedule retry if possible
            if (transaction.canRetry()) {
                transaction.scheduleRetry();
            }

            transactionRepository.save(transaction);

            // Mark invoice as failed
            invoiceService.markAsFailed(invoice.getId());

            // Mark subscription as past due
            if (subscription.getStatus() == SubscriptionStatus.ACTIVE ||
                    subscription.getStatus() == SubscriptionStatus.TRIAL) {
                subscriptionService.markPastDue(subscription.getId());
            }

            log.warn("Payment failed: invoice={}, error={}: {}",
                    invoice.getInvoiceNumber(), result.getErrorCode(), result.getErrorMessage());

            return PaymentResult.failure(transaction.getId(), result.getErrorCode(), result.getErrorMessage());
        }
    }

    /**
     * Process payment with new card (and optionally save it)
     */
    @Transactional
    public PaymentResult processPaymentWithNewCard(Invoice invoice, CardDetails cardDetails, String buyerIp, boolean saveCard) {
        log.info("Processing payment with new card: invoice={}, saveCard={}",
                invoice.getInvoiceNumber(), saveCard);

        User user = invoice.getUser();
        Subscription subscription = invoice.getSubscription();
        String conversationId = UUID.randomUUID().toString();

        // Create payment transaction record
        PaymentTransaction transaction = PaymentTransaction.builder()
                .invoice(invoice)
                .amount(invoice.getTotalAmount())
                .currency(invoice.getCurrency())
                .status(PaymentStatus.PROCESSING)
                .iyzicoConversationId(conversationId)
                .build();
        transaction = transactionRepository.save(transaction);

        // Build iyzico payment request
        IyzicoPaymentRequest request = IyzicoPaymentRequest.builder()
                .conversationId(conversationId)
                .price(invoice.getSubtotal())
                .paidPrice(invoice.getTotalAmount())
                .cardHolderName(cardDetails.getCardHolderName())
                .cardNumber(cardDetails.getCardNumber())
                .expireMonth(cardDetails.getExpireMonth())
                .expireYear(cardDetails.getExpireYear())
                .cvc(cardDetails.getCvc())
                .registerCard(saveCard)
                .buyerId(user.getId().toString())
                .buyerName(user.getFirstName())
                .buyerSurname(user.getLastName())
                .buyerEmail(user.getEmail())
                .buyerIdentityNumber("11111111111")
                .buyerAddress("Istanbul")
                .buyerCity("Istanbul")
                .buyerIp(buyerIp)
                .itemId(subscription.getPlan().getId().toString())
                .itemName(subscription.getPlan().getName() + " Abonelik")
                .build();

        // Process payment
        IyzicoPaymentResult result = iyzicoClient.createPayment(request);

        if (result.isSuccess()) {
            transaction.setStatus(PaymentStatus.SUCCESS);
            transaction.setIyzicoPaymentId(result.getPaymentId());
            transaction.setProviderResponse(result.getRawResponse());
            transactionRepository.save(transaction);

            invoiceService.markAsPaid(invoice.getId());

            // If card was saved, store it
            if (saveCard && result.getRawResponse() != null) {
                saveCardFromPaymentResult(user, result.getRawResponse(), cardDetails.getCardHolderName());
            }

            // Activate subscription
            if (subscription.getStatus() == SubscriptionStatus.PENDING_PAYMENT ||
                    subscription.getStatus() == SubscriptionStatus.TRIAL) {
                subscriptionService.activateSubscription(subscription.getId());
            }

            return PaymentResult.success(transaction.getId(), result.getPaymentId());
        } else {
            transaction.setStatus(PaymentStatus.FAILED);
            transaction.setFailureCode(result.getErrorCode());
            transaction.setFailureMessage(result.getErrorMessage());
            transactionRepository.save(transaction);

            invoiceService.markAsFailed(invoice.getId());

            return PaymentResult.failure(transaction.getId(), result.getErrorCode(), result.getErrorMessage());
        }
    }

    /**
     * Refund a payment
     */
    @Transactional
    public PaymentResult refundPayment(UUID transactionId, String ip) {
        PaymentTransaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        if (transaction.getStatus() != PaymentStatus.SUCCESS) {
            throw new IllegalStateException("Can only refund successful payments");
        }

        String paymentTransactionId = (String) transaction.getProviderResponse().get("paymentTransactionId");
        if (paymentTransactionId == null) {
            throw new IllegalStateException("Payment transaction ID not found");
        }

        IyzicoRefundResult result = iyzicoClient.refundPayment(paymentTransactionId, transaction.getAmount(), ip);

        if (result.isSuccess()) {
            transaction.setStatus(PaymentStatus.REFUNDED);
            transactionRepository.save(transaction);

            // Mark invoice as refunded
            Invoice invoice = transaction.getInvoice();
            invoice.setStatus(InvoiceStatus.REFUNDED);

            return PaymentResult.success(transaction.getId(), result.getPaymentId());
        } else {
            return PaymentResult.failure(transaction.getId(), result.getErrorCode(), result.getErrorMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void saveCardFromPaymentResult(User user, Map<String, Object> response, String cardHolderName) {
        try {
            Map<String, Object> cardInfo = (Map<String, Object>) response.get("cardAssociation");
            String cardUserKey = (String) response.get("cardUserKey");
            String cardToken = (String) response.get("cardToken");

            if (cardToken != null) {
                PaymentMethod pm = PaymentMethod.builder()
                        .user(user)
                        .iyzicoCardUserKey(cardUserKey)
                        .iyzicoCardToken(cardToken)
                        .cardLastFour((String) response.get("lastFourDigits"))
                        .cardBrand((String) response.get("cardAssociation"))
                        .cardFamily((String) response.get("cardFamily"))
                        .cardHolderName(cardHolderName)
                        .cardExpMonth(1) // Will be updated from card details
                        .cardExpYear(2030)
                        .cardBankName((String) response.get("cardBankName"))
                        .isDefault(paymentMethodRepository.countByUserIdAndIsActiveTrue(user.getId()) == 0)
                        .build();

                paymentMethodRepository.save(pm);
            }
        } catch (Exception e) {
            log.warn("Failed to save card from payment result", e);
        }
    }

    /**
     * Payment result DTO
     */
    public record PaymentResult(
            boolean success,
            UUID transactionId,
            String paymentId,
            String errorCode,
            String errorMessage
    ) {
        public static PaymentResult success(UUID transactionId, String paymentId) {
            return new PaymentResult(true, transactionId, paymentId, null, null);
        }

        public static PaymentResult failure(UUID transactionId, String errorCode, String errorMessage) {
            return new PaymentResult(false, transactionId, null, errorCode, errorMessage);
        }
    }

    /**
     * Card details for new card payment
     */
    public record CardDetails(
            String cardHolderName,
            String cardNumber,
            String expireMonth,
            String expireYear,
            String cvc
    ) {}
}
