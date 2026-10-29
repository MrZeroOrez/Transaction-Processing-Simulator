package com.chandramohan.banking;

import static org.junit.jupiter.api.Assertions.*;

import com.chandramohan.banking.auth.*;
import java.math.*;
import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.regex.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

class HttpSupport {
  @LocalServerPort int port;
  @Autowired LabStore users;
  @Autowired JdbcTemplate db;
  final HttpClient client = HttpClient.newHttpClient();

  HttpResponse<String> call(String method, String path, String body, String token, String key)
      throws Exception {
    var b =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1" + path))
            .header("Content-Type", "application/json");
    if (token != null) b.header("Authorization", "Bearer " + token);
    if (key != null) b.header("Idempotency-Key", key);
    return client.send(
        b.method(
                method,
                body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(body))
            .build(),
        HttpResponse.BodyHandlers.ofString());
  }

  String field(String json, String key) {
    var m = Pattern.compile("\"" + key + "\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
    assertTrue(m.find(), json);
    return m.group(1);
  }

  String register(String email) throws Exception {
    var r =
        call(
            "POST",
            "/auth/register",
            "{\"email\":\"" + email + "\",\"password\":\"Test-password-123\"}",
            null,
            null);
    assertEquals(201, r.statusCode(), r.body());
    return field(r.body(), "accessToken");
  }

  String email() {
    return UUID.randomUUID() + "@example.test";
  }

  String owner() {
    var e = email();
    return users.register(e, "unused-test-hash").orElseThrow().id().toString();
  }

  void eq(String expected, BigDecimal actual) {
    assertEquals(0, new BigDecimal(expected).compareTo(actual));
  }
}
