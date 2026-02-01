package com.ecommerce.sellerx.ai;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoreAiSettingsRepository extends JpaRepository<StoreAiSettings, UUID> {

    Optional<StoreAiSettings> findByStoreId(UUID storeId);

    boolean existsByStoreId(UUID storeId);
}
