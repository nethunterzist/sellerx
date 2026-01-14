package com.ecommerce.sellerx.categories;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class TrendyolCategoryController {
    
    private final TrendyolCategoryService categoryService;
    
    @GetMapping
    public ResponseEntity<CategoriesResponse> getAllCategories() {
        log.info("GET /api/categories - Get all categories");
        CategoriesResponse response = categoryService.getAllCategories();
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/bulk-insert")
    public ResponseEntity<String> bulkInsertCategories(@RequestBody CategoryBulkInsertRequest request) {
        log.info("POST /api/categories/bulk-insert - Bulk insert {} categories", 
                request.getResult() != null ? request.getResult().size() : 0);
        
        try {
            String result = categoryService.bulkInsertCategories(request);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error during bulk insert: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body("Error during bulk insert: " + e.getMessage());
        }
    }
}
