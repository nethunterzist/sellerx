package com.ecommerce.sellerx.expenses;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ExpenseCategoryMapper {

    default ExpenseCategoryDto toDto(ExpenseCategory category, long expenseCount) {
        return new ExpenseCategoryDto(
            category.getId(),
            category.getName(),
            category.getCreatedAt(),
            category.getUpdatedAt(),
            expenseCount
        );
    }
}
