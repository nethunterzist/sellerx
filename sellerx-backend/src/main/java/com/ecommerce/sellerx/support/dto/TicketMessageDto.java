package com.ecommerce.sellerx.support.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for ticket message data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketMessageDto {

    private Long id;
    private Long senderId;
    private String senderName;
    private String senderEmail;
    private String message;
    private Boolean isAdminReply;
    private LocalDateTime createdAt;
}
