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
 * User billing address for e-invoice generation
 */
@Entity
@Table(name = "billing_addresses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Paraşüt contact ID for sync
     */
    @Column(name = "parasut_contact_id", length = 100)
    private String parasutContactId;

    @Enumerated(EnumType.STRING)
    @Column(name = "address_type", nullable = false, length = 20)
    private BillingAddressType addressType;

    // Contact info
    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    /**
     * Company title (for corporate addresses)
     */
    @Column(name = "company_title", length = 200)
    private String companyTitle;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(length = 255)
    private String email;

    // Tax info
    @Column(name = "tax_office", length = 100)
    private String taxOffice;

    /**
     * TC Kimlik (11 digits) for INDIVIDUAL
     * Vergi No (10 digits) for CORPORATE
     */
    @Column(name = "tax_number", length = 20)
    private String taxNumber;

    // Address
    @Column(name = "address_line1", nullable = false, length = 500)
    private String addressLine1;

    @Column(name = "address_line2", length = 500)
    private String addressLine2;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 100)
    private String district;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String country = "TR";

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

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
     * Check if this is a corporate address
     */
    public boolean isCorporate() {
        return addressType == BillingAddressType.CORPORATE;
    }

    /**
     * Get full address as single string
     */
    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        sb.append(addressLine1);
        if (addressLine2 != null && !addressLine2.isBlank()) {
            sb.append(", ").append(addressLine2);
        }
        sb.append(", ").append(district);
        sb.append(", ").append(city);
        if (postalCode != null && !postalCode.isBlank()) {
            sb.append(" ").append(postalCode);
        }
        return sb.toString();
    }

    /**
     * Get display name (full name or company title)
     */
    public String getDisplayName() {
        if (isCorporate() && companyTitle != null && !companyTitle.isBlank()) {
            return companyTitle;
        }
        return fullName;
    }
}
