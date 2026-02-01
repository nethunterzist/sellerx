package com.ecommerce.sellerx.support;

/**
 * Status of a support ticket.
 */
public enum TicketStatus {
    OPEN,           // Newly created, awaiting response
    IN_PROGRESS,    // Being worked on by support
    WAITING_CUSTOMER, // Waiting for customer response
    RESOLVED,       // Issue resolved, awaiting confirmation
    CLOSED          // Ticket closed
}
