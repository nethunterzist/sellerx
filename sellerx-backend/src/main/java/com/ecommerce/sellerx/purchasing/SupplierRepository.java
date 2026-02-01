package com.ecommerce.sellerx.purchasing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    List<Supplier> findByStoreIdOrderByNameAsc(UUID storeId);

    Optional<Supplier> findByStoreIdAndId(UUID storeId, Long id);

    boolean existsByStoreIdAndName(UUID storeId, String name);
}
