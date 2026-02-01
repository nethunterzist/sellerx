package com.ecommerce.sellerx.billing;

import com.ecommerce.sellerx.users.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Stored payment method using iyzico card tokenization
 * Never store raw card data - only tokenized references
 */
@Entity
@Table(name = "payment_methods")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentMethodType type = PaymentMethodType.CREDIT_CARD;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String provider = "iyzico";

    /**
     * iyzico user key for card storage
     */
    @Column(name = "iyzico_card_user_key")
    private String iyzicoCardUserKey;

    /**
     * Tokenized card reference from iyzico
     * NEVER store actual card number
     */
    @Column(name = "iyzico_card_token", nullable = false)
    private String iyzicoCardToken;

    // Card display info (masked, for UI only)
    @Column(name = "card_last_four", nullable = false, length = 4)
    private String cardLastFour;

    @Column(name = "card_brand", length = 50)
    private String cardBrand;

    @Column(name = "card_family", length = 100)
    private String cardFamily;

    @Column(name = "card_holder_name")
    private String cardHolderName;

    @Column(name = "card_exp_month", nullable = false)
    private Integer cardExpMonth;

    @Column(name = "card_exp_year", nullable = false)
    private Integer cardExpYear;

    @Column(name = "card_bank_name", length = 100)
    private String cardBankName;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Check if card is expired
     */
    public boolean isExpired() {
        LocalDateTime now = LocalDateTime.now();
        int currentYear = now.getYear();
        int currentMonth = now.getMonthValue();

        // Card expires at the end of the expiration month
        if (cardExpYear < currentYear) {
            return true;
        }
        if (cardExpYear == currentYear && cardExpMonth < currentMonth) {
            return true;
        }
        return false;
    }

    /**
     * Get masked card number for display
     */
    public String getMaskedCardNumber() {
        return "**** **** **** " + cardLastFour;
    }

    /**
     * Get expiration date formatted as MM/YY
     */
    public String getExpirationDisplay() {
        return String.format("%02d/%02d", cardExpMonth, cardExpYear % 100);
    }
}
