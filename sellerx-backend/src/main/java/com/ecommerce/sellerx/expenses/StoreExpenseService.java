package com.ecommerce.sellerx.expenses;

import com.ecommerce.sellerx.common.exception.ResourceNotFoundException;
import com.ecommerce.sellerx.common.exception.UnauthorizedAccessException;
import com.ecommerce.sellerx.products.TrendyolProductRepository;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import com.ecommerce.sellerx.stores.StoreNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
public class StoreExpenseService {

    private final StoreExpenseRepository storeExpenseRepository;
    private final ExpenseCategoryRepository expenseCategoryRepository;
    private final StoreRepository storeRepository;
    private final TrendyolProductRepository productRepository;
    private final StoreExpenseMapper storeExpenseMapper;
    private final ExpenseCategoryMapper expenseCategoryMapper;
    
    public StoreExpensesResponse getExpensesByStore(UUID storeId) {
        return getExpensesByStore(storeId, null, null);
    }

    @Transactional
    public StoreExpensesResponse getExpensesByStore(UUID storeId, LocalDateTime startDate, LocalDateTime endDate) {
        // Store'un varlığını kontrol et
        storeRepository.findById(storeId)
            .orElseThrow(() -> new StoreNotFoundException("Store not found"));

        List<StoreExpense> expenses;
        BigDecimal totalExpense;

        // Tarih filtresi varsa uygula
        if (startDate != null && endDate != null) {
            // Önce bu aralıkta olması gereken tekrarlayan giderleri oluştur
            generateMissingRecurringInstances(storeId, startDate.toLocalDate(), endDate.toLocalDate());

            expenses = storeExpenseRepository.findByStoreIdAndDateBetween(storeId, startDate, endDate);
            // Filtrelenmiş giderlerin toplamını hesapla
            totalExpense = expenses.stream()
                .map(StoreExpense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        } else {
            expenses = storeExpenseRepository.findByStoreIdWithRelations(storeId);
            totalExpense = storeExpenseRepository.getTotalExpensesByStoreId(storeId);
        }

        if (totalExpense == null) {
            totalExpense = BigDecimal.ZERO;
        }

        List<StoreExpenseDto> expenseDtos = expenses.stream()
            .map(storeExpenseMapper::toDto)
            .toList();

        return new StoreExpensesResponse(totalExpense, expenseDtos);
    }

    /**
     * Generate missing recurring expense instances for a date range.
     * This ensures that when filtering by date, all expected recurring expenses are present.
     */
    private void generateMissingRecurringInstances(UUID storeId, LocalDate startDate, LocalDate endDate) {
        // Get all active recurring templates for this store
        List<StoreExpense> templates = storeExpenseRepository.findActiveRecurringTemplatesByStoreId(storeId, endDate);

        if (templates.isEmpty()) {
            return;
        }

        int generated = 0;
        LocalDate today = LocalDate.now();

        for (StoreExpense template : templates) {
            LocalDate templateStartDate = template.getDate().toLocalDate();
            LocalDate effectiveStartDate = startDate.isBefore(templateStartDate) ? templateStartDate : startDate;
            LocalDate effectiveEndDate = endDate.isAfter(today) ? today : endDate;

            // Generate instances for each day in the range that matches the frequency
            LocalDate currentDate = effectiveStartDate;
            while (!currentDate.isAfter(effectiveEndDate)) {
                if (shouldGenerateForDate(template, currentDate)) {
                    // Check if instance already exists
                    if (!storeExpenseRepository.existsInstanceForDate(template.getId(), currentDate)) {
                        createInstance(template, currentDate);
                        generated++;
                    }
                }
                currentDate = currentDate.plusDays(1);
            }

            // Update lastGeneratedDate if we generated anything up to today
            if (generated > 0 && !effectiveEndDate.isAfter(today)) {
                template.setLastGeneratedDate(effectiveEndDate);
                storeExpenseRepository.save(template);
            }
        }

        if (generated > 0) {
            log.info("[RecurringExpense] Backfilled {} instances for store {} in range {} to {}",
                    generated, storeId, startDate, endDate);
        }
    }

    private boolean shouldGenerateForDate(StoreExpense template, LocalDate date) {
        LocalDate templateStartDate = template.getDate().toLocalDate();

        // Don't generate before start date
        if (date.isBefore(templateStartDate)) {
            return false;
        }

        // Don't generate after end date
        if (template.getEndDate() != null && date.isAfter(template.getEndDate().toLocalDate())) {
            return false;
        }

        // Check frequency-specific rules
        return switch (template.getFrequency()) {
            case DAILY -> true;
            case WEEKLY -> templateStartDate.getDayOfWeek() == date.getDayOfWeek();
            case MONTHLY -> {
                int templateDay = templateStartDate.getDayOfMonth();
                int currentDay = date.getDayOfMonth();
                int lastDayOfMonth = date.lengthOfMonth();

                // If template day is beyond month's length, use last day
                if (templateDay > lastDayOfMonth) {
                    yield currentDay == lastDayOfMonth;
                }
                yield templateDay == currentDay;
            }
            case YEARLY -> templateStartDate.getMonth() == date.getMonth() &&
                    templateStartDate.getDayOfMonth() == date.getDayOfMonth();
            case ONE_TIME -> false;
        };
    }

    private void createInstance(StoreExpense template, LocalDate date) {
        StoreExpense instance = new StoreExpense();
        instance.setStore(template.getStore());
        instance.setName(template.getName());
        instance.setAmount(template.getAmount());
        instance.setExpenseCategory(template.getExpenseCategory());
        instance.setProduct(template.getProduct());
        instance.setFrequency(template.getFrequency()); // Keep original frequency from template
        instance.setDate(date.atStartOfDay());
        instance.setVatRate(template.getVatRate());
        instance.setIsVatDeductible(template.getIsVatDeductible());
        instance.setIsRecurringTemplate(false);
        instance.setParentExpense(template);
        instance.calculateVatFields();

        storeExpenseRepository.save(instance);
    }
    
    @Deprecated // Use store-specific method instead
    public List<ExpenseCategoryDto> getAllExpenseCategories() {
        return expenseCategoryRepository.findAllByOrderByNameAsc()
            .stream()
            .map(cat -> expenseCategoryMapper.toDto(cat, 0))
            .toList();
    }

    public List<ExpenseCategoryDto> getExpenseCategoriesByStore(UUID storeId) {
        storeRepository.findById(storeId)
            .orElseThrow(() -> new StoreNotFoundException("Store not found"));

        return expenseCategoryRepository.findByStoreIdOrderByNameAsc(storeId)
            .stream()
            .map(cat -> {
                long expenseCount = expenseCategoryRepository.countExpensesByCategoryId(cat.getId());
                return expenseCategoryMapper.toDto(cat, expenseCount);
            })
            .toList();
    }

    @Transactional
    public ExpenseCategoryDto createExpenseCategory(UUID storeId, CreateExpenseCategoryRequest request) {
        Store store = storeRepository.findById(storeId)
            .orElseThrow(() -> new StoreNotFoundException("Store not found"));

        // Check for duplicate name in same store
        if (expenseCategoryRepository.existsByStoreIdAndName(storeId, request.name())) {
            throw new ExpenseCategoryDuplicateException("Bu isimde bir kategori zaten mevcut: " + request.name());
        }

        ExpenseCategory category = new ExpenseCategory();
        category.setStore(store);
        category.setName(request.name());
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());

        expenseCategoryRepository.save(category);
        return expenseCategoryMapper.toDto(category, 0);
    }

