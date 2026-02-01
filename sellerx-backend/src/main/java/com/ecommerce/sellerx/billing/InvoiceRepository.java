package com.ecommerce.sellerx.billing;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    Page<Invoice> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<Invoice> findBySubscriptionIdOrderByCreatedAtDesc(UUID subscriptionId);

    Optional<Invoice> findByIdAndUserId(UUID id, Long userId);

    @Query("SELECT i FROM Invoice i WHERE i.status = 'PENDING' AND i.dueDate <= :dueDate")
    List<Invoice> findOverdueInvoices(@Param("dueDate") LocalDateTime dueDate);

    @Query("SELECT i FROM Invoice i WHERE i.user.id = :userId AND i.status = 'PAID' ORDER BY i.paidAt DESC")
    List<Invoice> findPaidInvoicesByUser(@Param("userId") Long userId);

    @Query("SELECT i FROM Invoice i WHERE i.subscription.id = :subscriptionId AND i.billingPeriodStart = :periodStart")
    Optional<Invoice> findBySubscriptionAndPeriod(@Param("subscriptionId") UUID subscriptionId, @Param("periodStart") LocalDateTime periodStart);

    @Query("SELECT MAX(CAST(SUBSTRING(i.invoiceNumber, 10) AS int)) FROM Invoice i WHERE i.invoiceNumber LIKE :prefix%")
    Integer findMaxInvoiceNumberByPrefix(@Param("prefix") String prefix);

    @Query("SELECT SUM(i.totalAmount) FROM Invoice i WHERE i.user.id = :userId AND i.status = 'PAID' AND i.paidAt >= :since")
    java.math.BigDecimal sumPaidAmountByUserSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    long countByUserIdAndStatus(Long userId, InvoiceStatus status);
}
