package com.ecommerce.sellerx.expenses;

import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.products.TrendyolProduct;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "store_expenses")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class StoreExpense {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @EqualsAndHashCode.Include
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_category_id", nullable = false)
    private ExpenseCategory expenseCategory;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private TrendyolProduct product; // NULL = Genel kategori
    
    @Column(name = "date", nullable = false)
    private LocalDateTime date;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false, columnDefinition = "expense_frequency")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private ExpenseFrequency frequency;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    // VAT fields for KDV Mahsuplasmasi
    @Column(name = "vat_rate")
    private Integer vatRate; // 0, 1, 10, or 20 percent

    @Column(name = "vat_amount", precision = 10, scale = 2)
    private BigDecimal vatAmount;

    @Column(name = "is_vat_deductible")
    @Builder.Default
    private Boolean isVatDeductible = true;

    @Column(name = "net_amount", precision = 10, scale = 2)
    private BigDecimal netAmount; // amount - vatAmount

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    void prePersist() {
        if (date == null) date = LocalDateTime.now();
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Calculate VAT amount based on amount and vatRate
     * Amount is VAT-EXCLUSIVE (KDV hariç) - user enters net amount
     * VAT = Amount * vatRate / 100
     */
    public void calculateVatFields() {
        if (this.amount != null && this.vatRate != null && this.vatRate > 0) {
            // VAT Amount = Amount * vatRate / 100 for VAT-exclusive amounts
            this.vatAmount = this.amount.multiply(BigDecimal.valueOf(this.vatRate))
                    .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            // netAmount = amount (since amount is already VAT-exclusive)
            this.netAmount = this.amount;
        } else {
            this.vatAmount = BigDecimal.ZERO;
            this.netAmount = this.amount;
        }
    }
}
