package com.ecommerce.sellerx.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when Trendyol API operations fail.
 */
@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class TrendyolApiException extends RuntimeException {

    public TrendyolApiException(String message) {
        super(message);
    }

    public TrendyolApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public static TrendyolApiException orderFetchFailed(String storeId, Throwable cause) {
        return new TrendyolApiException("Failed to fetch orders from Trendyol for store: " + storeId, cause);
    }
}
