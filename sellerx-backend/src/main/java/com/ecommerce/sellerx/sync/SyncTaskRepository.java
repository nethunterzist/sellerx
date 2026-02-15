package com.ecommerce.sellerx.sync;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SyncTaskRepository extends JpaRepository<SyncTask, UUID> {

    /**
     * Find task by ID and store ID (for security - verify user owns the store)
     */
    Optional<SyncTask> findByIdAndStoreId(UUID taskId, UUID storeId);

    /**
     * Find recent tasks for a store
     */
    List<SyncTask> findByStoreIdOrderByCreatedAtDesc(UUID storeId);

    /**
     * Find active (running or pending) tasks for a store
     */
    @Query("SELECT t FROM SyncTask t WHERE t.storeId = :storeId AND t.status IN ('PENDING', 'RUNNING')")
    List<SyncTask> findActiveTasksByStoreId(@Param("storeId") UUID storeId);

    /**
     * Find active task of a specific type for a store
     */
    @Query("SELECT t FROM SyncTask t WHERE t.storeId = :storeId AND t.taskType = :taskType " +
           "AND t.status IN ('PENDING', 'RUNNING')")
    Optional<SyncTask> findActiveTaskByStoreIdAndType(
            @Param("storeId") UUID storeId,
            @Param("taskType") SyncTaskType taskType);

    /**
     * Find latest task of a specific type for a store
     */
    Optional<SyncTask> findFirstByStoreIdAndTaskTypeOrderByCreatedAtDesc(UUID storeId, SyncTaskType taskType);

    /**
     * Clean up old completed tasks (for maintenance)
     */
    @Query("DELETE FROM SyncTask t WHERE t.status IN ('COMPLETED', 'FAILED', 'CANCELLED') " +
           "AND t.createdAt < :cutoffDate")
    void deleteOldTasks(@Param("cutoffDate") LocalDateTime cutoffDate);
}
