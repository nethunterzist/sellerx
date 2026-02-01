package com.ecommerce.sellerx.billing.service;

import com.ecommerce.sellerx.billing.*;
import com.ecommerce.sellerx.billing.dto.AddPaymentMethodRequest;
import com.ecommerce.sellerx.billing.dto.PaymentMethodDto;
import com.ecommerce.sellerx.billing.iyzico.*;
import com.ecommerce.sellerx.users.User;
import com.ecommerce.sellerx.users.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Payment method management service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final IyzicoApiClient iyzicoClient;
    private final UserService userService;

    /**
     * Get all payment methods for user
     */
    public List<PaymentMethodDto> getPaymentMethods(Long userId) {
        return paymentMethodRepository.findByUserIdOrderByDefaultAndCreated(userId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Get default payment method for user
     */
    public Optional<PaymentMethod> getDefaultPaymentMethod(Long userId) {
        return paymentMethodRepository.findByUserIdAndIsDefaultTrueAndIsActiveTrue(userId);
    }

    /**
     * Add a new payment method
     */
    @Transactional
    public PaymentMethodDto addPaymentMethod(Long userId, AddPaymentMethodRequest request) {
        log.info("Adding payment method for user: {}", userId);

        User user = userService.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Get existing card user key if exists
        String cardUserKey = paymentMethodRepository.findByUserIdAndIsActiveTrue(userId)
                .stream()
                .findFirst()
                .map(PaymentMethod::getIyzicoCardUserKey)
                .orElse(null);

        // Store card in iyzico
        IyzicoCardRequest cardRequest = IyzicoCardRequest.builder()
                .cardUserKey(cardUserKey)
                .email(user.getEmail())
                .externalId(user.getId().toString())
                .cardAlias(request.getCardAlias() != null ? request.getCardAlias() : "Kart")
                .cardHolderName(request.getCardHolderName())
                .cardNumber(request.getCardNumber())
                .expireMonth(request.getExpireMonth())
                .expireYear(request.getExpireYear())
                .build();

        IyzicoCardResult result = iyzicoClient.storeCard(cardRequest);

        if (!result.isSuccess()) {
            throw new IllegalStateException("Failed to store card: " + result.getErrorMessage());
        }

        // Check if this card is already stored
        if (paymentMethodRepository.existsByUserIdAndIyzicoCardTokenAndIsActiveTrue(userId, result.getCardToken())) {
            throw new IllegalStateException("This card is already saved");
        }

        // Determine if this should be default
        boolean shouldBeDefault = request.isSetAsDefault() ||
                paymentMethodRepository.countByUserIdAndIsActiveTrue(userId) == 0;

        // Clear existing default if setting new default
        if (shouldBeDefault) {
            paymentMethodRepository.clearDefaultForUser(userId);
        }

        // Create payment method
        PaymentMethod paymentMethod = PaymentMethod.builder()
                .user(user)
                .iyzicoCardUserKey(result.getCardUserKey())
                .iyzicoCardToken(result.getCardToken())
                .cardLastFour(result.getLastFourDigits())
                .cardBrand(result.getCardAssociation())
                .cardFamily(result.getCardFamily())
                .cardHolderName(request.getCardHolderName())
                .cardExpMonth(Integer.parseInt(request.getExpireMonth()))
                .cardExpYear(Integer.parseInt(request.getExpireYear()))
                .cardBankName(result.getCardBankName())
                .isDefault(shouldBeDefault)
                .isActive(true)
                .build();

        paymentMethod = paymentMethodRepository.save(paymentMethod);
        log.info("Payment method added: id={}", paymentMethod.getId());

        return toDto(paymentMethod);
    }

    /**
     * Delete a payment method
     */
    @Transactional
    public void deletePaymentMethod(Long userId, UUID paymentMethodId) {
        log.info("Deleting payment method: userId={}, paymentMethodId={}", userId, paymentMethodId);

        PaymentMethod paymentMethod = paymentMethodRepository.findByIdAndUserId(paymentMethodId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Payment method not found"));

        // Delete from iyzico
        boolean deleted = iyzicoClient.deleteCard(
                paymentMethod.getIyzicoCardUserKey(),
                paymentMethod.getIyzicoCardToken()
        );

        if (!deleted) {
            log.warn("Failed to delete card from iyzico, but continuing with local delete");
        }

        // Deactivate locally
        paymentMethodRepository.deactivate(paymentMethodId);

        // If this was default, set another as default
        if (paymentMethod.getIsDefault()) {
            paymentMethodRepository.findByUserIdOrderByDefaultAndCreated(userId)
                    .stream()
                    .filter(pm -> !pm.getId().equals(paymentMethodId) && pm.getIsActive())
                    .findFirst()
                    .ifPresent(pm -> {
                        pm.setIsDefault(true);
                        paymentMethodRepository.save(pm);
                    });
        }

        log.info("Payment method deleted: {}", paymentMethodId);
    }

    /**
     * Set a payment method as default
     */
    @Transactional
    public PaymentMethodDto setAsDefault(Long userId, UUID paymentMethodId) {
        log.info("Setting default payment method: userId={}, paymentMethodId={}", userId, paymentMethodId);

        PaymentMethod paymentMethod = paymentMethodRepository.findByIdAndUserId(paymentMethodId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Payment method not found"));

        if (!paymentMethod.getIsActive()) {
            throw new IllegalStateException("Cannot set inactive payment method as default");
        }

        paymentMethodRepository.clearDefaultForUser(userId);

        paymentMethod.setIsDefault(true);
        paymentMethod = paymentMethodRepository.save(paymentMethod);

        return toDto(paymentMethod);
    }

    private PaymentMethodDto toDto(PaymentMethod pm) {
        return PaymentMethodDto.builder()
                .id(pm.getId())
                .type(pm.getType().name())
                .cardLastFour(pm.getCardLastFour())
                .cardBrand(pm.getCardBrand())
                .cardFamily(pm.getCardFamily())
                .cardHolderName(pm.getCardHolderName())
                .expirationMonth(pm.getCardExpMonth())
                .expirationYear(pm.getCardExpYear())
                .cardBankName(pm.getCardBankName())
                .isDefault(pm.getIsDefault())
                .isExpired(pm.isExpired())
                .maskedNumber(pm.getMaskedCardNumber())
                .expirationDisplay(pm.getExpirationDisplay())
                .build();
    }
}
