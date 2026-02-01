package com.ecommerce.sellerx.ai;

import com.ecommerce.sellerx.ai.dto.CreateKnowledgeRequest;
import com.ecommerce.sellerx.ai.dto.KnowledgeBaseDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeBaseController {

    private final KnowledgeBaseService service;

    @GetMapping("/stores/{storeId}")
    public ResponseEntity<List<KnowledgeBaseDto>> getByStore(@PathVariable UUID storeId) {
        return ResponseEntity.ok(service.getByStoreId(storeId));
    }

    @GetMapping("/stores/{storeId}/category/{category}")
    public ResponseEntity<List<KnowledgeBaseDto>> getByStoreAndCategory(
            @PathVariable UUID storeId,
            @PathVariable String category) {
        return ResponseEntity.ok(service.getByStoreIdAndCategory(storeId, category));
    }

    @PostMapping("/stores/{storeId}")
    public ResponseEntity<KnowledgeBaseDto> create(
            @PathVariable UUID storeId,
            @Valid @RequestBody CreateKnowledgeRequest request) {
        return ResponseEntity.ok(service.create(storeId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<KnowledgeBaseDto> update(
            @PathVariable UUID id,
            @Valid @RequestBody CreateKnowledgeRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<Void> toggleActive(
            @PathVariable UUID id,
            @RequestParam boolean active) {
        service.toggleActive(id, active);
        return ResponseEntity.ok().build();
    }
}
