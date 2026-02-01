package com.ecommerce.sellerx.returns;

import com.ecommerce.sellerx.returns.dto.ClaimItem;
import com.ecommerce.sellerx.stores.Store;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "trendyol_claims")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendyolClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "claim_id", nullable = false)
    private String claimId;

    @Column(name = "order_number")
    private String orderNumber;

    @Column(name = "customer_first_name")
    private String customerFirstName;

    @Column(name = "customer_last_name")
    private String customerLastName;

    @Column(name = "claim_date")
    private LocalDateTime claimDate;

    @Column(name = "cargo_tracking_number")
    private String cargoTrackingNumber;

    @Column(name = "cargo_tracking_link")
    private String cargoTrackingLink;

    @Column(name = "cargo_provider_name")
    private String cargoProviderName;

    @Column(name = "status", nullable = false)
    private String status;

    @Type(JsonBinaryType.class)
    @Column(name = "items", columnDefinition = "jsonb")
    @Builder.Default
    private List<ClaimItem> items = new ArrayList<>();

    @Column(name = "last_modified_date")
    private LocalDateTime lastModifiedDate;

    @Column(name = "synced_at")
    @Builder.Default
    private LocalDateTime syncedAt = LocalDateTime.now();

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Helper to get customer full name
    public String getCustomerFullName() {
        String first = customerFirstName != null ? customerFirstName : "";
        String last = customerLastName != null ? customerLastName : "";
        return (first + " " + last).trim();
    }

    // Helper to get total item count
    public int getTotalItemCount() {
        if (items == null) return 0;
        return items.stream()
                .mapToInt(item -> item.getQuantity() != null ? item.getQuantity().intValue() : 1)
                .sum();
    }
}
