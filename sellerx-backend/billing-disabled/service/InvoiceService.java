package com.ecommerce.sellerx.billing.service;

import com.ecommerce.sellerx.billing.*;
import com.ecommerce.sellerx.billing.config.SubscriptionConfig;
import com.ecommerce.sellerx.users.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Invoice management service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final SubscriptionConfig config;

    private static final DateTimeFormatter YEAR_FORMAT = DateTimeFormatter.ofPattern("yyyy");

    /**
     * Create an invoice for subscription payment
     */
    @Transactional
    public Invoice createInvoice(Subscription subscription, LocalDateTime periodStart, LocalDateTime periodEnd) {
        log.info("Creating invoice: subscriptionId={}, period={} to {}",
                subscription.getId(), periodStart, periodEnd);

        User user = subscription.getUser();
        SubscriptionPrice price = subscription.getPrice();
        SubscriptionPlan plan = subscription.getPlan();

        // Calculate amounts
        BigDecimal subtotal = price.getPriceAmount();
        BigDecimal taxRate = new BigDecimal(config.getInvoice().getTaxRate());
        BigDecimal taxAmount = subtotal.multiply(taxRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = subtotal.add(taxAmount);

        // Generate invoice number
        String invoiceNumber = generateInvoiceNumber();

        // Create line items
        List<Map<String, Object>> lineItems = new ArrayList<>();
        Map<String, Object> item = new HashMap<>();
        item.put("description", plan.getName() + " - " + subscription.getBillingCycle().getDisplayName());
        item.put("quantity", 1);
        item.put("unitPrice", subtotal);
        item.put("amount", subtotal);
        lineItems.add(item);

        Invoice invoice = Invoice.builder()
                .subscription(subscription)
                .user(user)
                .invoiceNumber(invoiceNumber)
                .invoiceSeries(config.getInvoice().getSeries())
                .status(InvoiceStatus.PENDING)
                .subtotal(subtotal)
                .taxRate(taxRate)
                .taxAmount(taxAmount)
                .totalAmount(totalAmount)
                .currency(price.getCurrency())
                .billingPeriodStart(periodStart)
                .billingPeriodEnd(periodEnd)
                .dueDate(periodStart.plusDays(7))
                .lineItems(lineItems)
                .build();

        invoice = invoiceRepository.save(invoice);
        log.info("Invoice created: {}", invoice.getInvoiceNumber());

        return invoice;
    }

    /**
     * Mark invoice as paid
     */
    @Transactional
    public Invoice markAsPaid(UUID invoiceId) {
        Invoice invoice = findById(invoiceId);
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setPaidAt(LocalDateTime.now());
        return invoiceRepository.save(invoice);
    }

    /**
     * Mark invoice as failed
     */
    @Transactional
    public Invoice markAsFailed(UUID invoiceId) {
        Invoice invoice = findById(invoiceId);
        invoice.setStatus(InvoiceStatus.FAILED);
        return invoiceRepository.save(invoice);
    }

    /**
     * Void an invoice
     */
    @Transactional
    public Invoice voidInvoice(UUID invoiceId) {
        Invoice invoice = findById(invoiceId);

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new IllegalStateException("Cannot void a paid invoice");
        }

        invoice.setStatus(InvoiceStatus.VOID);
        return invoiceRepository.save(invoice);
    }

    /**
     * Get invoice by ID
     */
    public Invoice findById(UUID id) {
        return invoiceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + id));
    }

    /**
     * Get invoices for user
     */
    public Page<Invoice> findByUserId(Long userId, Pageable pageable) {
        return invoiceRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * Get invoice for user (validates ownership)
     */
    public Optional<Invoice> findByIdAndUserId(UUID invoiceId, Long userId) {
        return invoiceRepository.findByIdAndUserId(invoiceId, userId);
    }

    /**
     * Find overdue invoices
     */
    public List<Invoice> findOverdueInvoices() {
        return invoiceRepository.findOverdueInvoices(LocalDateTime.now());
    }

    /**
     * Check if invoice exists for subscription and period
     */
    public Optional<Invoice> findBySubscriptionAndPeriod(UUID subscriptionId, LocalDateTime periodStart) {
        return invoiceRepository.findBySubscriptionAndPeriod(subscriptionId, periodStart);
    }

    /**
     * Generate invoice number: INV-2026-000001
     */
    private String generateInvoiceNumber() {
        String year = LocalDateTime.now().format(YEAR_FORMAT);
        String prefix = "INV-" + year + "-";

        Integer maxNumber = invoiceRepository.findMaxInvoiceNumberByPrefix(prefix);
        int nextNumber = (maxNumber != null ? maxNumber : 0) + 1;

        return prefix + String.format("%06d", nextNumber);
    }
}
