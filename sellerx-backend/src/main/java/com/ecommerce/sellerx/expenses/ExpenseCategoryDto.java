package com.ecommerce.sellerx.expenses;

import java.time.LocalDateTime;
import java.util.UUID;

public record ExpenseCategoryDto(
    UUID id,
    String name,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
