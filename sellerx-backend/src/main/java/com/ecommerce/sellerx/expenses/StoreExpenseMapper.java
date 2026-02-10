package com.ecommerce.sellerx.expenses;

import org.mapstruct.*;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface StoreExpenseMapper {
    
    @Mapping(target = "expenseCategoryId", source = "expenseCategory.id")
    @Mapping(target = "expenseCategoryName", source = "expenseCategory.name")
    @Mapping(target = "storeId", source = "store.id")
    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productTitle", expression = "java(mapProductTitle(storeExpense.getProduct()))")
    @Mapping(target = "frequencyDisplayName", source = "frequency", qualifiedByName = "mapFrequencyDisplayName")
    @Mapping(target = "parentExpenseId", source = "parentExpense.id")
    StoreExpenseDto toDto(StoreExpense storeExpense);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "expenseCategory", ignore = true)
    @Mapping(target = "store", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "vatRate", ignore = true)
    @Mapping(target = "vatAmount", ignore = true)
    @Mapping(target = "isVatDeductible", ignore = true)
    @Mapping(target = "netAmount", ignore = true)
    @Mapping(target = "isRecurringTemplate", ignore = true)
    @Mapping(target = "parentExpense", ignore = true)
    @Mapping(target = "lastGeneratedDate", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    StoreExpense toEntity(CreateStoreExpenseRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "expenseCategory", ignore = true)
    @Mapping(target = "store", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "vatRate", ignore = true)
    @Mapping(target = "vatAmount", ignore = true)
    @Mapping(target = "isVatDeductible", ignore = true)
    @Mapping(target = "netAmount", ignore = true)
    @Mapping(target = "isRecurringTemplate", ignore = true)
    @Mapping(target = "parentExpense", ignore = true)
    @Mapping(target = "lastGeneratedDate", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void update(UpdateStoreExpenseRequest request, @MappingTarget StoreExpense storeExpense);
    
    default String mapProductTitle(com.ecommerce.sellerx.products.TrendyolProduct product) {
        return product != null ? product.getTitle() : "Genel";
    }
    
    @Named("mapFrequencyDisplayName")
    default String mapFrequencyDisplayName(ExpenseFrequency frequency) {
        return frequency != null ? frequency.getDisplayName() : null;
    }
}
