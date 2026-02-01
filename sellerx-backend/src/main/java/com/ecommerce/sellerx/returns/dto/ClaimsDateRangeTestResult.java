package com.ecommerce.sellerx.returns.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClaimsDateRangeTestResult {
    private boolean success;
    private String message;

    // Test parameters
    private LocalDate startDate;
    private LocalDate endDate;
    private int yearsBack;

    // API response info
    private Integer httpStatus;
    private Integer totalElements;
    private Integer totalPages;
    private Integer claimsInFirstPage;

    // Error info (if any)
    private String errorCode;
    private String errorMessage;

    // Timing
    private long requestDurationMs;

    // Multiple test results (for batch testing)
    private List<SingleTestResult> testResults;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SingleTestResult {
        private int yearsBack;
        private LocalDate startDate;
        private LocalDate endDate;
        private boolean success;
        private Integer totalElements;
        private Integer httpStatus;
        private String errorMessage;
        private long durationMs;
    }
}
