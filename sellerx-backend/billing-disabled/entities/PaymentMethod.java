package com.ecommerce.sellerx.billing;

import com.ecommerce.sellerx.users.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;

/**
 * Stored payment method (iyzico card)
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

    // iyzico tokens
    @Column(name = "iyzico_card_user_key", length = 500)
    private String iyzicoCardUserKey;

    @Column(name = "iyzico_card_token", length = 500)
    private String iyzicoCardToken;

    // Card info (safe to store)
    @Column(name = "card_last_four", length = 4)
    private String cardLastFour;

    @Column(name = "card_brand", length = 20)
    private String cardBrand;

    @Column(name = "card_family", length = 50)
    private String cardFamily;

    @Column(name = "card_holder_name", length = 100)
    private String cardHolderName;

    @Column(name = "card_exp_month")
    private Integer cardExpMonth;

    @Column(name = "card_exp_year")
    private Integer cardExpYear;

    @Column(name = "card_bank_name", length = 100)
    private String cardBankName;

    // Status
    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
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
        if (cardExpMonth == null || cardExpYear == null) {
            return false;
        }
        YearMonth expiry = YearMonth.of(cardExpYear, cardExpMonth);
        return YearMonth.now().isAfter(expiry);
    }

    /**
     * Get masked card number for display
     */
    public String getMaskedCardNumber() {
        if (cardLastFour == null) {
            return "****";
        }
        return "**** **** **** " + cardLastFour;
    }

    /**
     * Get expiration display
     */
    public String getExpirationDisplay() {
        if (cardExpMonth == null || cardExpYear == null) {
            return "N/A";
        }
        return String.format("%02d/%d", cardExpMonth, cardExpYear % 100);
    }
}
