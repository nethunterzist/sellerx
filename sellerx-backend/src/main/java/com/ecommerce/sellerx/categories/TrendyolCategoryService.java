package com.ecommerce.sellerx.categories;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrendyolCategoryService {
    
    private final TrendyolCategoryRepository categoryRepository;
    private final TrendyolCategoryMapper categoryMapper;
    
    public CategoriesResponse getAllCategories() {
        log.info("Fetching all categories");
        List<TrendyolCategory> categories = categoryRepository.findAllOrderByCategoryName();
        List<TrendyolCategoryDto> categoryDtos = categoryMapper.toDtoList(categories);
        return new CategoriesResponse(categoryDtos);
    }
    
    @Transactional
    public String bulkInsertCategories(CategoryBulkInsertRequest request) {
        log.info("Starting bulk insert of {} categories", request.getResult().size());
        
        int insertedCount = 0;
        int skippedCount = 0;
        int batchSize = 50; // Reduced batch size for better performance
        
        List<CategoryBulkInsertRequest.CategoryInsertDto> categories = request.getResult();
        
        // Process in batches to avoid memory/transaction issues
        for (int i = 0; i < categories.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, categories.size());
            List<CategoryBulkInsertRequest.CategoryInsertDto> batch = categories.subList(i, endIndex);
            
            log.info("Processing batch {}/{}: items {} to {}", 
                    (i / batchSize) + 1, 
                    (categories.size() + batchSize - 1) / batchSize,
                    i + 1, 
                    endIndex);
            
            // Get existing category IDs for this batch
            List<Long> batchCategoryIds = batch.stream()
                    .map(CategoryBulkInsertRequest.CategoryInsertDto::getCategoryId)
                    .toList();
            
            List<Long> existingIds = categoryRepository.findExistingCategoryIds(batchCategoryIds);
            
            List<TrendyolCategory> categoriesToSave = new ArrayList<>();
            
            for (CategoryBulkInsertRequest.CategoryInsertDto insertDto : batch) {
                try {
                    // Check if category already exists
                    if (existingIds.contains(insertDto.getCategoryId())) {
                        log.debug("Category with ID {} already exists, skipping", insertDto.getCategoryId());
                        skippedCount++;
                        continue;
                    }
                    
                    // Convert to entity
                    TrendyolCategory category = categoryMapper.fromInsertDto(insertDto);
                    categoriesToSave.add(category);
                    
                } catch (Exception e) {
                    log.error("Error processing category with ID {}: {}", insertDto.getCategoryId(), e.getMessage());
                    skippedCount++;
                }
            }
            
            // Batch save
            if (!categoriesToSave.isEmpty()) {
                try {
                    categoryRepository.saveAll(categoriesToSave);
                    insertedCount += categoriesToSave.size();
                    log.info("Successfully saved batch of {} categories", categoriesToSave.size());
                } catch (Exception e) {
                    log.error("Error saving batch: {}", e.getMessage());
                    skippedCount += categoriesToSave.size();
                }
            }
            
            // Log progress every batch
            log.info("Progress: Inserted {} / Skipped {} so far", insertedCount, skippedCount);
        }
        
        log.info("Bulk insert completed. Total Inserted: {}, Total Skipped: {}", insertedCount, skippedCount);
        return String.format("Bulk insert completed. Inserted: %d, Skipped: %d", insertedCount, skippedCount);
    }
}
