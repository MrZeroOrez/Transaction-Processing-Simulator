package com.chandramohan.banking.domain;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class TransferController {
  private final TransferService service;

  public TransferController(TransferService service) {
    this.service = service;
  }

  public record Name(@NotBlank @Size(max = 80) String name) {}

  public record Order(
      @NotNull UUID sourceId,
      @NotNull UUID targetId,
      @NotNull @DecimalMin("0.01") @DecimalMax("1000000") @Digits(integer = 7, fraction = 2)
          BigDecimal amount,
      @Size(max = 140) String note) {}

  @GetMapping("/accounts")
  public Object accounts(@AuthenticationPrincipal Jwt j) {
    return service.accounts(j.getSubject());
  }

  @PostMapping("/accounts")
  @ResponseStatus(HttpStatus.CREATED)
  public Object create(@AuthenticationPrincipal Jwt j, @Valid @RequestBody Name n) {
    return service.create(j.getSubject(), n.name());
  }

  @PostMapping("/transfers")
  @ResponseStatus(HttpStatus.CREATED)
  public Object transfer(
      @AuthenticationPrincipal Jwt j,
      @Valid @RequestBody Order o,
      @RequestHeader("Idempotency-Key") String key) {
    return service.transfer(
        j.getSubject(),
        o.sourceId().toString(),
        o.targetId().toString(),
        o.amount(),
        o.note(),
        key);
  }

  @GetMapping("/transfers")
  public Object history(@AuthenticationPrincipal Jwt j) {
    return service.history(j.getSubject());
  }

  @GetMapping("/accounts/{id}/ledger")
  public Object ledger(@AuthenticationPrincipal Jwt j, @PathVariable UUID id) {
    return service.ledger(j.getSubject(), id.toString());
  }

  @GetMapping("/accounts/{id}/reconciliation")
  public Object reconcile(@AuthenticationPrincipal Jwt j, @PathVariable UUID id) {
    return service.reconcile(j.getSubject(), id.toString());
  }

  @GetMapping("/audit")
  public Object audit(@AuthenticationPrincipal Jwt j) {
    return service.audit(j.getSubject());
  }

  @GetMapping(value = "/transfers/export", produces = "text/csv")
  public ResponseEntity<String> export(@AuthenticationPrincipal Jwt j) {
    var out = new StringBuilder("id,sourceId,targetId,amount,note,createdAt\n");
    for (var t : service.history(j.getSubject())) {
      String note = t.note();
      if (!note.stripLeading().isEmpty() && "=+-@".indexOf(note.stripLeading().charAt(0)) >= 0
          || note.startsWith("\t")
          || note.startsWith("\r")
          || note.startsWith("\n")) note = "'" + note;
      out.append(
              String.join(
                  ",",
                  t.id(),
                  t.sourceId(),
                  t.targetId(),
                  t.amount().toString(),
                  Support.csv(note),
                  t.createdAt()))
          .append("\n");
    }
    return ResponseEntity.ok()
        .header("Content-Disposition", "attachment; filename=transfers.csv")
        .body(out.toString());
  }
}
