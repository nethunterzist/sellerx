package com.ecommerce.sellerx.support.dto;

import com.ecommerce.sellerx.support.TicketCategory;
import com.ecommerce.sellerx.support.TicketPriority;
import com.ecommerce.sellerx.support.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for support ticket data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketDto {

    private Long id;
    private String ticketNumber;
    private String subject;
    private TicketStatus status;
    private TicketPriority priority;
    private TicketCategory category;

    // User info
    private Long userId;
    private String userName;
    private String userEmail;

    // Store info (optional)
    private UUID storeId;
    private String storeName;

    // Assigned admin info (optional)
    private Long assignedToId;
    private String assignedToName;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime closedAt;

    // Message count for list views
    private Integer messageCount;

    // Messages for detail view
    private List<TicketMessageDto> messages;
}
