package com.ecommerce.sellerx.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a requested resource cannot be found.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceType, String identifier) {
        super(String.format("%s not found: %s", resourceType, identifier));
    }

    public static ResourceNotFoundException stockInfo(String productId) {
        return new ResourceNotFoundException("Stock info", productId);
    }

    public static ResourceNotFoundException stockInfoForDate(String date) {
        return new ResourceNotFoundException("No stock info found for date: " + date);
    }
}
