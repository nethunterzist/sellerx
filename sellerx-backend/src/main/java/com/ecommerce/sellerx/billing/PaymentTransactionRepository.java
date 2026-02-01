package com.ecommerce.sellerx.billing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, UUID> {

    List<PaymentTransaction> findByInvoiceIdOrderByCreatedAtDesc(UUID invoiceId);

    Optional<PaymentTransaction> findByIyzicoPaymentId(String iyzicoPaymentId);

    Optional<PaymentTransaction> findByIyzicoConversationId(String conversationId);

    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.status = 'FAILED' AND pt.attemptNumber < 3 AND pt.nextRetryAt <= :retryTime")
    List<PaymentTransaction> findTransactionsToRetry(@Param("retryTime") LocalDateTime retryTime);

    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.invoice.id = :invoiceId AND pt.status = 'SUCCESS'")
    Optional<PaymentTransaction> findSuccessfulPaymentForInvoice(@Param("invoiceId") UUID invoiceId);

    @Query("SELECT pt FROM PaymentTransaction pt WHERE pt.invoice.id = :invoiceId ORDER BY pt.attemptNumber DESC")
    List<PaymentTransaction> findByInvoiceOrderByAttempt(@Param("invoiceId") UUID invoiceId);

    @Query("SELECT COUNT(pt) FROM PaymentTransaction pt WHERE pt.invoice.id = :invoiceId AND pt.status = 'FAILED'")
    int countFailedAttemptsForInvoice(@Param("invoiceId") UUID invoiceId);

    @Query("SELECT pt FROM PaymentTransaction pt JOIN pt.invoice i WHERE i.user.id = :userId ORDER BY pt.createdAt DESC")
    List<PaymentTransaction> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    boolean existsByIyzicoPaymentId(String iyzicoPaymentId);

    boolean existsByIyzicoConversationId(String conversationId);
}
