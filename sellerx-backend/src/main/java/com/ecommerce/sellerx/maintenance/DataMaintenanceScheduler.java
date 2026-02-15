package com.ecommerce.sellerx.maintenance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Scheduled service for database maintenance tasks.
 * Handles data retention policies and materialized view refresh.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class DataMaintenanceScheduler {

    private final JdbcTemplate jdbcTemplate;

    // ============================================
    // DATA RETENTION CLEANUP - Daily at 03:00
    // ============================================

    /**
     * Runs all retention cleanup functions.
     * Deletes old data from high-growth tables:
     * - webhook_events: 90 days
     * - sent emails: 30 days
     * - failed emails: 90 days
     * - activity_logs: 1 year
     * - sync_tasks: 30 days
     */
    @Scheduled(cron = "0 0 3 * * ?") // Every day at 03:00
    @SchedulerLock(name = "retentionCleanup", lockAtLeastFor = "PT5M", lockAtMostFor = "PT30M")
    public void runRetentionCleanup() {
        log.info("[MAINTENANCE] Starting data retention cleanup...");
        long startTime = System.currentTimeMillis();

        try {
            // Cleanup webhook events (90 days)
            Long webhookDeleted = jdbcTemplate.queryForObject(
                "SELECT deleted_count FROM cleanup_old_webhook_events(90)",
                Long.class
            );
            log.info("[MAINTENANCE] Deleted {} old webhook events", webhookDeleted);

            // Cleanup sent emails (30 days)
            Long sentEmailsDeleted = jdbcTemplate.queryForObject(
                "SELECT deleted_count FROM cleanup_old_sent_emails(30)",
                Long.class
            );
            log.info("[MAINTENANCE] Deleted {} old sent emails", sentEmailsDeleted);

            // Cleanup failed emails (90 days)
            Long failedEmailsDeleted = jdbcTemplate.queryForObject(
                "SELECT deleted_count FROM cleanup_old_failed_emails(90)",
                Long.class
            );
            log.info("[MAINTENANCE] Deleted {} old failed emails", failedEmailsDeleted);

            // Cleanup activity logs (365 days)
            Long activityDeleted = jdbcTemplate.queryForObject(
                "SELECT deleted_count FROM cleanup_old_activity_logs(365)",
                Long.class
            );
            log.info("[MAINTENANCE] Deleted {} old activity logs", activityDeleted);

            // Cleanup sync tasks (30 days)
            Long syncDeleted = jdbcTemplate.queryForObject(
                "SELECT deleted_count FROM cleanup_old_sync_tasks(30)",
                Long.class
            );
            log.info("[MAINTENANCE] Deleted {} old sync tasks", syncDeleted);

            long duration = System.currentTimeMillis() - startTime;
            log.info("[MAINTENANCE] Data retention cleanup completed in {}ms. Total deleted: {}",
                duration,
                (webhookDeleted != null ? webhookDeleted : 0) +
                (sentEmailsDeleted != null ? sentEmailsDeleted : 0) +
                (failedEmailsDeleted != null ? failedEmailsDeleted : 0) +
                (activityDeleted != null ? activityDeleted : 0) +
                (syncDeleted != null ? syncDeleted : 0)
            );
        } catch (Exception e) {
            log.error("[MAINTENANCE] Data retention cleanup failed: {}", e.getMessage(), e);
        }
    }

    // ============================================
    // MATERIALIZED VIEW REFRESH - Every 15 minutes
    // ============================================

    /**
     * Refreshes dashboard materialized views for fast query performance.
     * Uses CONCURRENTLY to avoid blocking reads.
     */
    @Scheduled(cron = "0 */15 * * * ?") // Every 15 minutes
    @SchedulerLock(name = "refreshDashboardViews", lockAtLeastFor = "PT1M", lockAtMostFor = "PT10M")
    public void refreshDashboardViews() {
        log.info("[MAINTENANCE] Starting materialized view refresh...");
        long startTime = System.currentTimeMillis();

        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(
                "SELECT view_name, refresh_time_ms FROM refresh_dashboard_views(TRUE)"
            );

            for (Map<String, Object> result : results) {
                log.info("[MAINTENANCE] Refreshed {} in {}ms",
                    result.get("view_name"),
                    result.get("refresh_time_ms")
                );
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("[MAINTENANCE] Materialized view refresh completed in {}ms", duration);
        } catch (Exception e) {
            log.error("[MAINTENANCE] Materialized view refresh failed: {}", e.getMessage(), e);
        }
    }

    // ============================================
    // TABLE BLOAT CHECK - Daily at 04:00
    // ============================================

    /**
     * Checks table bloat and logs warnings for tables with high dead tuple ratio.
     */
    @Scheduled(cron = "0 0 4 * * ?") // Every day at 04:00
    @SchedulerLock(name = "tableBloatCheck", lockAtLeastFor = "PT1M", lockAtMostFor = "PT5M")
    public void checkTableBloat() {
        log.info("[MAINTENANCE] Checking table bloat...");

        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(
                "SELECT * FROM check_table_bloat()"
            );

            for (Map<String, Object> result : results) {
                String tableName = (String) result.get("table_name");
                Number deadRatio = (Number) result.get("dead_ratio");
                Number deadTuples = (Number) result.get("dead_tuples");

                if (deadRatio != null && deadRatio.doubleValue() > 10.0) {
                    log.warn("[MAINTENANCE] High bloat on {}: {}% dead rows ({} dead tuples)",
                        tableName,
                        deadRatio,
                        deadTuples
                    );
                } else {
                    log.debug("[MAINTENANCE] Table {} bloat: {}% ({} dead tuples)",
                        tableName,
                        deadRatio != null ? deadRatio : 0,
                        deadTuples != null ? deadTuples : 0
                    );
                }
            }
        } catch (Exception e) {
            log.error("[MAINTENANCE] Table bloat check failed: {}", e.getMessage(), e);
        }
    }

    // ============================================
    // MANUAL TRIGGER METHODS (for testing/admin)
    // ============================================

    /**
     * Manually trigger retention cleanup.
     * @return Summary of deleted records
     */
    public String triggerRetentionCleanup() {
        runRetentionCleanup();
        return "Retention cleanup triggered. Check logs for details.";
    }

    /**
     * Manually trigger materialized view refresh.
     * @return Summary of refreshed views
     */
    public String triggerViewRefresh() {
        refreshDashboardViews();
        return "View refresh triggered. Check logs for details.";
    }
}
