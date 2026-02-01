package com.ecommerce.sellerx.support.dto;

import com.ecommerce.sellerx.support.TicketStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating ticket status (admin only).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTicketStatusRequest {

    @NotNull(message = "Durum seçilmelidir")
    private TicketStatus status;
}
