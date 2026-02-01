package com.ecommerce.sellerx.ai;

import com.ecommerce.sellerx.ai.dto.CreateKnowledgeRequest;
import com.ecommerce.sellerx.ai.dto.KnowledgeBaseDto;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeBaseService {

    private final StoreKnowledgeBaseRepository repository;
    private final StoreRepository storeRepository;

    public List<KnowledgeBaseDto> getByStoreId(UUID storeId) {
        return repository.findByStoreIdOrderByPriorityDesc(storeId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public List<KnowledgeBaseDto> getByStoreIdAndCategory(UUID storeId, String category) {
        return repository.findByStoreIdAndCategoryAndIsActiveTrueOrderByPriorityDesc(storeId, category)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public KnowledgeBaseDto create(UUID storeId, CreateKnowledgeRequest request) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Mağaza bulunamadı: " + storeId));

        StoreKnowledgeBase knowledge = StoreKnowledgeBase.builder()
                .store(store)
                .category(request.getCategory())
                .title(request.getTitle())
                .content(request.getContent())
                .keywords(request.getKeywords())
                .priority(request.getPriority() != null ? request.getPriority() : 0)
                .isActive(true)
                .build();

        knowledge = repository.save(knowledge);
        log.info("Created knowledge base item {} for store {}", knowledge.getId(), storeId);

        return toDto(knowledge);
    }

    @Transactional
    public KnowledgeBaseDto update(UUID id, CreateKnowledgeRequest request) {
        StoreKnowledgeBase knowledge = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bilgi bulunamadı: " + id));

        knowledge.setCategory(request.getCategory());
        knowledge.setTitle(request.getTitle());
        knowledge.setContent(request.getContent());
        knowledge.setKeywords(request.getKeywords());
        if (request.getPriority() != null) {
            knowledge.setPriority(request.getPriority());
        }

        knowledge = repository.save(knowledge);
        log.info("Updated knowledge base item {}", id);

        return toDto(knowledge);
    }

    @Transactional
    public void delete(UUID id) {
        repository.deleteById(id);
        log.info("Deleted knowledge base item {}", id);
    }

    @Transactional
    public void toggleActive(UUID id, boolean active) {
        StoreKnowledgeBase knowledge = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bilgi bulunamadı: " + id));
        knowledge.setIsActive(active);
        repository.save(knowledge);
    }

    private KnowledgeBaseDto toDto(StoreKnowledgeBase entity) {
        return KnowledgeBaseDto.builder()
                .id(entity.getId())
                .storeId(entity.getStore().getId())
                .category(entity.getCategory())
                .title(entity.getTitle())
                .content(entity.getContent())
                .keywords(entity.getKeywords())
                .isActive(entity.getIsActive())
                .priority(entity.getPriority())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
