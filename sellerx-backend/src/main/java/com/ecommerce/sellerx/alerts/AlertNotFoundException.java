package com.ecommerce.sellerx.alerts;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when an alert is not found.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class AlertNotFoundException extends RuntimeException {

    public AlertNotFoundException(String message) {
        super(message);
    }
}
