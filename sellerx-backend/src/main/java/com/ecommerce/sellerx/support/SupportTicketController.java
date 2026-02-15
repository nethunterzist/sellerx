package com.ecommerce.sellerx.support;

import com.ecommerce.sellerx.support.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

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

    // === Attachments ===

    /**
     * Get ticket attachments.
     * GET /api/support/tickets/{id}/attachments
     */
    @GetMapping("/{id}/attachments")
    public ResponseEntity<List<TicketAttachmentDto>> getAttachments(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        List<TicketAttachmentDto> attachments = ticketService.getAttachments(id, userId, false);
        return ResponseEntity.ok(attachments);
    }

    /**
     * Upload attachment to ticket.
     * POST /api/support/tickets/{id}/attachments
     */
    @PostMapping(value = "/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TicketAttachmentDto> uploadAttachment(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {
        Long userId = getCurrentUserId();
        TicketAttachmentDto dto = ticketService.addAttachment(
                id, userId, false,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                file.getBytes());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    /**
     * Download ticket attachment.
     * GET /api/support/tickets/{id}/attachments/{attachmentId}/download
     */
    @GetMapping("/{id}/attachments/{attachmentId}/download")
    public ResponseEntity<byte[]> downloadAttachment(
            @PathVariable Long id,
            @PathVariable Long attachmentId) {
        Long userId = getCurrentUserId();
        TicketAttachment attachment = ticketService.getAttachmentWithData(id, attachmentId, userId, false);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.getFileName() + "\"")
                .contentType(MediaType.parseMediaType(attachment.getFileType() != null ? attachment.getFileType() : "application/octet-stream"))
                .body(attachment.getFileData());
    }

    /**
     * Delete ticket attachment.
     * DELETE /api/support/tickets/{id}/attachments/{attachmentId}
     */
    @DeleteMapping("/{id}/attachments/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(
            @PathVariable Long id,
            @PathVariable Long attachmentId) {
        Long userId = getCurrentUserId();
        ticketService.deleteAttachment(id, attachmentId, userId, false);
        return ResponseEntity.noContent().build();
    }
}
