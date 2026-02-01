package com.ecommerce.sellerx.financial;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing failed chunk records during historical sync.
 */
public interface HistoricalSyncFailedChunkRepository extends JpaRepository<HistoricalSyncFailedChunk, UUID> {
    
    /**
     * Find a failed chunk record for a specific store and date range.
     */
    Optional<HistoricalSyncFailedChunk> findByStoreIdAndChunkStartDateAndChunkEndDate(
        UUID storeId, LocalDateTime chunkStartDate, LocalDateTime chunkEndDate);
    
    /**
     * Find all permanently failed chunks (retryCount >= 5) for a store.
     */
    @Query("SELECT f FROM HistoricalSyncFailedChunk f WHERE f.storeId = :storeId AND f.retryCount >= 5")
    List<HistoricalSyncFailedChunk> findPermanentlyFailedChunks(@Param("storeId") UUID storeId);
    
    /**
     * Delete all failed chunk records for a store (used when sync completes successfully).
     */
    void deleteByStoreId(UUID storeId);
}