    @Transactional
    public ExpenseCategoryDto updateExpenseCategory(UUID storeId, UUID categoryId, UpdateExpenseCategoryRequest request) {
        storeRepository.findById(storeId)
            .orElseThrow(() -> new StoreNotFoundException("Store not found"));

        ExpenseCategory category = expenseCategoryRepository.findById(categoryId)
            .orElseThrow(() -> new ExpenseCategoryNotFoundException("Expense category not found"));

        // Verify the category belongs to this store
        if (!category.getStore().getId().equals(storeId)) {
            throw new ExpenseCategoryNotFoundException("Expense category not found");
        }

        // Check for duplicate name in same store (excluding current category)
        if (!category.getName().equals(request.name()) &&
            expenseCategoryRepository.existsByStoreIdAndName(storeId, request.name())) {
            throw new ExpenseCategoryDuplicateException("Bu isimde bir kategori zaten mevcut: " + request.name());
        }

        category.setName(request.name());
        category.setUpdatedAt(LocalDateTime.now());

        expenseCategoryRepository.save(category);

        long expenseCount = expenseCategoryRepository.countExpensesByCategoryId(categoryId);
        return expenseCategoryMapper.toDto(category, expenseCount);
    }

    @Transactional
    public void deleteExpenseCategory(UUID storeId, UUID categoryId) {
        storeRepository.findById(storeId)
            .orElseThrow(() -> new StoreNotFoundException("Store not found"));

        ExpenseCategory category = expenseCategoryRepository.findById(categoryId)
            .orElseThrow(() -> new ExpenseCategoryNotFoundException("Expense category not found"));

        // Verify the category belongs to this store
        if (!category.getStore().getId().equals(storeId)) {
            throw new ExpenseCategoryNotFoundException("Expense category not found");
        }

        // Check if category is in use
        long expenseCount = expenseCategoryRepository.countExpensesByCategoryId(categoryId);
        if (expenseCount > 0) {
            throw new ExpenseCategoryInUseException(
                "Bu kategori silinemez. Bu kategoriye ait " + expenseCount + " adet gider kaydı bulunmaktadır.",
                expenseCount
            );
        }

        expenseCategoryRepository.delete(category);
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
                .orElseThrow(() -> new ResourceNotFoundException("Product", request.productId().toString()));

            // Product'ın bu store'a ait olduğunu kontrol et
            if (!product.getStore().getId().equals(storeId)) {
                throw new UnauthorizedAccessException("Product", request.productId().toString());
            }

            expense.setProduct(product);
        }

