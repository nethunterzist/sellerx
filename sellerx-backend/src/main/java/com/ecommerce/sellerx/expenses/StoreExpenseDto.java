package com.ecommerce.sellerx.expenses;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record StoreExpenseDto(
    UUID id,
    UUID expenseCategoryId,
    String expenseCategoryName,
    UUID storeId,
    UUID productId,
    String productTitle, // "Genel" if productId is null
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime date,
    ExpenseFrequency frequency,
    String frequencyDisplayName,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime endDate, // For recurring expenses
    Boolean isRecurringTemplate, // true = this is a template, false = this is an instance
    UUID parentExpenseId, // For instances: ID of the template this was generated from
    String name,
    BigDecimal amount,
    Integer vatRate,           // null = faturasız işlem
    BigDecimal vatAmount,
    Boolean isVatDeductible,
    BigDecimal netAmount,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime createdAt,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime updatedAt
) {}
