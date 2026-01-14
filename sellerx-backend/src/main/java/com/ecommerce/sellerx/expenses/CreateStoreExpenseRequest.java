package com.ecommerce.sellerx.expenses;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CreateStoreExpenseRequest(
    @NotNull(message = "Expense category ID is required")
    UUID expenseCategoryId,
    
    UUID productId, // NULL = Genel kategori
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime date, // NULL = now()
    
    @NotNull(message = "Frequency is required")
    ExpenseFrequency frequency,
    
    @NotBlank(message = "Name is required")
    String name,
    
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    BigDecimal amount
) {}
