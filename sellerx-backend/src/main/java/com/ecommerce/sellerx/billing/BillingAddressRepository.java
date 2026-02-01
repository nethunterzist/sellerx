package com.ecommerce.sellerx.billing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BillingAddressRepository extends JpaRepository<BillingAddress, UUID> {

    List<BillingAddress> findByUserIdOrderByIsDefaultDescCreatedAtDesc(Long userId);

    Optional<BillingAddress> findByUserIdAndIsDefaultTrue(Long userId);

    Optional<BillingAddress> findByIdAndUserId(UUID id, Long userId);

    Optional<BillingAddress> findByParasutContactId(String parasutContactId);

    @Modifying
    @Query("UPDATE BillingAddress ba SET ba.isDefault = false WHERE ba.user.id = :userId")
    void clearDefaultForUser(@Param("userId") Long userId);

    long countByUserId(Long userId);

    @Query("SELECT ba FROM BillingAddress ba WHERE ba.user.id = :userId AND ba.addressType = :type")
    List<BillingAddress> findByUserIdAndType(@Param("userId") Long userId, @Param("type") BillingAddressType type);
}
