package com.example.studioapp_api.exception;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;     // <<<--- ADD THIS IMPORT
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;  // <<<--- ADD THIS IMPORT
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.validation.FieldError;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.HashMap;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // Handler for when an entity is not found (e.g., findById fails)
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Object> handleEntityNotFoundException(
            EntityNotFoundException ex, WebRequest request) {
        
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now());
        body.put("status", HttpStatus.NOT_FOUND.value());
        body.put("error", HttpStatus.NOT_FOUND.getReasonPhrase());
        body.put("message", ex.getMessage());
        body.put("path", request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    // Handler for invalid arguments
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", HttpStatus.BAD_REQUEST.getReasonPhrase());
        body.put("message", ex.getMessage());
        body.put("path", request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // Handler for database integrity violations
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex, WebRequest request) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now());
        body.put("status", HttpStatus.CONFLICT.value());
        body.put("error", HttpStatus.CONFLICT.getReasonPhrase());
        String message = "Database integrity conflict. This could be due to a duplicate entry " +
                         "or an attempt to delete data that is still referenced.";
        if (ex.getMostSpecificCause() != null && ex.getMostSpecificCause().getMessage() != null) {
             message = "Database integrity conflict: " + ex.getMostSpecificCause().getMessage();
        }
        body.put("message", message);
        body.put("path", request.getDescription(false).replace("uri=", ""));

        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }
    
    // A generic fallback handler for other unexpected exceptions NOT ALREADY HANDLED BY ResponseEntityExceptionHandler
    // Ensure this is distinct from what ResponseEntityExceptionHandler already covers, or override those methods too.
    @ExceptionHandler(Exception.class) // This will catch any Exception not handled by more specific handlers above or in the base class
    public ResponseEntity<Object> handleGenericException(
            Exception ex, WebRequest request) {
        
        // Check if this exception is already handled by the base class to avoid ambiguity if not careful
        // For instance, if we didn't override handleMethodArgumentNotValid, and it fell through to here,
        // this generic handler might still be invoked.
        // However, since we WILL override handleMethodArgumentNotValid, this is for truly *other* exceptions.

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
        body.put("message", "An unexpected error occurred: " + ex.getMessage());
        body.put("path", request.getDescription(false).replace("uri=", ""));

        // logger.error("Unhandled exception:", ex); 

        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // CORRECTED METHOD for handling @Valid validation errors
    @Override // <<<--- ADD @Override ANNOTATION
    protected ResponseEntity<Object> handleMethodArgumentNotValid( // <<<--- CHANGE METHOD SIGNATURE
            MethodArgumentNotValidException ex, 
            HttpHeaders headers,         
            HttpStatusCode status,        
            WebRequest request) {
        
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now());
        body.put("status", status.value()); // Use the status passed in (will be BAD_REQUEST)
        body.put("error", HttpStatus.valueOf(status.value()).getReasonPhrase()); 
        body.put("path", request.getDescription(false).replace("uri=", ""));

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });
        body.put("message", "Validation failed for request body.");
        body.put("fieldErrors", fieldErrors); 

        return new ResponseEntity<>(body, headers, status); // Use all three parameters for the response
    }
}