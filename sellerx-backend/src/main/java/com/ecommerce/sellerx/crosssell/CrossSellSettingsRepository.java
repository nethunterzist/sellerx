package com.ecommerce.sellerx.crosssell;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CrossSellSettingsRepository extends JpaRepository<CrossSellSettings, UUID> {

    Optional<CrossSellSettings> findByStoreId(UUID storeId);
}
