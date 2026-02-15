package com.ecommerce.sellerx.products;

import com.ecommerce.sellerx.products.dto.BuyboxInfoDto;
import com.ecommerce.sellerx.products.dto.BuyboxSummaryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/stores/{storeId}/buybox")
@RequiredArgsConstructor
@Slf4j
public class BuyboxController {

    private final BuyboxService buyboxService;

    @GetMapping
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    public ResponseEntity<Page<BuyboxInfoDto>> getBuyboxProducts(
            @PathVariable UUID storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "buyboxOrder") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        Page<BuyboxInfoDto> products = buyboxService.getBuyboxProducts(
                storeId, search, status, sortBy, sortDir, page, size);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/summary")
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    public ResponseEntity<BuyboxSummaryDto> getBuyboxSummary(@PathVariable UUID storeId) {
        BuyboxSummaryDto summary = buyboxService.getBuyboxSummary(storeId);
        return ResponseEntity.ok(summary);
    }

    @PostMapping("/sync")
    @PreAuthorize("@userSecurityRules.canAccessStore(authentication, #storeId)")
    public ResponseEntity<Void> syncBuybox(@PathVariable UUID storeId) {
        log.info("Manual buybox sync triggered for store: {}", storeId);
        buyboxService.syncBuyboxForStore(storeId);
        return ResponseEntity.accepted().build();
    }
}
