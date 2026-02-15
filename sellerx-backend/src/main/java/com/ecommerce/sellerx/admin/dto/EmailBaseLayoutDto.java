package com.ecommerce.sellerx.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailBaseLayoutDto {

    private UUID id;
    private String headerHtml;
    private String footerHtml;
    private String styles;
    private String logoUrl;
    private String primaryColor;
    private OffsetDateTime updatedAt;
}
