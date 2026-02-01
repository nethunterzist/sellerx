package com.ecommerce.sellerx.purchasing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderAttachmentRepository extends JpaRepository<PurchaseOrderAttachment, Long> {

    List<PurchaseOrderAttachment> findByPurchaseOrderIdOrderByUploadedAtDesc(Long purchaseOrderId);

    Optional<PurchaseOrderAttachment> findByPurchaseOrderIdAndId(Long purchaseOrderId, Long attachmentId);
}
