package com.ecommerce.sellerx.common.encryption;

import com.ecommerce.sellerx.stores.MarketplaceCredentials;
import com.ecommerce.sellerx.stores.Store;
import com.ecommerce.sellerx.stores.TrendyolCredentials;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * JPA Entity Listener that handles transparent encryption/decryption of credentials.
 * - Encrypts credentials before persisting to database
 * - Decrypts credentials after loading from database
 */
@Component
@Slf4j
public class CredentialsEntityListener {

    private static EncryptionService encryptionService;

    @Autowired
    public void setEncryptionService(EncryptionService service) {
        encryptionService = service;
    }

    @PrePersist
    @PreUpdate
    public void beforeSave(Object entity) {
        if (entity instanceof Store store && encryptionService != null && encryptionService.isEnabled()) {
            MarketplaceCredentials credentials = store.getCredentials();
            if (credentials instanceof TrendyolCredentials tc) {
                // Only encrypt if not already encrypted
                if (tc.getApiKey() != null && !encryptionService.isEncrypted(tc.getApiKey())) {
                    store.setCredentials(TrendyolCredentials.builder()
                            .apiKey(encryptionService.encrypt(tc.getApiKey()))
                            .apiSecret(encryptionService.encrypt(tc.getApiSecret()))
                            .sellerId(tc.getSellerId())
                            .integrationCode(tc.getIntegrationCode())
                            .Token(tc.getToken())
                            .build());
                    log.debug("Encrypted credentials for store: {}", store.getId());
                }
            }
        }
    }

    @PostLoad
    public void afterLoad(Object entity) {
        if (entity instanceof Store store && encryptionService != null && encryptionService.isEnabled()) {
            MarketplaceCredentials credentials = store.getCredentials();
            if (credentials instanceof TrendyolCredentials tc) {
                // Only decrypt if encrypted
                if (tc.getApiKey() != null && encryptionService.isEncrypted(tc.getApiKey())) {
                    store.setCredentials(TrendyolCredentials.builder()
                            .apiKey(encryptionService.decrypt(tc.getApiKey()))
                            .apiSecret(encryptionService.decrypt(tc.getApiSecret()))
                            .sellerId(tc.getSellerId())
                            .integrationCode(tc.getIntegrationCode())
                            .Token(tc.getToken())
                            .build());
                    log.debug("Decrypted credentials for store: {}", store.getId());
                }
            }
        }
    }
}
