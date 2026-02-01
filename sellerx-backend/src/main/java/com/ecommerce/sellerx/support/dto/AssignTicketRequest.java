package com.ecommerce.sellerx.support.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for assigning a ticket to an admin.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignTicketRequest {

    @NotNull(message = "Admin ID gereklidir")
    private Long adminId;
}
