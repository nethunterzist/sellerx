package com.ecommerce.sellerx.support.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for adding a message to a ticket.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddMessageRequest {

    @NotBlank(message = "Mesaj boş olamaz")
    private String message;
}
