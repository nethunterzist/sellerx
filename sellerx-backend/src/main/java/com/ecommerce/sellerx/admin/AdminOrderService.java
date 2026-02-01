package com.ecommerce.sellerx.admin;

import com.ecommerce.sellerx.admin.dto.AdminOrderStatsDto;
import com.ecommerce.sellerx.admin.dto.AdminRecentOrderDto;
import com.ecommerce.sellerx.orders.TrendyolOrder;
import com.ecommerce.sellerx.orders.TrendyolOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminOrderService {

    private final TrendyolOrderRepository orderRepository;

    private static final List<String> ORDER_STATUSES = List.of(
            "Created", "Picking", "Shipped", "Delivered", "Returned", "Cancelled"
    );

    /**
     * Get platform-wide order statistics for the admin panel.
     * Uses existing repository queries that operate across all stores.
     */
    public AdminOrderStatsDto getOrderStats() {
        log.info("Admin fetching order stats");

        long totalOrders = orderRepository.count();

        // Today: midnight to end of day
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(LocalTime.MAX);
        long ordersToday = orderRepository.countAllOrdersBetween(todayStart, todayEnd);

        // This week: Monday to now
        LocalDateTime weekStart = LocalDate.now()
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .atStartOfDay();
        long ordersThisWeek = orderRepository.countAllOrdersBetween(weekStart, todayEnd);

        // This month: 1st of month to now
        LocalDateTime monthStart = LocalDate.now()
                .withDayOfMonth(1)
                .atStartOfDay();
        long ordersThisMonth = orderRepository.countAllOrdersBetween(monthStart, todayEnd);

        // Status breakdown
        Map<String, Long> statusBreakdown = new LinkedHashMap<>();
        for (String status : ORDER_STATUSES) {
            statusBreakdown.put(status, orderRepository.countAllOrdersByStatus(status));
        }

        return AdminOrderStatsDto.builder()
                .totalOrders(totalOrders)
                .ordersToday(ordersToday)
                .ordersThisWeek(ordersThisWeek)
                .ordersThisMonth(ordersThisMonth)
                .statusBreakdown(statusBreakdown)
                .build();
    }

    /**
     * Get recent orders across all stores, paginated and sorted by orderDate descending.
     */
    public Page<AdminRecentOrderDto> getRecentOrders(Pageable pageable) {
        log.info("Admin fetching recent orders, page={}, size={}", pageable.getPageNumber(), pageable.getPageSize());

        Page<TrendyolOrder> orders = orderRepository.findAllByOrderByOrderDateDesc(pageable);

        return orders.map(order -> AdminRecentOrderDto.builder()
                .id(order.getId())
                .orderNumber(order.getTyOrderNumber())
                .storeName(order.getStore() != null ? order.getStore().getStoreName() : "Unknown")
                .status(order.getStatus())
                .totalPrice(order.getTotalPrice())
                .orderDate(order.getOrderDate())
                .build());
    }
}
