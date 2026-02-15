package com.ecommerce.sellerx.support;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketAttachmentRepository extends JpaRepository<TicketAttachment, Long> {

    List<TicketAttachment> findByTicketIdOrderByUploadedAtDesc(Long ticketId);

    Optional<TicketAttachment> findByTicketIdAndId(Long ticketId, Long attachmentId);
}
