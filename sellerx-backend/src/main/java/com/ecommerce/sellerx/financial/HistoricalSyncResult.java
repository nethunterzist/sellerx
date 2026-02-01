package com.ecommerce.sellerx.financial;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Result of historical settlement sync operation.
 * Contains statistics about how many orders were created from settlement data.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HistoricalSyncResult {

    /**
     * Status of the sync operation:
     * - COMPLETED: All historical data synced successfully
     * - PARTIAL: Some chunks failed, but most data was synced
     * - SKIPPED: Store is less than 90 days old, nothing to sync
     * - FAILED: Complete sync failure
     */
    private String status;

    /**
     * Reason for the status (especially for SKIPPED or FAILED)
     */
    private String reason;

    /**
     * Number of historical orders created from settlement data
     */
    private int ordersCreated;

    /**
     * Total number of settlement records processed
     */
    private int settlementsProcessed;

    /**
     * Number of chunks that failed to process
     */
    private int failedChunks;

    /**
     * Number of chunks that were skipped due to permanent failure (retryCount >= 5)
     */
    private int skippedChunks;

    /**
     * Total number of chunks processed
     */
    private int totalChunks;

    /**
     * Start date of the historical sync range
     */
    private LocalDateTime syncedFrom;

    /**
     * End date of the historical sync range (90 days ago from sync time)
     */
    private LocalDateTime syncedTo;

    /**
     * Create a SKIPPED result for stores less than 90 days old
     */
    public static HistoricalSyncResult skipped(String reason) {
        return HistoricalSyncResult.builder()
                .status("SKIPPED")
                .reason(reason)
                .ordersCreated(0)
                .settlementsProcessed(0)
                .failedChunks(0)
                .totalChunks(0)
                .build();
    }

    /**
     * Create a FAILED result
     */
    public static HistoricalSyncResult failed(String reason) {
        return HistoricalSyncResult.builder()
                .status("FAILED")
                .reason(reason)
                .ordersCreated(0)
                .settlementsProcessed(0)
                .build();
    }
}
