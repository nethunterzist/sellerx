package com.ecommerce.sellerx.admin;

import com.ecommerce.sellerx.admin.dto.*;
import com.ecommerce.sellerx.users.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminBillingService {

    private final UserRepository userRepository;

    public Page<AdminSubscriptionListDto> getSubscriptions(String status, String planName, Pageable pageable) {
        // Billing module disabled - return empty page
        log.debug("Billing module disabled - returning empty subscriptions page");
        return new PageImpl<>(List.of(), pageable, 0);
    }

    public AdminSubscriptionDetailDto getSubscriptionDetail(Long id) {
        log.debug("Billing module disabled - returning placeholder subscription detail for id: {}", id);
        return AdminSubscriptionDetailDto.builder()
                .id(id)
                .userEmail("N/A")
                .planName("Billing Disabled")
                .status("inactive")
                .paymentHistory(List.of())
                .build();
    }

    public AdminRevenueStatsDto getRevenueStats() {
        long totalUsers = userRepository.count();
        log.debug("Billing module disabled - returning zero revenue stats with {} total users", totalUsers);
        return AdminRevenueStatsDto.builder()
                .mrr(BigDecimal.ZERO)
                .arr(BigDecimal.ZERO)
                .activeCount(0L)
                .trialCount(0L)
                .cancelledCount(0L)
                .churnRate(0.0)
                .totalUsers(totalUsers)
                .billingEnabled(false)
                .build();
    }

    public List<AdminRevenueHistoryDto> getRevenueHistory() {
        // Return last 12 months with zero revenue
        log.debug("Billing module disabled - returning 12-month zero revenue history");
        List<AdminRevenueHistoryDto> history = new ArrayList<>();
        YearMonth current = YearMonth.now();
        for (int i = 11; i >= 0; i--) {
            YearMonth month = current.minusMonths(i);
            history.add(AdminRevenueHistoryDto.builder()
                    .month(month.toString())
                    .totalRevenue(BigDecimal.ZERO)
                    .subscriptionCount(0L)
                    .build());
        }
        return history;
    }

    public Page<AdminPaymentDto> getPayments(Pageable pageable) {
        // Billing module disabled - return empty page
        log.debug("Billing module disabled - returning empty payments page");
        return new PageImpl<>(List.of(), pageable, 0);
    }
}
