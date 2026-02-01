package com.ecommerce.sellerx.alerts;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when alert rule validation fails.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class AlertRuleValidationException extends RuntimeException {

    public AlertRuleValidationException(String message) {
        super(message);
    }
}
