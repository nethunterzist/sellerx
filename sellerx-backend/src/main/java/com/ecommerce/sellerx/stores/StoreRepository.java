package com.ecommerce.sellerx.stores;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import java.util.Optional;

import java.util.List;
import com.ecommerce.sellerx.users.User;

public interface StoreRepository extends JpaRepository<Store, UUID> {
    List<Store> findAllByUser(User user);
    
    List<Store> findByMarketplace(String marketplace);
    
    // Case-insensitive marketplace search
    @Query("SELECT s FROM Store s WHERE LOWER(s.marketplace) = LOWER(:marketplace)")
    List<Store> findByMarketplaceIgnoreCase(@Param("marketplace") String marketplace);
    
    // Find store by seller ID (from JSONB credentials)
    @Query(value = "SELECT * FROM stores s WHERE s.credentials->>'sellerId' = :sellerId", nativeQuery = true)
    Optional<Store> findBySellerId(@Param("sellerId") String sellerId);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM Store s WHERE s.id = :id AND s.user = :user")
    void deleteByIdAndUser(@Param("id") UUID id, @Param("user") User user);

    // Find all stores that have completed initial sync (ready for scheduled operations)
    List<Store> findByInitialSyncCompletedTrue();

    // Count by sync status (for admin dashboard stats)
    long countBySyncStatus(SyncStatus syncStatus);

    // Count stores with sync errors (FAILED status or has error message)
    @Query("SELECT COUNT(s) FROM Store s WHERE s.syncStatus = 'FAILED' OR s.syncErrorMessage IS NOT NULL")
    long countWithSyncErrors();

    // Count stores with webhook errors (FAILED status or has error message)
    @Query("SELECT COUNT(s) FROM Store s WHERE s.webhookStatus = 'FAILED' OR s.webhookErrorMessage IS NOT NULL")
    long countWithWebhookErrors();

    // Paginated search by store name or user email (for admin search)
    @Query("SELECT s FROM Store s WHERE LOWER(s.storeName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(s.user.email) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Store> searchByStoreNameOrUserEmail(@Param("query") String query, Pageable pageable);
}
