package com.ecommerce.sellerx.sync;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity to track async sync operations.
 * Allows users to check sync progress without blocking.
 */
@Entity
@Table(name = "sync_tasks", indexes = {
    @Index(name = "idx_sync_task_store_type", columnList = "store_id, task_type"),
    @Index(name = "idx_sync_task_status", columnList = "status"),
    @Index(name = "idx_sync_task_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SyncTask {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "task_type", nullable = false, length = 20)
    private SyncTaskType taskType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SyncTaskStatus status;

    @Column(name = "progress_percentage")
    private Integer progressPercentage;

    @Column(name = "current_page")
    private Integer currentPage;

    @Column(name = "total_pages")
    private Integer totalPages;

    @Column(name = "items_processed")
    private Integer itemsProcessed;

    @Column(name = "items_new")
    private Integer itemsNew;

    @Column(name = "items_updated")
    private Integer itemsUpdated;

    @Column(name = "items_skipped")
    private Integer itemsSkipped;

    @Column(name = "items_failed")
    private Integer itemsFailed;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = SyncTaskStatus.PENDING;
        }
        if (progressPercentage == null) {
            progressPercentage = 0;
        }
    }
}
