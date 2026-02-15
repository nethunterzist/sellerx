package com.ecommerce.sellerx.email.service;

import com.ecommerce.sellerx.email.EmailStatus;
import com.ecommerce.sellerx.email.EmailType;
import com.ecommerce.sellerx.email.entity.EmailQueue;
import com.ecommerce.sellerx.email.repository.EmailQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for managing email queue operations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailQueueService {

    private final EmailQueueRepository queueRepository;

    /**
     * Add email to queue for immediate sending.
     */
    @Transactional
    public EmailQueue enqueue(EmailType emailType, String recipientEmail, String recipientName,
                              Map<String, Object> variables) {
        return enqueue(emailType, recipientEmail, recipientName, variables, null, null);
    }

    /**
     * Add email to queue with optional scheduling.
     */
    @Transactional
    public EmailQueue enqueue(EmailType emailType, String recipientEmail, String recipientName,
                              Map<String, Object> variables, OffsetDateTime scheduledAt, UUID userId) {

        // Check for duplicate prevention (same email, same type in last hour)
        if (isDuplicateEmail(recipientEmail, emailType)) {
            log.warn("[EMAIL-QUEUE] Duplicate email prevented: type={}, to={}", emailType, recipientEmail);
            return null;
        }

        EmailQueue email = EmailQueue.builder()
                .emailType(emailType)
                .recipientEmail(recipientEmail)
                .recipientName(recipientName)
                .variables(variables != null ? variables : new HashMap<>())
                .status(EmailStatus.PENDING)
                .scheduledAt(scheduledAt)
                .userId(userId)
                .retryCount(0)
                .maxRetries(3)
                .build();

        EmailQueue saved = queueRepository.save(email);
        log.info("[EMAIL-QUEUE] Enqueued: id={}, type={}, to={}, scheduled={}",
                saved.getId(), emailType, recipientEmail, scheduledAt);

        return saved;
    }

    /**
     * Add email for a user (with user context).
     */
    @Transactional
    public EmailQueue enqueueForUser(EmailType emailType, String recipientEmail, String recipientName,
                                     Map<String, Object> variables, Long userId) {
        UUID userUuid = userId != null ? UUID.nameUUIDFromBytes(userId.toString().getBytes()) : null;
        return enqueue(emailType, recipientEmail, recipientName, variables, null, userUuid);
    }

    /**
     * Schedule email for later.
     */
    @Transactional
    public EmailQueue schedule(EmailType emailType, String recipientEmail, String recipientName,
                               Map<String, Object> variables, OffsetDateTime scheduledAt) {
        return enqueue(emailType, recipientEmail, recipientName, variables, scheduledAt, null);
    }

    /**
     * Mark email as sent.
     */
    @Transactional
    public void markSent(UUID emailId) {
        queueRepository.findById(emailId).ifPresent(email -> {
            email.markSent();
            queueRepository.save(email);
            log.info("[EMAIL-QUEUE] Marked sent: id={}", emailId);
        });
    }

    /**
     * Mark email as failed with error.
     */
    @Transactional
    public void markFailed(UUID emailId, String errorMessage) {
        queueRepository.findById(emailId).ifPresent(email -> {
            email.markFailed(errorMessage);
            queueRepository.save(email);
            log.warn("[EMAIL-QUEUE] Marked failed: id={}, retry={}/{}, error={}",
                    emailId, email.getRetryCount(), email.getMaxRetries(), errorMessage);
        });
    }

    /**
     * Get queue statistics.
     */
    public QueueStats getStats() {
        return new QueueStats(
                queueRepository.countByStatus(EmailStatus.PENDING),
                queueRepository.countByStatus(EmailStatus.SENDING),
                queueRepository.countByStatus(EmailStatus.SENT),
                queueRepository.countByStatus(EmailStatus.FAILED)
        );
    }

    /**
     * Check if duplicate email was recently sent.
     */
    private boolean isDuplicateEmail(String email, EmailType emailType) {
        // Prevent same email type to same address within 1 hour
        OffsetDateTime oneHourAgo = OffsetDateTime.now().minusHours(1);
        return queueRepository.existsRecentEmail(email, emailType, oneHourAgo);
    }

    /**
     * Queue statistics record.
     */
    public record QueueStats(long pending, long sending, long sent, long failed) {
        public long total() {
            return pending + sending + sent + failed;
        }
    }
}
