package com.ecommerce.sellerx.support;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for TicketMessage entity.
 */
@Repository
public interface TicketMessageRepository extends JpaRepository<TicketMessage, Long> {

    /**
     * Find all messages for a ticket ordered by creation time.
     */
    List<TicketMessage> findByTicketIdOrderByCreatedAtAsc(Long ticketId);

    /**
     * Find all messages for a ticket with pagination.
     */
    Page<TicketMessage> findByTicketIdOrderByCreatedAtAsc(Long ticketId, Pageable pageable);

    /**
     * Count messages for a ticket.
     */
    long countByTicketId(Long ticketId);

    /**
     * Find latest message for a ticket.
     */
    TicketMessage findFirstByTicketIdOrderByCreatedAtDesc(Long ticketId);
}
