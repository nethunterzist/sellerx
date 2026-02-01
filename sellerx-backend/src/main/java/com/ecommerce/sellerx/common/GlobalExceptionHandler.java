package com.ecommerce.sellerx.common;

import com.ecommerce.sellerx.common.exception.UnauthorizedAccessException;
import com.ecommerce.sellerx.stores.StoreNotFoundException;
import com.ecommerce.sellerx.users.UserNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorDto> handleUnreadableMessage(HttpMessageNotReadableException ex, WebRequest request) {
        log.warn("[EXCEPTION] HttpMessageNotReadableException on {}: {}",
            request.getDescription(false),
            ex.getMessage());
        return ResponseEntity.badRequest().body(
                new ErrorDto("Invalid request body")
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(
            MethodArgumentNotValidException exception,
            WebRequest request
    ) {
        var errors = new HashMap<String, String>();

        exception.getBindingResult().getFieldErrors().forEach(error -> {
            errors.put(error.getField(), error.getDefaultMessage());
        });

        log.warn("[EXCEPTION] Validation failed on {}: {}",
            request.getDescription(false),
            errors);

        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorDto> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        log.warn("[EXCEPTION] Access denied on {}: {}",
            request.getDescription(false),
            ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(new ErrorDto("Access denied"));
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<ErrorDto> handleUnauthorizedAccess(UnauthorizedAccessException ex, WebRequest request) {
        log.warn("[EXCEPTION] Unauthorized access on {}: {}",
            request.getDescription(false),
            ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(new ErrorDto(ex.getMessage()));
    }

    @ExceptionHandler(StoreNotFoundException.class)
    public ResponseEntity<ErrorDto> handleStoreNotFound(StoreNotFoundException ex, WebRequest request) {
        log.warn("[EXCEPTION] Store not found on {}: {}",
            request.getDescription(false),
            ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ErrorDto(ex.getMessage()));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorDto> handleUserNotFound(UserNotFoundException ex, WebRequest request) {
        log.warn("[EXCEPTION] User not found on {}: {}",
            request.getDescription(false),
            ex.getMessage());
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ErrorDto("User not found"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDto> handleGenericException(Exception ex, WebRequest request) {
        log.error("[EXCEPTION] Unhandled exception on {}: {}",
            request.getDescription(false),
            ex.getMessage(),
            ex);  // Full stack trace logged

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorDto("An unexpected error occurred"));
    }
}
