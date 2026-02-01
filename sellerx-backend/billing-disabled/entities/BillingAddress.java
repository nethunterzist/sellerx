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
 * Billing address for invoicing
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

    @Enumerated(EnumType.STRING)
    @Column(name = "address_type", nullable = false, length = 20)
    @Builder.Default
    private BillingAddressType addressType = BillingAddressType.INDIVIDUAL;

    // Personal info
    @Column(name = "first_name", length = 50)
    private String firstName;

    @Column(name = "last_name", length = 50)
    private String lastName;

    // Company info (for corporate)
    @Column(name = "company_name", length = 200)
    private String companyName;

    @Column(name = "tax_number", length = 20)
    private String taxNumber;

    @Column(name = "tax_office", length = 100)
    private String taxOffice;

    // Address
    @Column(name = "address_line1", nullable = false)
    private String addressLine1;

    @Column(name = "address_line2")
    private String addressLine2;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(length = 100)
    private String district;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(length = 2)
    @Builder.Default
    private String country = "TR";

    // Contact
    @Column(length = 20)
    private String phone;

    @Column(name = "is_default")
    @Builder.Default
    private Boolean isDefault = false;

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
     * Get display name for address
     */
    public String getDisplayName() {
        if (addressType == BillingAddressType.CORPORATE && companyName != null) {
            return companyName;
        }
        if (firstName != null && lastName != null) {
            return firstName + " " + lastName;
        }
        return "Address";
    }

    /**
     * Get formatted address
     */
    public String getFormattedAddress() {
        StringBuilder sb = new StringBuilder();
        sb.append(addressLine1);
        if (addressLine2 != null && !addressLine2.isBlank()) {
            sb.append(", ").append(addressLine2);
        }
        if (district != null) {
            sb.append(", ").append(district);
        }
        sb.append(", ").append(city);
        if (postalCode != null) {
            sb.append(" ").append(postalCode);
        }
        return sb.toString();
    }
}
