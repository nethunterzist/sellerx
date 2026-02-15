package com.ecommerce.sellerx.email.scheduler;

import com.ecommerce.sellerx.email.EmailService;
import com.ecommerce.sellerx.email.EmailStatus;
import com.ecommerce.sellerx.email.SmtpEmailConfig;
import com.ecommerce.sellerx.email.entity.EmailQueue;
import com.ecommerce.sellerx.email.repository.EmailQueueRepository;
import com.ecommerce.sellerx.email.service.EmailQueueService;
import com.ecommerce.sellerx.email.service.EmailTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Scheduled processor for email queue.
 * Processes pending emails and handles retries.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailQueueProcessor {

    private final EmailQueueRepository queueRepository;
    private final EmailQueueService queueService;
    private final EmailTemplateService templateService;
    private final EmailService emailService;
    private final SmtpEmailConfig emailConfig;

    /**
     * Process email queue every minute.
     * Uses ShedLock to prevent concurrent execution in clustered environments.
     */
    @Scheduled(fixedRate = 60000) // Every 1 minute
    @SchedulerLock(name = "emailQueueProcessor", lockAtMostFor = "5m", lockAtLeastFor = "30s")
    @Transactional
    public void processQueue() {
        if (!emailConfig.isEnabled()) {
            return;
        }

        int batchSize = emailConfig.getQueue().getBatchSize();
        List<EmailQueue> pendingEmails = queueRepository.findEmailsToProcess(batchSize);

        if (pendingEmails.isEmpty()) {
            return;
        }

        log.info("[EMAIL-PROCESSOR] Processing {} emails", pendingEmails.size());

        int sent = 0;
        int failed = 0;

        for (EmailQueue email : pendingEmails) {
            try {
                processEmail(email);
                sent++;
            } catch (Exception e) {
                log.error("[EMAIL-PROCESSOR] Failed to process email id={}: {}",
                        email.getId(), e.getMessage());
                failed++;
            }
        }

        log.info("[EMAIL-PROCESSOR] Batch complete: sent={}, failed={}", sent, failed);
    }

    /**
     * Process single email.
     */
    private void processEmail(EmailQueue email) {
        // Mark as sending
        email.setStatus(EmailStatus.SENDING);
        queueRepository.save(email);

        try {
            // Render template
            EmailTemplateService.RenderedEmail rendered = templateService.render(
                    email.getEmailType(),
                    email.getVariables()
            );

            // Use pre-rendered subject/body if available, otherwise use template
            String subject = email.getSubject() != null ? email.getSubject() : rendered.subject();
            String body = email.getBody() != null ? email.getBody() : rendered.body();

            // Send email
            emailService.sendEmail(email.getRecipientEmail(), subject, body);

            // Mark as sent
            queueService.markSent(email.getId());

        } catch (Exception e) {
            // Mark as failed (will retry if under max retries)
            queueService.markFailed(email.getId(), e.getMessage());
            throw e;
        }
    }

    /**
     * Cleanup old emails (runs daily at 3 AM).
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @SchedulerLock(name = "emailQueueCleanup", lockAtMostFor = "30m", lockAtLeastFor = "5m")
    @Transactional
    public void cleanupOldEmails() {
        int cleanupDays = emailConfig.getQueue().getCleanupDays();
        OffsetDateTime cutoff = OffsetDateTime.now().minusDays(cleanupDays);

        int deletedSent = queueRepository.deleteOldSentEmails(cutoff);
        int deletedFailed = queueRepository.deleteOldFailedEmails(cutoff);

        if (deletedSent > 0 || deletedFailed > 0) {
            log.info("[EMAIL-CLEANUP] Deleted {} sent and {} failed emails older than {} days",
                    deletedSent, deletedFailed, cleanupDays);
        }
    }
}
