package com.ecommerce.sellerx.support;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for SupportTicket entity.
 */
@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {

    /**
     * Find ticket by ticket number.
     */
    Optional<SupportTicket> findByTicketNumber(String ticketNumber);

    /**
     * Find all tickets for a user.
     */
    List<SupportTicket> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Find all tickets for a user with pagination.
     */
    Page<SupportTicket> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Find all tickets for a specific store.
     */
    List<SupportTicket> findByStoreIdOrderByCreatedAtDesc(UUID storeId);

    /**
     * Find all tickets by status.
     */
    Page<SupportTicket> findByStatusOrderByCreatedAtDesc(TicketStatus status, Pageable pageable);

    /**
     * Find all tickets by priority.
     */
    Page<SupportTicket> findByPriorityOrderByCreatedAtDesc(TicketPriority priority, Pageable pageable);

    /**
     * Find all tickets assigned to a specific admin.
     */
    Page<SupportTicket> findByAssignedToIdOrderByCreatedAtDesc(Long adminId, Pageable pageable);

    /**
     * Find open/in-progress tickets (not closed).
     */
    @Query("SELECT t FROM SupportTicket t WHERE t.status NOT IN ('RESOLVED', 'CLOSED') ORDER BY t.createdAt DESC")
    Page<SupportTicket> findActiveTickets(Pageable pageable);

    /**
     * Count tickets by status for a user.
     */
    long countByUserIdAndStatus(Long userId, TicketStatus status);

    /**
     * Count all tickets by status.
     */
    long countByStatus(TicketStatus status);

    /**
     * Search tickets by subject or ticket number.
     */
    @Query("SELECT t FROM SupportTicket t WHERE " +
           "LOWER(t.subject) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(t.ticketNumber) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "ORDER BY t.createdAt DESC")
    Page<SupportTicket> searchTickets(@Param("query") String query, Pageable pageable);

    /**
     * Find tickets with filters (for admin).
     */
    @Query("SELECT t FROM SupportTicket t WHERE " +
           "(:status IS NULL OR t.status = :status) AND " +
           "(:priority IS NULL OR t.priority = :priority) AND " +
           "(:category IS NULL OR t.category = :category) " +
           "ORDER BY t.createdAt DESC")
    Page<SupportTicket> findWithFilters(
            @Param("status") TicketStatus status,
            @Param("priority") TicketPriority priority,
            @Param("category") TicketCategory category,
            Pageable pageable);

    /**
     * Get the next ticket number sequence.
     */
    @Query(value = "SELECT COUNT(*) + 1 FROM support_tickets WHERE DATE(created_at) = CURRENT_DATE", nativeQuery = true)
    int getNextTicketSequence();
}
