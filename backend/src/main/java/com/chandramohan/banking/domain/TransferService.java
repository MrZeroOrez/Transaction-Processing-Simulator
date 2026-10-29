package com.chandramohan.banking.domain;

import static com.chandramohan.banking.domain.Support.*;

import java.math.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferService {
  public record Account(String id, String name, BigDecimal balance, String createdAt) {}

  public record Transfer(
      String id,
      String sourceId,
      String targetId,
      BigDecimal amount,
      String note,
      String createdAt) {}

  public record Entry(
      String id, String transferId, String kind, BigDecimal amount, String createdAt) {}

  public record Reconciliation(
      String accountId,
      BigDecimal balance,
      BigDecimal ledgerTotal,
      boolean matches,
      boolean transferPairsBalanced) {}

  public record Event(String action, String resourceId, String createdAt) {}

  private final JdbcTemplate db;

  public TransferService(JdbcTemplate db) {
    this.db = db;
  }

  public List<Account> accounts(String owner) {
    return db.query(
        "SELECT * FROM accounts WHERE owner_id=? ORDER BY created_at,id",
        (r, n) ->
            new Account(
                r.getString("id"),
                r.getString("name"),
                r.getBigDecimal("balance"),
                r.getString("created_at")),
        owner);
  }

  private Account owned(String owner, String id, boolean lock) {
    return db
        .query(
            "SELECT * FROM accounts WHERE owner_id=? AND id=?" + (lock ? " FOR UPDATE" : ""),
            (r, n) ->
                new Account(
                    r.getString("id"),
                    r.getString("name"),
                    r.getBigDecimal("balance"),
                    r.getString("created_at")),
            owner,
            id)
        .stream()
        .findFirst()
        .orElseThrow(Support::missing);
  }

  @Transactional
  public Account create(String owner, String name) {
    String id = id(), time = now();
    db.update(
        "INSERT INTO accounts(id,owner_id,name,balance,created_at) VALUES(?,?,?,10000,?)",
        id,
        owner,
        name.strip(),
        time);
    db.update(
        "INSERT INTO ledger(id,account_id,transfer_id,kind,amount,created_at)"
            + " VALUES(?,?,NULL,'DEMO_OPENING',10000,?)",
        id(),
        id,
        time);
    audit(owner, "ACCOUNT_CREATED", id);
    return owned(owner, id, false);
  }

  private void audit(String owner, String action, String resource) {
    db.update(
        "INSERT INTO audit(id,owner_id,action,resource_id,created_at) VALUES(?,?,?,?,?)",
        id(),
        owner,
        action,
        resource,
        now());
  }

  @Transactional
  public Transfer transfer(
      String owner, String source, String target, BigDecimal amount, String note, String key) {
    if (key == null || !key.matches("[A-Za-z0-9_-]{8,80}"))
      throw bad("Use an 8-80 character request key");
    if (source == null || target == null || source.equals(target))
      throw bad("Select two different account IDs");
    if (amount == null
        || amount.scale() > 2
        || amount.compareTo(new BigDecimal("0.01")) < 0
        || amount.compareTo(new BigDecimal("1000000.00")) > 0)
      throw bad("Amount must be 0.01-1000000.00 with at most two decimal places");
    if (note == null) note = "";
    if (note.length() > 140) throw bad("Note must be at most 140 characters");
    amount = money(amount);
    db.queryForObject("SELECT id FROM app_users WHERE id=? FOR UPDATE", String.class, owner);
    var prior =
        db.query(
            "SELECT * FROM transfers WHERE owner_id=? AND request_key=?",
            (r, n) ->
                new Transfer(
                    r.getString("id"),
                    r.getString("source_id"),
                    r.getString("target_id"),
                    r.getBigDecimal("amount"),
                    r.getString("note"),
                    r.getString("created_at")),
            owner,
            key);
    if (!prior.isEmpty()) {
      var t = prior.get(0);
      if (!t.sourceId().equals(source)
          || !t.targetId().equals(target)
          || t.amount().compareTo(amount) != 0
          || !t.note().equals(note))
        throw conflict("Request key already used for another transfer");
      return t;
    }
    // Check source ownership before revealing anything about the destination.
    owned(owner, source, false);
    // Every transfer locks both accounts in the same sorted order to avoid A->B / B->A deadlocks.
    var ids = new ArrayList<>(List.of(source, target));
    Collections.sort(ids);
    for (String account : ids) {
      var rows = db.queryForList("SELECT id FROM accounts WHERE id=? FOR UPDATE", account);
      if (rows.isEmpty()) throw missing();
    }
    var from = owned(owner, source, false);
    if (from.balance().compareTo(amount) < 0) throw conflict("Insufficient funds");
    var t = new Transfer(id(), source, target, amount, note, now());
    db.update("UPDATE accounts SET balance=balance-? WHERE id=?", amount, source);
    db.update("UPDATE accounts SET balance=balance+? WHERE id=?", amount, target);
    db.update(
        "INSERT INTO transfers(id,owner_id,source_id,target_id,amount,request_key,note,created_at)"
            + " VALUES(?,?,?,?,?,?,?,?)",
        t.id(),
        owner,
        source,
        target,
        amount,
        key,
        note,
        t.createdAt());
    db.update(
        "INSERT INTO ledger(id,account_id,transfer_id,kind,amount,created_at)"
            + " VALUES(?,?,?,'DEBIT',?,?)",
        id(),
        source,
        t.id(),
        amount.negate(),
        t.createdAt());
    db.update(
        "INSERT INTO ledger(id,account_id,transfer_id,kind,amount,created_at)"
            + " VALUES(?,?,?,'CREDIT',?,?)",
        id(),
        target,
        t.id(),
        amount,
        t.createdAt());
    audit(owner, "TRANSFER_POSTED", t.id());
    db.update("INSERT INTO event_outbox(id,resource_id,event_type,sent) VALUES(?,?,?,FALSE)",id(),t.id(),"TRANSFER_POSTED");
    return t;
  }

  public List<Transfer> history(String owner) {
    return db.query(
        "SELECT t.* FROM transfers t WHERE source_id IN(SELECT id FROM accounts WHERE owner_id=?)"
            + " OR target_id IN(SELECT id FROM accounts WHERE owner_id=?) ORDER BY created_at"
            + " DESC,id",
        (r, n) ->
            new Transfer(
                r.getString("id"),
                r.getString("source_id"),
                r.getString("target_id"),
                r.getBigDecimal("amount"),
                r.getString("note"),
                r.getString("created_at")),
        owner,
        owner);
  }

  public List<Entry> ledger(String owner, String account) {
    owned(owner, account, false);
    return db.query(
        "SELECT * FROM ledger WHERE account_id=? ORDER BY created_at DESC,id",
        (r, n) ->
            new Entry(
                r.getString("id"),
                r.getString("transfer_id"),
                r.getString("kind"),
                r.getBigDecimal("amount"),
                r.getString("created_at")),
        account);
  }

  @Transactional
  public Reconciliation reconcile(String owner, String account) {
    var a = owned(owner, account, true);
    var total =
        db.queryForObject(
            "SELECT COALESCE(SUM(amount),0) FROM ledger WHERE account_id=?",
            BigDecimal.class,
            account);
    int broken =
        db.queryForObject(
            "SELECT COUNT(*) FROM (SELECT t.id FROM transfers t LEFT JOIN ledger l ON"
                + " l.transfer_id=t.id WHERE t.source_id=? OR t.target_id=? GROUP BY t.id HAVING"
                + " COUNT(l.id)<>2 OR COALESCE(SUM(l.amount),0)<>0) broken",
            Integer.class,
            account,
            account);
    return new Reconciliation(
        account, a.balance(), money(total), a.balance().compareTo(total) == 0, broken == 0);
  }

  public List<Event> audit(String owner) {
    return db.query(
        "SELECT * FROM audit WHERE owner_id=? ORDER BY created_at DESC,id",
        (r, n) ->
            new Event(r.getString("action"), r.getString("resource_id"), r.getString("created_at")),
        owner);
  }
}
