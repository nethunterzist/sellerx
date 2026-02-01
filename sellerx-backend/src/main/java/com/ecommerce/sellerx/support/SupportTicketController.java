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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for user support ticket operations.
 */
@RestController
@RequestMapping("/api/support/tickets")
@RequiredArgsConstructor
public class SupportTicketController {

    private final SupportTicketService ticketService;

    /**
     * Get current authenticated user's ID from security context.
     */
    private Long getCurrentUserId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    /**
     * Create a new support ticket.
     * POST /api/support/tickets
     */
    @PostMapping
    public ResponseEntity<TicketDto> createTicket(
            @Valid @RequestBody CreateTicketRequest request) {
        Long userId = getCurrentUserId();
        TicketDto ticket = ticketService.createTicket(userId, request);
        return ResponseEntity.ok(ticket);
    }

    /**
     * Get user's tickets.
     * GET /api/support/tickets
     */
    @GetMapping
    public ResponseEntity<Page<TicketDto>> getMyTickets(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        if (pageable.getPageSize() > 100) {
            pageable = PageRequest.of(pageable.getPageNumber(), 100, pageable.getSort());
        }
        Long userId = getCurrentUserId();
        Page<TicketDto> tickets = ticketService.getUserTickets(userId, pageable);
        return ResponseEntity.ok(tickets);
    }

    /**
     * Get ticket detail.
     * GET /api/support/tickets/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<TicketDto> getTicket(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        TicketDto ticket = ticketService.getTicketForUser(id, userId);
        return ResponseEntity.ok(ticket);
    }

    /**
     * Add message to ticket.
     * POST /api/support/tickets/{id}/messages
     */
    @PostMapping("/{id}/messages")
    public ResponseEntity<TicketMessageDto> addMessage(
            @PathVariable Long id,
            @Valid @RequestBody AddMessageRequest request) {
        Long userId = getCurrentUserId();
        TicketMessageDto message = ticketService.addUserMessage(id, userId, request);
        return ResponseEntity.ok(message);
    }
}
