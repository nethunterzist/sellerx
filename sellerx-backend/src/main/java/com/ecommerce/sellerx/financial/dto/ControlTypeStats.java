package com.ecommerce.sellerx.financial.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for Control Type Statistics summary
 * Used for displaying stats in the dashboard for each control type
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ControlTypeStats {

    // Control type identifier
    private String controlType;  // CARGO_OVERCHARGE, RETURN_REFUND, SALE_SHORTFALL

    // Counts
    private int totalChecked;    // Total items checked
    private int issuesFound;     // Number of issues detected
    private int normalCount;     // Items without issues

    // Amounts
    private BigDecimal totalIssueAmount;     // Sum of all issue amounts
    private BigDecimal averageIssueAmount;   // Average issue amount
    private BigDecimal maxIssueAmount;       // Largest single issue

    // Period
    private LocalDate periodStart;
    private LocalDate periodEnd;

    // Calculated percentages
    private BigDecimal issuePercentage;      // (issuesFound / totalChecked) * 100

    /**
     * Calculate and set the issue percentage
     */
    public void calculateIssuePercentage() {
        if (totalChecked > 0) {
            this.issuePercentage = BigDecimal.valueOf(issuesFound)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalChecked), 2, java.math.RoundingMode.HALF_UP);
        } else {
            this.issuePercentage = BigDecimal.ZERO;
        }
    }
}
