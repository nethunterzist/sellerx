package com.ecommerce.sellerx.billing;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EInvoiceRepository extends JpaRepository<EInvoice, UUID> {

    Optional<EInvoice> findByInvoiceId(UUID invoiceId);

    Optional<EInvoice> findByParasutId(String parasutId);

    Page<EInvoice> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    @Query("SELECT e FROM EInvoice e WHERE e.status = 'PENDING'")
    List<EInvoice> findPendingEInvoices();

    @Query("SELECT e FROM EInvoice e WHERE e.status = 'DRAFT' AND e.invoice.status = 'PAID'")
    List<EInvoice> findDraftEInvoicesForPaidInvoices();

    @Query("SELECT e FROM EInvoice e WHERE e.user.id = :userId AND e.status = 'APPROVED' ORDER BY e.approvedAt DESC")
    List<EInvoice> findApprovedByUser(@Param("userId") Long userId);

    Optional<EInvoice> findByIdAndUserId(UUID id, Long userId);

    long countByUserIdAndStatus(Long userId, EInvoiceStatus status);
}
