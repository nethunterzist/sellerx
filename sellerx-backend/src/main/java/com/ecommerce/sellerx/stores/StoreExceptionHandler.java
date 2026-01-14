package com.ecommerce.sellerx.stores;

import com.ecommerce.sellerx.common.ErrorDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class StoreExceptionHandler {
    @ExceptionHandler(StoreNotFoundException.class)
    public ResponseEntity<ErrorDto> handleStoreNotFound() {
        return ResponseEntity.status(404).body(new ErrorDto("Store not found"));
    }
}
