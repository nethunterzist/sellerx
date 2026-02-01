package com.ecommerce.sellerx.support.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for ticket statistics.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketStatsDto {

    private long totalTickets;
    private long openTickets;
    private long inProgressTickets;
    private long waitingCustomerTickets;
    private long resolvedTickets;
    private long closedTickets;
}
