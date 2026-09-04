package com.api.batterymantra.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import org.springframework.security.access.AccessDeniedException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex,
            WebRequest request) {
        ErrorResponse error = new ErrorResponse(
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value(),
                LocalDateTime.now(),
                request.getDescription(false).replace("uri=", "")
        );
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex,
            WebRequest request) {
        
        String rootMsg = ex.getMostSpecificCause() != null 
                ? ex.getMostSpecificCause().getMessage() 
                : (ex.getMessage() != null ? ex.getMessage() : "");
        String lowerMsg = rootMsg.toLowerCase();
        
        String message;
        
        if (lowerMsg.contains("value too long") || lowerMsg.contains("too long") || lowerMsg.contains("string data, right truncation")) {
            // Extract column name if possible
            String col = "";
            try {
                int idx = lowerMsg.indexOf("\"");
                if (idx >= 0) {
                    int end = lowerMsg.indexOf("\"", idx + 1);
                    if (end > idx) col = " (" + rootMsg.substring(idx + 1, end) + ")";
                }
            } catch (Exception ignored) {}
            message = "The text you entered is too long for the field" + col + ". Please shorten it and try again.";
        } else if (lowerMsg.contains("duplicate") || lowerMsg.contains("unique") || lowerMsg.contains("already exists") || lowerMsg.contains("unique_violation")) {
            // Try to extract which field caused the duplicate
            String field = "";
            try {
                if (lowerMsg.contains("category_name")) field = " (Category Name)";
                else if (lowerMsg.contains("brand_name")) field = " (Brand Name)";
                else if (lowerMsg.contains("seo_slug")) field = " (SEO Slug)";
                else if (lowerMsg.contains("coupon_code")) field = " (Coupon Code)";
            } catch (Exception ignored) {}
            message = "A record with this value already exists" + field + ". Please use a unique value.";
        } else if (lowerMsg.contains("foreign key") || lowerMsg.contains("referenced") || lowerMsg.contains("constraint")) {
            message = "This record cannot be modified or deleted because it is linked to other data. Please remove those references first.";
        } else if (lowerMsg.contains("not-null") || lowerMsg.contains("null value")) {
            message = "A required field is missing. Please fill in all required fields and try again.";
        } else {
            message = "Could not save the data. Please check your input and try again.";
        }
        
        ErrorResponse error = new ErrorResponse(
                message,
                HttpStatus.CONFLICT.value(),
                LocalDateTime.now(),
                request.getDescription(false).replace("uri=", "")
        );
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex,
            WebRequest request) {
        ErrorResponse error = new ErrorResponse(
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value(),
                LocalDateTime.now(),
                request.getDescription(false).replace("uri=", "")
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFoundException(
            UsernameNotFoundException ex,
            WebRequest request) {
        ErrorResponse error = new ErrorResponse(
                "User not found: " + ex.getMessage(),
                HttpStatus.NOT_FOUND.value(),
                LocalDateTime.now(),
                request.getDescription(false).replace("uri=", "")
        );
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(
            BadCredentialsException ex,
            WebRequest request) {
        ErrorResponse error = new ErrorResponse(
                "Invalid username or password",
                HttpStatus.UNAUTHORIZED.value(),
                LocalDateTime.now(),
                request.getDescription(false).replace("uri=", "")
        );
        return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex,
            WebRequest request) {
        ErrorResponse error = new ErrorResponse(
                "Access is denied: " + ex.getMessage(),
                HttpStatus.FORBIDDEN.value(),
                LocalDateTime.now(),
                request.getDescription(false).replace("uri=", "")
        );
        return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(
            org.springframework.web.server.ResponseStatusException ex,
            WebRequest request) {
        ErrorResponse error = new ErrorResponse(
                ex.getReason() != null ? ex.getReason() : ex.getMessage(),
                ex.getStatusCode().value(),
                LocalDateTime.now(),
                request.getDescription(false).replace("uri=", "")
        );
        return new ResponseEntity<>(error, ex.getStatusCode());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(
            Exception ex,
            WebRequest request) {
        ex.printStackTrace(); // Log the actual exception trace for debugging
        ErrorResponse error = new ErrorResponse(
                "Something went wrong. Please try again later or contact support if the issue persists.",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                LocalDateTime.now(),
                request.getDescription(false).replace("uri=", "")
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}