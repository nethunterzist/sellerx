package com.ecommerce.sellerx.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailBaseLayoutUpdateRequest {

    @NotBlank(message = "Header HTML is required")
    private String headerHtml;

    @NotBlank(message = "Footer HTML is required")
    private String footerHtml;

    private String styles;

    private String logoUrl;

    @NotBlank(message = "Primary color is required")
    private String primaryColor;
}
