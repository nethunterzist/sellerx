package com.ecommerce.sellerx.financial;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity for tracking failed chunks during historical sync.
 * Prevents infinite retry loops by tracking retry count per chunk.
 * Chunks with retryCount >= 5 are considered permanently failed and skipped.
 */
@Entity
@Table(name = "historical_sync_failed_chunks")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistoricalSyncFailedChunk {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private UUID id;
    
    @Column(name = "store_id", nullable = false)
    private UUID storeId;
    
    @Column(name = "chunk_start_date", nullable = false)
    private LocalDateTime chunkStartDate;
    
    @Column(name = "chunk_end_date", nullable = false)
    private LocalDateTime chunkEndDate;
    
    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;
    
    @Column(name = "last_error_message", columnDefinition = "TEXT")
    private String lastErrorMessage;
    
    @Column(name = "failed_at", nullable = false)
    @Builder.Default
    private LocalDateTime failedAt = LocalDateTime.now();
}
