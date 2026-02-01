package com.ecommerce.sellerx.support;

import com.ecommerce.sellerx.support.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for admin support ticket operations.
 */
@RestController
@RequestMapping("/api/admin/support/tickets")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminSupportController {

    private final SupportTicketService ticketService;

    /**
     * Get current authenticated user's ID from security context.
     */
    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    /**
     * Get all tickets with optional filters.
     * GET /api/admin/support/tickets
     */
    @GetMapping
    public ResponseEntity<Page<TicketDto>> getAllTickets(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) TicketCategory category,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        if (pageable.getPageSize() > 100) {
            pageable = PageRequest.of(pageable.getPageNumber(), 100, pageable.getSort());
        }
        Page<TicketDto> tickets = ticketService.getAllTickets(status, priority, category, pageable);
        return ResponseEntity.ok(tickets);
    }

    /**
     * Get active tickets (not closed).
     * GET /api/admin/support/tickets/active
     */
    @GetMapping("/active")
    public ResponseEntity<Page<TicketDto>> getActiveTickets(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        if (pageable.getPageSize() > 100) {
            pageable = PageRequest.of(pageable.getPageNumber(), 100, pageable.getSort());
        }
        Page<TicketDto> tickets = ticketService.getActiveTickets(pageable);
        return ResponseEntity.ok(tickets);
    }

    /**
     * Get ticket detail.
     * GET /api/admin/support/tickets/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<TicketDto> getTicket(@PathVariable Long id) {
        TicketDto ticket = ticketService.getTicketForAdmin(id);
        return ResponseEntity.ok(ticket);
    }

    /**
     * Add admin reply to ticket.
     * POST /api/admin/support/tickets/{id}/reply
     */
    @PostMapping("/{id}/reply")
    public ResponseEntity<TicketMessageDto> addReply(
            @PathVariable Long id,
            @Valid @RequestBody AddMessageRequest request) {
        Long adminId = getCurrentUserId();
        TicketMessageDto message = ticketService.addAdminReply(id, adminId, request);
        return ResponseEntity.ok(message);
    }

    /**
     * Update ticket status.
     * PUT /api/admin/support/tickets/{id}/status
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<TicketDto> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTicketStatusRequest request) {
        TicketDto ticket = ticketService.updateTicketStatus(id, request);
        return ResponseEntity.ok(ticket);
    }

    /**
     * Assign ticket to admin.
     * PUT /api/admin/support/tickets/{id}/assign
     */
    @PutMapping("/{id}/assign")
    public ResponseEntity<TicketDto> assignTicket(
            @PathVariable Long id,
            @Valid @RequestBody AssignTicketRequest request) {
        TicketDto ticket = ticketService.assignTicket(id, request);
        return ResponseEntity.ok(ticket);
    }

    /**
     * Search tickets.
     * GET /api/admin/support/tickets/search
     */
    @GetMapping("/search")
    public ResponseEntity<Page<TicketDto>> searchTickets(
            @RequestParam String q,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        if (pageable.getPageSize() > 100) {
            pageable = PageRequest.of(pageable.getPageNumber(), 100, pageable.getSort());
        }
        Page<TicketDto> tickets = ticketService.searchTickets(q, pageable);
        return ResponseEntity.ok(tickets);
    }

    /**
     * Get ticket statistics.
     * GET /api/admin/support/tickets/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<TicketStatsDto> getStats() {
        TicketStatsDto stats = ticketService.getTicketStats();
        return ResponseEntity.ok(stats);
    }
}
