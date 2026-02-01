package com.ecommerce.sellerx.support;

import com.ecommerce.sellerx.email.EmailService;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import com.ecommerce.sellerx.support.dto.*;
import com.ecommerce.sellerx.users.Role;
import com.ecommerce.sellerx.users.User;
import com.ecommerce.sellerx.users.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing support tickets.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SupportTicketService {

    private final SupportTicketRepository ticketRepository;
    private final TicketMessageRepository messageRepository;
    private final StoreRepository storeRepository;
    private final UserService userService;
    private final EmailService emailService;

    // === User Operations ===

    /**
     * Create a new support ticket.
     */
    @Transactional
    public TicketDto createTicket(Long userId, CreateTicketRequest request) {
        User user = userService.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Kullanıcı bulunamadı"));

        Store store = null;
        if (request.getStoreId() != null) {
            store = storeRepository.findById(request.getStoreId())
                    .orElseThrow(() -> new EntityNotFoundException("Mağaza bulunamadı"));
        }

        // Generate ticket number
        String ticketNumber = generateTicketNumber();

        // Create ticket
        SupportTicket ticket = SupportTicket.builder()
                .user(user)
                .store(store)
                .ticketNumber(ticketNumber)
                .subject(request.getSubject())
                .category(request.getCategory())
                .priority(request.getPriority())
                .status(TicketStatus.OPEN)
                .build();

        // Add initial message
        TicketMessage initialMessage = TicketMessage.builder()
                .ticket(ticket)
                .sender(user)
                .message(request.getMessage())
                .isAdminReply(false)
                .build();
        ticket.addMessage(initialMessage);

        ticket = ticketRepository.save(ticket);
        log.info("Created support ticket {} for user {}", ticketNumber, user.getEmail());

        // Send email notification
        emailService.sendTicketCreatedEmail(user.getEmail(), ticketNumber, request.getSubject());

        // Notify admins
        notifyAdminsNewTicket(ticket);

        return toDto(ticket, true);
    }

    /**
     * Get user's tickets.
     */
    public Page<TicketDto> getUserTickets(Long userId, Pageable pageable) {
        return ticketRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(ticket -> toDto(ticket, false));
    }

    /**
     * Get ticket detail for user (validates ownership).
     */
    public TicketDto getTicketForUser(Long ticketId, Long userId) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Destek talebi bulunamadı"));

        if (!ticket.getUser().getId().equals(userId)) {
            throw new SecurityException("Bu destek talebine erişim yetkiniz yok");
        }

        return toDto(ticket, true);
    }

    /**
     * Add message to ticket (user).
     */
    @Transactional
    public TicketMessageDto addUserMessage(Long ticketId, Long userId, AddMessageRequest request) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Destek talebi bulunamadı"));

        if (!ticket.getUser().getId().equals(userId)) {
            throw new SecurityException("Bu destek talebine erişim yetkiniz yok");
        }

        User user = userService.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Kullanıcı bulunamadı"));

        TicketMessage message = TicketMessage.builder()
                .ticket(ticket)
                .sender(user)
                .message(request.getMessage())
                .isAdminReply(false)
                .build();

        message = messageRepository.save(message);

        // Update ticket status if it was waiting for customer
        if (ticket.getStatus() == TicketStatus.WAITING_CUSTOMER) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
            ticketRepository.save(ticket);
        }

        log.info("User {} added message to ticket {}", user.getEmail(), ticket.getTicketNumber());

        // Notify assigned admin if any
        if (ticket.getAssignedTo() != null) {
            emailService.sendTicketReplyEmail(
                    ticket.getAssignedTo().getEmail(),
                    ticket.getTicketNumber(),
                    user.getName(),
                    truncateMessage(request.getMessage())
            );
        }

        return toMessageDto(message);
    }

    // === Admin Operations ===

    /**
     * Get all tickets (admin).
     */
    public Page<TicketDto> getAllTickets(TicketStatus status, TicketPriority priority,
                                          TicketCategory category, Pageable pageable) {
        return ticketRepository.findWithFilters(status, priority, category, pageable)
                .map(ticket -> toDto(ticket, false));
    }

    /**
     * Get active tickets (not closed).
     */
    public Page<TicketDto> getActiveTickets(Pageable pageable) {
        return ticketRepository.findActiveTickets(pageable)
                .map(ticket -> toDto(ticket, false));
    }

    /**
     * Get ticket detail (admin).
     */
    public TicketDto getTicketForAdmin(Long ticketId) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Destek talebi bulunamadı"));
        return toDto(ticket, true);
    }

    /**
     * Add admin reply to ticket.
     */
    @Transactional
    public TicketMessageDto addAdminReply(Long ticketId, Long adminId, AddMessageRequest request) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Destek talebi bulunamadı"));

        User admin = userService.findById(adminId)
                .orElseThrow(() -> new EntityNotFoundException("Admin kullanıcı bulunamadı"));

        TicketMessage message = TicketMessage.builder()
                .ticket(ticket)
                .sender(admin)
                .message(request.getMessage())
                .isAdminReply(true)
                .build();

        message = messageRepository.save(message);

        // Update ticket status
        ticket.setStatus(TicketStatus.WAITING_CUSTOMER);
        ticketRepository.save(ticket);

        log.info("Admin {} replied to ticket {}", admin.getEmail(), ticket.getTicketNumber());

        // Send email to user
        emailService.sendTicketReplyEmail(
                ticket.getUser().getEmail(),
                ticket.getTicketNumber(),
                admin.getName(),
                truncateMessage(request.getMessage())
        );

        return toMessageDto(message);
    }

    /**
     * Update ticket status.
     */
    @Transactional
    public TicketDto updateTicketStatus(Long ticketId, UpdateTicketStatusRequest request) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Destek talebi bulunamadı"));

        TicketStatus oldStatus = ticket.getStatus();
        ticket.setStatus(request.getStatus());

        if (request.getStatus() == TicketStatus.CLOSED) {
            ticket.close();
            // Send closed notification
            emailService.sendTicketClosedEmail(ticket.getUser().getEmail(), ticket.getTicketNumber());
        }

        ticket = ticketRepository.save(ticket);
        log.info("Ticket {} status changed from {} to {}",
                ticket.getTicketNumber(), oldStatus, request.getStatus());

        return toDto(ticket, false);
    }

    /**
     * Assign ticket to admin.
     */
    @Transactional
    public TicketDto assignTicket(Long ticketId, AssignTicketRequest request) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new EntityNotFoundException("Destek talebi bulunamadı"));

        User admin = userService.findById(request.getAdminId())
                .orElseThrow(() -> new EntityNotFoundException("Admin kullanıcı bulunamadı"));
        if (admin.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("Sadece admin kullanıcılara atama yapılabilir");
        }

        ticket.setAssignedTo(admin);
        if (ticket.getStatus() == TicketStatus.OPEN) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
        }

        ticket = ticketRepository.save(ticket);
        log.info("Ticket {} assigned to admin {}", ticket.getTicketNumber(), admin.getEmail());

        // Notify admin
        emailService.sendTicketAssignedEmail(
                admin.getEmail(),
                ticket.getTicketNumber(),
                ticket.getSubject(),
                ticket.getUser().getName()
        );

        return toDto(ticket, false);
    }

    /**
     * Search tickets.
     */
    public Page<TicketDto> searchTickets(String query, Pageable pageable) {
        return ticketRepository.searchTickets(query, pageable)
                .map(ticket -> toDto(ticket, false));
    }

    /**
     * Get ticket statistics.
     */
    public TicketStatsDto getTicketStats() {
        return TicketStatsDto.builder()
                .totalTickets(ticketRepository.count())
                .openTickets(ticketRepository.countByStatus(TicketStatus.OPEN))
                .inProgressTickets(ticketRepository.countByStatus(TicketStatus.IN_PROGRESS))
                .waitingCustomerTickets(ticketRepository.countByStatus(TicketStatus.WAITING_CUSTOMER))
                .resolvedTickets(ticketRepository.countByStatus(TicketStatus.RESOLVED))
                .closedTickets(ticketRepository.countByStatus(TicketStatus.CLOSED))
                .build();
    }

    // === Helper Methods ===

    private String generateTicketNumber() {
        String datePrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int sequence = ticketRepository.getNextTicketSequence();
        return String.format("TKT-%s-%03d", datePrefix, sequence);
    }

    private void notifyAdminsNewTicket(SupportTicket ticket) {
        // For now, we'll just log. In production, you'd get admin emails from config or database
        log.info("New ticket {} created. Admin notification would be sent here.", ticket.getTicketNumber());
        // TODO: Get admin emails and send notifications
    }

    private String truncateMessage(String message) {
        if (message == null) return "";
        return message.length() > 100 ? message.substring(0, 100) + "..." : message;
    }

    private TicketDto toDto(SupportTicket ticket, boolean includeMessages) {
        TicketDto.TicketDtoBuilder builder = TicketDto.builder()
                .id(ticket.getId())
                .ticketNumber(ticket.getTicketNumber())
                .subject(ticket.getSubject())
                .status(ticket.getStatus())
                .priority(ticket.getPriority())
                .category(ticket.getCategory())
                .userId(ticket.getUser().getId())
                .userName(ticket.getUser().getName())
                .userEmail(ticket.getUser().getEmail())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .closedAt(ticket.getClosedAt())
                .messageCount(ticket.getMessages().size());

        if (ticket.getStore() != null) {
            builder.storeId(ticket.getStore().getId())
                   .storeName(ticket.getStore().getStoreName());
        }

        if (ticket.getAssignedTo() != null) {
            builder.assignedToId(ticket.getAssignedTo().getId())
                   .assignedToName(ticket.getAssignedTo().getName());
        }

        if (includeMessages) {
            List<TicketMessageDto> messages = ticket.getMessages().stream()
                    .map(this::toMessageDto)
                    .collect(Collectors.toList());
            builder.messages(messages);
        }

        return builder.build();
    }

    private TicketMessageDto toMessageDto(TicketMessage message) {
        return TicketMessageDto.builder()
                .id(message.getId())
                .senderId(message.getSender().getId())
                .senderName(message.getSender().getName())
                .senderEmail(message.getSender().getEmail())
                .message(message.getMessage())
                .isAdminReply(message.getIsAdminReply())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
