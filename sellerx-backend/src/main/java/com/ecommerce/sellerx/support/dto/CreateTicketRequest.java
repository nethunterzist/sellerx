package com.ecommerce.sellerx.support.dto;

import com.ecommerce.sellerx.support.TicketCategory;
import com.ecommerce.sellerx.support.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for creating a new support ticket.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTicketRequest {

    @NotBlank(message = "Konu boş olamaz")
    @Size(max = 255, message = "Konu 255 karakterden uzun olamaz")
    private String subject;

    @NotBlank(message = "Mesaj boş olamaz")
    private String message;

    @NotNull(message = "Kategori seçilmelidir")
    private TicketCategory category;

    @Builder.Default
    private TicketPriority priority = TicketPriority.MEDIUM;

    /**
     * Optional store ID if ticket is related to a specific store.
     */
    private UUID storeId;
}
