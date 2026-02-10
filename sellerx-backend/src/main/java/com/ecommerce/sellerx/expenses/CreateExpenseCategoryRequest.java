package com.ecommerce.sellerx.expenses;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateExpenseCategoryRequest(
    @NotBlank(message = "Kategori adı boş olamaz")
    @Size(max = 255, message = "Kategori adı en fazla 255 karakter olabilir")
    String name
) {}
