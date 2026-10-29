package com.chandramohan.banking.domain;

import java.math.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class Support {
  private Support() {}

  public static ResponseStatusException bad(String m) {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST, m);
  }

  public static ResponseStatusException conflict(String m) {
    return new ResponseStatusException(HttpStatus.CONFLICT, m);
  }

  public static ResponseStatusException missing() {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found");
  }

  public static String id() {
    return java.util.UUID.randomUUID().toString();
  }

  public static String now() {
    return java.time.Instant.now().toString();
  }

  public static BigDecimal money(BigDecimal n) {
    return n.setScale(2, RoundingMode.HALF_UP);
  }

  public static String csv(String s) {
    return "\"" + s.replace("\"", "\"\"") + "\"";
  }
}
