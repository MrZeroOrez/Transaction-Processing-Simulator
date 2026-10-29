package com.chandramohan.banking.auth;

import java.util.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class LabStore {
  public record User(UUID id, String email, String passwordHash) {}

  private final JdbcTemplate db;

  public LabStore(JdbcTemplate db) {
    this.db = db;
  }

  public Optional<User> find(String email) {
    return db
        .query(
            "SELECT * FROM app_users WHERE email=?",
            (r, n) ->
                new User(
                    UUID.fromString(r.getString("id")),
                    r.getString("email"),
                    r.getString("password_hash")),
            email.strip().toLowerCase(Locale.ROOT))
        .stream()
        .findFirst();
  }

  public boolean hasUser(String id) {
    return id != null
        && db.queryForObject("SELECT COUNT(*) FROM app_users WHERE id=?", Integer.class, id) > 0;
  }

  public Optional<User> register(String email, String hash) {
    var u = new User(UUID.randomUUID(), email.strip().toLowerCase(Locale.ROOT), hash);
    try {
      db.update(
          "INSERT INTO app_users(id,email,password_hash) VALUES(?,?,?)",
          u.id().toString(),
          u.email(),
          u.passwordHash());
      return Optional.of(u);
    } catch (DuplicateKeyException e) {
      return Optional.empty();
    }
  }
}
