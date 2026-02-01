package com.ecommerce.sellerx.support;

import org.springframework.context.annotation.Configuration;

/**
 * Security rules for support ticket endpoints.
 *
 * Endpoint security:
 * - /api/support/tickets/** : Authenticated users (their own tickets)
 * - /api/admin/support/tickets/** : ADMIN role only (@PreAuthorize)
 *
 * The endpoints are secured by:
 * 1. JWT authentication filter (all authenticated routes)
 * 2. @PreAuthorize("hasRole('ADMIN')") on AdminSupportController
 * 3. User ID validation in SupportTicketService for user endpoints
 */
@Configuration
public class SupportSecurityRules {

    /**
     * User endpoints (require authentication):
     * - POST   /api/support/tickets           : Create ticket
     * - GET    /api/support/tickets           : List user's tickets
     * - GET    /api/support/tickets/{id}      : Get ticket detail (own only)
     * - POST   /api/support/tickets/{id}/messages : Add message (own only)
     */

    /**
     * Admin endpoints (require ADMIN role):
     * - GET    /api/admin/support/tickets           : List all tickets
     * - GET    /api/admin/support/tickets/active    : List active tickets
     * - GET    /api/admin/support/tickets/{id}      : Get any ticket detail
     * - POST   /api/admin/support/tickets/{id}/reply: Reply to ticket
     * - PUT    /api/admin/support/tickets/{id}/status: Update status
     * - PUT    /api/admin/support/tickets/{id}/assign: Assign to admin
     * - GET    /api/admin/support/tickets/search    : Search tickets
     * - GET    /api/admin/support/tickets/stats     : Get statistics
     */
}
