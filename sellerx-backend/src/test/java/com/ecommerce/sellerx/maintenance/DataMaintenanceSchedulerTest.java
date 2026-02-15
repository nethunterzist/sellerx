package com.ecommerce.sellerx.maintenance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DataMaintenanceScheduler.
 */
@ExtendWith(MockitoExtension.class)
class DataMaintenanceSchedulerTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private DataMaintenanceScheduler scheduler;

    @BeforeEach
    void setUp() {
        // Setup handled by @InjectMocks
    }

    // ============================================
    // RETENTION CLEANUP TESTS
    // ============================================

    @Test
    @DisplayName("Should run all retention cleanup functions")
    void shouldRunRetentionCleanup() {
        // Given
        when(jdbcTemplate.queryForObject(eq("SELECT deleted_count FROM cleanup_old_webhook_events(90)"), eq(Long.class)))
            .thenReturn(100L);
        when(jdbcTemplate.queryForObject(eq("SELECT deleted_count FROM cleanup_old_sent_emails(30)"), eq(Long.class)))
            .thenReturn(50L);
        when(jdbcTemplate.queryForObject(eq("SELECT deleted_count FROM cleanup_old_failed_emails(90)"), eq(Long.class)))
            .thenReturn(10L);
        when(jdbcTemplate.queryForObject(eq("SELECT deleted_count FROM cleanup_old_activity_logs(365)"), eq(Long.class)))
            .thenReturn(200L);
        when(jdbcTemplate.queryForObject(eq("SELECT deleted_count FROM cleanup_old_sync_tasks(30)"), eq(Long.class)))
            .thenReturn(25L);

        // When
        scheduler.runRetentionCleanup();

        // Then
        verify(jdbcTemplate).queryForObject(eq("SELECT deleted_count FROM cleanup_old_webhook_events(90)"), eq(Long.class));
        verify(jdbcTemplate).queryForObject(eq("SELECT deleted_count FROM cleanup_old_sent_emails(30)"), eq(Long.class));
        verify(jdbcTemplate).queryForObject(eq("SELECT deleted_count FROM cleanup_old_failed_emails(90)"), eq(Long.class));
        verify(jdbcTemplate).queryForObject(eq("SELECT deleted_count FROM cleanup_old_activity_logs(365)"), eq(Long.class));
        verify(jdbcTemplate).queryForObject(eq("SELECT deleted_count FROM cleanup_old_sync_tasks(30)"), eq(Long.class));
    }

    @Test
    @DisplayName("Should handle retention cleanup exception gracefully")
    void shouldHandleRetentionCleanupException() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
            .thenThrow(new RuntimeException("Database error"));

        // When - should not throw
        scheduler.runRetentionCleanup();

        // Then - exception is caught and logged
        verify(jdbcTemplate).queryForObject(anyString(), eq(Long.class));
    }

    // ============================================
    // MATERIALIZED VIEW REFRESH TESTS
    // ============================================

    @Test
    @DisplayName("Should refresh all dashboard materialized views")
    void shouldRefreshDashboardViews() {
        // Given
        List<Map<String, Object>> mockResults = List.of(
            Map.of("view_name", "mv_daily_order_stats", "refresh_time_ms", 150L),
            Map.of("view_name", "mv_monthly_order_stats", "refresh_time_ms", 100L),
            Map.of("view_name", "mv_city_sales_stats", "refresh_time_ms", 80L),
            Map.of("view_name", "mv_product_performance", "refresh_time_ms", 200L)
        );
        when(jdbcTemplate.queryForList(eq("SELECT view_name, refresh_time_ms FROM refresh_dashboard_views(TRUE)")))
            .thenReturn(mockResults);

        // When
        scheduler.refreshDashboardViews();

        // Then
        verify(jdbcTemplate).queryForList(eq("SELECT view_name, refresh_time_ms FROM refresh_dashboard_views(TRUE)"));
    }

    @Test
    @DisplayName("Should handle view refresh exception gracefully")
    void shouldHandleViewRefreshException() {
        // Given
        when(jdbcTemplate.queryForList(anyString()))
            .thenThrow(new RuntimeException("Refresh failed"));

        // When - should not throw
        scheduler.refreshDashboardViews();

        // Then - exception is caught and logged
        verify(jdbcTemplate).queryForList(anyString());
    }

    // ============================================
    // TABLE BLOAT CHECK TESTS
    // ============================================

    @Test
    @DisplayName("Should check table bloat and log warnings for high bloat")
    void shouldCheckTableBloat() {
        // Given
        List<Map<String, Object>> mockResults = List.of(
            Map.of("table_name", "trendyol_orders", "table_size_mb", 500.0,
                   "dead_tuples", 50000L, "live_tuples", 1000000L, "dead_ratio", 5.0),
            Map.of("table_name", "webhook_events", "table_size_mb", 100.0,
                   "dead_tuples", 20000L, "live_tuples", 100000L, "dead_ratio", 20.0) // High bloat
        );
        when(jdbcTemplate.queryForList(eq("SELECT * FROM check_table_bloat()")))
            .thenReturn(mockResults);

        // When
        scheduler.checkTableBloat();

        // Then
        verify(jdbcTemplate).queryForList(eq("SELECT * FROM check_table_bloat()"));
    }

    @Test
    @DisplayName("Should handle table bloat check exception gracefully")
    void shouldHandleBloatCheckException() {
        // Given
        when(jdbcTemplate.queryForList(anyString()))
            .thenThrow(new RuntimeException("Query failed"));

        // When - should not throw
        scheduler.checkTableBloat();

        // Then - exception is caught and logged
        verify(jdbcTemplate).queryForList(anyString());
    }

    // ============================================
    // MANUAL TRIGGER TESTS
    // ============================================

    @Test
    @DisplayName("Should trigger retention cleanup manually")
    void shouldTriggerRetentionCleanupManually() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class)))
            .thenReturn(0L);

        // When
        String result = scheduler.triggerRetentionCleanup();

        // Then
        verify(jdbcTemplate, times(5)).queryForObject(anyString(), eq(Long.class));
        assert result.contains("Retention cleanup triggered");
    }

    @Test
    @DisplayName("Should trigger view refresh manually")
    void shouldTriggerViewRefreshManually() {
        // Given
        when(jdbcTemplate.queryForList(anyString()))
            .thenReturn(List.of());

        // When
        String result = scheduler.triggerViewRefresh();

        // Then
        verify(jdbcTemplate).queryForList(anyString());
        assert result.contains("View refresh triggered");
    }
}
