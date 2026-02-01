package com.ecommerce.sellerx.qa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class AiApproveRequest {
    @NotNull
    private UUID logId;

    @NotBlank
    private String finalAnswer;
}
