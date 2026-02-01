package com.ecommerce.sellerx.ai;

import com.ecommerce.sellerx.ai.dto.AiSettingsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/ai-settings")
@RequiredArgsConstructor
public class AiSettingsController {

    private final AiSettingsService service;

    @GetMapping("/stores/{storeId}")
    public ResponseEntity<AiSettingsDto> getByStore(@PathVariable UUID storeId) {
        return ResponseEntity.ok(service.getByStoreId(storeId));
    }

    @PutMapping("/stores/{storeId}")
    public ResponseEntity<AiSettingsDto> update(
            @PathVariable UUID storeId,
            @RequestBody AiSettingsDto dto) {
        return ResponseEntity.ok(service.update(storeId, dto));
    }
}
