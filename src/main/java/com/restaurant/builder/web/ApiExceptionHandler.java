package com.restaurant.builder.web;

import com.restaurant.builder.application.OrderNotFoundException;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import org.springframework.validation.FieldError;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public final class ApiExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> invalid(IllegalArgumentException e) { return error(400, e.getMessage()); }
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> conflict(IllegalStateException e) { return error(409, e.getMessage()); }
    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<Map<String, String>> missing(OrderNotFoundException e) { return error(404, e.getMessage()); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> validation(MethodArgumentNotValidException e) {
        List<String> messages = new ArrayList<>();
        for (FieldError field : e.getBindingResult().getFieldErrors()) {
            messages.add(field.getField() + ": " + field.getDefaultMessage());
        }
        Collections.sort(messages);
        String message = "Invalid order details";
        if (!messages.isEmpty()) {
            message = messages.get(0);
        }
        return error(400, message);
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Map<String, String>> unreadable(Exception e) {
        return error(400, "Invalid request: check JSON, quantities, IDs, and fulfillment (DINE_IN or TAKEAWAY)");
    }
    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Map<String, String>> unsupported(Exception e) { return error(415, "Use Content-Type: application/json"); }
    private ResponseEntity<Map<String, String>> error(int status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message == null ? "Invalid request" : message));
    }
}
