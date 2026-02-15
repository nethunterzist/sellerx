package com.ecommerce.sellerx.email.repository;

import com.ecommerce.sellerx.email.EmailStatus;
import com.ecommerce.sellerx.email.EmailType;
import com.ecommerce.sellerx.email.entity.EmailQueue;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface EmailQueueRepository extends JpaRepository<EmailQueue, UUID> {

    /**
     * Find pending emails ready to be sent.
     * Either scheduled_at is null (send immediately) or scheduled_at is in the past.
     */
    @Query("""
        SELECT e FROM EmailQueue e
        WHERE e.status = :status
        AND (e.scheduledAt IS NULL OR e.scheduledAt <= :now)
        ORDER BY e.createdAt ASC
        """)
    List<EmailQueue> findPendingEmails(
            @Param("status") EmailStatus status,
            @Param("now") OffsetDateTime now,
            Pageable pageable);

    /**
     * Find emails ready to process (convenience method).
     */
    default List<EmailQueue> findEmailsToProcess(int batchSize) {
        return findPendingEmails(
                EmailStatus.PENDING,
                OffsetDateTime.now(),
                Pageable.ofSize(batchSize));
    }

    /**
     * Find emails by user.
     */
    List<EmailQueue> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /**
     * Find emails by type and status.
     */
    List<EmailQueue> findByEmailTypeAndStatus(EmailType emailType, EmailStatus status);

    /**
     * Count pending emails.
     */
    long countByStatus(EmailStatus status);

    /**
     * Count emails by type sent today.
     */
    @Query("""
        SELECT COUNT(e) FROM EmailQueue e
        WHERE e.emailType = :emailType
        AND e.status = 'SENT'
        AND e.sentAt >= :startOfDay
        """)
    long countSentTodayByType(
            @Param("emailType") EmailType emailType,
            @Param("startOfDay") OffsetDateTime startOfDay);

    /**
     * Delete old sent emails (cleanup job).
     */
    @Modifying
    @Query("DELETE FROM EmailQueue e WHERE e.status = 'SENT' AND e.sentAt < :before")
    int deleteOldSentEmails(@Param("before") OffsetDateTime before);

    /**
     * Delete old failed emails (cleanup job).
     */
    @Modifying
    @Query("DELETE FROM EmailQueue e WHERE e.status = 'FAILED' AND e.updatedAt < :before")
    int deleteOldFailedEmails(@Param("before") OffsetDateTime before);

    /**
     * Check if a similar email was already queued recently (prevent duplicates).
     */
    @Query("""
        SELECT COUNT(e) > 0 FROM EmailQueue e
        WHERE e.recipientEmail = :email
        AND e.emailType = :emailType
        AND e.createdAt >= :since
        """)
    boolean existsRecentEmail(
            @Param("email") String recipientEmail,
            @Param("emailType") EmailType emailType,
            @Param("since") OffsetDateTime since);
}
