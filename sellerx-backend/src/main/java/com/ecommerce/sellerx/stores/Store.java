package com.ecommerce.sellerx.stores;

import jakarta.persistence.*;
import com.vladmihalcea.hibernate.type.json.JsonBinaryType;
import org.hibernate.annotations.Type;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.ecommerce.sellerx.users.User;
import com.ecommerce.sellerx.expenses.ExpenseCategory;
import com.ecommerce.sellerx.common.encryption.CredentialsEntityListener;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "stores")
@EntityListeners(CredentialsEntityListener.class)
public class Store {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private java.util.UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "store_name", nullable = false)
    private String storeName;

    @Column(name = "marketplace", nullable = false)
    private String marketplace;

    @Type(JsonBinaryType.class)
    @Column(name = "credentials", columnDefinition = "jsonb", nullable = false)
    private MarketplaceCredentials credentials;
    
    @Column(name = "webhook_id")
    private String webhookId; // Trendyol webhook ID for this store

    @Enumerated(EnumType.STRING)
    @Column(name = "webhook_status")
    private WebhookStatus webhookStatus = WebhookStatus.PENDING;

    @Column(name = "webhook_error_message", columnDefinition = "TEXT")
    private String webhookErrorMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_status")
    private SyncStatus syncStatus = SyncStatus.PENDING;

    @Column(name = "sync_error_message", columnDefinition = "TEXT")
    private String syncErrorMessage;

    @Column(name = "initial_sync_completed")
    private Boolean initialSyncCompleted = false;

    /**
     * Status of historical settlement sync:
     * - null: Not started
     * - COMPLETED: All historical data synced
     * - PARTIAL: Some data missing (chunk failures)
     * - FAILED: Sync failed
     * - SKIPPED: Store is less than 90 days old
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "historical_sync_status")
    private HistoricalSyncStatus historicalSyncStatus;

    @Column(name = "historical_sync_date")
    private LocalDateTime historicalSyncDate;

    /**
     * Resumable sync checkpoint: Last successfully processed chunk date.
     * Used to resume sync from where it left off if interrupted.
     */
    @Column(name = "historical_sync_checkpoint_date")
    private LocalDateTime historicalSyncCheckpointDate;

    /**
     * Historical sync start date: When the current sync operation started.
     */
    @Column(name = "historical_sync_start_date")
    private LocalDateTime historicalSyncStartDate;

    /**
     * Total number of chunks to process in current sync operation.
     */
    @Column(name = "historical_sync_total_chunks")
    private Integer historicalSyncTotalChunks;

    /**
     * Number of chunks completed in current sync operation.
     */
    @Column(name = "historical_sync_completed_chunks")
    private Integer historicalSyncCompletedChunks;

    /**
     * Current date being processed. Used for frontend progress display.
     */
    @Column(name = "historical_sync_current_processing_date")
    private LocalDateTime historicalSyncCurrentProcessingDate;

    /**
     * Sync lock mechanism to prevent concurrent sync operations for the same store.
     * - syncLockAcquiredAt: When the lock was acquired (null = no lock)
     * - syncLockThreadId: Thread that acquired the lock (for debugging)
     * Locks older than 2 hours are considered stale and can be cleared.
     */
    @Column(name = "sync_lock_acquired_at")
    private LocalDateTime syncLockAcquiredAt;

    @Column(name = "sync_lock_thread_id")
    private String syncLockThreadId;

    /**
     * Parallel sync phases tracking.
     * Each phase has its own status: PENDING, ACTIVE, COMPLETED, FAILED
     * Phases: PRODUCTS, HISTORICAL, FINANCIAL, GAP, COMMISSIONS, RETURNS, QA
     */
    @Type(JsonBinaryType.class)
    @Column(name = "sync_phases", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, PhaseStatus> syncPhases = new HashMap<>();

    /**
     * Overall sync status for parallel execution:
     * - PENDING: Not started
     * - IN_PROGRESS: At least one phase is active
     * - COMPLETED: All phases completed successfully
     * - FAILED: At least one phase failed
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "overall_sync_status")
    private OverallSyncStatus overallSyncStatus;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Expense categories owned by this store - cascade delete when store is deleted
    @OneToMany(mappedBy = "store", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ExpenseCategory> expenseCategories = new ArrayList<>();
}
