package com.ecommerce.sellerx.ai;

import com.ecommerce.sellerx.ai.dto.AiSettingsDto;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiSettingsService {

    private final StoreAiSettingsRepository repository;
    private final StoreRepository storeRepository;

    public AiSettingsDto getByStoreId(UUID storeId) {
        StoreAiSettings settings = repository.findByStoreId(storeId)
                .orElseGet(() -> createDefaultSettings(storeId));
        return toDto(settings);
    }

    @Transactional
    public AiSettingsDto update(UUID storeId, AiSettingsDto dto) {
        StoreAiSettings settings = repository.findByStoreId(storeId)
                .orElseGet(() -> createDefaultSettings(storeId));

        if (dto.getAiEnabled() != null) settings.setAiEnabled(dto.getAiEnabled());
        if (dto.getAutoAnswer() != null) settings.setAutoAnswer(dto.getAutoAnswer());
        if (dto.getTone() != null) settings.setTone(dto.getTone());
        if (dto.getLanguage() != null) settings.setLanguage(dto.getLanguage());
        if (dto.getMaxAnswerLength() != null) settings.setMaxAnswerLength(dto.getMaxAnswerLength());
        if (dto.getIncludeGreeting() != null) settings.setIncludeGreeting(dto.getIncludeGreeting());
        if (dto.getIncludeSignature() != null) settings.setIncludeSignature(dto.getIncludeSignature());
        if (dto.getSignatureText() != null) settings.setSignatureText(dto.getSignatureText());
        if (dto.getConfidenceThreshold() != null) settings.setConfidenceThreshold(dto.getConfidenceThreshold());

        settings = repository.save(settings);
        log.info("Updated AI settings for store {}", storeId);

        return toDto(settings);
    }

    private StoreAiSettings createDefaultSettings(UUID storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Mağaza bulunamadı: " + storeId));

        StoreAiSettings settings = StoreAiSettings.builder()
                .store(store)
                .aiEnabled(false)
                .autoAnswer(false)
                .tone("professional")
                .language("tr")
                .maxAnswerLength(500)
                .includeGreeting(true)
                .includeSignature(true)
                .confidenceThreshold(new BigDecimal("0.80"))
                .build();

        return repository.save(settings);
    }

    private AiSettingsDto toDto(StoreAiSettings entity) {
        return AiSettingsDto.builder()
                .id(entity.getId())
                .storeId(entity.getStore().getId())
                .aiEnabled(entity.getAiEnabled())
                .autoAnswer(entity.getAutoAnswer())
                .tone(entity.getTone())
                .language(entity.getLanguage())
                .maxAnswerLength(entity.getMaxAnswerLength())
                .includeGreeting(entity.getIncludeGreeting())
                .includeSignature(entity.getIncludeSignature())
                .signatureText(entity.getSignatureText())
                .confidenceThreshold(entity.getConfidenceThreshold())
                .build();
    }
}
