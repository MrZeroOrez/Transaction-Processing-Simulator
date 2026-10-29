package com.chandramohan.banking.platform;

import java.util.Map;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiErrors {
  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<?> status(ResponseStatusException e) {
    return ResponseEntity.status(e.getStatusCode())
        .body(Map.of("error", e.getReason() == null ? "Request failed" : e.getReason()));
  }

  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class
  })
  ResponseEntity<?> invalid(Exception e) {
    return ResponseEntity.badRequest()
        .body(Map.of("error", "Invalid request. Check all fields, limits, and JSON types."));
  }

  @ExceptionHandler(ConcurrencyFailureException.class)
  ResponseEntity<?> busy(Exception e) {
    return ResponseEntity.status(409)
        .body(Map.of("error", "Concurrent update. Retry with the SAME request key."));
  }
}
