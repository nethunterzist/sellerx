package com.ecommerce.sellerx.expenses;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ExpenseCategoryMapper {
    
    ExpenseCategoryDto toDto(ExpenseCategory expenseCategory);
}