        // Date null ise şimdi olarak set et
        if (request.date() == null) {
            expense.setDate(LocalDateTime.now());
        }

        // KDV hesaplama
        expense.setVatRate(request.vatRate());
        expense.calculateVatFields();

        // Recurring expense logic
        if (request.frequency() != ExpenseFrequency.ONE_TIME) {
            expense.setIsRecurringTemplate(true);
            expense.setEndDate(request.endDate());

            // If start date is today, mark as already generated for today
            if (expense.getDate().toLocalDate().equals(LocalDate.now())) {
                expense.setLastGeneratedDate(LocalDate.now());
            }
        } else {
            expense.setIsRecurringTemplate(false);
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
            throw new UnauthorizedAccessException("Expense", expenseId.toString());
        }

        // Expense category'nin varlığını kontrol et
        var expenseCategory = expenseCategoryRepository.findById(request.expenseCategoryId())
            .orElseThrow(() -> new ExpenseCategoryNotFoundException("Expense category not found"));
        
        storeExpenseMapper.update(request, expense);
        expense.setExpenseCategory(expenseCategory);

        // KDV hesaplama
        expense.setVatRate(request.vatRate());
        expense.calculateVatFields();

        // Product varsa kontrol et ve set et
        if (request.productId() != null) {
            var product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", request.productId().toString()));

            // Product'ın bu store'a ait olduğunu kontrol et
            if (!product.getStore().getId().equals(storeId)) {
                throw new UnauthorizedAccessException("Product", request.productId().toString());
            }

            expense.setProduct(product);
        } else {
            expense.setProduct(null);
        }

        // Update recurring expense settings
        if (request.frequency() != ExpenseFrequency.ONE_TIME) {
            expense.setIsRecurringTemplate(true);
            expense.setEndDate(request.endDate());
        } else {
            expense.setIsRecurringTemplate(false);
            expense.setEndDate(null);
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
            throw new UnauthorizedAccessException("Expense", expenseId.toString());
        }

        storeExpenseRepository.delete(expense);
    }
}
