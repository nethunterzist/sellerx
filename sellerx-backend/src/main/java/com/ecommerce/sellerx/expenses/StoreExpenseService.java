package com.ecommerce.sellerx.expenses;

import com.ecommerce.sellerx.products.TrendyolProductRepository;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import com.ecommerce.sellerx.stores.StoreNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
public class StoreExpenseService {
    
    private final StoreExpenseRepository storeExpenseRepository;
    private final ExpenseCategoryRepository expenseCategoryRepository;
    private final StoreRepository storeRepository;
    private final TrendyolProductRepository productRepository;
    private final StoreExpenseMapper storeExpenseMapper;
    private final ExpenseCategoryMapper expenseCategoryMapper;
    
    public StoreExpensesResponse getExpensesByStore(UUID storeId) {
        // Store'un varlığını kontrol et
        storeRepository.findById(storeId)
            .orElseThrow(() -> new StoreNotFoundException("Store not found"));
        
        List<StoreExpense> expenses = storeExpenseRepository.findByStoreIdWithRelations(storeId);
        BigDecimal totalExpense = storeExpenseRepository.getTotalExpensesByStoreId(storeId);
        
        if (totalExpense == null) {
            totalExpense = BigDecimal.ZERO;
        }
        
        List<StoreExpenseDto> expenseDtos = expenses.stream()
            .map(storeExpenseMapper::toDto)
            .toList();
        
        return new StoreExpensesResponse(totalExpense, expenseDtos);
    }
    
    public List<ExpenseCategoryDto> getAllExpenseCategories() {
        return expenseCategoryRepository.findAllByOrderByNameAsc()
            .stream()
            .map(expenseCategoryMapper::toDto)
            .toList();
    }
    
    @Transactional
    public StoreExpenseDto createExpense(UUID storeId, CreateStoreExpenseRequest request) {
        // Store'un varlığını kontrol et
        Store store = storeRepository.findById(storeId)
            .orElseThrow(() -> new StoreNotFoundException("Store not found"));
        
        // Expense category'nin varlığını kontrol et
        var expenseCategory = expenseCategoryRepository.findById(request.expenseCategoryId())
            .orElseThrow(() -> new ExpenseCategoryNotFoundException("Expense category not found"));
        
        StoreExpense expense = storeExpenseMapper.toEntity(request);
        expense.setStore(store);
        expense.setExpenseCategory(expenseCategory);
        
        // Product varsa kontrol et ve set et
        if (request.productId() != null) {
            var product = productRepository.findById(request.productId())
                .orElseThrow(() -> new RuntimeException("Product not found"));
            
            // Product'ın bu store'a ait olduğunu kontrol et
            if (!product.getStore().getId().equals(storeId)) {
                throw new RuntimeException("Product does not belong to this store");
            }
            
            expense.setProduct(product);
        }
        
        // Date null ise şimdi olarak set et
        if (request.date() == null) {
            expense.setDate(LocalDateTime.now());
        }
        
        storeExpenseRepository.save(expense);
        return storeExpenseMapper.toDto(expense);
    }
    
    @Transactional
    public StoreExpenseDto updateExpense(UUID storeId, UUID expenseId, UpdateStoreExpenseRequest request) {
        // Store'un varlığını kontrol et
        storeRepository.findById(storeId)
            .orElseThrow(() -> new StoreNotFoundException("Store not found"));
        
        // Expense'in varlığını ve store'a ait olduğunu kontrol et
        StoreExpense expense = storeExpenseRepository.findById(expenseId)
            .orElseThrow(() -> new StoreExpenseNotFoundException("Store expense not found"));
        
        if (!expense.getStore().getId().equals(storeId)) {
            throw new RuntimeException("Expense does not belong to this store");
        }
        
        // Expense category'nin varlığını kontrol et
        var expenseCategory = expenseCategoryRepository.findById(request.expenseCategoryId())
            .orElseThrow(() -> new ExpenseCategoryNotFoundException("Expense category not found"));
        
        storeExpenseMapper.update(request, expense);
        expense.setExpenseCategory(expenseCategory);
        
        // Product varsa kontrol et ve set et
        if (request.productId() != null) {
            var product = productRepository.findById(request.productId())
                .orElseThrow(() -> new RuntimeException("Product not found"));
            
            // Product'ın bu store'a ait olduğunu kontrol et
            if (!product.getStore().getId().equals(storeId)) {
                throw new RuntimeException("Product does not belong to this store");
            }
            
            expense.setProduct(product);
        } else {
            expense.setProduct(null);
        }
        
        storeExpenseRepository.save(expense);
        return storeExpenseMapper.toDto(expense);
    }
    
    @Transactional
    public void deleteExpense(UUID storeId, UUID expenseId) {
        // Store'un varlığını kontrol et
        storeRepository.findById(storeId)
            .orElseThrow(() -> new StoreNotFoundException("Store not found"));
        
        // Expense'in varlığını ve store'a ait olduğunu kontrol et
        StoreExpense expense = storeExpenseRepository.findById(expenseId)
            .orElseThrow(() -> new StoreExpenseNotFoundException("Store expense not found"));
        
        if (!expense.getStore().getId().equals(storeId)) {
            throw new RuntimeException("Expense does not belong to this store");
        }
        
        storeExpenseRepository.delete(expense);
    }
}
