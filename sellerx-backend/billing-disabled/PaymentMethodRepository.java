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
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, UUID> {

    List<PaymentMethod> findByUserIdAndIsActiveTrue(Long userId);

    Optional<PaymentMethod> findByUserIdAndIsDefaultTrueAndIsActiveTrue(Long userId);

    Optional<PaymentMethod> findByIdAndUserId(UUID id, Long userId);

    Optional<PaymentMethod> findByIyzicoCardToken(String cardToken);

    @Query("SELECT pm FROM PaymentMethod pm WHERE pm.user.id = :userId AND pm.isActive = true ORDER BY pm.isDefault DESC, pm.createdAt DESC")
    List<PaymentMethod> findByUserIdOrderByDefaultAndCreated(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE PaymentMethod pm SET pm.isDefault = false WHERE pm.user.id = :userId AND pm.isActive = true")
    void clearDefaultForUser(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE PaymentMethod pm SET pm.isActive = false WHERE pm.id = :id")
    void deactivate(@Param("id") UUID id);

    long countByUserIdAndIsActiveTrue(Long userId);

    boolean existsByUserIdAndIyzicoCardTokenAndIsActiveTrue(Long userId, String cardToken);
}
