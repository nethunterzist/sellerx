package com.ecommerce.sellerx.stores;

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
}
