package com.ecommerce.sellerx.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a store is not properly configured for the requested operation.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidStoreConfigurationException extends RuntimeException {

    public InvalidStoreConfigurationException(String message) {
        super(message);
    }

    public static InvalidStoreConfigurationException notTrendyolStore(String storeId) {
        return new InvalidStoreConfigurationException("Store is not a Trendyol store: " + storeId);
    }

    public static InvalidStoreConfigurationException missingCredentials(String storeId) {
        return new InvalidStoreConfigurationException("Trendyol credentials not configured for store: " + storeId);
    }
}
